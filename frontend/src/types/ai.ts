export interface ProjectAnalysisResponse {
  healthScore?: number;
  summary?: string;
  risks?: string[];
  recommendations?: string[];
  nextActions?: string[];
  confidence?: number;
  processingTimeMs?: number;
  error?: string;
}

export interface TaskAssignmentRequest {
  title: string;
  description?: string;
  priority: string;
  dueDate?: string;
  projectId: string;
  workspaceId: string;
}

export interface MemberRanking {
  memberId: string;
  memberName: string;
  email: string;
  score: number;
  currentWorkload: string;
  openTasks: number;
  inProgressTasks: number;
  role: string;
  reason: string;
}

export interface TaskAssignmentResponse {
  recommendedAssignee?: string;
  recommendedAssigneeId?: string;
  confidence?: number;
  ranking?: MemberRanking[];
  warnings?: string[];
  reason?: string;
  processingTimeMs?: number;
  error?: string;
}

export interface ActionItem {
  title: string;
  priority?: "LOW" | "MEDIUM" | "HIGH" | "URGENT";
  assignee?: string;
  dueDate?: string;
}

export interface MeetingMinutes {
  summary?: string;
  keyDecisions?: string[];
  actionItems?: string[];
  risks?: string[];
  nextSteps?: string[];
}

export interface Requirements {
  overview?: string;
  functional?: string[];
  nonFunctional?: string[];
  acceptanceCriteria?: string[];
}

export interface DocumentAiResponse {
  summary?: string;
  rewrittenContent?: string;
  actionItems?: string[];
  meetingMinutes?: string;
  requirements?: string;
  keywords?: string[];
  confidence?: number;
  processingTimeMs?: number;
  error?: string;
}

export interface WorkspaceQuestionRequest {
  workspaceId: string;
  question: string;
}

export interface WorkspaceAnswerResponse {
  answer?: string;
  sources?: string[];
  confidence?: number;
  relatedProjects?: RelatedItem[];
  relatedTasks?: RelatedItem[];
  suggestions?: string[];
  processingTimeMs?: number;
  error?: string;
}

export interface RelatedItem {
  id?: string;
  name?: string;
  type?: string;
  status?: string;
  description?: string;
}

export interface ChatMessage {
  id: string;
  role: "user" | "assistant";
  content: string;
  timestamp: number;
  sources?: string[];
  relatedProjects?: RelatedItem[];
  relatedTasks?: RelatedItem[];
  suggestions?: string[];
  confidence?: number;
  processingTimeMs?: number;
}

export interface ConversationContext {
  workspaceId: string;
  messages: ChatMessage[];
}
