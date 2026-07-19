"use client";

import * as React from "react";
import { CheckSquare, Square, AlertTriangle, Clock, User } from "lucide-react";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Checkbox } from "@/components/ui/checkbox";
import { cn } from "@/lib/utils";

interface MarkdownRendererProps {
  content: string;
  className?: string;
}

export function MarkdownRenderer({ content, className }: MarkdownRendererProps) {
  if (!content) {
    return (
      <p className="text-sm text-muted-foreground italic">No content available</p>
    );
  }

  const renderMarkdown = (text: string) => {
    const lines = text.split("\n");
    const elements: React.ReactNode[] = [];
    let inCodeBlock = false;
    let codeContent: string[] = [];
    let codeLanguage = "";

    lines.forEach((line, index) => {
      // Code block
      if (line.startsWith("```")) {
        if (inCodeBlock) {
          elements.push(
            <pre key={`code-${index}`} className="bg-muted p-3 rounded-lg overflow-x-auto my-2">
              <code className="text-sm font-mono">{codeContent.join("\n")}</code>
            </pre>
          );
          codeContent = [];
          codeLanguage = "";
          inCodeBlock = false;
        } else {
          codeLanguage = line.slice(3).trim();
          inCodeBlock = true;
        }
        return;
      }

      if (inCodeBlock) {
        codeContent.push(line);
        return;
      }

      // Headers
      if (line.startsWith("### ")) {
        elements.push(
          <h4 key={index} className="text-base font-semibold mt-4 mb-2">
            {renderInlineMarkdown(line.slice(4))}
          </h4>
        );
        return;
      }
      if (line.startsWith("## ")) {
        elements.push(
          <h3 key={index} className="text-lg font-semibold mt-4 mb-2">
            {renderInlineMarkdown(line.slice(3))}
          </h3>
        );
        return;
      }
      if (line.startsWith("# ")) {
        elements.push(
          <h2 key={index} className="text-xl font-bold mt-4 mb-2">
            {renderInlineMarkdown(line.slice(2))}
          </h2>
        );
        return;
      }

      // Quote
      if (line.startsWith("> ")) {
        elements.push(
          <blockquote
            key={index}
            className="border-l-4 border-primary pl-4 py-1 my-2 text-muted-foreground italic"
          >
            {renderInlineMarkdown(line.slice(2))}
          </blockquote>
        );
        return;
      }

      // List items
      if (line.match(/^[-*] /)) {
        elements.push(
          <li key={index} className="ml-4 list-disc">
            {renderInlineMarkdown(line.slice(2))}
          </li>
        );
        return;
      }
      if (line.match(/^\d+\. /)) {
        elements.push(
          <li key={index} className="ml-4 list-decimal">
            {renderInlineMarkdown(line.replace(/^\d+\. /, ""))}
          </li>
        );
        return;
      }

      // Task list
      if (line.match(/^- \[ \]/)) {
        elements.push(
          <div key={index} className="flex items-center gap-2 ml-4">
            <Square className="w-4 h-4 text-muted-foreground" />
            <span>{renderInlineMarkdown(line.slice(5))}</span>
          </div>
        );
        return;
      }
      if (line.match(/^- \[x\] /i)) {
        elements.push(
          <div key={index} className="flex items-center gap-2 ml-4">
            <CheckSquare className="w-4 h-4 text-green-500" />
            <span className="line-through text-muted-foreground">
              {renderInlineMarkdown(line.slice(6))}
            </span>
          </div>
        );
        return;
      }

      // Table (simplified)
      if (line.includes("|") && line.trim().startsWith("|")) {
        const cells = line.split("|").filter((c) => c.trim());
        const isHeader = cells.every((c) => c.trim().match(/^-+$/));
        if (!isHeader) {
          elements.push(
            <div key={index} className="flex gap-4 py-1">
              {cells.map((cell, i) => (
                <span key={i} className="flex-1 text-sm">
                  {renderInlineMarkdown(cell.trim())}
                </span>
              ))}
            </div>
          );
        }
        return;
      }

      // Empty line
      if (!line.trim()) {
        elements.push(<div key={index} className="h-2" />);
        return;
      }

      // Paragraph
      elements.push(
        <p key={index} className="text-sm leading-relaxed mb-2">
          {renderInlineMarkdown(line)}
        </p>
      );
    });

    return elements;
  };

  const renderInlineMarkdown = (text: string): React.ReactNode => {
    const escape = (s: string) =>
      s.replace(/&/g, "&amp;").replace(/</g, "&lt;").replace(/>/g, "&gt;").replace(/"/g, "&quot;").replace(/'/g, "&#39;");

    let safe = escape(text);
    safe = safe.replace(/\*\*(.+?)\*\*/g, "<strong>$1</strong>");
    safe = safe.replace(/(^|[^*])\*(?!\*)(.+?)\*(?!\*)/g, "$1<em>$2</em>");
    safe = safe.replace(/`(.+?)`/g, "<code class='bg-muted px-1 rounded text-xs'>$1</code>");

    return <span dangerouslySetInnerHTML={{ __html: safe }} />;
  };

  return <div className={cn("space-y-1", className)}>{renderMarkdown(content)}</div>;
}

interface ActionItemsRendererProps {
  items: string[];
}

export function ActionItemsRenderer({ items }: ActionItemsRendererProps) {
  const [completed, setCompleted] = React.useState<Set<number>>(new Set());

  if (!items || items.length === 0) {
    return (
      <p className="text-sm text-muted-foreground italic">No action items found</p>
    );
  }

  const toggleItem = (index: number) => {
    setCompleted((prev) => {
      const next = new Set(prev);
      if (next.has(index)) {
        next.delete(index);
      } else {
        next.add(index);
      }
      return next;
    });
  };

  return (
    <div className="space-y-3">
      <div className="flex items-center justify-between">
        <span className="text-sm font-medium">{items.length} Action Items</span>
        <Badge variant="secondary">{completed.size}/{items.length}</Badge>
      </div>
      <div className="space-y-2">
        {items.map((item, index) => (
          <div
            key={index}
            className={cn(
              "flex items-start gap-3 p-3 rounded-lg border transition-colors",
              completed.has(index)
                ? "bg-muted/50 border-muted"
                : "bg-card hover:bg-muted/50"
            )}
          >
            <button
              onClick={() => toggleItem(index)}
              className="mt-0.5 flex-shrink-0"
            >
              {completed.has(index) ? (
                <CheckSquare className="w-5 h-5 text-green-500" />
              ) : (
                <Square className="w-5 h-5 text-muted-foreground" />
              )}
            </button>
            <div className="flex-1">
              <p
                className={cn(
                  "text-sm",
                  completed.has(index) && "line-through text-muted-foreground"
                )}
              >
                {item}
              </p>
            </div>
          </div>
        ))}
      </div>
      <Button variant="outline" size="sm" className="w-full mt-4">
        <AlertTriangle className="w-4 h-4 mr-2" />
        Create Tasks
      </Button>
    </div>
  );
}

interface MeetingMinutesRendererProps {
  content: string;
}

export function MeetingMinutesRenderer({ content }: MeetingMinutesRendererProps) {
  const parseMeetingMinutes = (text: string) => {
    const sections: {
      summary?: string;
      decisions?: string[];
      actions?: string[];
      risks?: string[];
      nextSteps?: string[];
    } = {};

    const lines = text.split("\n");
    let currentSection: keyof typeof sections | null = null;
    let currentItems: string[] = [];

    lines.forEach((line) => {
      const trimmed = line.trim();
      const lowerLine = trimmed.toLowerCase();

      if (lowerLine.includes("summary") || lowerLine.includes("tóm tắt")) {
        if (currentSection && currentItems.length > 0) {
          (sections as any)[currentSection] = [...currentItems];
        }
        currentSection = "summary";
        currentItems = [];
        return;
      }

      if (lowerLine.includes("decision") || lowerLine.includes("quyết định")) {
        if (currentSection && currentItems.length > 0) {
          (sections as any)[currentSection] = [...currentItems];
        }
        currentSection = "decisions";
        currentItems = [];
        return;
      }

      if (lowerLine.includes("action") || lowerLine.includes("hành động")) {
        if (currentSection && currentItems.length > 0) {
          (sections as any)[currentSection] = [...currentItems];
        }
        currentSection = "actions";
        currentItems = [];
        return;
      }

      if (lowerLine.includes("risk") || lowerLine.includes("rủi ro")) {
        if (currentSection && currentItems.length > 0) {
          (sections as any)[currentSection] = [...currentItems];
        }
        currentSection = "risks";
        currentItems = [];
        return;
      }

      if (lowerLine.includes("next") || lowerLine.includes("tiếp theo")) {
        if (currentSection && currentItems.length > 0) {
          (sections as any)[currentSection] = [...currentItems];
        }
        currentSection = "nextSteps";
        currentItems = [];
        return;
      }

      if (currentSection && trimmed) {
        const cleanLine = trimmed.replace(/^[-*•]\s*/, "").replace(/^\d+\.\s*/, "");
        if (cleanLine) {
          currentItems.push(cleanLine);
        }
      } else if (!currentSection && trimmed) {
        sections.summary = (sections.summary || "") + trimmed + "\n";
      }
    });

    if (currentSection && currentItems.length > 0) {
      (sections as any)[currentSection] = currentItems;
    }

    return sections;
  };

  const sections = parseMeetingMinutes(content);

  return (
    <div className="space-y-4">
      {sections.summary && (
        <div>
          <h4 className="text-sm font-semibold mb-2 flex items-center gap-2">
            <Badge variant="outline">Summary</Badge>
          </h4>
          <p className="text-sm text-muted-foreground whitespace-pre-wrap">
            {sections.summary}
          </p>
        </div>
      )}

      {sections.decisions && sections.decisions.length > 0 && (
        <div>
          <h4 className="text-sm font-semibold mb-2 flex items-center gap-2">
            <Badge variant="default">Key Decisions</Badge>
          </h4>
          <ul className="space-y-1">
            {sections.decisions.map((item, index) => (
              <li key={index} className="flex items-start gap-2 text-sm">
                <span className="text-primary mt-1">•</span>
                <span>{item}</span>
              </li>
            ))}
          </ul>
        </div>
      )}

      {sections.actions && sections.actions.length > 0 && (
        <div>
          <h4 className="text-sm font-semibold mb-2 flex items-center gap-2">
            <Badge variant="secondary">Action Items</Badge>
          </h4>
          <ul className="space-y-1">
            {sections.actions.map((item, index) => (
              <li key={index} className="flex items-start gap-2 text-sm">
                <Square className="w-4 h-4 mt-0.5 text-muted-foreground" />
                <span>{item}</span>
              </li>
            ))}
          </ul>
        </div>
      )}

      {sections.risks && sections.risks.length > 0 && (
        <div>
          <h4 className="text-sm font-semibold mb-2 flex items-center gap-2">
            <AlertTriangle className="w-4 h-4 text-orange-500" />
            <Badge variant="destructive">Risks</Badge>
          </h4>
          <ul className="space-y-1">
            {sections.risks.map((item, index) => (
              <li key={index} className="flex items-start gap-2 text-sm text-orange-600 dark:text-orange-400">
                <span className="mt-1">•</span>
                <span>{item}</span>
              </li>
            ))}
          </ul>
        </div>
      )}

      {sections.nextSteps && sections.nextSteps.length > 0 && (
        <div>
          <h4 className="text-sm font-semibold mb-2 flex items-center gap-2">
            <Clock className="w-4 h-4 text-blue-500" />
            <Badge variant="outline">Next Steps</Badge>
          </h4>
          <ul className="space-y-1">
            {sections.nextSteps.map((item, index) => (
              <li key={index} className="flex items-start gap-2 text-sm">
                <span className="text-blue-500 mt-1">•</span>
                <span>{item}</span>
              </li>
            ))}
          </ul>
        </div>
      )}

      {!sections.summary && !sections.decisions && !sections.actions && (
        <MarkdownRenderer content={content} />
      )}
    </div>
  );
}

interface RequirementsRendererProps {
  content: string;
}

export function RequirementsRenderer({ content }: RequirementsRendererProps) {
  const parseRequirements = (text: string) => {
    const sections: {
      overview?: string;
      functional?: string[];
      nonFunctional?: string[];
      acceptanceCriteria?: string[];
    } = {};

    const lines = text.split("\n");
    let currentSection: keyof typeof sections | null = null;
    let currentItems: string[] = [];

    lines.forEach((line) => {
      const trimmed = line.trim();
      const lowerLine = trimmed.toLowerCase();

      if (lowerLine.includes("overview") || lowerLine.includes("tổng quan")) {
        if (currentSection && currentItems.length > 0) {
          (sections as any)[currentSection] = [...currentItems];
        }
        currentSection = "overview";
        currentItems = [];
        return;
      }

      if (lowerLine.includes("functional") || lowerLine.includes("chức năng")) {
        if (currentSection && currentItems.length > 0) {
          (sections as any)[currentSection] = currentItems;
        }
        currentSection = "functional";
        currentItems = [];
        return;
      }

      if (lowerLine.includes("non-functional") || lowerLine.includes("phi chức năng")) {
        if (currentSection && currentItems.length > 0) {
          (sections as any)[currentSection] = currentItems;
        }
        currentSection = "nonFunctional";
        currentItems = [];
        return;
      }

      if (lowerLine.includes("acceptance") || lowerLine.includes("criteria") || lowerLine.includes("tiêu chí")) {
        if (currentSection && currentItems.length > 0) {
          (sections as any)[currentSection] = currentItems;
        }
        currentSection = "acceptanceCriteria";
        currentItems = [];
        return;
      }

      if (currentSection && trimmed) {
        const cleanLine = trimmed.replace(/^[-*•]\s*/, "").replace(/^\d+\.\s*/, "");
        if (cleanLine) {
          currentItems.push(cleanLine);
        }
      } else if (!currentSection && trimmed) {
        sections.overview = (sections.overview || "") + trimmed + "\n";
      }
    });

    if (currentSection && currentItems.length > 0) {
      (sections as any)[currentSection] = currentItems;
    }

    return sections;
  };

  const sections = parseRequirements(content);

  return (
    <div className="space-y-4">
      {sections.overview && (
        <div>
          <h4 className="text-sm font-semibold mb-2">
            <Badge variant="default">Overview</Badge>
          </h4>
          <p className="text-sm text-muted-foreground whitespace-pre-wrap">
            {sections.overview}
          </p>
        </div>
      )}

      {sections.functional && sections.functional.length > 0 && (
        <div>
          <h4 className="text-sm font-semibold mb-2">
            <Badge variant="secondary">Functional Requirements</Badge>
          </h4>
          <ul className="space-y-2">
            {sections.functional.map((item, index) => (
              <li key={index} className="flex items-start gap-2 text-sm">
                <span className="text-primary font-medium mt-0.5">FR-{index + 1}.</span>
                <span>{item}</span>
              </li>
            ))}
          </ul>
        </div>
      )}

      {sections.nonFunctional && sections.nonFunctional.length > 0 && (
        <div>
          <h4 className="text-sm font-semibold mb-2">
            <Badge variant="outline">Non-Functional Requirements</Badge>
          </h4>
          <ul className="space-y-2">
            {sections.nonFunctional.map((item, index) => (
              <li key={index} className="flex items-start gap-2 text-sm">
                <span className="text-muted-foreground font-medium mt-0.5">NFR-{index + 1}.</span>
                <span>{item}</span>
              </li>
            ))}
          </ul>
        </div>
      )}

      {sections.acceptanceCriteria && sections.acceptanceCriteria.length > 0 && (
        <div>
          <h4 className="text-sm font-semibold mb-2">
            <Badge variant="default">Acceptance Criteria</Badge>
          </h4>
          <ul className="space-y-2">
            {sections.acceptanceCriteria.map((item, index) => (
              <li key={index} className="flex items-start gap-2 text-sm">
                <CheckSquare className="w-4 h-4 mt-0.5 text-green-500 flex-shrink-0" />
                <span>{item}</span>
              </li>
            ))}
          </ul>
        </div>
      )}

      {!sections.overview && !sections.functional && (
        <MarkdownRenderer content={content} />
      )}
    </div>
  );
}
