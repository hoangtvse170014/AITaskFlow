"use client";

import * as React from "react";
import { Download, FileText, BarChart3, PieChart, Loader2 } from "lucide-react";
import { Button } from "@/components/ui/button";
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogTrigger,
} from "@/components/ui/dialog";
import { taskApi } from "@/lib/api";
import type { Task, TaskStatus, TaskPriority } from "@/types";

interface ProjectReportProps {
  projectId: string;
  projectName: string;
}

interface ReportData {
  totalTasks: number;
  completedTasks: number;
  inProgressTasks: number;
  reviewTasks: number;
  todoTasks: number;
  overdueTasks: number;
  tasksByPriority: Record<string, number>;
  completionRate: number;
}

export function ProjectReport({ projectId, projectName }: ProjectReportProps) {
  const [isOpen, setIsOpen] = React.useState(false);
  const [isLoading, setIsLoading] = React.useState(false);
  const [reportData, setReportData] = React.useState<ReportData | null>(null);
  const [tasks, setTasks] = React.useState<Task[]>([]);

  React.useEffect(() => {
    if (isOpen && !reportData) {
      generateReport();
    }
  }, [isOpen]);

  const generateReport = async () => {
    setIsLoading(true);
    try {
      const response = await taskApi.getAll(projectId);
      if (response.data.success && response.data.data) {
        const allTasks = Object.values(response.data.data).flat() as Task[];
        setTasks(allTasks);
        
        const completed = allTasks.filter((t) => t.status === "DONE").length;
        const inProgress = allTasks.filter((t) => t.status === "IN_PROGRESS").length;
        const review = allTasks.filter((t) => t.status === "REVIEW").length;
        const todo = allTasks.filter((t) => t.status === "TODO").length;
        const overdue = allTasks.filter((t) => t.overdue).length;

        const priorityCount: Record<string, number> = {};
        allTasks.forEach((t) => {
          priorityCount[t.priority] = (priorityCount[t.priority] || 0) + 1;
        });

        setReportData({
          totalTasks: allTasks.length,
          completedTasks: completed,
          inProgressTasks: inProgress,
          reviewTasks: review,
          todoTasks: todo,
          overdueTasks: overdue,
          tasksByPriority: priorityCount,
          completionRate: allTasks.length > 0 ? Math.round((completed / allTasks.length) * 100) : 0,
        });
      }
    } catch (error) {
      console.error("Failed to generate report:", error);
    } finally {
      setIsLoading(false);
    }
  };

  const exportToCSV = () => {
    if (!tasks.length) return;

    const headers = ["Task Key", "Title", "Status", "Priority", "Assignee", "Due Date", "Overdue"];
    const rows = tasks.map((t) => [
      t.taskKey,
      `"${t.title}"`,
      t.status,
      t.priority,
      t.assignee?.fullName || "Unassigned",
      t.dueDate || "",
      t.overdue ? "Yes" : "No",
    ]);

    const csv = [headers.join(","), ...rows.map((r) => r.join(","))].join("\n");
    const blob = new Blob([csv], { type: "text/csv" });
    const url = URL.createObjectURL(blob);
    const a = document.createElement("a");
    a.href = url;
    a.download = `${projectName}-report-${new Date().toISOString().split("T")[0]}.csv`;
    a.click();
    URL.revokeObjectURL(url);
  };

  const getPriorityColor = (priority: string) => {
    switch (priority) {
      case "LOW": return "bg-green-500";
      case "MEDIUM": return "bg-yellow-500";
      case "HIGH": return "bg-orange-500";
      case "URGENT": return "bg-red-500";
      default: return "bg-gray-500";
    }
  };

  return (
    <Dialog open={isOpen} onOpenChange={setIsOpen}>
      <DialogTrigger asChild>
        <Button variant="outline" size="sm">
          <FileText className="w-4 h-4 mr-2" />
          Export Report
        </Button>
      </DialogTrigger>
      <DialogContent className="max-w-2xl">
        <DialogHeader>
          <DialogTitle>Project Report: {projectName}</DialogTitle>
        </DialogHeader>

        {isLoading ? (
          <div className="flex items-center justify-center py-12">
            <Loader2 className="w-8 h-8 animate-spin text-muted-foreground" />
          </div>
        ) : reportData ? (
          <div className="space-y-6">
            {/* Summary Stats */}
            <div className="grid grid-cols-4 gap-4">
              <div className="bg-muted rounded-lg p-4 text-center">
                <p className="text-3xl font-bold">{reportData.totalTasks}</p>
                <p className="text-sm text-muted-foreground">Total Tasks</p>
              </div>
              <div className="bg-green-500/10 rounded-lg p-4 text-center">
                <p className="text-3xl font-bold text-green-600">{reportData.completedTasks}</p>
                <p className="text-sm text-muted-foreground">Completed</p>
              </div>
              <div className="bg-blue-500/10 rounded-lg p-4 text-center">
                <p className="text-3xl font-bold text-blue-600">{reportData.inProgressTasks}</p>
                <p className="text-sm text-muted-foreground">In Progress</p>
              </div>
              <div className="bg-red-500/10 rounded-lg p-4 text-center">
                <p className="text-3xl font-bold text-red-600">{reportData.overdueTasks}</p>
                <p className="text-sm text-muted-foreground">Overdue</p>
              </div>
            </div>

            {/* Completion Rate */}
            <div className="space-y-2">
              <div className="flex items-center justify-between">
                <h3 className="font-semibold">Completion Progress</h3>
                <span className="text-2xl font-bold">{reportData.completionRate}%</span>
              </div>
              <div className="h-4 bg-muted rounded-full overflow-hidden">
                <div
                  className="h-full bg-green-500 rounded-full transition-all"
                  style={{ width: `${reportData.completionRate}%` }}
                />
              </div>
            </div>

            {/* Tasks by Priority */}
            <div className="space-y-3">
              <h3 className="font-semibold">Tasks by Priority</h3>
              <div className="space-y-2">
                {["URGENT", "HIGH", "MEDIUM", "LOW"].map((priority) => {
                  const count = reportData.tasksByPriority[priority] || 0;
                  const percentage = reportData.totalTasks > 0 ? (count / reportData.totalTasks) * 100 : 0;
                  return (
                    <div key={priority} className="flex items-center gap-3">
                      <div className={`w-3 h-3 rounded-full ${getPriorityColor(priority)}`} />
                      <span className="w-20 text-sm">{priority}</span>
                      <div className="flex-1 h-2 bg-muted rounded-full overflow-hidden">
                        <div
                          className={`h-full ${getPriorityColor(priority)} rounded-full`}
                          style={{ width: `${percentage}%` }}
                        />
                      </div>
                      <span className="w-8 text-sm text-right">{count}</span>
                    </div>
                  );
                })}
              </div>
            </div>

            {/* Tasks by Status */}
            <div className="space-y-3">
              <h3 className="font-semibold">Tasks by Status</h3>
              <div className="grid grid-cols-4 gap-3">
                <div className="border rounded-lg p-3">
                  <p className="text-2xl font-bold">{reportData.todoTasks}</p>
                  <p className="text-sm text-muted-foreground">To Do</p>
                </div>
                <div className="border rounded-lg p-3">
                  <p className="text-2xl font-bold">{reportData.inProgressTasks}</p>
                  <p className="text-sm text-muted-foreground">In Progress</p>
                </div>
                <div className="border rounded-lg p-3">
                  <p className="text-2xl font-bold">{reportData.reviewTasks}</p>
                  <p className="text-sm text-muted-foreground">Review</p>
                </div>
                <div className="border rounded-lg p-3">
                  <p className="text-2xl font-bold">{reportData.completedTasks}</p>
                  <p className="text-sm text-muted-foreground">Done</p>
                </div>
              </div>
            </div>

            {/* Export Button */}
            <div className="flex justify-end">
              <Button onClick={exportToCSV}>
                <Download className="w-4 h-4 mr-2" />
                Export to CSV
              </Button>
            </div>
          </div>
        ) : null}
      </DialogContent>
    </Dialog>
  );
}
