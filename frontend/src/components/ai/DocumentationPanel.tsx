"use client";

import * as React from "react";
import { useMutation } from "@tanstack/react-query";
import {
  FileText,
  Sparkles,
  Loader2,
  RefreshCw,
  Copy,
  Check,
  Download,
  ListChecks,
  Users,
  ClipboardCheck,
  CalendarClock,
  GitBranchPlus,
  History,
  Code2,
  X,
  AlertCircle,
  ChevronRight,
} from "lucide-react";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { ScrollArea } from "@/components/ui/scroll-area";
import { Separator } from "@/components/ui/separator";
import { aiApi } from "@/lib/ai-api";
import { MarkdownRenderer } from "./DocumentRenderers";
import type {
  DocumentType,
  DocumentationRequest,
  DocumentationResponse,
} from "@/types/ai";
import { toast } from "react-hot-toast";
import { cn } from "@/lib/utils";

interface DocumentationPanelProps {
  workspaceId: string;
  projectId?: string;
  sprintName?: string;
  pageId?: string;
  className?: string;
}

interface DocTypeOption {
  id: DocumentType;
  label: string;
  description: string;
  icon: React.ElementType;
  badge?: string;
}

const DOC_OPTIONS: DocTypeOption[] = [
  {
    id: "SRS",
    label: "Software Requirements Specification",
    description: "FR / NFR / AC for a project or topic",
    icon: FileText,
  },
  {
    id: "USER_STORIES",
    label: "User Stories",
    description: "As a / I want / So that + AC per task",
    icon: Users,
  },
  {
    id: "ACCEPTANCE_CRITERIA",
    label: "Acceptance Criteria",
    description: "Given / When / Then for every task",
    icon: ClipboardCheck,
  },
  {
    id: "MEETING_MINUTES",
    label: "Meeting Minutes",
    description: "Attendees, agenda, decisions, actions",
    icon: CalendarClock,
  },
  {
    id: "SPRINT_REVIEW",
    label: "Sprint Review",
    description: "Planned vs delivered, metrics",
    icon: GitBranchPlus,
  },
  {
    id: "RETROSPECTIVE",
    label: "Sprint Retrospective",
    description: "What went well / poorly / improve",
    icon: History,
  },
  {
    id: "TECHNICAL_SPEC",
    label: "Technical Specification",
    description: "Architecture, modules, data model",
    icon: Code2,
  },
];

