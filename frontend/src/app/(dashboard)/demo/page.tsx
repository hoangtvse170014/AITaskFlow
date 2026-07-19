"use client";

import * as React from "react";
import { useParams } from "next/navigation";
import { MainLayout } from "@/components/layout/MainLayout";
import { Header } from "@/components/layout/Header";
import { DemoModePanel } from "@/components/ai/DemoModePanel";
import { useWorkspaceStore } from "@/store/workspaceStore";
import { Sparkles, Wand2, Calendar, Users, ListChecks, AlertTriangle } from "lucide-react";
import { Card, CardContent } from "@/components/ui/card";

const CAPABILITIES = [
  {
    icon: Wand2,
    title: "Phân tích yêu cầu",
    description: "AI đọc ý tưởng và tạo description, scope, milestones.",
  },
  {
    icon: Calendar,
    title: "Lập kế hoạch sprint",
    description: "Tự động tạo 4 sprint với capacity và timeline rõ ràng.",
  },
  {
    icon: ListChecks,
    title: "Sinh 35-50 tasks",
    description: "Mỗi task có description, AC, risks, story points, estimate.",
  },
  {
    icon: Users,
    title: "Phân công thành viên",
    description: "Cân bằng workload, đảm bảo đúng chuyên môn cho từng task.",
  },
  {
    icon: AlertTriangle,
    title: "Sinh risks + workload",
    description: "Tổng hợp risks từ dependencies và capacity của team.",
  },
];

export default function DemoPage() {
  const params = useParams();
  const { currentWorkspace } = useWorkspaceStore();
  const workspaceId =
    currentWorkspace?.id || (params.workspaceId as string) || "";

  return (
    <MainLayout>
      <Header title="Demo Mode" />
      <div className="p-6 space-y-6">
        <Card>
          <CardContent className="py-6">
            <div className="flex items-start gap-4">
              <div className="flex items-center justify-center w-12 h-12 rounded-xl bg-gradient-to-br from-primary/15 to-violet-500/15 text-primary">
                <Sparkles className="w-6 h-6" />
              </div>
              <div className="flex-1">
                <h1 className="text-xl font-semibold">
                  Tạo dự án từ một ý tưởng — chỉ với 1 cú click
                </h1>
                <p className="text-sm text-muted-foreground mt-1 leading-relaxed">
                  Demo Mode chạy toàn bộ pipeline AI: phân tích yêu cầu, lên
                  sprint, sinh task &amp; subtask, phân công thành viên, tính
                  workload, sinh risks, tạo timeline, và refresh dashboard —
                  hoàn toàn tự động. Chỉ cần nhập ý tưởng dự án, AI sẽ làm phần
                  còn lại.
                </p>
              </div>
            </div>
          </CardContent>
        </Card>

        <div className="grid grid-cols-1 md:grid-cols-3 lg:grid-cols-5 gap-3">
          {CAPABILITIES.map((c) => {
            const Icon = c.icon;
            return (
              <div
                key={c.title}
                className="rounded-lg border bg-card p-3 flex flex-col gap-1"
              >
                <Icon className="h-4 w-4 text-primary" />
                <p className="text-sm font-medium">{c.title}</p>
                <p className="text-xs text-muted-foreground leading-snug">
                  {c.description}
                </p>
              </div>
            );
          })}
        </div>

        {workspaceId ? (
          <DemoModePanel workspaceId={workspaceId} />
        ) : (
          <Card>
            <CardContent className="py-12 text-center text-muted-foreground">
              Vui lòng chọn workspace trước khi chạy Demo Mode.
            </CardContent>
          </Card>
        )}
      </div>
    </MainLayout>
  );
}