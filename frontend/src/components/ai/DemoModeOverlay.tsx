"use client";

import * as React from "react";
import { X } from "lucide-react";
import { Button } from "@/components/ui/button";
import { DemoModePanel } from "./DemoModePanel";
import { cn } from "@/lib/utils";

interface DemoModeOverlayProps {
  open: boolean;
  onClose: () => void;
  workspaceId?: string;
  className?: string;
}

/**
 * Modal overlay that hosts the DemoModePanel. Used by the floating AI button
 * "Try Demo Mode" entry point and by the /demo page.
 */
export function DemoModeOverlay({
  open,
  onClose,
  workspaceId,
  className,
}: DemoModeOverlayProps) {
  React.useEffect(() => {
    if (!open) return;
    const onKey = (e: KeyboardEvent) => {
      if (e.key === "Escape") onClose();
    };
    window.addEventListener("keydown", onKey);
    document.body.style.overflow = "hidden";
    return () => {
      window.removeEventListener("keydown", onKey);
      document.body.style.overflow = "";
    };
  }, [open, onClose]);

  if (!open) return null;

  return (
    <div
      className="fixed inset-0 z-[60] flex items-center justify-center bg-black/50 backdrop-blur-sm animate-in fade-in duration-200 p-4"
      onClick={(e) => {
        if (e.target === e.currentTarget) onClose();
      }}
    >
      <div
        className={cn(
          "relative w-full max-w-3xl max-h-[90vh] overflow-y-auto rounded-2xl bg-background shadow-2xl animate-in zoom-in-95 slide-in-from-bottom-4 duration-300",
          className,
        )}
      >
        <Button
          variant="ghost"
          size="icon"
          className="absolute right-3 top-3 z-10"
          onClick={onClose}
          aria-label="Close demo overlay"
        >
          <X className="h-4 w-4" />
        </Button>
        <div className="p-4 sm:p-6">
          {workspaceId ? (
            <DemoModePanel workspaceId={workspaceId} onSuccess={() => { /* leave open so user can review */ }} />
          ) : (
            <DemoModePanel />
          )}
        </div>
      </div>
    </div>
  );
}