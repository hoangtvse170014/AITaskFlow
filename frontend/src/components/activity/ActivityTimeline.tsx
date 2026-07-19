"use client";

import * as React from "react";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Avatar } from "@/components/ui/avatar";
import { Badge } from "@/components/ui/badge";
import {
  Activity,
  Plus,
  Pencil,
  Trash2,
  UserPlus,
  UserMinus,
  Shield,
  FolderPlus,
  CheckCircle,
  MessageSquare,
  Crown,
} from "lucide-react";
import { format, formatDistanceToNow } from "date-fns";
import { api } from "@/lib/api";
import { useWorkspaceStore } from "@/store/workspaceStore";

interface ActivityItem {
  id: string;
  action: string;
  entityType: string;
  entityId: string;
  user: {
    id: string;
    email: string;
    fullName: string;
    avatarUrl: string | null;
  };
  metadata: Record<string, any>;
  createdAt: string;
}

export function ActivityTimeline() {
  const { currentWorkspace } = useWorkspaceStore();
  const [activities, setActivities] = React.useState<ActivityItem[]>([]);
  const [isLoading, setIsLoading] = React.useState(true);

  React.useEffect(() => {
    if (currentWorkspace) {
      fetchActivities();
    }
  }, [currentWorkspace]);

  const fetchActivities = async () => {
    if (!currentWorkspace) return;
    setIsLoading(true);
    try {
      const response = await api.get(`/activity/workspace/${currentWorkspace.id}?limit=50`);
      if (response.data.success) {
        setActivities(response.data.data);
      }
    } catch (error) {
      console.error("Failed to fetch activities:", error);
    } finally {
      setIsLoading(false);
    }
  };

  const getActionIcon = (action: string, entityType: string) => {
    switch (action) {
      case "CREATED":
        if (entityType === "PROJECT") return <FolderPlus className="w-4 h-4 text-green-500" />;
        if (entityType === "TASK") return <Plus className="w-4 h-4 text-green-500" />;
        return <Plus className="w-4 h-4 text-green-500" />;
      case "UPDATED":
        return <Pencil className="w-4 h-4 text-blue-500" />;
      case "DELETED":
        return <Trash2 className="w-4 h-4 text-red-500" />;
      case "INVITED":
        return <UserPlus className="w-4 h-4 text-purple-500" />;
      case "REMOVED":
        return <UserMinus className="w-4 h-4 text-orange-500" />;
      case "ROLE_CHANGED":
        return <Shield className="w-4 h-4 text-indigo-500" />;
      case "COMPLETED":
        return <CheckCircle className="w-4 h-4 text-green-500" />;
      case "COMMENTED":
        return <MessageSquare className="w-4 h-4 text-yellow-500" />;
      default:
        return <Activity className="w-4 h-4 text-gray-500" />;
    }
  };

  const getActionText = (activity: ActivityItem) => {
    const { action, entityType, metadata } = activity;
    const userName = activity.user.fullName;

    switch (action) {
      case "CREATED":
        if (entityType === "WORKSPACE") return `${userName} created workspace`;
        if (entityType === "PROJECT") return `${userName} created project "${metadata?.projectName || 'Unknown'}"`;
        if (entityType === "TASK") return `${userName} created task "${metadata?.taskKey || 'Unknown'}"`;
        return `${userName} created ${entityType.toLowerCase()}`;
      case "UPDATED":
        if (entityType === "PROJECT") return `${userName} updated project "${metadata?.projectName || 'Unknown'}"`;
        if (entityType === "TASK") return `${userName} updated task "${metadata?.taskTitle || 'Unknown'}"`;
        return `${userName} updated ${entityType.toLowerCase()}`;
      case "DELETED":
        if (entityType === "TASK") return `${userName} deleted task "${metadata?.taskTitle || 'Unknown'}"`;
        return `${userName} deleted ${entityType.toLowerCase()}`;
      case "INVITED":
        return `${userName} invited ${metadata?.invitedEmail || 'someone'} as ${metadata?.role || 'member'}`;
      case "REMOVED":
        return `${userName} removed ${metadata?.memberEmail || 'someone'} from workspace`;
      case "ROLE_CHANGED":
        return `${userName} changed ${metadata?.memberEmail || 'member'}'s role from ${metadata?.oldRole || '?'} to ${metadata?.newRole || '?'}`;
      case "ASSIGNED":
        return `${userName} assigned task to ${metadata?.assigneeEmail || 'someone'}`;
      case "COMPLETED":
        return `${userName} completed task "${metadata?.taskTitle || 'Unknown'}"`;
      case "COMMENTED":
        return `${userName} commented on task`;
      default:
        return `${userName} performed ${action.toLowerCase()} on ${entityType.toLowerCase()}`;
    }
  };

  const getEntityBadge = (entityType: string) => {
    const colors: Record<string, string> = {
      WORKSPACE: "bg-purple-500/10 text-purple-500 border-purple-500/20",
      PROJECT: "bg-blue-500/10 text-blue-500 border-blue-500/20",
      TASK: "bg-green-500/10 text-green-500 border-green-500/20",
      MEMBER: "bg-orange-500/10 text-orange-500 border-orange-500/20",
      PAGE: "bg-yellow-500/10 text-yellow-500 border-yellow-500/20",
    };
    return colors[entityType] || "bg-gray-500/10 text-gray-500 border-gray-500/20";
  };

  if (isLoading) {
    return (
      <Card>
        <CardHeader>
          <CardTitle className="flex items-center gap-2">
            <Activity className="w-5 h-5" />
            Activity
          </CardTitle>
        </CardHeader>
        <CardContent>
          <div className="space-y-4">
            {[1, 2, 3, 4, 5].map((i) => (
              <div key={i} className="flex items-start gap-3 animate-pulse">
                <div className="w-8 h-8 rounded-full bg-muted" />
                <div className="flex-1 space-y-2">
                  <div className="h-4 bg-muted rounded w-3/4" />
                  <div className="h-3 bg-muted rounded w-1/4" />
                </div>
              </div>
            ))}
          </div>
        </CardContent>
      </Card>
    );
  }

  if (activities.length === 0) {
    return (
      <Card>
        <CardHeader>
          <CardTitle className="flex items-center gap-2">
            <Activity className="w-5 h-5" />
            Activity
          </CardTitle>
        </CardHeader>
        <CardContent>
          <div className="text-center py-8 text-muted-foreground">
            <Activity className="w-12 h-12 mx-auto mb-3 opacity-50" />
            <p>No activity yet</p>
            <p className="text-sm">Activity will appear here as you work</p>
          </div>
        </CardContent>
      </Card>
    );
  }

  return (
    <Card>
      <CardHeader>
        <CardTitle className="flex items-center gap-2">
          <Activity className="w-5 h-5" />
          Activity
        </CardTitle>
      </CardHeader>
      <CardContent>
        <div className="relative">
          <div className="absolute left-4 top-0 bottom-0 w-px bg-border" />
          <div className="space-y-4">
            {activities.map((activity, index) => (
              <div key={activity.id} className="relative flex items-start gap-3 pl-10">
                <div className="absolute left-2 -translate-x-1/2 w-6 h-6 rounded-full bg-background border-2 border-border flex items-center justify-center z-10">
                  {getActionIcon(activity.action, activity.entityType)}
                </div>
                <div className="flex-1 min-w-0">
                  <div className="flex items-center gap-2 flex-wrap">
                    <Avatar
                      src={activity.user.avatarUrl}
                      fallback={activity.user.fullName}
                      size="sm"
                    />
                    <span className="text-sm font-medium truncate">
                      {activity.user.fullName}
                    </span>
                    <Badge
                      variant="outline"
                      className={`text-xs ${getEntityBadge(activity.entityType)}`}
                    >
                      {activity.entityType}
                    </Badge>
                  </div>
                  <p className="text-sm text-muted-foreground mt-1">
                    {getActionText(activity)}
                  </p>
                  <p className="text-xs text-muted-foreground mt-1">
                    {formatDistanceToNow(new Date(activity.createdAt), { addSuffix: true })}
                  </p>
                </div>
              </div>
            ))}
          </div>
        </div>
      </CardContent>
    </Card>
  );
}
