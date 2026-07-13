"use client";

import * as React from "react";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { Avatar } from "@/components/ui/avatar";
import { Button } from "@/components/ui/button";
import { Users, AlertTriangle, TrendingUp, CheckCircle } from "lucide-react";
import { api } from "@/lib/api";
import { useWorkspaceStore } from "@/store/workspaceStore";

interface WorkloadData {
  workspaceId: string;
  members: MemberData[];
  heatmap: {
    dates: string[];
    rows: HeatmapRow[];
  };
  summary: {
    averageWorkload: number;
    overloadedMembers: number;
    underutilizedMembers: number;
    balancedMembers: number;
  };
}

interface MemberData {
  memberId: string;
  memberName: string;
  email: string;
  avatarUrl: string;
  role: string;
  openTasks: number;
  inProgressTasks: number;
  completedTasks: number;
  workloadPercentage: number;
  status: string;
}

interface HeatmapRow {
  memberId: string;
  memberName: string;
  avatarUrl: string;
  values: number[];
  statuses: string[];
}

export function WorkloadView() {
  const { currentWorkspace } = useWorkspaceStore();
  const [data, setData] = React.useState<WorkloadData | null>(null);
  const [isLoading, setIsLoading] = React.useState(true);

  React.useEffect(() => {
    if (currentWorkspace) {
      fetchWorkload();
    }
  }, [currentWorkspace]);

  const fetchWorkload = async () => {
    if (!currentWorkspace) return;
    setIsLoading(true);
    try {
      const response = await api.get<{ success: boolean; data: WorkloadData }>(
        `/workloads/workspace/${currentWorkspace.id}`
      );
      if (response.data.success) {
        setData(response.data.data);
      }
    } catch (error) {
      console.error("Failed to fetch workload:", error);
    } finally {
      setIsLoading(false);
    }
  };

  const recalculateWorkload = async () => {
    if (!currentWorkspace) return;
    try {
      await api.post(`/workloads/workspace/${currentWorkspace.id}/recalculate`);
      fetchWorkload();
    } catch (error) {
      console.error("Failed to recalculate workload:", error);
    }
  };

  const getStatusColor = (status: string) => {
    switch (status) {
      case "OVERLOADED":
        return "bg-red-500/20 text-red-600 border-red-500/30";
      case "BALANCED":
        return "bg-green-500/20 text-green-600 border-green-500/30";
      case "UNDERUTILIZED":
        return "bg-yellow-500/20 text-yellow-600 border-yellow-500/30";
      default:
        return "bg-gray-500/20 text-gray-600 border-gray-500/30";
    }
  };

  const getHeatmapColor = (value: number) => {
    if (value >= 80) return "bg-red-500";
    if (value >= 50) return "bg-orange-400";
    if (value >= 25) return "bg-yellow-400";
    return "bg-green-400";
  };

  if (isLoading) {
    return (
      <Card>
        <CardHeader>
          <CardTitle className="flex items-center gap-2">
            <Users className="w-5 h-5" />
            Team Workload
          </CardTitle>
        </CardHeader>
        <CardContent>
          <div className="animate-pulse space-y-4">
            {[1, 2, 3].map((i) => (
              <div key={i} className="flex items-center gap-4">
                <div className="w-10 h-10 bg-muted rounded-full" />
                <div className="flex-1 space-y-2">
                  <div className="h-4 bg-muted rounded w-1/3" />
                  <div className="h-2 bg-muted rounded" />
                </div>
              </div>
            ))}
          </div>
        </CardContent>
      </Card>
    );
  }

  if (!data) {
    return (
      <Card>
        <CardContent className="py-12 text-center text-muted-foreground">
          <p>Failed to load workload data</p>
        </CardContent>
      </Card>
    );
  }

  return (
    <div className="space-y-6">
      {/* Summary Cards */}
      <div className="grid gap-4 md:grid-cols-4">
        <Card>
          <CardContent className="p-4">
            <div className="flex items-center gap-3">
              <div className="p-2 rounded-lg bg-blue-500/10">
                <Users className="w-5 h-5 text-blue-500" />
              </div>
              <div>
                <p className="text-2xl font-bold">{data.members.length}</p>
                <p className="text-xs text-muted-foreground">Team Members</p>
              </div>
            </div>
          </CardContent>
        </Card>

        <Card>
          <CardContent className="p-4">
            <div className="flex items-center gap-3">
              <div className="p-2 rounded-lg bg-red-500/10">
                <AlertTriangle className="w-5 h-5 text-red-500" />
              </div>
              <div>
                <p className="text-2xl font-bold">{data.summary.overloadedMembers}</p>
                <p className="text-xs text-muted-foreground">Overloaded</p>
              </div>
            </div>
          </CardContent>
        </Card>

        <Card>
          <CardContent className="p-4">
            <div className="flex items-center gap-3">
              <div className="p-2 rounded-lg bg-green-500/10">
                <TrendingUp className="w-5 h-5 text-green-500" />
              </div>
              <div>
                <p className="text-2xl font-bold">{data.summary.balancedMembers}</p>
                <p className="text-xs text-muted-foreground">Balanced</p>
              </div>
            </div>
          </CardContent>
        </Card>

        <Card>
          <CardContent className="p-4">
            <div className="flex items-center gap-3">
              <div className="p-2 rounded-lg bg-yellow-500/10">
                <CheckCircle className="w-5 h-5 text-yellow-500" />
              </div>
              <div>
                <p className="text-2xl font-bold">{data.summary.underutilizedMembers}</p>
                <p className="text-xs text-muted-foreground">Underutilized</p>
              </div>
            </div>
          </CardContent>
        </Card>
      </div>

      {/* Heatmap */}
      <Card>
        <CardHeader className="flex flex-row items-center justify-between">
          <CardTitle className="flex items-center gap-2">
            <Users className="w-5 h-5" />
            Workload Heatmap
          </CardTitle>
          <Button variant="outline" size="sm" onClick={recalculateWorkload}>
            Recalculate
          </Button>
        </CardHeader>
        <CardContent>
          <div className="overflow-x-auto">
            <table className="w-full">
              <thead>
                <tr>
                  <th className="text-left p-2 text-sm font-medium">Member</th>
                  {data.heatmap.dates.map((date) => (
                    <th key={date} className="p-2 text-xs font-medium text-muted-foreground">
                      {new Date(date).toLocaleDateString("en-US", { weekday: "short" })}
                    </th>
                  ))}
                  <th className="p-2 text-sm font-medium">Status</th>
                </tr>
              </thead>
              <tbody>
                {data.heatmap.rows.map((row) => (
                  <tr key={row.memberId} className="border-t">
                    <td className="p-2">
                      <div className="flex items-center gap-2">
                        <Avatar src={row.avatarUrl} fallback={row.memberName} size="sm" />
                        <span className="text-sm font-medium truncate">{row.memberName}</span>
                      </div>
                    </td>
                    {row.values.map((value, index) => (
                      <td key={index} className="p-1">
                        <div
                          className={`w-8 h-8 rounded ${getHeatmapColor(value)}`}
                          title={`${value}% workload`}
                        />
                      </td>
                    ))}
                    <td className="p-2">
                      <Badge className={getStatusColor(row.statuses[row.statuses.length - 1]?.toUpperCase() || "BALANCED")}>
                        {row.statuses[row.statuses.length - 1] === "overloaded" ? "High" :
                         row.statuses[row.statuses.length - 1] === "balanced" ? "Balanced" : "Light"}
                      </Badge>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>

          {/* Legend */}
          <div className="flex items-center gap-4 mt-4 text-xs text-muted-foreground">
            <span>Workload:</span>
            <div className="flex items-center gap-1">
              <div className="w-4 h-4 rounded bg-green-400" />
              <span>Light</span>
            </div>
            <div className="flex items-center gap-1">
              <div className="w-4 h-4 rounded bg-yellow-400" />
              <span>Medium</span>
            </div>
            <div className="flex items-center gap-1">
              <div className="w-4 h-4 rounded bg-orange-400" />
              <span>High</span>
            </div>
            <div className="flex items-center gap-1">
              <div className="w-4 h-4 rounded bg-red-500" />
              <span>Overloaded</span>
            </div>
          </div>
        </CardContent>
      </Card>

      {/* Member List */}
      <Card>
        <CardHeader>
          <CardTitle>Team Members</CardTitle>
        </CardHeader>
        <CardContent>
          <div className="space-y-4">
            {data.members.map((member) => (
              <div
                key={member.memberId}
                className="flex items-center gap-4 p-4 rounded-lg border"
              >
                <Avatar src={member.avatarUrl} fallback={member.memberName} size="lg" />
                <div className="flex-1 min-w-0">
                  <div className="flex items-center gap-2">
                    <p className="font-medium">{member.memberName}</p>
                    <Badge variant="outline" className="text-xs">
                      {member.role}
                    </Badge>
                  </div>
                  <p className="text-sm text-muted-foreground">{member.email}</p>
                </div>
                <div className="flex items-center gap-6">
                  <div className="text-center">
                    <p className="text-2xl font-bold">{member.openTasks}</p>
                    <p className="text-xs text-muted-foreground">Open</p>
                  </div>
                  <div className="text-center">
                    <p className="text-2xl font-bold">{member.inProgressTasks}</p>
                    <p className="text-xs text-muted-foreground">In Progress</p>
                  </div>
                  <div className="text-center">
                    <p className="text-2xl font-bold">{member.completedTasks}</p>
                    <p className="text-xs text-muted-foreground">Done</p>
                  </div>
                  <Badge className={`${getStatusColor(member.status)} border`}>
                    {member.status.replace("_", " ")}
                  </Badge>
                </div>
              </div>
            ))}
          </div>
        </CardContent>
      </Card>
    </div>
  );
}
