import { api } from "./api";
import type { ApiResponse } from "@/types";
import type {
  ProjectAnalysisResponse,
  TaskAssignmentRequest,
  TaskAssignmentResponse,
  DocumentAiResponse,
  WorkspaceQuestionRequest,
  WorkspaceAnswerResponse,
  SprintGenerateRequest,
  SprintGenerateResponse,
  DocumentationRequest,
  DocumentationResponse,
  DocumentationTypeInfo,
  DemoModeRequest,
  DemoModeResponse,
  DemoModeInfo,
} from "@/types/ai";

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

  generateSprint: async (data: SprintGenerateRequest): Promise<SprintGenerateResponse> => {
    const response = await api.post(`/ai/workspace/sprint/generate`, data);
    return unwrapAiResponse<SprintGenerateResponse>(response);
  },

  // ---- AI Documentation ----

  generateDocumentation: async (data: DocumentationRequest): Promise<DocumentationResponse> => {
    const response = await api.post(`/ai/documentation`, data);
    return unwrapAiResponse<DocumentationResponse>(response);
  },

  listDocumentationTypes: async (): Promise<DocumentationTypeInfo[]> => {
    const response = await api.get(`/ai/documentation/types`);
    const payload = response.data as any;
    if (payload && typeof payload === "object" && "data" in payload) {
      return (payload.data ?? []) as DocumentationTypeInfo[];
    }
    if (Array.isArray(payload)) return payload as DocumentationTypeInfo[];
    if (payload && Array.isArray(payload.data)) return payload.data as DocumentationTypeInfo[];
    return [];
  },

  // ---- Demo Mode ----

  startDemo: async (data: DemoModeRequest): Promise<DemoModeResponse> => {
    const response = await api.post(`/ai/demo/start`, data);
    return unwrapAiResponse<DemoModeResponse>(response);
  },

  getDemoInfo: async (): Promise<DemoModeInfo> => {
    const response = await api.get(`/ai/demo/info`);
    return unwrapAiResponse<DemoModeInfo>(response);
  },
};
