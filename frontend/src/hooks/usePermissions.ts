import { useMemo } from 'react';
import { useWorkspaceStore } from '@/store/workspaceStore';
import { hasPermission, hasAnyPermission, type WorkspaceRole } from '@/types';

export function usePermissions() {
  const { currentWorkspace } = useWorkspaceStore();
  const role = currentWorkspace?.role as WorkspaceRole | undefined;

  return useMemo(() => ({
    role,

    can: (permission: string) => {
      if (!role) return false;
      return hasPermission(role, permission);
    },

    canAny: (permissions: string[]) => {
      if (!role) return false;
      return hasAnyPermission(role, permissions);
    },

    canViewWorkspace: () => hasPermission(role!, 'workspace:view'),
    canEditWorkspace: () => hasPermission(role!, 'workspace:edit'),
    canDeleteWorkspace: () => hasPermission(role!, 'workspace:delete'),
    canManageWorkspace: () => hasPermission(role!, 'workspace:manage'),

    canViewMembers: () => hasPermission(role!, 'member:view'),
    canInviteMembers: () => hasPermission(role!, 'member:invite'),
    canManageMembers: () => hasPermission(role!, 'member:manage'),
    canRemoveMembers: () => hasPermission(role!, 'member:remove'),

    canViewProjects: () => hasPermission(role!, 'project:view'),
    canCreateProjects: () => hasPermission(role!, 'project:create'),
    canEditProjects: () => hasPermission(role!, 'project:edit'),
    canDeleteProjects: () => hasPermission(role!, 'project:delete'),
    canManageProjects: () => hasPermission(role!, 'project:manage'),

    canViewTasks: () => hasPermission(role!, 'task:view'),
    canCreateTasks: () => hasPermission(role!, 'task:create'),
    canEditTasks: () => hasPermission(role!, 'task:edit'),
    canDeleteTasks: () => hasPermission(role!, 'task:delete'),
    canAssignTasks: () => hasPermission(role!, 'task:assign'),

    canViewPages: () => hasPermission(role!, 'page:view'),
    canCreatePages: () => hasPermission(role!, 'page:create'),
    canEditPages: () => hasPermission(role!, 'page:edit'),
    canDeletePages: () => hasPermission(role!, 'page:delete'),
    canArchivePages: () => hasPermission(role!, 'page:archive'),

    canViewSettings: () => hasPermission(role!, 'settings:view'),
    canEditSettings: () => hasPermission(role!, 'settings:edit'),

    canViewBilling: () => hasPermission(role!, 'billing:view'),
    canManageBilling: () => hasPermission(role!, 'billing:manage'),

    isOwner: () => role === 'OWNER',
    isAdmin: () => role === 'OWNER' || role === 'ADMIN',
    isManager: () => role === 'OWNER' || role === 'ADMIN' || role === 'MANAGER',
  }), [role]);
}
