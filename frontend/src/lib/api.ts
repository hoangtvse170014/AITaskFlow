import axios, { AxiosError, InternalAxiosRequestConfig } from "axios";
import type { ApiResponse } from "@/types";
import toast from "react-hot-toast";

const API_BASE_URL = process.env.NEXT_PUBLIC_API_URL || "https://grown-smell-roland-manuals.trycloudflare.com/api";

export const api = axios.create({
  baseURL: API_BASE_URL,
  timeout: 10000,
  headers: {
    "Content-Type": "application/json",
  },
});

api.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    if (typeof window !== "undefined") {
      const token = localStorage.getItem("accessToken");
      if (token) {
        config.headers.Authorization = `Bearer ${token}`;
      }
    }
    return config;
  },
  (error) => Promise.reject(error)
);

api.interceptors.response.use(
  (response) => response,
  async (error: AxiosError<ApiResponse<unknown>>) => {
    const originalRequest = error.config;

    if (error.response?.status === 401 && originalRequest) {
      const refreshToken = localStorage.getItem("refreshToken");
      if (refreshToken) {
        try {
          const response = await axios.post<ApiResponse<{ accessToken: string; refreshToken: string }>>(
            `${API_BASE_URL}/auth/refresh`,
            { refreshToken }
          );
          
          if (response.data.success && response.data.data) {
            const { accessToken, refreshToken: newRefreshToken } = response.data.data;
            localStorage.setItem("accessToken", accessToken);
            localStorage.setItem("refreshToken", newRefreshToken);
            
            originalRequest.headers.Authorization = `Bearer ${accessToken}`;
            return api(originalRequest);
          }
        } catch {
          localStorage.removeItem("accessToken");
          localStorage.removeItem("refreshToken");
          if (typeof window !== "undefined") {
            window.location.href = "/login";
          }
        }
      }
    }

    if (error.response?.data?.message) {
      toast.error(error.response.data.message);
    }

    return Promise.reject(error);
  }
);

export const authApi = {
  register: (data: { email: string; password: string; fullName: string }) =>
    api.post<ApiResponse<{ user: any; accessToken: string; refreshToken: string }>>("/auth/register", data),
  
  login: (data: { email: string; password: string }) =>
    api.post<ApiResponse<{ user: any; accessToken: string; refreshToken: string }>>("/auth/login", data),
  
  refresh: (refreshToken: string) =>
    api.post<ApiResponse<{ user: any; accessToken: string; refreshToken: string }>>("/auth/refresh", { refreshToken }),
  
  me: () => api.get<ApiResponse<any>>("/auth/me"),
};

export const workspaceApi = {
  getAll: () => api.get<ApiResponse<any[]>>("/workspaces"),
  getById: (id: string) => api.get<ApiResponse<any>>(`/workspaces/${id}`),
  create: (data: { name: string; description?: string }) =>
    api.post<ApiResponse<any>>("/workspaces", data),
  update: (id: string, data: { name: string; description?: string }) =>
    api.put<ApiResponse<any>>(`/workspaces/${id}`, data),
  delete: (id: string) => api.delete<ApiResponse<void>>(`/workspaces/${id}`),
  inviteMember: (id: string, data: { email: string; role: string }) =>
    api.post<ApiResponse<any>>(`/workspaces/${id}/invite`, data),
  getMembers: (id: string) => api.get<ApiResponse<any[]>>(`/workspaces/${id}/members`),
  removeMember: (workspaceId: string, memberId: string) =>
    api.delete<ApiResponse<void>>(`/workspaces/${workspaceId}/members/${memberId}`),
};

