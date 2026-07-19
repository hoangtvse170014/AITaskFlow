import { create } from "zustand";
import type { Project } from "@/types";
import { projectApi } from "@/lib/api";
import toast from "react-hot-toast";

interface ProjectState {
  projects: Project[];
  currentProject: Project | null;
  isLoading: boolean;
  fetchProjects: (workspaceId: string) => Promise<void>;
  setCurrentProject: (project: Project | null) => void;
  createProject: (
    workspaceId: string,
    data: {
      name: string;
      key: string;
      description?: string;
      color?: string;
      icon?: string;
    }
  ) => Promise<void>;
  updateProject: (
    workspaceId: string,
    projectId: string,
    data: Partial<{
      name: string;
      key: string;
      description?: string;
      color?: string;
      icon?: string;
    }>
  ) => Promise<void>;
  deleteProject: (workspaceId: string, projectId: string) => Promise<void>;
}

export const useProjectStore = create<ProjectState>((set) => ({
  projects: [],
  currentProject: null,
  isLoading: false,

  fetchProjects: async (workspaceId: string) => {
    set({ isLoading: true });
    try {
      const response = await projectApi.getAll(workspaceId);
      if (response.data.success && response.data.data) {
        set({ projects: response.data.data, isLoading: false });
      }
    } catch (error) {
      set({ isLoading: false });
      throw error;
    }
  },

  setCurrentProject: (project: Project | null) => {
    set({ currentProject: project });
  },

  createProject: async (
    workspaceId: string,
    data: {
      name: string;
      key: string;
      description?: string;
      color?: string;
      icon?: string;
    }
  ) => {
    try {
      const response = await projectApi.create(workspaceId, data);
      if (response.data.success && response.data.data) {
        set((state) => ({
          projects: [...state.projects, response.data.data],
        }));
        toast.success("Project created successfully");
      }
    } catch (error) {
      throw error;
    }
  },

  updateProject: async (
    workspaceId: string,
    projectId: string,
    data: Partial<{
      name: string;
      key: string;
      description?: string;
      color?: string;
      icon?: string;
    }>
  ) => {
    try {
      const response = await projectApi.update(workspaceId, projectId, data);
      if (response.data.success && response.data.data) {
        set((state) => ({
          projects: state.projects.map((p) =>
            p.id === projectId ? response.data.data : p
          ),
          currentProject:
            state.currentProject?.id === projectId
              ? response.data.data
              : state.currentProject,
        }));
        toast.success("Project updated successfully");
      }
    } catch (error) {
      throw error;
    }
  },

  deleteProject: async (workspaceId: string, projectId: string) => {
    try {
      await projectApi.delete(workspaceId, projectId);
      set((state) => ({
        projects: state.projects.filter((p) => p.id !== projectId),
        currentProject:
          state.currentProject?.id === projectId ? null : state.currentProject,
      }));
      toast.success("Project deleted successfully");
    } catch (error) {
      throw error;
    }
  },
}));
