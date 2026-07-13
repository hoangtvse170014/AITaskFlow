"use client";

import * as React from "react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { Toaster } from "react-hot-toast";
import { FloatingAIButton } from "@/components/ai/FloatingAIButton";
import { WorkspaceAssistantPanel } from "@/components/ai/WorkspaceAssistantPanel";
import { ErrorBoundary } from "@/components/providers/ErrorBoundary";

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

  // Get workspace ID from URL or store
  const workspaceId = React.useMemo(() => {
    if (typeof window === "undefined") return "default";
    const match = window.location.pathname.match(/\/workspace\/([^/]+)/);
    return match ? match[1] : "default";
  }, []);

  return (
    <>
      <FloatingAIButton onClick={() => setIsOpen(!isOpen)} isOpen={isOpen} />
      <WorkspaceAssistantPanel
        workspaceId={workspaceId}
        open={isOpen}
        onClose={() => setIsOpen(false)}
      />
    </>
  );
}