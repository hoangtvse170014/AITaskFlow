import { create } from "zustand";
import type { Workspace } from "@/types";
import { workspaceApi } from "@/lib/api";
import toast from "react-hot-toast";

interface WorkspaceState {
  workspaces: Workspace[];
  currentWorkspace: Workspace | null;
  isLoading: boolean;
  fetchWorkspaces: () => Promise<void>;
  setCurrentWorkspace: (workspace: Workspace) => void;
  createWorkspace: (name: string, description?: string) => Promise<void>;
  updateWorkspace: (id: string, name: string, description?: string) => Promise<void>;
  deleteWorkspace: (id: string) => Promise<void>;
}

export const useWorkspaceStore = create<WorkspaceState>((set, get) => ({
  workspaces: [],
  currentWorkspace: null,
  isLoading: false,

  fetchWorkspaces: async () => {
    set({ isLoading: true });
    try {
      const response = await workspaceApi.getAll();
      if (response.data.success && response.data.data) {
        set({ workspaces: response.data.data, isLoading: false });
        
        const { currentWorkspace } = get();
        if (!currentWorkspace && response.data.data.length > 0) {
          set({ currentWorkspace: response.data.data[0] });
        }
      }
    } catch (error) {
      set({ isLoading: false });
      throw error;
    }
  },

  setCurrentWorkspace: (workspace: Workspace) => {
    set({ currentWorkspace: workspace });
  },

  createWorkspace: async (name: string, description?: string) => {
    try {
      const response = await workspaceApi.create({ name, description });
      if (response.data.success && response.data.data) {
        const newWorkspace = response.data.data;
        set((state) => ({
          workspaces: [...state.workspaces, newWorkspace],
          currentWorkspace: newWorkspace,
        }));
        toast.success("Workspace created successfully");
      }
    } catch (error) {
      throw error;
    }
  },

  updateWorkspace: async (id: string, name: string, description?: string) => {
    try {
      const response = await workspaceApi.update(id, { name, description });
      if (response.data.success && response.data.data) {
        const updatedWorkspace = response.data.data;
        set((state) => ({
          workspaces: state.workspaces.map((w) =>
            w.id === id ? updatedWorkspace : w
          ),
          currentWorkspace:
            state.currentWorkspace?.id === id
              ? updatedWorkspace
              : state.currentWorkspace,
        }));
        toast.success("Workspace updated successfully");
      }
    } catch (error) {
      throw error;
    }
  },

  deleteWorkspace: async (id: string) => {
    try {
      await workspaceApi.delete(id);
      set((state) => {
        const filtered = state.workspaces.filter((w) => w.id !== id);
        return {
          workspaces: filtered,
          currentWorkspace:
            state.currentWorkspace?.id === id
              ? filtered[0] || null
              : state.currentWorkspace,
        };
      });
      toast.success("Workspace deleted successfully");
    } catch (error) {
      throw error;
    }
  },
}));
