"use client";

import * as React from "react";
import { useEffect } from "react";
import { useParams, useRouter } from "next/navigation";
import { MainLayout } from "@/components/layout/MainLayout";
import { Header } from "@/components/layout/Header";
import { SmartDashboard } from "@/components/dashboard/SmartDashboard";
import { DocumentationPanel } from "@/components/ai/DocumentationPanel";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { useWorkspaceStore } from "@/store/workspaceStore";

export default function WorkspaceDashboardPage() {
  const params = useParams();
  const router = useRouter();
  const workspaceId = params.workspaceId as string;
  const { setCurrentWorkspace, workspaces, currentWorkspace, fetchWorkspaces } = useWorkspaceStore();
  const [isLoading, setIsLoading] = React.useState(true);

  useEffect(() => {
    async function loadWorkspace() {
      setIsLoading(true);
      try {
        // Get current store state directly
        const store = useWorkspaceStore.getState();
        
        // Fetch workspaces if not loaded
        if (store.workspaces.length === 0) {
          await fetchWorkspaces();
        }
        
        // Get updated state
        const updatedStore = useWorkspaceStore.getState();
        const found = updatedStore.workspaces.find(w => w.id === workspaceId);
        
        if (found) {
          setCurrentWorkspace(found);
        } else if (updatedStore.workspaces.length > 0) {
          // Workspace not found, use first workspace
          setCurrentWorkspace(updatedStore.workspaces[0]);
          router.replace(`/dashboard/${updatedStore.workspaces[0].id}`);
          return;
        }
      } catch (error) {
        console.error("Failed to load workspace:", error);
      } finally {
        setIsLoading(false);
      }
    }
    
    if (workspaceId) {
      loadWorkspace();
    }
  }, [workspaceId, fetchWorkspaces, router, setCurrentWorkspace]);

  if (isLoading) {
    return (
      <MainLayout>
        <div className="flex items-center justify-center h-screen">
          <div className="text-muted-foreground">Loading dashboard...</div>
        </div>
      </MainLayout>
    );
  }

  return (
    <MainLayout>
      <Header title="Dashboard" />
      <div className="p-6">
        <Tabs defaultValue="smart" className="w-full">
          <TabsList className="mb-6">
            <TabsTrigger value="smart">Smart Dashboard</TabsTrigger>
            <TabsTrigger value="overview">Overview</TabsTrigger>
            <TabsTrigger value="documentation">AI Docs</TabsTrigger>
          </TabsList>
          <TabsContent value="smart">
            <SmartDashboard />
          </TabsContent>
          <TabsContent value="overview">
            <div className="text-center py-12 text-muted-foreground">
              <p>Use Smart Dashboard for enhanced analytics and AI suggestions</p>
            </div>
          </TabsContent>
          <TabsContent value="documentation">
            <DocumentationPanel workspaceId={workspaceId} />
          </TabsContent>
        </Tabs>
      </div>
    </MainLayout>
  );
}
