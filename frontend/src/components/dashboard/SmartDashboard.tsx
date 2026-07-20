"use client";

import * as React from "react";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { Avatar } from "@/components/ui/avatar";
import {
  CheckSquare,
  Clock,
  AlertTriangle,
  TrendingUp,
  Users,
  Zap,
  ArrowRight,
  CheckCircle,
  Circle,
  XCircle,
} from "lucide-react";
import { api } from "@/lib/api";
import { useWorkspaceStore } from "@/store/workspaceStore";
import { format, formatDistanceToNow } from "date-fns";
import Link from "next/link";
import { QuickActions } from "./QuickActions";

interface SmartDashboardData {
  myTasks: {
    todo: Task[];
    inProgress: Task[];
    review: Task[];
    totalCount: number;
  };
  overdueTasks: Task[];
  upcomingDeadlines: Deadline[];
  recentActivity: Activity[];
  workloadChart: {
    members: MemberWorkload[];
    averageLoad: number;
  };
  aiSuggestions: AiSuggestion[];
  teamPerformance: TeamPerformance;
  stats: DashboardStats;
}

interface Task {
  id: string;
  title: string;
  status: string;
  priority: string;
  projectName: string;
  projectKey: string;
  taskKey: string;
  dueDate: string;
  assigneeName: string;
  overdue: boolean;
}

interface Deadline {
  taskId: string;
  taskKey: string;
  title: string;
  dueDate: string;
  daysLeft: number;
  priority: string;
}

interface Activity {
  userId: string;
  userName: string;
  userAvatar: string;
  action: string;
  entityType: string;
  entityId: string;
  description: string;
  timestamp: string;
}

interface MemberWorkload {
  memberId: string;
  memberName: string;
  avatarUrl: string;
  openTasks: number;
  completedTasks: number;
  loadPercentage: number;
  status: string;
}

interface AiSuggestion {
  id: string;
  type: string;
  title: string;
  description: string;
  priority: string;
  actionLabel: string;
}

interface TeamPerformance {
  completionRate: number;
  averageTasksPerMember: number;
  productivityScore: number;
  activeMembers: number;
  newTasksThisWeek: number;
  completedTasksThisWeek: number;
  dailyActivity: { date: string; tasksCreated: number; tasksCompleted: number }[];
}

interface DashboardStats {
  totalTasks: number;
  completedTasks: number;
  inProgressTasks: number;
  todoTasks: number;
  delayedTasks: number;
  completionRate: number;
  overdueCount: number;
  tasksByPriority: Record<string, number>;
  tasksByStatus: Record<string, number>;
}

const priorityColors: Record<string, string> = {
  LOW: "bg-gray-100 text-gray-600",
  MEDIUM: "bg-blue-100 text-blue-600",
  HIGH: "bg-orange-100 text-orange-600",
  URGENT: "bg-red-100 text-red-600",
};

const statusIcons: Record<string, React.ReactNode> = {
  TODO: <Circle className="w-4 h-4" />,
  IN_PROGRESS: <TrendingUp className="w-4 h-4 text-blue-500" />,
  REVIEW: <Clock className="w-4 h-4 text-yellow-500" />,
  DONE: <CheckCircle className="w-4 h-4 text-green-500" />,
};

