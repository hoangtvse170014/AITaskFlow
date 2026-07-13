"use client";

import * as React from "react";
import { useMutation } from "@tanstack/react-query";
import {
  Bot,
  Send,
  Loader2,
  Copy,
  Check,
  RefreshCw,
  Trash2,
  X,
  Wifi,
  WifiOff,
  Sparkles,
  ChevronDown,
  ExternalLink,
} from "lucide-react";
import { Button } from "@/components/ui/button";
import { ScrollArea } from "@/components/ui/scroll-area";
import { Badge } from "@/components/ui/badge";
import { Card, CardContent } from "@/components/ui/card";
import { aiApi } from "@/lib/ai-api";
import { toast } from "react-hot-toast";
import { cn } from "@/lib/utils";
import type { ChatMessage, WorkspaceAnswerResponse } from "@/types/ai";

const STORAGE_KEY = "taskflow-workspace-chat";

const SUGGESTED_QUESTIONS = [
  "Which member is overloaded?",
  "Summarize this workspace.",
  "What should be my priority?",
  "Which tasks are overdue?",
  "Which project is most risky?",
  "What should we do next week?",
  "Show project health.",
];

interface WorkspaceAssistantPanelProps {
  workspaceId: string;
  open: boolean;
  onClose: () => void;
}

function loadConversation(workspaceId: string): ChatMessage[] {
  if (typeof window === "undefined") return [];
  try {
    const stored = localStorage.getItem(`${STORAGE_KEY}-${workspaceId}`);
    if (stored) {
      return JSON.parse(stored);
    }
  } catch {
    // Ignore
  }
  return [];
}

function saveConversation(workspaceId: string, messages: ChatMessage[]) {
  if (typeof window === "undefined") return;
  try {
    localStorage.setItem(`${STORAGE_KEY}-${workspaceId}`, JSON.stringify(messages));
  } catch {
    // Ignore
  }
}

function TypingIndicator() {
  return (
    <div className="flex items-center gap-1 p-3">
      <span className="sr-only">AI is thinking...</span>
      <div className="flex gap-1">
        {[0, 1, 2].map((i) => (
          <div
            key={i}
            className="w-2 h-2 bg-muted-foreground/60 rounded-full animate-bounce"
            style={{ animationDelay: `${i * 150}ms`, animationDuration: "600ms" }}
          />
        ))}
      </div>
    </div>
  );
}

function MarkdownRenderer({ content }: { content: string }) {
  const renderMarkdown = (text: string): React.ReactNode[] => {
    const lines = text.split("\n");
    const elements: React.ReactNode[] = [];
    let inCodeBlock = false;
    let codeContent: string[] = [];

    lines.forEach((line, index) => {
      if (line.startsWith("```")) {
        if (inCodeBlock) {
          elements.push(
            <pre key={`code-${index}`} className="bg-muted p-3 rounded-lg overflow-x-auto my-2 text-sm">
              <code>{codeContent.join("\n")}</code>
            </pre>
          );
          codeContent = [];
          inCodeBlock = false;
        } else {
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
          <h4 key={index} className="text-sm font-semibold mt-3 mb-1">
            {line.slice(4)}
          </h4>
        );
        return;
      }
      if (line.startsWith("## ")) {
        elements.push(
          <h3 key={index} className="text-base font-semibold mt-3 mb-1">
            {line.slice(3)}
          </h3>
        );
        return;
      }
      if (line.startsWith("# ")) {
        elements.push(
          <h2 key={index} className="text-lg font-bold mt-3 mb-1">
            {line.slice(2)}
          </h2>
        );
        return;
      }

      // List items
      if (line.match(/^[-*] /)) {
        elements.push(
          <li key={index} className="ml-4 list-disc text-sm">
            {renderInline(line.slice(2))}
          </li>
        );
        return;
      }
      if (line.match(/^\d+\. /)) {
        elements.push(
          <li key={index} className="ml-4 list-decimal text-sm">
            {renderInline(line.replace(/^\d+\. /, ""))}
          </li>
        );
        return;
      }

      // Empty line
      if (!line.trim()) {
        elements.push(<div key={index} className="h-2" />);
        return;
      }

      // Paragraph
      elements.push(
        <p key={index} className="text-sm leading-relaxed mb-1">
          {renderInline(line)}
        </p>
      );
    });

    return elements;
  };

  const renderInline = (text: string): React.ReactNode => {
    const parts = text.split(/(\*\*.*?\*\*|\*.*?\*|`.*?`|\[.*?\]\(.*?\))/g);
    return parts.map((part, i) => {
      if (part.startsWith("**") && part.endsWith("**")) {
        return <strong key={i}>{part.slice(2, -2)}</strong>;
      }
      if (part.startsWith("*") && part.endsWith("*")) {
        return <em key={i}>{part.slice(1, -1)}</em>;
      }
      if (part.startsWith("`") && part.endsWith("`")) {
        return (
          <code key={i} className="bg-muted px-1 rounded text-xs">
            {part.slice(1, -1)}
          </code>
        );
      }
      if (part.match(/\[.*?\]\(.*?\)/)) {
        const match = part.match(/\[(.*?)\]\((.*?)\)/);
        if (match) {
          return (
            <a
              key={i}
              href={match[2]}
              target="_blank"
              rel="noopener noreferrer"
              className="text-primary underline"
            >
              {match[1]}
            </a>
          );
        }
      }
      return part;
    });
  };

  return <div className="space-y-1">{renderMarkdown(content)}</div>;
}

