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
  /** Related projects from the answer. */
  relatedProjects?: RelatedItem[];
  /** Related tasks from the answer. */
  relatedTasks?: RelatedItem[];
  /** Related workspace members from the answer (new). */
  relatedMembers?: RelatedItem[];
  /** Related goals/OKRs from the answer (new). */
  relatedGoals?: RelatedItem[];
  /** Related pages/documents from the answer (new). */
  relatedPages?: RelatedItem[];
  suggestions?: string[];
  /** Detected question intent, e.g. "SUMMARIZE_WORKSPACE" or "MOST_RISKY_PROJECT". */
  intent?: string;
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
  relatedMembers?: RelatedItem[];
  relatedGoals?: RelatedItem[];
  relatedPages?: RelatedItem[];
  suggestions?: string[];
  confidence?: number;
  processingTimeMs?: number;
}

export interface ConversationContext {
  workspaceId: string;
  messages: ChatMessage[];
}

// Sprint generation types
export interface SprintGenerateRequest {
  workspaceId: string;
  basedOnProjectId?: string;
  durationDays?: number;
  maxTasks?: number;
  includeDone?: boolean;
}

export interface SprintGenerateResponse {
  sprintGoal?: string;
  tasks?: SprintTask[];
  capacity?: TeamCapacity;
  risks?: string[];
  suggestions?: string[];
  confidence?: number;
  processingTimeMs?: number;
  error?: string;
}

export interface SprintTask {
  taskId?: string;
  taskKey?: string;
  title?: string;
  projectName?: string;
  priority?: string;
  status?: string;
  dueDate?: string;
  suggestedAssigneeId?: string;
  suggestedAssigneeName?: string;
  effort?: string; // S / M / L
  reason?: string;
}

export interface TeamCapacity {
  totalMembers?: number;
  availableMembers?: number;
  overloadedMembers?: number;
  breakdown?: MemberCapacity[];
}

export interface MemberCapacity {
  memberId?: string;
  name?: string;
  role?: string;
  currentWorkload?: number;
  canTakeMore?: "yes" | "limited" | "no";
}

// =====================================================
// AI Documentation types
// =====================================================

export type DocumentType =
  | "SRS"
  | "USER_STORIES"
  | "ACCEPTANCE_CRITERIA"
  | "MEETING_MINUTES"
  | "SPRINT_REVIEW"
  | "RETROSPECTIVE"
  | "TECHNICAL_SPEC";

export interface DocumentationRequest {
  workspaceId: string;
  documentType: DocumentType;
  projectId?: string;
  sprintId?: string;
  sprintName?: string;
  pageId?: string;
  topic?: string;
  author?: string;
  durationDays?: number;
  attendees?: string[];
  audience?: string;
}

export interface DocSection {
  level: number;
  heading: string;
  anchor?: string;
}

export interface DocumentationResponse {
  documentType?: DocumentType;
  markdown?: string;
  title?: string;
  sections?: DocSection[];
  keywords?: string[];
  source?: "GROQ" | "LOCAL_FALLBACK" | "DEMO_MODE";
  confidence?: number;
  processingTimeMs?: number;
  error?: string;
}

export interface DocumentationTypeInfo {
  id: DocumentType;
  label: string;
  endpoint: string;
}

// =====================================================
// Demo Mode types
// =====================================================

export type DemoStageId =
  | "ANALYZING_REQUIREMENTS"
  | "PLANNING_SPRINTS"
  | "GENERATING_TASKS"
  | "ASSIGNING_MEMBERS"
  | "CREATING_ENTITIES"
  | "REFRESHING_DASHBOARD"
  | "COMPLETED";

export type StageStatus = "PENDING" | "RUNNING" | "DONE" | "FAILED" | "SKIPPED";

export interface DemoStageResult {
  stage: DemoStageId;
  label: string;
  status: StageStatus;
  durationMs?: number;
  detail?: string;
}

export interface DemoModeCounts {
  sprintsCreated: number;
  tasksCreated: number;
  subtasksCreated: number;
  assignmentsApplied: number;
  risksGenerated: number;
}

export interface DemoModeTimeline {
  startDate?: string;
  endDate?: string;
  totalWeeks?: number;
  totalEstimatedHours?: number;
  totalStoryPoints?: number;
}

export interface DemoModeWorkloadEntry {
  memberId?: string;
  memberName?: string;
  taskCount?: number;
  estimatedHours?: number;
  workloadPercentage?: number;
}

export interface DemoModeRequest {
  workspaceId: string;
  projectIdea: string;
  technologyStack?: string;
  teamSize?: string;
  weeksDeadline?: number;
  idempotencyKey?: string;
}

export interface DemoModeResponse {
  success: boolean;
  idempotent?: boolean;
  currentStage?: DemoStageId;
  stages?: DemoStageResult[];
  counts?: DemoModeCounts;
  timeline?: DemoModeTimeline;
  workload?: DemoModeWorkloadEntry[];
  risks?: string[];
  projectId?: string;
  projectName?: string;
  refreshSignals?: string[];
  steps?: string[];
  warnings?: string[];
  startedAt?: string;
  finishedAt?: string;
  processingTimeMs?: number;
  error?: string;
}

export interface DemoModeInfo {
  name: string;
  description: string;
  stages: Array<{ id: DemoStageId; label: string; icon: string }>;
  defaultTargetTasks: string;
  defaultTargetSubtasks: string;
  defaultSprints: number;
  idempotent: boolean;
}
