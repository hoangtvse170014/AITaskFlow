export interface User {
  id: string;
  email: string;
  fullName: string;
  avatarUrl: string | null;
}

export interface AuthResponse {
  user: User;
  accessToken: string;
  refreshToken: string;
}

export interface Role {
  id: string;
  name: string;
  description: string;
  priority: number;
  permissions: string[];
}

export interface WorkspaceMember {
  id: string;
  workspaceId: string;
  user: User;
  role: string;
  roleName: string;
  rolePriority: number;
  permissions: string[];
  invitedAt: string;
  joinedAt: string | null;
  isActive: boolean;
}

export interface Workspace {
  id: string;
  name: string;
  slug: string;
  description: string | null;
  owner: User;
  role: WorkspaceRole;
  memberCount: number;
  projectCount: number;
  createdAt: string;
  updatedAt: string;
}

export type WorkspaceRole = 'OWNER' | 'ADMIN' | 'MANAGER' | 'MEMBER' | 'GUEST';

export const ROLE_PERMISSIONS = {
  OWNER: [
    'workspace:view', 'workspace:edit', 'workspace:delete', 'workspace:manage',
    'member:view', 'member:invite', 'member:manage', 'member:remove',
    'project:view', 'project:create', 'project:edit', 'project:delete', 'project:manage',
    'task:view', 'task:create', 'task:edit', 'task:delete', 'task:assign',
    'page:view', 'page:create', 'page:edit', 'page:delete', 'page:archive',
    'settings:view', 'settings:edit', 'billing:view', 'billing:manage'
  ],
  ADMIN: [
    'workspace:view', 'workspace:edit', 'workspace:manage',
    'member:view', 'member:invite', 'member:manage', 'member:remove',
    'project:view', 'project:create', 'project:edit', 'project:delete', 'project:manage',
    'task:view', 'task:create', 'task:edit', 'task:delete', 'task:assign',
    'page:view', 'page:create', 'page:edit', 'page:delete', 'page:archive',
    'settings:view', 'settings:edit', 'billing:view'
  ],
  MANAGER: [
    'workspace:view', 'member:view',
    'project:view', 'project:create', 'project:edit', 'project:manage',
    'task:view', 'task:create', 'task:edit', 'task:delete', 'task:assign',
    'page:view', 'page:create', 'page:edit', 'page:delete', 'page:archive',
    'settings:view'
  ],
  MEMBER: [
    'workspace:view', 'member:view',
    'project:view', 'project:create',
    'task:view', 'task:create', 'task:edit',
    'page:view', 'page:create', 'page:edit'
  ],
  GUEST: [
    'workspace:view', 'member:view',
    'project:view', 'task:view', 'page:view'
  ]
} as const;

export function hasPermission(role: WorkspaceRole, permission: string | `${string}:${string}`): boolean {
  return ROLE_PERMISSIONS[role]?.includes(permission as any) ?? false;
}

export function hasAnyPermission(role: WorkspaceRole, permissions: string[]): boolean {
  return permissions.some(p => hasPermission(role, p));
}

export const PERMISSION_LABELS: Record<string, string> = {
  'workspace:view': 'View Workspace',
  'workspace:edit': 'Edit Workspace',
  'workspace:delete': 'Delete Workspace',
  'workspace:manage': 'Manage Workspace Settings',
  'member:view': 'View Members',
  'member:invite': 'Invite Members',
  'member:manage': 'Manage Members',
  'member:remove': 'Remove Members',
  'project:view': 'View Projects',
  'project:create': 'Create Projects',
  'project:edit': 'Edit Projects',
  'project:delete': 'Delete Projects',
  'project:manage': 'Manage Projects',
  'task:view': 'View Tasks',
  'task:create': 'Create Tasks',
  'task:edit': 'Edit Tasks',
  'task:delete': 'Delete Tasks',
  'task:assign': 'Assign Tasks',
  'page:view': 'View Pages',
  'page:create': 'Create Pages',
  'page:edit': 'Edit Pages',
  'page:delete': 'Delete Pages',
  'page:archive': 'Archive Pages',
  'settings:view': 'View Settings',
  'settings:edit': 'Edit Settings',
  'billing:view': 'View Billing',
  'billing:manage': 'Manage Billing'
};

export interface Project {
  id: string;
  workspaceId: string;
  name: string;
  key: string;
  description: string | null;
  color: string;
  icon: string | null;
  taskCount: number;
  completedTaskCount: number;
  createdAt: string;
  updatedAt: string;
}

export interface Label {
  name: string;
  color: string;
}

export interface ChecklistItem {
  id: string;
  text: string;
  completed: boolean;
}

export interface Task {
  id: string;
  projectId: string;
  taskKey: string;
  taskNumber: number;
  title: string;
  description: string | null;
  status: TaskStatus;
  priority: TaskPriority;
  assignee: User | null;
  assigneeId?: string;
  reporter: User;
  dueDate: string | null;
  overdue: boolean;
  labels: Label[] | null;
  checklist: ChecklistItem[] | null;
  completedChecklistItems: number;
  totalChecklistItems: number;
  position: number;
  createdAt: string;
  updatedAt: string;
}

export type TaskStatus = 'TODO' | 'IN_PROGRESS' | 'REVIEW' | 'DONE';
export type TaskPriority = 'LOW' | 'MEDIUM' | 'HIGH' | 'URGENT';

export interface Comment {
  id: string;
  taskId: string;
  author: User;
  content: string;
  createdAt: string;
  updatedAt: string;
}

export interface ActivityLog {
  id: string;
  taskId: string;
  user: User;
  action: string;
  fieldChanged: string | null;
  oldValue: string | null;
  newValue: string | null;
  createdAt: string;
}

export interface DashboardStats {
  totalTasks: number;
  completedTasks: number;
  inProgressTasks: number;
  todoTasks: number;
  delayedTasks: number;
  completionRate: number;
  tasksByPriority: Record<string, number>;
  tasksByStatus: Record<string, number>;
  recentTasks: Task[];
  overdueTasks: Task[];
}

export interface ApiResponse<T> {
  success: boolean;
  data: T;
  message?: string;
  errors?: Record<string, string>;
}
