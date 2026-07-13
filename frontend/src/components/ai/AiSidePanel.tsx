"use client";

import * as React from "react";
import { useMutation } from "@tanstack/react-query";
import {
  Sparkles,
  X,
  RefreshCw,
  Copy,
  Check,
  Plus,
  Loader2,
  AlertCircle,
  ChevronRight,
} from "lucide-react";
import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import { ScrollArea } from "@/components/ui/scroll-area";
import { Badge } from "@/components/ui/badge";
import { Separator } from "@/components/ui/separator";
import { aiApi } from "@/lib/ai-api";
import { toast } from "react-hot-toast";
import { cn } from "@/lib/utils";
import type { AiActionType } from "./AiToolbar";
import { getAiActionLabel, getAiActionDescription } from "./AiToolbar";
import type { DocumentAiResponse } from "@/types/ai";
import {
  MarkdownRenderer,
  ActionItemsRenderer,
  MeetingMinutesRenderer,
  RequirementsRenderer,
} from "./DocumentRenderers";

interface AiSidePanelProps {
  pageId: string;
  open: boolean;
  onClose: () => void;
  onInsert: (content: string, type: string) => void;
}

export function AiSidePanel({ pageId, open, onClose, onInsert }: AiSidePanelProps) {
  const [currentAction, setCurrentAction] = React.useState<AiActionType | null>(null);
  const [copied, setCopied] = React.useState(false);

  const actionMutation = useMutation({
    mutationFn: async (action: AiActionType) => {
      switch (action) {
        case "summarize":
          return aiApi.summarizePage(pageId);
        case "rewrite":
          return aiApi.rewritePage(pageId);
        case "improve":
          return aiApi.improvePage(pageId);
        case "actions":
          return aiApi.extractActions(pageId);
        case "meeting":
          return aiApi.generateMeetingMinutes(pageId);
        case "requirements":
          return aiApi.generateRequirements(pageId);
        default:
          throw new Error("Unknown action");
      }
    },
    onSuccess: () => {
      // Success handled by result
    },
    onError: (error) => {
      toast.error(error instanceof Error ? error.message : "Failed to process");
    },
  });

  const handleAction = (action: AiActionType) => {
    setCurrentAction(action);
    actionMutation.mutate(action);
  };

  const handleCopy = async () => {
    const content = getResultContent(actionMutation.data);
    if (content) {
      await navigator.clipboard.writeText(content);
      setCopied(true);
      toast.success("Copied to clipboard");
      setTimeout(() => setCopied(false), 2000);
    }
  };

  const handleInsert = () => {
    const content = getResultContent(actionMutation.data);
    if (content) {
      onInsert(content, currentAction || "");
      toast.success("Inserted into page");
    }
  };

  const handleReplace = () => {
    const content = getResultContent(actionMutation.data);
    if (content) {
      onInsert(content, currentAction || "");
      onClose();
    }
  };

  const handleClose = () => {
    setCurrentAction(null);
    actionMutation.reset();
    onClose();
  };

  const getResultContent = (data?: DocumentAiResponse): string => {
    if (!data) return "";
    switch (currentAction) {
      case "summarize":
        return data.summary || "";
      case "rewrite":
      case "improve":
        return data.rewrittenContent || "";
      case "actions":
        return (data.actionItems || []).join("\n");
      case "meeting":
        return data.meetingMinutes || "";
      case "requirements":
        return data.requirements || "";
      default:
        return "";
    }
  };

  if (!open) return null;

  return (
    <div className="fixed right-0 top-0 h-full w-96 bg-background border-l shadow-xl z-50 flex flex-col animate-in slide-in-from-right duration-300">
      {/* Header */}
      <div className="flex items-center justify-between p-4 border-b">
        <div className="flex items-center gap-2">
          <Sparkles className="w-5 h-5 text-primary" />
          <h2 className="font-semibold">Document AI</h2>
        </div>
        <Button variant="ghost" size="icon" onClick={handleClose}>
          <X className="w-4 h-4" />
        </Button>
      </div>

      {/* Content */}
      <ScrollArea className="flex-1">
        <div className="p-4 space-y-4">
          {!currentAction ? (
            <div className="space-y-3">
              <p className="text-sm text-muted-foreground">
                Select an action to enhance your document
              </p>
              <ActionButton
                action="summarize"
                onClick={handleAction}
                isLoading={false}
              />
              <ActionButton
                action="rewrite"
                onClick={handleAction}
                isLoading={false}
              />
              <ActionButton
                action="improve"
                onClick={handleAction}
                isLoading={false}
              />
              <ActionButton
                action="actions"
                onClick={handleAction}
                isLoading={false}
              />
              <ActionButton
                action="meeting"
                onClick={handleAction}
                isLoading={false}
              />
              <ActionButton
                action="requirements"
                onClick={handleAction}
                isLoading={false}
              />
            </div>
          ) : (
            <div className="space-y-4">
              {/* Current Action Header */}
              <div className="flex items-center gap-2">
                <Badge variant="secondary">
                  <Sparkles className="w-3 h-3 mr-1" />
                  {getAiActionLabel(currentAction)}
                </Badge>
              </div>

              {/* Loading State */}
              {actionMutation.isPending && (
                <Card className="border-dashed">
                  <CardContent className="py-8">
                    <div className="flex flex-col items-center gap-3">
                      <Loader2 className="w-8 h-8 animate-spin text-primary" />
                      <p className="text-sm text-muted-foreground">
                        Thinking...
                      </p>
                    </div>
                  </CardContent>
                </Card>
              )}

              {/* Error State */}
              {actionMutation.isError && (
                <Card className="border-destructive/50 bg-destructive/10">
                  <CardContent className="py-6">
                    <div className="flex flex-col items-center gap-3 text-center">
                      <AlertCircle className="w-8 h-8 text-destructive" />
                      <p className="text-sm text-destructive">
                        {actionMutation.error instanceof Error
                          ? actionMutation.error.message
                          : "Failed to process"}
                      </p>
                      <Button
                        variant="outline"
                        size="sm"
                        onClick={() => actionMutation.mutate(currentAction)}
                      >
                        <RefreshCw className="w-4 h-4 mr-2" />
                        Try Again
                      </Button>
                    </div>
                  </CardContent>
                </Card>
              )}

              {/* Result */}
              {actionMutation.isSuccess && actionMutation.data && (
                <div className="space-y-4">
                  {/* Metadata */}
                  {actionMutation.data.processingTimeMs && (
                    <div className="text-xs text-muted-foreground">
                      Processed in {Math.round(actionMutation.data.processingTimeMs / 1000)}s
                      {actionMutation.data.confidence && (
                        <> • {(actionMutation.data.confidence * 100).toFixed(0)}% confidence</>
                      )}
                    </div>
                  )}

                  {/* Result Content */}
                  <div className="rounded-lg border bg-card">
                    <div className="p-4">
                      {currentAction === "summarize" && (
                        <MarkdownRenderer
                          content={actionMutation.data.summary || ""}
                        />
                      )}
                      {(currentAction === "rewrite" || currentAction === "improve") && (
                        <MarkdownRenderer
                          content={actionMutation.data.rewrittenContent || ""}
                        />
                      )}
                      {currentAction === "actions" && (
                        <ActionItemsRenderer
                          items={actionMutation.data.actionItems || []}
                        />
                      )}
                      {currentAction === "meeting" && (
                        <MeetingMinutesRenderer
                          content={actionMutation.data.meetingMinutes || ""}
                        />
                      )}
                      {currentAction === "requirements" && (
                        <RequirementsRenderer
                          content={actionMutation.data.requirements || ""}
                        />
                      )}
                    </div>
                  </div>
                </div>
              )}
            </div>
          )}
        </div>
      </ScrollArea>

      {/* Footer */}
      {currentAction && actionMutation.isSuccess && (
        <div className="p-4 border-t space-y-2">
          <div className="flex gap-2">
            <Button size="sm" className="flex-1" onClick={handleInsert}>
              <Plus className="w-4 h-4 mr-2" />
              Insert
            </Button>
            <Button
              variant="outline"
              size="sm"
              onClick={handleCopy}
            >
              {copied ? (
                <Check className="w-4 h-4" />
              ) : (
                <Copy className="w-4 h-4" />
              )}
            </Button>
            <Button
              variant="outline"
              size="sm"
              onClick={() => actionMutation.mutate(currentAction)}
              disabled={actionMutation.isPending}
            >
              <RefreshCw
                className={cn("w-4 h-4", actionMutation.isPending && "animate-spin")}
              />
            </Button>
          </div>
          <Button
            variant="ghost"
            size="sm"
            className="w-full"
            onClick={handleClose}
          >
            Close
          </Button>
        </div>
      )}
    </div>
  );
}

function ActionButton({
  action,
  onClick,
  isLoading,
}: {
  action: AiActionType;
  onClick: (action: AiActionType) => void;
  isLoading: boolean;
}) {
  return (
    <button
      onClick={() => onClick(action)}
      disabled={isLoading}
      className={cn(
        "w-full text-left p-3 rounded-lg border transition-colors hover:bg-muted",
        isLoading && "opacity-50 pointer-events-none"
      )}
    >
      <div className="flex items-center gap-3">
        <div className="flex items-center justify-center w-10 h-10 rounded-lg bg-primary/10">
          <Sparkles className="w-5 h-5 text-primary" />
        </div>
        <div className="flex-1">
          <div className="font-medium text-sm">
            {getAiActionLabel(action)}
          </div>
          <div className="text-xs text-muted-foreground">
            {getAiActionDescription(action)}
          </div>
        </div>
        <ChevronRight className="w-4 h-4 text-muted-foreground" />
      </div>
    </button>
  );
}
