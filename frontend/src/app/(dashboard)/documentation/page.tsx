"use client";

import * as React from "react";
import { useParams } from "next/navigation";
import { MainLayout } from "@/components/layout/MainLayout";
import { Header } from "@/components/layout/Header";
import { DocumentationPanel } from "@/components/ai/DocumentationPanel";
import { useWorkspaceStore } from "@/store/workspaceStore";
import { Sparkles, FileText } from "lucide-react";
import { Card, CardContent } from "@/components/ui/card";

export default function DocumentationPage() {
  const params = useParams();
  const { currentWorkspace } = useWorkspaceStore();
  const workspaceId =
    currentWorkspace?.id || (params.workspaceId as string) || "";

  return (
    <MainLayout>
      <Header title="AI Documentation" />
      <div className="p-6 space-y-6">
        <Card>
          <CardContent className="py-6">
            <div className="flex items-start gap-4">
              <div className="flex items-center justify-center w-12 h-12 rounded-xl bg-primary/10 text-primary">
                <Sparkles className="w-6 h-6" />
              </div>
              <div className="flex-1">
                <h1 className="text-xl font-semibold flex items-center gap-2">
                  <FileText className="w-5 h-5 text-muted-foreground" />
                  Generate project documentation with AI
                </h1>
                <p className="text-sm text-muted-foreground mt-1 leading-relaxed">
                  TaskFlow DocGen tự động tạo tài liệu chuyên nghiệp từ dữ liệu
                  workspace thực. Tất cả tài liệu được viết bằng Markdown, không
                  lưu vào cơ sở dữ liệu — bạn có thể sao chép, tải xuống hoặc
                  chỉnh sửa trước khi dùng.
                </p>
                <ul className="grid grid-cols-1 md:grid-cols-2 gap-x-6 gap-y-1 mt-3 text-xs text-muted-foreground">
                  <li>• Software Requirements Specification (SRS)</li>
                  <li>• User Stories</li>
                  <li>• Acceptance Criteria</li>
                  <li>• Meeting Minutes</li>
                  <li>• Sprint Review</li>
                  <li>• Sprint Retrospective</li>
                  <li>• Technical Specification</li>
                </ul>
              </div>
            </div>
          </CardContent>
        </Card>

        {workspaceId ? (
          <DocumentationPanel workspaceId={workspaceId} />
        ) : (
          <Card>
            <CardContent className="py-12 text-center text-muted-foreground">
              Vui lòng chọn workspace trước khi tạo tài liệu.
            </CardContent>
          </Card>
        )}
      </div>
    </MainLayout>
  );
}
