"use client";

import * as React from "react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { Toaster } from "react-hot-toast";
import { FloatingAIButton } from "@/components/ai/FloatingAIButton";
import { WorkspaceAssistantPanel } from "@/components/ai/WorkspaceAssistantPanel";
import { DemoModeOverlay } from "@/components/ai/DemoModeOverlay";
import { ErrorBoundary } from "@/components/providers/ErrorBoundary";
import { useWorkspaceStore } from "@/store/workspaceStore";

export function Providers({ children }: { children: React.ReactNode }) {
  const [queryClient] = React.useState(
    () =>
      new QueryClient({
        defaultOptions: {
          queries: {
            staleTime: 60 * 1000,
            refetchOnWindowFocus: false,
          },
        },
      })
  );

  return (
    <ErrorBoundary>
      <QueryClientProvider client={queryClient}>
        {children}
        <Toaster position="top-right" />
        <AIAssistantWrapper />
      </QueryClientProvider>
    </ErrorBoundary>
  );
}

function AIAssistantWrapper() {
  const [isOpen, setIsOpen] = React.useState(false);
  const [isDemoOpen, setIsDemoOpen] = React.useState(false);
  const { currentWorkspace } = useWorkspaceStore();

  // Get workspace ID from URL first, then fallback to store
  const workspaceId = React.useMemo(() => {
    if (typeof window === "undefined") return null;
    const match = window.location.pathname.match(/\/workspace\/([^/]+)/);
    if (match) return match[1];
    // Fallback to currentWorkspace from store
    return currentWorkspace?.id || null;
  }, [currentWorkspace]);

  // Don't render panel if no valid workspace
  if (!workspaceId) {
    return (
      <>
        <FloatingAIButton onClick={() => setIsOpen(!isOpen)} isOpen={isOpen} />
        <DemoModeOverlay
          open={isDemoOpen}
          onClose={() => setIsDemoOpen(false)}
        />
      </>
    );
  }

  return (
    <>
      <FloatingAIButton onClick={() => setIsOpen(!isOpen)} isOpen={isOpen} />
      <WorkspaceAssistantPanel
        workspaceId={workspaceId}
        open={isOpen}
        onClose={() => setIsOpen(false)}
        onOpenDemo={() => setIsDemoOpen(true)}
      />
      <DemoModeOverlay
        open={isDemoOpen}
        onClose={() => setIsDemoOpen(false)}
        workspaceId={workspaceId}
      />
    </>
  );
}