export const projectApi = {
  getAll: (workspaceId: string) =>
    api.get<ApiResponse<any[]>>(`/workspaces/${workspaceId}/projects`),
  getById: (workspaceId: string, projectId: string) =>
    api.get<ApiResponse<any>>(`/workspaces/${workspaceId}/projects/${projectId}`),
  create: (workspaceId: string, data: { name: string; key: string; description?: string; color?: string; icon?: string }) =>
    api.post<ApiResponse<any>>(`/workspaces/${workspaceId}/projects`, data),
  update: (workspaceId: string, projectId: string, data: Partial<{ name: string; key: string; description?: string; color?: string; icon?: string }>) =>
    api.put<ApiResponse<any>>(`/workspaces/${workspaceId}/projects/${projectId}`, data),
  delete: (workspaceId: string, projectId: string) =>
    api.delete<ApiResponse<void>>(`/workspaces/${workspaceId}/projects/${projectId}`),
  getMembers: (workspaceId: string, projectId: string) =>
    api.get<ApiResponse<any[]>>(`/workspaces/${workspaceId}/projects/${projectId}/members`),
  addMember: (workspaceId: string, projectId: string, data: { email: string; role: string }) =>
    api.post<ApiResponse<any>>(`/workspaces/${workspaceId}/projects/${projectId}/members`, data),
  removeMember: (workspaceId: string, projectId: string, memberId: string) =>
    api.delete<ApiResponse<void>>(`/workspaces/${workspaceId}/projects/${projectId}/members/${memberId}`),
};

export const userApi = {
  search: (query: string) =>
    api.get<ApiResponse<any[]>>(`/users/search?query=${encodeURIComponent(query)}`),
};

export const taskApi = {
  getAll: (projectId: string) =>
    api.get<ApiResponse<Record<string, any[]>>>(`/projects/${projectId}/tasks`),
  getById: (projectId: string, taskId: string) =>
    api.get<ApiResponse<any>>(`/projects/${projectId}/tasks/${taskId}`),
  create: (projectId: string, data: any) =>
    api.post<ApiResponse<any>>(`/projects/${projectId}/tasks`, data),
  update: (projectId: string, taskId: string, data: any) =>
    api.put<ApiResponse<any>>(`/projects/${projectId}/tasks/${taskId}`, data),
  updatePosition: (projectId: string, taskId: string, data: { status: string; position: number }) =>
    api.patch<ApiResponse<any>>(`/projects/${projectId}/tasks/${taskId}/position`, data),
  delete: (projectId: string, taskId: string) =>
    api.delete<ApiResponse<void>>(`/projects/${projectId}/tasks/${taskId}`),
  getComments: (taskId: string) =>
    api.get<ApiResponse<any[]>>(`/tasks/${taskId}/comments`),
  addComment: (taskId: string, data: { content: string }) =>
    api.post<ApiResponse<any>>(`/tasks/${taskId}/comments`, data),
  deleteComment: (taskId: string, commentId: string) =>
    api.delete<ApiResponse<void>>(`/tasks/${taskId}/comments/${commentId}`),
  getActivity: (taskId: string) =>
    api.get<ApiResponse<any[]>>(`/tasks/${taskId}/activity`),
  getSubTasks: (taskId: string) =>
    api.get<ApiResponse<any[]>>(`/tasks/${taskId}/subtasks`),
  createSubTask: (taskId: string, data: { title: string }) =>
    api.post<ApiResponse<any>>(`/tasks/${taskId}/subtasks`, data),
  updateSubTask: (taskId: string, subTaskId: string, data: { title: string }) =>
    api.put<ApiResponse<any>>(`/tasks/${taskId}/subtasks/${subTaskId}`, data),
  toggleSubTask: (taskId: string, subTaskId: string) =>
    api.patch<ApiResponse<any>>(`/tasks/${taskId}/subtasks/${subTaskId}/toggle`),
  deleteSubTask: (taskId: string, subTaskId: string) =>
    api.delete<ApiResponse<void>>(`/tasks/${taskId}/subtasks/${subTaskId}`),
};

export const dashboardApi = {
  getStats: (workspaceId: string) =>
    api.get<ApiResponse<any>>(`/dashboard/stats/${workspaceId}`),
};

