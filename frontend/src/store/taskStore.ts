import { create } from "zustand";
import type { Task, TaskStatus } from "@/types";
import { taskApi } from "@/lib/api";
import toast from "react-hot-toast";

interface TaskState {
  tasks: Record<TaskStatus, Task[]>;
  currentTask: Task | null;
  isLoading: boolean;
  fetchTasks: (projectId: string) => Promise<void>;
  setCurrentTask: (task: Task | null) => void;
  createTask: (projectId: string, data: Partial<Task>) => Promise<void>;
  updateTask: (
    projectId: string,
    taskId: string,
    data: Partial<Task>
  ) => Promise<void>;
  updateTaskPosition: (
    projectId: string,
    taskId: string,
    status: TaskStatus,
    position: number
  ) => Promise<void>;
  deleteTask: (projectId: string, taskId: string) => Promise<void>;
  moveTask: (
    taskId: string,
    fromStatus: TaskStatus,
    toStatus: TaskStatus,
    newPosition: number
  ) => void;
}

const initialTasks: Record<TaskStatus, Task[]> = {
  TODO: [],
  IN_PROGRESS: [],
  REVIEW: [],
  DONE: [],
};

export const useTaskStore = create<TaskState>((set) => ({
  tasks: initialTasks,
  currentTask: null,
  isLoading: false,

  fetchTasks: async (projectId: string) => {
    set({ isLoading: true });
    try {
      const response = await taskApi.getAll(projectId);
      if (response.data.success && response.data.data) {
        set({ tasks: response.data.data, isLoading: false });
      }
    } catch (error) {
      set({ isLoading: false });
      throw error;
    }
  },

  setCurrentTask: (task: Task | null) => {
    set({ currentTask: task });
  },

  createTask: async (projectId: string, data: Partial<Task>) => {
    try {
      const response = await taskApi.create(projectId, data);
      if (response.data.success && response.data.data) {
        const newTask = response.data.data as Task;
        set((state) => ({
          tasks: {
            ...state.tasks,
            [newTask.status]: [...state.tasks[newTask.status], newTask],
          },
        }));
        toast.success("Task created successfully");
      }
    } catch (error) {
      throw error;
    }
  },

  updateTask: async (
    projectId: string,
    taskId: string,
    data: Partial<Task>
  ) => {
    try {
      const response = await taskApi.update(projectId, taskId, data);
      if (response.data.success && response.data.data) {
        const updatedTask = response.data.data as Task;
        
        // Remove from old status and add to new status if status changed
        set((state) => {
          const newTasks = { ...state.tasks };
          
          // Find and remove task from all columns
          (Object.keys(newTasks) as TaskStatus[]).forEach((status) => {
            newTasks[status] = newTasks[status].filter((t) => t.id !== taskId);
          });
          
          // Add to new status column
          newTasks[updatedTask.status] = [...newTasks[updatedTask.status], updatedTask];
          
          return {
            tasks: newTasks,
            currentTask:
              state.currentTask?.id === taskId ? updatedTask : state.currentTask,
          };
        });
      }
    } catch (error) {
      throw error;
    }
  },

  updateTaskPosition: async (
    projectId: string,
    taskId: string,
    status: TaskStatus,
    position: number
  ) => {
    try {
      await taskApi.updatePosition(projectId, taskId, { status, position });
    } catch (error) {
      // Caller is expected to refetch server state on error (see projects/[projectId]/page.tsx).
      throw error;
    }
  },

  deleteTask: async (projectId: string, taskId: string) => {
    try {
      await taskApi.delete(projectId, taskId);
      set((state) => ({
        tasks: {
          ...state.tasks,
          TODO: state.tasks.TODO.filter((t) => t.id !== taskId),
          IN_PROGRESS: state.tasks.IN_PROGRESS.filter((t) => t.id !== taskId),
          REVIEW: state.tasks.REVIEW.filter((t) => t.id !== taskId),
          DONE: state.tasks.DONE.filter((t) => t.id !== taskId),
        },
        currentTask:
          state.currentTask?.id === taskId ? null : state.currentTask,
      }));
      toast.success("Task deleted successfully");
    } catch (error) {
      throw error;
    }
  },

  moveTask: (
    taskId: string,
    fromStatus: TaskStatus,
    toStatus: TaskStatus,
    newPosition: number
  ) => {
    set((state) => {
      const newTasks = { ...state.tasks };
      const task = newTasks[fromStatus].find((t) => t.id === taskId);
      
      if (!task) return state;

      // Remove from source
      newTasks[fromStatus] = newTasks[fromStatus].filter((t) => t.id !== taskId);
      
      // Update task status
      const movedTask = { ...task, status: toStatus, position: newPosition };
      
      // Add to destination
      const destTasks = [...newTasks[toStatus]];
      destTasks.splice(newPosition, 0, movedTask);
      newTasks[toStatus] = destTasks;

      return { tasks: newTasks };
    });
  },
}));