export function SmartDashboard() {
  const { currentWorkspace } = useWorkspaceStore();
  const [data, setData] = React.useState<SmartDashboardData | null>(null);
  const [isLoading, setIsLoading] = React.useState(true);

  React.useEffect(() => {
    if (currentWorkspace) {
      fetchDashboard();
    }
  }, [currentWorkspace]);

  const fetchDashboard = async () => {
    if (!currentWorkspace) return;
    setIsLoading(true);
    try {
      const response = await api.get<{ success: boolean; data: SmartDashboardData }>(
        `/dashboard/smart?workspaceId=${currentWorkspace.id}`
      );
      if (response.data.success) {
        setData(response.data.data);
      }
    } catch (error) {
      console.error("Failed to fetch dashboard:", error);
    } finally {
      setIsLoading(false);
    }
  };

  if (isLoading) {
    return (
      <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-4">
        {[1, 2, 3, 4].map((i) => (
          <Card key={i} className="animate-pulse">
            <CardHeader className="pb-2">
              <div className="h-4 bg-muted rounded w-1/2" />
            </CardHeader>
            <CardContent>
              <div className="h-8 bg-muted rounded w-3/4" />
            </CardContent>
          </Card>
        ))}
      </div>
    );
  }

  if (!data) {
    return (
      <Card>
        <CardContent className="py-12 text-center text-muted-foreground">
          <p>Failed to load dashboard data</p>
        </CardContent>
      </Card>
    );
  }

  return (
    <div className="space-y-6">
      {/* Stats Cards */}
      <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-4">
        <Card>
          <CardHeader className="flex flex-row items-center justify-between pb-2">
            <CardTitle className="text-sm font-medium text-muted-foreground">
              Total Tasks
            </CardTitle>
            <CheckSquare className="w-4 h-4 text-muted-foreground" />
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold">{data.stats.totalTasks}</div>
            <p className="text-xs text-muted-foreground">
              {data.stats.completedTasks} completed
            </p>
          </CardContent>
        </Card>

        <Card>
          <CardHeader className="flex flex-row items-center justify-between pb-2">
            <CardTitle className="text-sm font-medium text-muted-foreground">
              In Progress
            </CardTitle>
            <TrendingUp className="w-4 h-4 text-blue-500" />
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold">{data.stats.inProgressTasks}</div>
            <p className="text-xs text-muted-foreground">
              {Math.round(data.stats.completionRate)}% completion rate
            </p>
          </CardContent>
        </Card>

        <Card>
          <CardHeader className="flex flex-row items-center justify-between pb-2">
            <CardTitle className="text-sm font-medium text-muted-foreground">
              Overdue
            </CardTitle>
            <AlertTriangle className="w-4 h-4 text-red-500" />
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold text-red-500">{data.stats.overdueCount}</div>
            <p className="text-xs text-muted-foreground">Tasks past deadline</p>
          </CardContent>
        </Card>

        <Card>
          <CardHeader className="flex flex-row items-center justify-between pb-2">
            <CardTitle className="text-sm font-medium text-muted-foreground">
              Team Performance
            </CardTitle>
            <Users className="w-4 h-4 text-muted-foreground" />
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold">{data.teamPerformance.productivityScore}%</div>
            <p className="text-xs text-muted-foreground">
              {data.teamPerformance.activeMembers} active members
            </p>
          </CardContent>
        </Card>
      </div>

      {/* Quick Actions */}
      <QuickActions />

      <div className="grid gap-6 lg:grid-cols-2">
        {/* My Tasks */}
        <Card>
          <CardHeader>
            <CardTitle className="flex items-center gap-2">
              <CheckSquare className="w-5 h-5" />
              My Tasks
              <Badge variant="secondary">{data.myTasks.totalCount}</Badge>
            </CardTitle>
          </CardHeader>
          <CardContent>
            <div className="space-y-4">
              {["TODO", "IN_PROGRESS", "REVIEW"].map((status) => {
                const tasks = status === "TODO" ? data.myTasks.todo : 
                             status === "IN_PROGRESS" ? data.myTasks.inProgress : 
                             data.myTasks.review;
                return (
                  <div key={status}>
                    <div className="flex items-center gap-2 mb-2">
                      {statusIcons[status]}
                      <span className="text-sm font-medium capitalize">
                        {status.replace("_", " ")} ({tasks.length})
                      </span>
                    </div>
                    {tasks.slice(0, 3).map((task) => (
                      <div key={task.id} className="pl-6 space-y-1">
                        <Link
                          href={`/projects/${task.id}`}
                          className="block text-sm hover:text-primary"
                        >
                          <span className="text-muted-foreground">{task.taskKey}</span> {task.title}
                        </Link>
                      </div>
                    ))}
                    {tasks.length > 3 && (
                      <p className="pl-6 text-xs text-muted-foreground">
                        +{tasks.length - 3} more
                      </p>
                    )}
                  </div>
                );
              })}
            </div>
          </CardContent>
        </Card>

        {/* AI Suggestions */}
        <Card>
          <CardHeader>
            <CardTitle className="flex items-center gap-2">
              <Zap className="w-5 h-5 text-yellow-500" />
              AI Suggestions
            </CardTitle>
          </CardHeader>
          <CardContent>
            {data.aiSuggestions.length === 0 ? (
              <p className="text-sm text-muted-foreground text-center py-4">
                No suggestions at the moment
              </p>
            ) : (
              <div className="space-y-3">
                {data.aiSuggestions.map((suggestion) => (
                  <div
                    key={suggestion.id}
                    className="flex items-start gap-3 p-3 rounded-lg bg-accent/50"
                  >
                    <div className="flex-1">
                      <p className="text-sm font-medium">{suggestion.title}</p>
                      <p className="text-xs text-muted-foreground mt-1">
                        {suggestion.description}
                      </p>
                    </div>
                    <Badge
                      variant={
                        suggestion.priority === "HIGH" ? "destructive" :
                        suggestion.priority === "MEDIUM" ? "default" : "secondary"
                      }
                      className="shrink-0"
                    >
                      {suggestion.priority}
                    </Badge>
                  </div>
                ))}
              </div>
            )}
          </CardContent>
        </Card>
      </div>

      <div className="grid gap-6 lg:grid-cols-3">
        {/* Workload */}
        <Card>
          <CardHeader>
            <CardTitle className="flex items-center gap-2">
              <Users className="w-5 h-5" />
              Team Workload
            </CardTitle>
          </CardHeader>
          <CardContent>
            <div className="space-y-4">
              {data.workloadChart.members.slice(0, 5).map((member) => (
                <div key={member.memberId} className="flex items-center gap-3">
                  <Avatar src={member.avatarUrl} fallback={member.memberName} size="sm" />
                  <div className="flex-1 min-w-0">
                    <div className="flex items-center justify-between">
                      <p className="text-sm font-medium truncate">{member.memberName}</p>
                      <span className="text-xs text-muted-foreground">{member.openTasks} tasks</span>
                    </div>
                    <div className="mt-1 h-2 bg-muted rounded-full overflow-hidden">
                      <div
                        className={`h-full rounded-full ${
                          member.status === "OVERLOADED" ? "bg-red-500" :
                          member.status === "BALANCED" ? "bg-green-500" : "bg-yellow-500"
                        }`}
                        style={{ width: `${Math.min(member.loadPercentage, 100)}%` }}
                      />
                    </div>
                  </div>
                </div>
              ))}
            </div>
          </CardContent>
        </Card>

        {/* Upcoming Deadlines */}
        <Card>
          <CardHeader>
            <CardTitle className="flex items-center gap-2">
              <Clock className="w-5 h-5" />
              Upcoming Deadlines
            </CardTitle>
          </CardHeader>
          <CardContent>
            {data.upcomingDeadlines.length === 0 ? (
              <p className="text-sm text-muted-foreground text-center py-4">
                No upcoming deadlines
              </p>
            ) : (
              <div className="space-y-3">
                {data.upcomingDeadlines.slice(0, 5).map((deadline) => (
                  <div key={deadline.taskId} className="flex items-center gap-3">
                    <div className="flex-1 min-w-0">
                      <p className="text-sm truncate">{deadline.title}</p>
                      <p className="text-xs text-muted-foreground">{deadline.taskKey}</p>
                    </div>
                    <Badge
                      variant={deadline.daysLeft <= 1 ? "destructive" : "secondary"}
                    >
                      {deadline.daysLeft === 0 ? "Today" : 
                       deadline.daysLeft === 1 ? "Tomorrow" :
                       `${deadline.daysLeft} days`}
                    </Badge>
                  </div>
                ))}
              </div>
            )}
          </CardContent>
        </Card>

        {/* Recent Activity */}
        <Card>
          <CardHeader>
            <CardTitle className="flex items-center gap-2">
              <TrendingUp className="w-5 h-5" />
              Recent Activity
            </CardTitle>
          </CardHeader>
          <CardContent>
            {data.recentActivity.length === 0 ? (
              <p className="text-sm text-muted-foreground text-center py-4">
                No recent activity
              </p>
            ) : (
              <div className="space-y-3">
                {data.recentActivity.slice(0, 5).map((activity, index) => (
                  <div key={index} className="flex items-start gap-3">
                    <Avatar src={activity.userAvatar} fallback={activity.userName} size="sm" />
                    <div className="flex-1 min-w-0">
                      <p className="text-sm">{activity.description}</p>
                      <p className="text-xs text-muted-foreground">
                        {formatDistanceToNow(new Date(activity.timestamp), { addSuffix: true })}
                      </p>
                    </div>
                  </div>
                ))}
              </div>
            )}
          </CardContent>
        </Card>
      </div>
    </div>
  );
}
