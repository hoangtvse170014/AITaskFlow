"use client";

import * as React from "react";
import { useParams, useRouter } from "next/navigation";
import { MainLayout } from "@/components/layout/MainLayout";
import { Header } from "@/components/layout/Header";
import { SmartDashboard } from "@/components/dashboard/SmartDashboard";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { useWorkspaceStore } from "@/store/workspaceStore";
import { useEffect } from "react";

export default function WorkspaceDashboardPage() {
  const params = useParams();
  const router = useRouter();
  const workspaceId = params.workspaceId as string;
  const { setCurrentWorkspace, workspaces, fetchWorkspaces } = useWorkspaceStore();

  useEffect(() => {
    if (workspaceId) {
      setCurrentWorkspace(workspaceId);
    }
  }, [workspaceId, setCurrentWorkspace]);

  // If workspaces loaded but current workspace not found, redirect
  React.useEffect(() => {
    if (workspaces.length > 0 && workspaceId) {
      const found = workspaces.find(w => w.id === workspaceId);
      if (!found) {
        // Workspace not found, redirect to general dashboard
        router.push("/dashboard");
      }
    }
  }, [workspaces, workspaceId, router]);

  return (
    <MainLayout>
      <Header
        title="Dashboard"
      />
      <div className="p-6">
        <Tabs defaultValue="smart" className="w-full">
          <TabsList className="mb-6">
            <TabsTrigger value="smart">Smart Dashboard</TabsTrigger>
            <TabsTrigger value="overview">Overview</TabsTrigger>
          </TabsList>
          <TabsContent value="smart">
            <SmartDashboard />
          </TabsContent>
          <TabsContent value="overview">
            <div className="text-center py-12 text-muted-foreground">
              <p>Use Smart Dashboard for enhanced analytics and AI suggestions</p>
            </div>
          </TabsContent>
        </Tabs>
      </div>
    </MainLayout>
  );
}
