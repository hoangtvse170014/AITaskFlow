"use client";

import * as React from "react";
import {
  Sparkles,
  FileText,
  RefreshCw,
  Wand2,
  ListChecks,
  Users,
  FileCheck,
  Loader2,
} from "lucide-react";
import { Button } from "@/components/ui/button";
import { Tooltip, TooltipContent, TooltipTrigger } from "@/components/ui/tooltip";
import { cn } from "@/lib/utils";

export type AiActionType =
  | "summarize"
  | "rewrite"
  | "improve"
  | "actions"
  | "meeting"
  | "requirements";

interface AiToolbarProps {
  onAction: (action: AiActionType) => void;
  isLoading: boolean;
  disabled?: boolean;
}

const AI_ACTIONS: { action: AiActionType; label: string; icon: React.ElementType }[] = [
  { action: "summarize", label: "Summarize", icon: FileText },
  { action: "rewrite", label: "Rewrite", icon: RefreshCw },
  { action: "improve", label: "Improve Writing", icon: Wand2 },
  { action: "actions", label: "Extract Actions", icon: ListChecks },
  { action: "meeting", label: "Meeting Minutes", icon: Users },
  { action: "requirements", label: "Requirements", icon: FileCheck },
];

export function AiToolbar({ onAction, isLoading, disabled }: AiToolbarProps) {
  return (
    <div className="flex items-center gap-1 p-2 border-b bg-muted/50">
      <div className="flex items-center gap-1">
        <Sparkles className="w-4 h-4 text-primary mr-2" />
        {AI_ACTIONS.map(({ action, label, icon: Icon }) => (
          <Tooltip key={action} delayDuration={300}>
            <TooltipTrigger asChild>
              <Button
                variant="ghost"
                size="sm"
                className={cn(
                  "h-8 px-3 text-xs gap-1.5",
                  isLoading && "opacity-50 pointer-events-none"
                )}
                disabled={disabled || isLoading}
                onClick={() => onAction(action)}
              >
                {isLoading ? (
                  <Loader2 className="w-3.5 h-3.5 animate-spin" />
                ) : (
                  <Icon className="w-3.5 h-3.5" />
                )}
                <span className="hidden sm:inline">{label}</span>
              </Button>
            </TooltipTrigger>
            <TooltipContent side="bottom" className="text-xs">
              <p>{label}</p>
            </TooltipContent>
          </Tooltip>
        ))}
      </div>
    </div>
  );
}

export function getAiActionLabel(action: AiActionType): string {
  const labels: Record<AiActionType, string> = {
    summarize: "Summarize Document",
    rewrite: "Rewrite Content",
    improve: "Improve Writing",
    actions: "Extract Action Items",
    meeting: "Generate Meeting Minutes",
    requirements: "Generate Requirements",
  };
  return labels[action];
}

export function getAiActionDescription(action: AiActionType): string {
  const descriptions: Record<AiActionType, string> = {
    summarize: "Get a concise summary of the current document",
    rewrite: "Rewrite the content with improved clarity and flow",
    improve: "Enhance the writing quality and style",
    actions: "Extract actionable tasks from the document",
    meeting: "Generate structured meeting minutes",
    requirements: "Generate detailed requirements from notes",
  };
  return descriptions[action];
}