export function WorkspaceAssistantPanel({ workspaceId, open, onClose }: WorkspaceAssistantPanelProps) {
  const [messages, setMessages] = React.useState<ChatMessage[]>([]);
  const [input, setInput] = React.useState("");
  const [copiedId, setCopiedId] = React.useState<string | null>(null);
  const [isOnline, setIsOnline] = React.useState(true);
  const scrollRef = React.useRef<HTMLDivElement>(null);
  const inputRef = React.useRef<HTMLTextAreaElement>(null);

  // Load conversation on mount
  React.useEffect(() => {
    const stored = loadConversation(workspaceId);
    setMessages(stored);
  }, [workspaceId]);

  // Save conversation on change
  React.useEffect(() => {
    if (messages.length > 0) {
      saveConversation(workspaceId, messages);
    }
  }, [messages, workspaceId]);

  // Auto-scroll to bottom
  React.useEffect(() => {
    if (scrollRef.current) {
      scrollRef.current.scrollTop = scrollRef.current.scrollHeight;
    }
  }, [messages]);

  const chatMutation = useMutation({
    mutationFn: async (question: string) => {
      const response = await aiApi.askWorkspace({ workspaceId, question });
      return response;
    },
    onSuccess: (data: WorkspaceAnswerResponse, question: string) => {
      // Add user message
      const userMessage: ChatMessage = {
        id: `user-${Date.now()}`,
        role: "user",
        content: question,
        timestamp: Date.now(),
      };

      // Add assistant message
      const assistantMessage: ChatMessage = {
        id: `assistant-${Date.now()}`,
        role: "assistant",
        content: data.answer || "I couldn't find an answer to your question.",
        timestamp: Date.now(),
        sources: data.sources,
        relatedProjects: data.relatedProjects,
        relatedTasks: data.relatedTasks,
        suggestions: data.suggestions,
        confidence: data.confidence,
        processingTimeMs: data.processingTimeMs,
      };

      setMessages((prev) => [...prev, userMessage, assistantMessage]);
    },
    onError: (error) => {
      toast.error(error instanceof Error ? error.message : "Failed to send message");
      setIsOnline(false);
    },
  });

  const handleSend = () => {
    if (!input.trim() || chatMutation.isPending) return;
    setIsOnline(true);
    chatMutation.mutate(input.trim());
    setInput("");
  };

  const handleKeyDown = (e: React.KeyboardEvent) => {
    if (e.key === "Enter" && !e.shiftKey) {
      e.preventDefault();
      handleSend();
    }
  };

  const handleCopy = async (content: string, id: string) => {
    await navigator.clipboard.writeText(content);
    setCopiedId(id);
    toast.success("Copied to clipboard");
    setTimeout(() => setCopiedId(null), 2000);
  };

  const handleClear = () => {
    setMessages([]);
    localStorage.removeItem(`${STORAGE_KEY}-${workspaceId}`);
    toast.success("Conversation cleared");
  };

  const handleSuggestedQuestion = (question: string) => {
    setInput(question);
    inputRef.current?.focus();
  };

  if (!open) return null;

  return (
    <div className="fixed bottom-24 right-6 w-[420px] h-[600px] bg-background border rounded-xl shadow-2xl z-50 flex flex-col animate-in slide-in-from-bottom-4 fade-in duration-300">
      {/* Header */}
      <div className="flex items-center justify-between p-4 border-b bg-muted/50 rounded-t-xl">
        <div className="flex items-center gap-2">
          <div className="w-8 h-8 rounded-full bg-primary/10 flex items-center justify-center">
            <Bot className="w-4 h-4 text-primary" />
          </div>
          <div>
            <div className="flex items-center gap-1.5">
              <span className="font-semibold text-sm">TaskFlow AI</span>
              <Sparkles className="w-3 h-3 text-primary" />
            </div>
            <div className="flex items-center gap-1 text-xs text-muted-foreground">
              {isOnline ? (
                <>
                  <Wifi className="w-3 h-3 text-green-500" />
                  <span>Online</span>
                </>
              ) : (
                <>
                  <WifiOff className="w-3 h-3 text-red-500" />
                  <span>Offline</span>
                </>
              )}
            </div>
          </div>
        </div>
        <Button variant="ghost" size="icon" className="h-8 w-8" onClick={onClose}>
          <X className="w-4 h-4" />
        </Button>
      </div>

      {/* Messages */}
      <ScrollArea ref={scrollRef} className="flex-1 p-4">
        <div className="space-y-4">
          {messages.length === 0 && (
            <div className="text-center py-8">
              <div className="w-16 h-16 rounded-full bg-primary/10 flex items-center justify-center mx-auto mb-4">
                <Bot className="w-8 h-8 text-primary" />
              </div>
              <h3 className="font-semibold mb-2">Workspace Assistant</h3>
              <p className="text-sm text-muted-foreground mb-6">
                Ask me anything about your workspace, projects, and tasks.
              </p>

              {/* Suggested Questions */}
              <div className="space-y-2">
                <p className="text-xs text-muted-foreground font-medium">Suggested Questions</p>
                <div className="flex flex-wrap gap-2 justify-center">
                  {SUGGESTED_QUESTIONS.slice(0, 4).map((q) => (
                    <button
                      key={q}
                      onClick={() => handleSuggestedQuestion(q)}
                      className="text-xs px-3 py-1.5 rounded-full border bg-card hover:bg-muted transition-colors"
                    >
                      {q}
                    </button>
                  ))}
                </div>
              </div>
            </div>
          )}

          {messages.map((message) => (
            <div
              key={message.id}
              className={cn(
                "flex",
                message.role === "user" ? "justify-end" : "justify-start"
              )}
            >
              <div
                className={cn(
                  "max-w-[85%] rounded-2xl px-4 py-3",
                  message.role === "user"
                    ? "bg-primary text-primary-foreground"
                    : "bg-muted"
                )}
              >
                {message.role === "assistant" && (
                  <div className="flex items-center gap-1 mb-2 text-xs text-muted-foreground">
                    <Bot className="w-3 h-3" />
                    <span>TaskFlow AI</span>
                    {message.processingTimeMs && (
                      <span>• {Math.round(message.processingTimeMs / 1000)}s</span>
                    )}
                  </div>
                )}

                <MarkdownRenderer content={message.content} />

                {/* Related Items */}
                {message.role === "assistant" && (message.relatedProjects?.length || message.relatedTasks?.length) && (
                  <div className="mt-3 pt-3 border-t border-border/50">
                    {message.relatedProjects && message.relatedProjects.length > 0 && (
                      <div className="mb-2">
                        <p className="text-xs font-medium mb-1">Related Projects</p>
                        <div className="flex flex-wrap gap-1">
                          {message.relatedProjects.slice(0, 3).map((item, i) => (
                            <Badge key={i} variant="secondary" className="text-xs">
                              {item.name}
                            </Badge>
                          ))}
                        </div>
                      </div>
                    )}
                    {message.relatedTasks && message.relatedTasks.length > 0 && (
                      <div>
                        <p className="text-xs font-medium mb-1">Related Tasks</p>
                        <div className="space-y-1">
                          {message.relatedTasks.slice(0, 3).map((item, i) => (
                            <div key={i} className="flex items-center gap-1 text-xs">
                              <span className="text-muted-foreground">•</span>
                              <span>{item.name}</span>
                              {item.status && (
                                <Badge variant="outline" className="text-xs ml-1">
                                  {item.status}
                                </Badge>
                              )}
                            </div>
                          ))}
                        </div>
                      </div>
                    )}
                  </div>
                )}

                {/* Actions */}
                <div className="flex items-center gap-1 mt-2 pt-2 border-t border-black/10 dark:border-white/10">
                  <button
                    onClick={() => handleCopy(message.content, message.id)}
                    className="p-1 rounded hover:bg-black/10 dark:hover:bg-white/10"
                  >
                    {copiedId === message.id ? (
                      <Check className="w-3 h-3" />
                    ) : (
                      <Copy className="w-3 h-3" />
                    )}
                  </button>
                  {message.role === "assistant" && (
                    <button
                      onClick={() => {
                        // Regenerate - just re-send the last user message
                      }}
                      className="p-1 rounded hover:bg-black/10 dark:hover:bg-white/10"
                    >
                      <RefreshCw className="w-3 h-3" />
                    </button>
                  )}
                </div>
              </div>
            </div>
          ))}

          {chatMutation.isPending && (
            <div className="flex justify-start">
              <div className="bg-muted rounded-2xl px-4 py-3">
                <div className="flex items-center gap-2 mb-2">
                  <Bot className="w-4 h-4 text-primary" />
                  <span className="text-sm font-medium">Thinking...</span>
                </div>
                <TypingIndicator />
              </div>
            </div>
          )}
        </div>
      </ScrollArea>

      {/* More Suggestions */}
      {messages.length > 0 && messages.length < 10 && (
        <div className="px-4 pb-2">
          <div className="flex flex-wrap gap-1">
            {SUGGESTED_QUESTIONS.slice(4).map((q) => (
              <button
                key={q}
                onClick={() => handleSuggestedQuestion(q)}
                className="text-xs px-2 py-1 rounded-full border bg-card hover:bg-muted transition-colors"
              >
                {q}
              </button>
            ))}
          </div>
        </div>
      )}

      {/* Input */}
      <div className="p-4 border-t bg-muted/50 rounded-b-xl">
        <div className="flex items-end gap-2">
          <div className="flex-1 relative">
            <textarea
              ref={inputRef}
              value={input}
              onChange={(e) => setInput(e.target.value)}
              onKeyDown={handleKeyDown}
              placeholder="Ask about your workspace..."
              rows={1}
              className="w-full resize-none rounded-lg border bg-background px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-primary"
              style={{ maxHeight: "120px" }}
            />
          </div>
          <Button
            size="icon"
            onClick={handleSend}
            disabled={!input.trim() || chatMutation.isPending}
          >
            {chatMutation.isPending ? (
              <Loader2 className="w-4 h-4 animate-spin" />
            ) : (
              <Send className="w-4 h-4" />
            )}
          </Button>
        </div>
        {messages.length > 0 && (
          <div className="flex justify-between items-center mt-2">
            <button
              onClick={handleClear}
              className="text-xs text-muted-foreground hover:text-destructive flex items-center gap-1"
            >
              <Trash2 className="w-3 h-3" />
              Clear
            </button>
            <span className="text-xs text-muted-foreground">
              {messages.length} messages
            </span>
          </div>
        )}
      </div>
    </div>
  );
}
