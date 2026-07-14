"use client";

import * as React from "react";
import { useRouter } from "next/navigation";
import { MainLayout } from "@/components/layout/MainLayout";
import { Header } from "@/components/layout/Header";
import { SmartDashboard } from "@/components/dashboard/SmartDashboard";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { useWorkspaceStore } from "@/store/workspaceStore";

export default function DashboardPage() {
  const router = useRouter();
  const { currentWorkspace } = useWorkspaceStore();

  React.useEffect(() => {
    if (currentWorkspace) {
      router.replace(`/dashboard/${currentWorkspace.id}`);
    }
  }, [currentWorkspace, router]);

  if (!currentWorkspace) {
    return (
      <MainLayout>
        <div className="flex items-center justify-center h-screen">
          <div className="text-muted-foreground">Loading workspace...</div>
        </div>
      </MainLayout>
    );
  }

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
