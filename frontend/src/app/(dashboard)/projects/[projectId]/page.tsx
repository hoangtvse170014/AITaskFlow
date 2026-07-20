"use client";

import * as React from "react";
import { useParams, useRouter } from "next/navigation";
import {
  DndContext,
  DragOverlay,
  closestCorners,
  KeyboardSensor,
  PointerSensor,
  useSensor,
  useSensors,
  DragStartEvent,
  DragEndEvent,
  DragOverEvent,
} from "@dnd-kit/core";
import {
  SortableContext,
  verticalListSortingStrategy,
} from "@dnd-kit/sortable";
import { ArrowLeft, Plus, Loader2, LayoutGrid, List, Users, Calendar } from "lucide-react";
import { MainLayout } from "@/components/layout/MainLayout";
import { Header } from "@/components/layout/Header";
import { KanbanColumn } from "@/components/tasks/KanbanColumn";
import { TaskCard } from "@/components/tasks/TaskCard";
import { TaskModal } from "@/components/tasks/TaskModal";
import { CreateTaskDialog } from "@/components/tasks/CreateTaskDialog";
import { TaskTableView } from "@/components/tasks/TaskTableView";
import { ProjectMembersDialog } from "@/components/projects/ProjectMembersDialog";
import { ProjectReport } from "@/components/projects/ProjectReport";
import { AiDashboardCard } from "@/components/ai/AiDashboardCard";
import { DocumentationPanel } from "@/components/ai/DocumentationPanel";
import { TaskFilters } from "@/components/tasks/TaskFilters";
import { TaskSearch } from "@/components/tasks/TaskSearch";
import { TaskCalendarView } from "@/components/tasks/TaskCalendarView";
import { useTaskStore } from "@/store/taskStore";
import { useProjectStore } from "@/store/projectStore";
import { useWorkspaceStore } from "@/store/workspaceStore";
import type { Task, TaskStatus } from "@/types";
import { Button } from "@/components/ui/button";
import Link from "next/link";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";

const COLUMNS: { id: TaskStatus; title: string }[] = [
  { id: "TODO", title: "To Do" },
  { id: "IN_PROGRESS", title: "In Progress" },
  { id: "REVIEW", title: "Review" },
  { id: "DONE", title: "Done" },
];

type ViewMode = "kanban" | "table" | "calendar";

