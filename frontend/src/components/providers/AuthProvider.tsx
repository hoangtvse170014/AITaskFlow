"use client";

import * as React from "react";
import { useRouter } from "next/navigation";
import { useAuthStore } from "@/store/authStore";
import { useWorkspaceStore } from "@/store/workspaceStore";
import { Loader2 } from "lucide-react";

interface AuthProviderProps {
  children: React.ReactNode;
}

export function AuthProvider({ children }: AuthProviderProps) {
  const router = useRouter();
  const { checkAuth, isAuthenticated, isLoading } = useAuthStore();
  const { fetchWorkspaces } = useWorkspaceStore();

  React.useEffect(() => {
    checkAuth();
  }, [checkAuth]);

  React.useEffect(() => {
    if (!isLoading && !isAuthenticated) {
      const publicPaths = ["/login", "/register"];
      const currentPath = window.location.pathname;

      if (!publicPaths.some((path) => currentPath.startsWith(path))) {
        router.push("/login");
      }
    }

    if (isAuthenticated) {
      fetchWorkspaces().catch(() => {
        // Do not block rendering if workspace fetch fails.
      });
    }
  }, [isAuthenticated, isLoading, router, fetchWorkspaces]);

  if (isLoading) {
    return (
      <div className="min-h-screen flex items-center justify-center bg-background">
        <div className="text-center">
          <Loader2 className="w-8 h-8 animate-spin text-primary mx-auto mb-4" />
          <p className="text-muted-foreground">Loading...</p>
        </div>
      </div>
    );
  }

  return <>{children}</>;
}