export function DocumentationPanel({
  workspaceId,
  projectId,
  sprintName,
  pageId,
  className,
}: DocumentationPanelProps) {
  const [selected, setSelected] = React.useState<DocumentType | null>(null);
  const [topic, setTopic] = React.useState("");
  const [author, setAuthor] = React.useState("");
  const [audience, setAudience] = React.useState("Engineering Team");
  const [durationDays, setDurationDays] = React.useState<number>(14);
  const [copied, setCopied] = React.useState(false);
  const [view, setView] = React.useState<"preview" | "raw">("preview");

  const mutation = useMutation<DocumentationResponse, Error>({
    mutationFn: () => {
      if (!selected) {
        return Promise.reject(new Error("Please select a document type"));
      }
      const request: DocumentationRequest = {
        workspaceId,
        documentType: selected,
        projectId,
        sprintName,
        pageId,
        topic: topic.trim() || undefined,
        author: author.trim() || undefined,
        audience: audience.trim() || undefined,
        durationDays:
          selected === "SPRINT_REVIEW" || selected === "RETROSPECTIVE"
            ? durationDays
            : undefined,
      };
      return aiApi.generateDocumentation(request);
    },
    onError: (error) => {
      toast.error(error.message || "Documentation generation failed");
    },
  });

  const handleGenerate = () => {
    if (!selected) {
      toast.error("Vui lòng chọn loại tài liệu");
      return;
    }
    mutation.mutate();
  };

  const handleCopy = async () => {
    if (mutation.data?.markdown) {
      await navigator.clipboard.writeText(mutation.data.markdown);
      setCopied(true);
      toast.success("Copied Markdown to clipboard");
      setTimeout(() => setCopied(false), 2000);
    }
  };

  const handleDownload = () => {
    if (!mutation.data?.markdown || !mutation.data.documentType) return;
    const blob = new Blob([mutation.data.markdown], { type: "text/markdown" });
    const url = URL.createObjectURL(blob);
    const a = document.createElement("a");
    a.href = url;
    a.download = `${mutation.data.documentType.toLowerCase()}.md`;
    a.click();
    URL.revokeObjectURL(url);
  };

  const handleReset = () => {
    setSelected(null);
    setTopic("");
    mutation.reset();
  };

  return (
    <Card className={cn("border-dashed", className)}>
      <CardHeader className="pb-3">
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-2">
            <Sparkles className="w-5 h-5 text-primary" />
            <CardTitle className="text-lg">AI Documentation</CardTitle>
          </div>
          {mutation.data && (
            <Button variant="ghost" size="sm" onClick={handleReset}>
              <X className="w-4 h-4 mr-1" />
              New document
            </Button>
          )}
        </div>
        <p className="text-xs text-muted-foreground">
          Tạo tự động SRS, User Stories, Acceptance Criteria, Meeting Minutes,
          Sprint Review, Retrospective, Technical Specification.
        </p>
      </CardHeader>

      <CardContent className="space-y-4">
        {!mutation.data && (
          <>
            {/* Document type picker */}
            <div className="grid grid-cols-1 md:grid-cols-2 gap-2">
              {DOC_OPTIONS.map((opt) => {
                const Icon = opt.icon;
                const isSelected = selected === opt.id;
                return (
                  <button
                    key={opt.id}
                    type="button"
                    onClick={() => setSelected(opt.id)}
                    className={cn(
                      "text-left p-3 rounded-lg border transition-colors",
                      isSelected
                        ? "border-primary bg-primary/5"
                        : "hover:bg-muted border-border"
                    )}
                  >
                    <div className="flex items-start gap-3">
                      <div
                        className={cn(
                          "flex items-center justify-center w-9 h-9 rounded-lg",
                          isSelected
                            ? "bg-primary text-primary-foreground"
                            : "bg-primary/10 text-primary"
                        )}
                      >
                        <Icon className="w-4 h-4" />
                      </div>
                      <div className="flex-1 min-w-0">
                        <div className="font-medium text-sm">{opt.label}</div>
                        <div className="text-xs text-muted-foreground">
                          {opt.description}
                        </div>
                      </div>
                      {isSelected && (
                        <Check className="w-4 h-4 text-primary flex-shrink-0" />
                      )}
                    </div>
                  </button>
                );
              })}
            </div>

            {/* Optional context */}
            {selected && (
              <>
                <Separator />
                <div className="grid grid-cols-1 md:grid-cols-2 gap-3">
                  <div className="space-y-1">
                    <label className="text-xs font-medium text-muted-foreground">
                      Topic (optional)
                    </label>
                    <input
                      type="text"
                      value={topic}
                      onChange={(e) => setTopic(e.target.value)}
                      placeholder="e.g. Build Grocery Management System"
                      className="w-full px-3 py-2 rounded-md border bg-background text-sm focus:outline-none focus:ring-2 focus:ring-primary/40"
                    />
                  </div>
                  <div className="space-y-1">
                    <label className="text-xs font-medium text-muted-foreground">
                      Author (optional)
                    </label>
                    <input
                      type="text"
                      value={author}
                      onChange={(e) => setAuthor(e.target.value)}
                      placeholder="e.g. Senior BA"
                      className="w-full px-3 py-2 rounded-md border bg-background text-sm focus:outline-none focus:ring-2 focus:ring-primary/40"
                    />
                  </div>
                  <div className="space-y-1">
                    <label className="text-xs font-medium text-muted-foreground">
                      Audience
                    </label>
                    <input
                      type="text"
                      value={audience}
                      onChange={(e) => setAudience(e.target.value)}
                      placeholder="Engineering Team"
                      className="w-full px-3 py-2 rounded-md border bg-background text-sm focus:outline-none focus:ring-2 focus:ring-primary/40"
                    />
                  </div>
                  {(selected === "SPRINT_REVIEW" || selected === "RETROSPECTIVE") && (
                    <div className="space-y-1">
                      <label className="text-xs font-medium text-muted-foreground">
                        Sprint duration (days)
                      </label>
                      <input
                        type="number"
                        value={durationDays}
                        onChange={(e) => setDurationDays(Number(e.target.value) || 14)}
                        min={1}
                        max={60}
                        className="w-full px-3 py-2 rounded-md border bg-background text-sm focus:outline-none focus:ring-2 focus:ring-primary/40"
                      />
                    </div>
                  )}
                </div>

                <Button onClick={handleGenerate} disabled={mutation.isPending} className="w-full">
                  {mutation.isPending ? (
                    <>
                      <Loader2 className="w-4 h-4 mr-2 animate-spin" />
                      Generating Markdown...
                    </>
                  ) : (
                    <>
                      <Sparkles className="w-4 h-4 mr-2" />
                      Generate document
                      <ChevronRight className="w-4 h-4 ml-1" />
                    </>
                  )}
                </Button>
              </>
            )}
          </>
        )}

        {/* Error state */}
        {mutation.isError && (
          <Card className="border-destructive/50 bg-destructive/10">
            <CardContent className="py-6">
              <div className="flex flex-col items-center gap-3 text-center">
                <AlertCircle className="w-8 h-8 text-destructive" />
                <p className="text-sm text-destructive">
                  {mutation.error.message}
                </p>
                <Button
                  variant="outline"
                  size="sm"
                  onClick={handleGenerate}
                  disabled={mutation.isPending}
                >
                  <RefreshCw className="w-4 h-4 mr-2" />
                  Try Again
                </Button>
              </div>
            </CardContent>
          </Card>
        )}

        {/* Result */}
        {mutation.isSuccess && mutation.data?.markdown && (
          <div className="space-y-3">
            <div className="flex flex-wrap items-center gap-2 text-xs">
              <Badge variant="secondary">
                <FileText className="w-3 h-3 mr-1" />
                {mutation.data.documentType}
              </Badge>
              {mutation.data.source && (
                <Badge variant={mutation.data.source === "GROQ" ? "default" : "outline"}>
                  {mutation.data.source === "GROQ" ? "AI Generated" : "Local Fallback"}
                </Badge>
              )}
              {mutation.data.confidence !== undefined && (
                <Badge variant="outline">
                  {Math.round((mutation.data.confidence || 0) * 100)}% confidence
                </Badge>
              )}
              {mutation.data.processingTimeMs !== undefined && (
                <span className="text-muted-foreground">
                  in {Math.round((mutation.data.processingTimeMs || 0) / 1000)}s
                </span>
              )}
            </div>

            {/* TOC */}
            {mutation.data.sections && mutation.data.sections.length > 0 && (
              <Card className="bg-muted/30">
                <CardContent className="p-3">
                  <div className="text-xs font-semibold mb-2 flex items-center gap-1">
                    <ListChecks className="w-3 h-3" />
                    Outline
                  </div>
                  <ul className="space-y-1 text-xs">
                    {mutation.data.sections.slice(0, 12).map((s, i) => (
                      <li
                        key={`${s.heading}-${i}`}
                        className={cn(
                          s.level === 1 ? "font-medium" : "ml-3 text-muted-foreground"
                        )}
                      >
                        {s.level === 1 ? "▸" : "·"} {s.heading}
                      </li>
                    ))}
                  </ul>
                </CardContent>
              </Card>
            )}

            {/* View toggle */}
            <div className="flex items-center gap-1 border rounded-md p-1 w-fit">
              <button
                type="button"
                onClick={() => setView("preview")}
                className={cn(
                  "text-xs px-3 py-1 rounded",
                  view === "preview" ? "bg-primary text-primary-foreground" : "hover:bg-muted"
                )}
              >
                Preview
              </button>
              <button
                type="button"
                onClick={() => setView("raw")}
                className={cn(
                  "text-xs px-3 py-1 rounded",
                  view === "raw" ? "bg-primary text-primary-foreground" : "hover:bg-muted"
                )}
              >
                Markdown
              </button>
            </div>

            <ScrollArea className="max-h-[60vh] rounded-md border bg-card">
              <div className="p-4">
                {view === "preview" ? (
                  <MarkdownRenderer content={mutation.data.markdown} />
                ) : (
                  <pre className="text-xs whitespace-pre-wrap font-mono">
                    {mutation.data.markdown}
                  </pre>
                )}
              </div>
            </ScrollArea>

            {/* Actions */}
            <div className="flex flex-wrap gap-2">
              <Button size="sm" onClick={handleDownload}>
                <Download className="w-4 h-4 mr-2" />
                Download .md
              </Button>
              <Button size="sm" variant="outline" onClick={handleCopy}>
                {copied ? (
                  <Check className="w-4 h-4 mr-2" />
                ) : (
                  <Copy className="w-4 h-4 mr-2" />
                )}
                {copied ? "Copied" : "Copy Markdown"}
              </Button>
              <Button
                size="sm"
                variant="outline"
                onClick={handleGenerate}
                disabled={mutation.isPending}
              >
                <RefreshCw
                  className={cn(
                    "w-4 h-4 mr-2",
                    mutation.isPending && "animate-spin"
                  )}
                />
                Regenerate
              </Button>
            </div>

            {mutation.data.keywords && mutation.data.keywords.length > 0 && (
              <div className="flex flex-wrap items-center gap-1 pt-1">
                <span className="text-xs text-muted-foreground">Tags:</span>
                {mutation.data.keywords.map((k, i) => (
                  <Badge key={`${k}-${i}`} variant="outline" className="text-xs">
                    {k}
                  </Badge>
                ))}
              </div>
            )}
          </div>
        )}
      </CardContent>
    </Card>
  );
}