export default function ProjectBoardPage() {
  const params = useParams();
  const router = useRouter();
  const projectId = params.projectId as string;
  const { projects, fetchProjects } = useProjectStore();
  const { tasks, isLoading, fetchTasks, updateTaskPosition, moveTask, deleteTask, currentTask, setCurrentTask } = useTaskStore();
  const { currentWorkspace } = useWorkspaceStore();
  
  const [activeTask, setActiveTask] = React.useState<Task | null>(null);
  const [isCreateOpen, setIsCreateOpen] = React.useState(false);
  const [createColumn, setCreateColumn] = React.useState<TaskStatus>("TODO");
  const [viewMode, setViewMode] = React.useState<ViewMode>("kanban");
  const [isMembersOpen, setIsMembersOpen] = React.useState(false);
  const [searchQuery, setSearchQuery] = React.useState("");
  const [filters, setFilters] = React.useState({
    status: [] as string[],
    priority: [] as string[],
    assignee: [] as string[],
  });

  const currentProject = projects.find((p) => p.id === projectId);
  const isAdmin = currentWorkspace?.role === "OWNER" || currentWorkspace?.role === "ADMIN";

  // Fetch projects if not loaded
  React.useEffect(() => {
    if (projects.length === 0) {
      fetchProjects();
    }
  }, [projects.length, fetchProjects]);

  // Fetch tasks when project is available
  React.useEffect(() => {
    if (projectId) {
      fetchTasks(projectId);
    }
  }, [projectId, fetchTasks]);

  // Get all tasks as a flat array
  const allTasks = React.useMemo(() => {
    return Object.values(tasks).flat();
  }, [tasks]);

  const sensors = useSensors(
    useSensor(PointerSensor, {
      activationConstraint: {
        distance: 8,
      },
    }),
    useSensor(KeyboardSensor)
  );

  const handleDragStart = (event: DragStartEvent) => {
    const { active } = event;
    const taskId = active.id as string;
    
    // Find the task across all columns
    for (const status of Object.keys(tasks) as TaskStatus[]) {
      const task = tasks[status].find((t) => t.id === taskId);
      if (task) {
        setActiveTask(task);
        break;
      }
    }
  };

  const handleDragOver = (event: DragOverEvent) => {
    const { active, over } = event;
    
    if (!over) return;

    const activeId = active.id as string;
    const overId = over.id as string;

    // Find source and destination columns
    let sourceColumn: TaskStatus | null = null;
    let destColumn: TaskStatus | null = null;

    for (const status of Object.keys(tasks) as TaskStatus[]) {
      if (tasks[status].some((t) => t.id === activeId)) {
        sourceColumn = status;
      }
      if (tasks[status].some((t) => t.id === overId) || overId === status) {
        destColumn = status;
      }
    }

    if (sourceColumn && destColumn && sourceColumn !== destColumn) {
      const destTasks = [...tasks[destColumn]];
      const sourceTasks = tasks[sourceColumn].filter((t) => t.id !== activeId);
      
      // Find the task being moved
      const taskToMove = tasks[sourceColumn].find((t) => t.id === activeId);
      if (taskToMove) {
        const updatedTask = { ...taskToMove, status: destColumn };
        
        // Optimistic update
        moveTask(activeId, sourceColumn, destColumn, destTasks.length);
      }
    }
  };

  const handleDragEnd = async (event: DragEndEvent) => {
    const { active, over } = event;
    setActiveTask(null);

    if (!over) return;

    const activeId = active.id as string;
    const overId = over.id as string;

    // Find destination
    let destColumn: TaskStatus | null = null;
    let destIndex = 0;

    for (let i = 0; i < COLUMNS.length; i++) {
      const column = COLUMNS[i];
      const taskIndex = tasks[column.id].findIndex((t) => t.id === overId);
      if (taskIndex !== -1) {
        destColumn = column.id;
        destIndex = taskIndex;
        break;
      }
      if (overId === column.id) {
        destColumn = column.id;
        destIndex = tasks[column.id].length;
        break;
      }
    }

    if (!destColumn) return;

    // Find source
    let sourceColumn: TaskStatus | null = null;
    for (const status of Object.keys(tasks) as TaskStatus[]) {
      if (tasks[status].some((t) => t.id === activeId)) {
        sourceColumn = status;
        break;
      }
    }

    if (!sourceColumn) return;

    try {
      await updateTaskPosition(projectId, activeId, destColumn, destIndex);
    } catch {
      // Rollback is handled by store
      fetchTasks(projectId);
    }
  };

  const handleOpenCreate = (column: TaskStatus) => {
    setCreateColumn(column);
    setIsCreateOpen(true);
  };

  const handleEditTask = (task: Task) => {
    setCurrentTask(task);
  };

  const handleDeleteTask = async (taskId: string) => {
    if (confirm("Are you sure you want to delete this task?")) {
      await deleteTask(projectId, taskId);
    }
  };

  if (isLoading && Object.values(tasks).every((arr) => arr.length === 0)) {
    return (
      <MainLayout>
        <div className="flex items-center justify-center h-screen">
          <Loader2 className="w-8 h-8 animate-spin text-muted-foreground" />
        </div>
      </MainLayout>
    );
  }

  return (
    <MainLayout>
      <Header
        title={currentProject?.name || "Project Board"}
        subtitle={currentProject?.key ? `Project ${currentProject.key}` : "Loading..."}
        actions={
          <div className="flex items-center gap-2">
            <Button
              variant="outline"
              size="sm"
              onClick={() => setIsMembersOpen(true)}
            >
              <Users className="w-4 h-4 mr-2" />
              Members
            </Button>
            <TaskSearch
              searchQuery={searchQuery}
              onSearchChange={setSearchQuery}
            />
            <TaskFilters
              projectId={projectId}
              filters={filters}
              onFiltersChange={setFilters}
            />
            <ProjectReport
              projectId={projectId}
              projectName={currentProject?.name || "Project"}
            />
            <AiDashboardCard
              projectId={projectId}
              projectName={currentProject?.name || "Project"}
            />
            {currentWorkspace?.id && (
              <DocumentationPanel
                workspaceId={currentWorkspace.id}
                projectId={projectId}
              />
            )}
            <div className="flex items-center border rounded-md">
              <Button
                variant={viewMode === "kanban" ? "secondary" : "ghost"}
                size="sm"
                className="rounded-r-none"
                onClick={() => setViewMode("kanban")}
              >
                <LayoutGrid className="w-4 h-4 mr-1" />
                Board
              </Button>
              <Button
                variant={viewMode === "table" ? "secondary" : "ghost"}
                size="sm"
                className="rounded-none"
                onClick={() => setViewMode("table")}
              >
                <List className="w-4 h-4 mr-1" />
                Table
              </Button>
              <Button
                variant={viewMode === "calendar" ? "secondary" : "ghost"}
                size="sm"
                className="rounded-l-none"
                onClick={() => setViewMode("calendar")}
              >
                <Calendar className="w-4 h-4 mr-1" />
                Calendar
              </Button>
            </div>
            <Button onClick={() => handleOpenCreate("TODO")} size="sm">
              <Plus className="w-4 h-4 mr-2" />
              Add Task
            </Button>
          </div>
        }
      />
      
      <div className="p-6">
        <div className="mb-4">
          <Link href="/projects">
            <Button variant="ghost" size="sm">
              <ArrowLeft className="w-4 h-4 mr-2" />
              Back to Projects
            </Button>
          </Link>
        </div>

        {viewMode === "kanban" ? (
          <DndContext
            sensors={sensors}
            collisionDetection={closestCorners}
            onDragStart={handleDragStart}
            onDragOver={handleDragOver}
            onDragEnd={handleDragEnd}
          >
            <div className="flex gap-4 min-w-max overflow-x-auto pb-4">
              {COLUMNS.map((column) => (
                <KanbanColumn
                  key={column.id}
                  id={column.id}
                  title={column.title}
                  tasks={tasks[column.id]}
                  onAddTask={() => handleOpenCreate(column.id)}
                  onTaskClick={(task) => setCurrentTask(task)}
                />
              ))}
            </div>

            <DragOverlay>
              {activeTask && (
                <TaskCard task={activeTask} isDragging />
              )}
            </DragOverlay>
          </DndContext>
        ) : viewMode === "calendar" ? (
          <TaskCalendarView
            tasks={allTasks}
            onTaskClick={(task) => setCurrentTask(task)}
          />
        ) : (
          <TaskTableView
            tasks={allTasks}
            projectId={projectId}
            onEditTask={handleEditTask}
            onDeleteTask={handleDeleteTask}
          />
        )}
      </div>

      {/* Task Detail Modal */}
      {currentTask && (
        <TaskModal
          task={currentTask}
          projectId={projectId}
          onClose={() => setCurrentTask(null)}
        />
      )}

      {/* Create Task Dialog */}
      <CreateTaskDialog
        open={isCreateOpen}
        onOpenChange={setIsCreateOpen}
        projectId={projectId}
        defaultStatus={createColumn}
      />

      {/* Members Dialog */}
      <ProjectMembersDialog
        open={isMembersOpen}
        onOpenChange={setIsMembersOpen}
        projectId={projectId}
        isAdmin={isAdmin}
      />
    </MainLayout>
  );
}