export const pageApi = {
  getAll: (workspaceId: string) =>
    api.get<ApiResponse<any[]>>(`/workspaces/${workspaceId}/pages`),
  getTree: (workspaceId: string) =>
    api.get<ApiResponse<any[]>>(`/workspaces/${workspaceId}/pages/tree`),
  getById: (workspaceId: string, pageId: string) =>
    api.get<ApiResponse<any>>(`/workspaces/${workspaceId}/pages/${pageId}`),
  create: (workspaceId: string, data: { title: string; icon?: string; coverUrl?: string; slug?: string; parentId?: string; isPublic?: boolean }) =>
    api.post<ApiResponse<any>>(`/workspaces/${workspaceId}/pages`, data),
  update: (workspaceId: string, pageId: string, data: { title?: string; icon?: string | null; coverUrl?: string | null; slug?: string | null; parentId?: string | null; isPublic?: boolean; isArchived?: boolean; sidebarOrder?: number }) =>
    api.put<ApiResponse<any>>(`/workspaces/${workspaceId}/pages/${pageId}`, data),
  delete: (workspaceId: string, pageId: string) =>
    api.delete<ApiResponse<void>>(`/workspaces/${workspaceId}/pages/${pageId}`),
  archive: (workspaceId: string, pageId: string) =>
    api.post<ApiResponse<any>>(`/workspaces/${workspaceId}/pages/${pageId}/archive`),
  restore: (workspaceId: string, pageId: string) =>
    api.post<ApiResponse<any>>(`/workspaces/${workspaceId}/pages/${pageId}/restore`),
  duplicate: (workspaceId: string, pageId: string) =>
    api.post<ApiResponse<any>>(`/workspaces/${workspaceId}/pages/${pageId}/duplicate`),
  toggleFavorite: (workspaceId: string, pageId: string) =>
    api.post<ApiResponse<any>>(`/workspaces/${workspaceId}/pages/${pageId}/favorite`),
  search: (workspaceId: string, query: string) =>
    api.get<ApiResponse<any>>(`/workspaces/${workspaceId}/pages/search?query=${encodeURIComponent(query)}`),
  getFavorites: () =>
    api.get<ApiResponse<any[]>>(`/pages/favorites`),
};

export const blockApi = {
  getAll: (pageId: string) =>
    api.get<ApiResponse<any[]>>(`/pages/${pageId}/blocks`),
  create: (pageId: string, data: { blockType: string; content?: string; properties?: string; parentBlockId?: string; orderIndex?: number }) =>
    api.post<ApiResponse<any>>(`/pages/${pageId}/blocks`, data),
  update: (pageId: string, blockId: string, data: { content?: string | null; properties?: string | null; orderIndex?: number; isCollapsed?: boolean; parentBlockId?: string | null }) =>
    api.put<ApiResponse<any>>(`/pages/${pageId}/blocks/${blockId}`, data),
  delete: (pageId: string, blockId: string) =>
    api.delete<ApiResponse<void>>(`/pages/${pageId}/blocks/${blockId}`),
  reorder: (pageId: string, data: { blockId: string; afterBlockId?: string; newIndex?: number }) =>
    api.post<ApiResponse<any>>(`/pages/${pageId}/blocks/reorder`, data),
  duplicate: (pageId: string, blockIds: string[]) =>
    api.post<ApiResponse<any[]>>(`/pages/${pageId}/blocks/duplicate`, blockIds),
};

export const notificationApi = {
  getAll: (limit: number = 20) => api.get<ApiResponse<{ notifications: any[]; unreadCount: number }>>(`/notifications?limit=${limit}`),
  getUnreadCount: () => api.get<ApiResponse<{ count: number }>>(`/notifications/unread-count`),
  markAsRead: (notificationId: string) =>
    api.post<ApiResponse<void>>(`/notifications/${notificationId}/read`),
  markAllAsRead: () => api.post<ApiResponse<void>>(`/notifications/read-all`),
  delete: (notificationId: string) =>
    api.delete<ApiResponse<void>>(`/notifications/${notificationId}`),
};
