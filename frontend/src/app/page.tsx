"use client";

import { useEffect } from "react";
import { useRouter } from "next/navigation";
import { useAuthStore } from "@/store/authStore";
import { Loader2 } from "lucide-react";

export default function HomePage() {
  const router = useRouter();
  const { isAuthenticated, isLoading, checkAuth } = useAuthStore();

  useEffect(() => {
    checkAuth();
  }, [checkAuth]);

  useEffect(() => {
    if (!isLoading) {
      if (isAuthenticated) {
        router.push("/dashboard");
      } else {
        router.push("/login");
      }
    }
  }, [isAuthenticated, isLoading, router]);

  return (
    <div className="min-h-screen flex items-center justify-center bg-background">
      <div className="text-center">
        <div className="w-16 h-16 rounded-2xl bg-primary flex items-center justify-center mb-6 mx-auto">
          <span className="text-primary-foreground font-bold text-2xl">TF</span>
        </div>
        <Loader2 className="w-6 h-6 animate-spin text-primary mx-auto mb-4" />
        <p className="text-muted-foreground">Loading TaskFlow...</p>
      </div>
    </div>
  );
}
