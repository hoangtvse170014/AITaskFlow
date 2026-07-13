import { api } from "./api";
import type { ApiResponse } from "@/types";
import type { ProjectAnalysisResponse, TaskAssignmentRequest, TaskAssignmentResponse, DocumentAiResponse, WorkspaceQuestionRequest, WorkspaceAnswerResponse } from "@/types/ai";

function unwrapAiResponse<T>(response: { data: ApiResponse<T> | T }): T {
  const payload = response.data as any;
  if (payload && typeof payload === 'object' && 'success' in payload) {
    if (payload.success && payload.data !== undefined) {
      return payload.data as T;
    }
    throw new Error(payload.message || 'AI request failed');
  }
  return payload as T;
}

export const aiApi = {
  analyzeProject: async (projectId: string): Promise<ProjectAnalysisResponse> => {
    const response = await api.post(`/ai/projects/${projectId}/analyze`);
    return unwrapAiResponse<ProjectAnalysisResponse>(response);
  },

  recommendAssignee: async (data: TaskAssignmentRequest): Promise<TaskAssignmentResponse> => {
    const response = await api.post(`/ai/tasks/recommend`, data);
    return unwrapAiResponse<TaskAssignmentResponse>(response);
  },

  summarizePage: async (pageId: string): Promise<DocumentAiResponse> => {
    const response = await api.post(`/ai/pages/${pageId}/summarize`);
    return unwrapAiResponse<DocumentAiResponse>(response);
  },

  rewritePage: async (pageId: string): Promise<DocumentAiResponse> => {
    const response = await api.post(`/ai/pages/${pageId}/rewrite`);
    return unwrapAiResponse<DocumentAiResponse>(response);
  },

  improvePage: async (pageId: string): Promise<DocumentAiResponse> => {
    const response = await api.post(`/ai/pages/${pageId}/improve`);
    return unwrapAiResponse<DocumentAiResponse>(response);
  },

  extractActions: async (pageId: string): Promise<DocumentAiResponse> => {
    const response = await api.post(`/ai/pages/${pageId}/actions`);
    return unwrapAiResponse<DocumentAiResponse>(response);
  },

  generateMeetingMinutes: async (pageId: string): Promise<DocumentAiResponse> => {
    const response = await api.post(`/ai/pages/${pageId}/meeting`);
    return unwrapAiResponse<DocumentAiResponse>(response);
  },

  generateRequirements: async (pageId: string): Promise<DocumentAiResponse> => {
    const response = await api.post(`/ai/pages/${pageId}/requirements`);
    return unwrapAiResponse<DocumentAiResponse>(response);
  },

  askWorkspace: async (data: WorkspaceQuestionRequest): Promise<WorkspaceAnswerResponse> => {
    const response = await api.post(`/ai/workspace/chat`, data);
    return unwrapAiResponse<WorkspaceAnswerResponse>(response);
  },
};
