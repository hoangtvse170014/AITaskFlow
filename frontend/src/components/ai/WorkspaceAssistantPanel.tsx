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
  Users,
  Target,
  FileText,
  AlertTriangle,
  Calendar,
  BarChart3,
  Zap,
  ListChecks,
  ChevronRight,
  Clock,
  UserCheck,
  Wand2,
} from "lucide-react";
import { Button } from "@/components/ui/button";
import { ScrollArea } from "@/components/ui/scroll-area";
import { Badge } from "@/components/ui/badge";
import { aiApi } from "@/lib/ai-api";
import { toast } from "react-hot-toast";
import { cn } from "@/lib/utils";
import type {
  ChatMessage,
  WorkspaceAnswerResponse,
  RelatedItem,
  SprintGenerateResponse,
} from "@/types/ai";

const STORAGE_KEY = "taskflow-workspace-chat";

const QUICK_ACTIONS = [
  { icon: BarChart3,  label: "Tóm tắt workspace",  question: "Tóm tắt workspace" },
  { icon: AlertTriangle, label: "Dự án rủi ro nhất", question: "Dự án nào đang rủi ro nhất?" },
  { icon: Users,      label: "Ai đang quá tải?",  question: "Ai đang quá tải?" },
  { icon: Zap,        label: "Nên làm gì?",         question: "Tôi nên làm task nào trước?" },
  { icon: Calendar,   label: "Deadline sắp tới",     question: "Deadline sắp tới là gì?" },
  { icon: ListChecks, label: "Lên sprint",           question: "Lên sprint 2 tuần tới" },
  { icon: Clock,      label: "Báo cáo tuần",         question: "Báo cáo tuần này" },
  { icon: UserCheck,  label: "Blockers",              question: "Blockers hiện tại là gì?" },
];

interface WorkspaceAssistantPanelProps {
  workspaceId: string;
  open: boolean;
  onClose: () => void;
  onOpenDemo?: () => void;
}

function loadConversation(workspaceId: string): ChatMessage[] {
  if (typeof window === "undefined") return [];
  try {
    const stored = localStorage.getItem(`${STORAGE_KEY}-${workspaceId}`);
    if (stored) return JSON.parse(stored);
  } catch { /* ignore */ }
  return [];
}

function saveConversation(workspaceId: string, messages: ChatMessage[]) {
  if (typeof window === "undefined") return;
  try {
    localStorage.setItem(`${STORAGE_KEY}-${workspaceId}`, JSON.stringify(messages));
  } catch { /* ignore */ }
}

function TypingIndicator() {
  return (
    <div className="flex items-center gap-1 p-3">
      <span className="sr-only">AI đang suy nghĩ...</span>
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

function IntentBadge({ intent }: { intent?: string }) {
  const map: Record<string, { label: string; color: string }> = {
    SUMMARIZE_WORKSPACE:   { label: "Tóm tắt",    color: "bg-blue-100 text-blue-700 dark:bg-blue-900 dark:text-blue-300" },
    MOST_RISKY_PROJECT:    { label: "Rủi ro",     color: "bg-red-100 text-red-700 dark:bg-red-900 dark:text-red-300" },
    WHO_IS_OVERLOADED:     { label: "Workload",   color: "bg-orange-100 text-orange-700 dark:bg-orange-900 dark:text-orange-300" },
    WHAT_SHOULD_I_DO:      { label: "Ưu tiên",    color: "bg-purple-100 text-purple-700 dark:bg-purple-900 dark:text-purple-300" },
    GENERATE_SPRINT:      { label: "Sprint",      color: "bg-green-100 text-green-700 dark:bg-green-900 dark:text-green-300" },
    SUGGEST_ASSIGNEE:      { label: "Gợi ý giao", color: "bg-cyan-100 text-cyan-700 dark:bg-cyan-900 dark:text-cyan-300" },
    WEEKLY_REPORT:         { label: "Báo cáo tuần",color: "bg-teal-100 text-teal-700 dark:bg-teal-900 dark:text-teal-300" },
    DAILY_REPORT:          { label: "Daily",       color: "bg-indigo-100 text-indigo-700 dark:bg-indigo-900 dark:text-indigo-300" },
    BLOCKERS:              { label: "Blockers",    color: "bg-yellow-100 text-yellow-700 dark:bg-yellow-900 dark:text-yellow-300" },
    UPCOMING_DEADLINES:    { label: "Deadline",    color: "bg-pink-100 text-pink-700 dark:bg-pink-900 dark:text-pink-300" },
    GOALS_PROGRESS:        { label: "Goals",       color: "bg-lime-100 text-lime-700 dark:bg-lime-900 dark:text-lime-300" },
    PROJECT_HEALTH:        { label: "Health",      color: "bg-sky-100 text-sky-700 dark:bg-sky-900 dark:text-sky-300" },
  };
  if (!intent) return null;
  const cfg = map[intent];
  if (!cfg) return null;
  return (
    <span className={cn("inline-flex items-center px-1.5 py-0.5 rounded text-[10px] font-medium", cfg.color)}>
      {cfg.label}
    </span>
  );
}

function RelatedItemsRenderer({ message }: { message: ChatMessage }) {
  const hasRelated =
    (message.relatedProjects?.length ?? 0) > 0 ||
    (message.relatedTasks?.length ?? 0) > 0 ||
    (message.relatedMembers?.length ?? 0) > 0 ||
    (message.relatedGoals?.length ?? 0) > 0 ||
    (message.relatedPages?.length ?? 0) > 0;
  if (!hasRelated) return null;

  return (
    <div className="mt-3 pt-3 border-t border-border/50 space-y-2">
      {message.relatedProjects && message.relatedProjects.length > 0 && (
        <div>
          <p className="text-[10px] font-medium text-muted-foreground mb-1 flex items-center gap-1">
            <BarChart3 className="w-3 h-3" /> Dự án
          </p>
          <div className="flex flex-wrap gap-1">
            {message.relatedProjects.slice(0, 5).map((item, i) => (
              <Badge key={i} variant="secondary" className="text-[10px]">
                {item.name}
              </Badge>
            ))}
          </div>
        </div>
      )}

      {message.relatedTasks && message.relatedTasks.length > 0 && (
        <div>
          <p className="text-[10px] font-medium text-muted-foreground mb-1 flex items-center gap-1">
            <ListChecks className="w-3 h-3" /> Tasks
          </p>
          <div className="space-y-0.5">
            {message.relatedTasks.slice(0, 5).map((item, i) => (
              <div key={i} className="flex items-center gap-1 text-[10px]">
                <span className="text-muted-foreground">•</span>
                <span>{item.description || item.name}</span>
                {item.status && (
                  <Badge
                    variant="outline"
                    className={cn(
                      "text-[9px] px-1 py-0",
                      item.status === "DONE" && "bg-green-500/10 text-green-600 border-green-500/30",
                      item.status === "IN_PROGRESS" && "bg-blue-500/10 text-blue-600 border-blue-500/30",
                      item.status === "REVIEW" && "bg-yellow-500/10 text-yellow-600 border-yellow-500/30",
                      item.status === "TODO" && "bg-gray-500/10 text-gray-600 border-gray-500/30"
                    )}
                  >
                    {item.status}
                  </Badge>
                )}
              </div>
            ))}
          </div>
        </div>
      )}

      {message.relatedMembers && message.relatedMembers.length > 0 && (
        <div>
          <p className="text-[10px] font-medium text-muted-foreground mb-1 flex items-center gap-1">
            <Users className="w-3 h-3" /> Thành viên
          </p>
          <div className="flex flex-wrap gap-1">
            {message.relatedMembers.slice(0, 5).map((item, i) => (
              <Badge key={i} variant="outline" className="text-[10px]">
                {item.name}
                {item.status && <span className="ml-1 opacity-60">({item.status})</span>}
              </Badge>
            ))}
          </div>
        </div>
      )}

      {message.relatedGoals && message.relatedGoals.length > 0 && (
        <div>
          <p className="text-[10px] font-medium text-muted-foreground mb-1 flex items-center gap-1">
            <Target className="w-3 h-3" /> Goals
          </p>
          <div className="flex flex-wrap gap-1">
            {message.relatedGoals.slice(0, 5).map((item, i) => (
              <Badge key={i} variant="outline" className="text-[10px]">
                {item.name}
              </Badge>
            ))}
          </div>
        </div>
      )}

      {message.relatedPages && message.relatedPages.length > 0 && (
        <div>
          <p className="text-[10px] font-medium text-muted-foreground mb-1 flex items-center gap-1">
            <FileText className="w-3 h-3" /> Trang tài liệu
          </p>
          <div className="flex flex-wrap gap-1">
            {message.relatedPages.slice(0, 5).map((item, i) => (
              <Badge key={i} variant="outline" className="text-[10px]">
                {item.name}
              </Badge>
            ))}
          </div>
        </div>
      )}

      {message.suggestions && message.suggestions.length > 0 && (
        <div>
          <p className="text-[10px] font-medium text-muted-foreground mb-1">Gợi ý tiếp theo</p>
          <div className="flex flex-col gap-0.5">
            {message.suggestions.slice(0, 4).map((s, i) => (
              <button
                key={i}
                className="text-left text-[10px] text-primary hover:underline flex items-center gap-1"
              >
                <ChevronRight className="w-2.5 h-2.5" />
                {s}
              </button>
            ))}
          </div>
        </div>
      )}
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
      if (inCodeBlock) { codeContent.push(line); return; }

      if (line.startsWith("### ")) {
        elements.push(<h4 key={index} className="text-sm font-semibold mt-3 mb-1">{line.slice(4)}</h4>);
        return;
      }
      if (line.startsWith("## ")) {
        elements.push(<h3 key={index} className="text-base font-semibold mt-3 mb-1">{line.slice(3)}</h3>);
        return;
      }
      if (line.startsWith("# ")) {
        elements.push(<h2 key={index} className="text-lg font-bold mt-3 mb-1">{line.slice(2)}</h2>);
        return;
      }
      if (line.match(/^[-*] /)) {
        elements.push(<li key={index} className="ml-4 list-disc text-sm">{renderInline(line.slice(2))}</li>);
        return;
      }
      if (line.match(/^\d+\. /)) {
        elements.push(<li key={index} className="ml-4 list-decimal text-sm">{renderInline(line.replace(/^\d+\. /, ""))}</li>);
        return;
      }
      if (line.match(/^\|.+\|$/)) {
        // Simple table row rendering
        const cells = line.split("|").filter((_, i, a) => i > 0 && i < a.length - 1);
        if (cells.length > 0) {
          const isHeader = elements.length === 0 ||
            elements[elements.length - 1]?.toString().includes("|");
          elements.push(
            <div key={index} className={cn("flex gap-2 text-xs py-0.5 px-1",
              isHeader && "font-semibold bg-muted/50 rounded")}>
              {cells.map((cell, ci) => (
                <span key={ci} className="flex-1">{renderInline(cell.trim())}</span>
              ))}
            </div>
          );
          return;
        }
      }
      if (!line.trim()) { elements.push(<div key={index} className="h-2" />); return; }
      elements.push(<p key={index} className="text-sm leading-relaxed mb-1">{renderInline(line)}</p>);
    });
    return elements;
  };

  const renderInline = (text: string): React.ReactNode => {
    const parts = text.split(/(\*\*.*?\*\*|\*.*?\*|`.*?`|\[.*?\]\(.*?\))/g);
    return parts.map((part, i) => {
      if (part.startsWith("**") && part.endsWith("**")) return <strong key={i}>{part.slice(2, -2)}</strong>;
      if (part.startsWith("*") && part.endsWith("*")) return <em key={i}>{part.slice(1, -1)}</em>;
      if (part.startsWith("`") && part.endsWith("`")) return <code key={i} className="bg-muted px-1 rounded text-xs">{part.slice(1, -1)}</code>;
      if (part.match(/\[.*?\]\(.*?\)/)) {
        const match = part.match(/\[(.*?)\]\((.*?)\)/);
        if (match) return <a key={i} href={match[2]} target="_blank" rel="noopener noreferrer" className="text-primary underline">{match[1]}</a>;
      }
      return part;
    });
  };

  return <div className="space-y-1">{renderMarkdown(content)}</div>;
}

export function WorkspaceAssistantPanel({ workspaceId, open, onClose, onOpenDemo }: WorkspaceAssistantPanelProps) {
  const [messages, setMessages] = React.useState<ChatMessage[]>([]);
  const [input, setInput] = React.useState("");
  const [copiedId, setCopiedId] = React.useState<string | null>(null);
  const [isOnline, setIsOnline] = React.useState(true);
  const scrollRef = React.useRef<HTMLDivElement>(null);
  const inputRef = React.useRef<HTMLTextAreaElement>(null);

  React.useEffect(() => {
    const stored = loadConversation(workspaceId);
    setMessages(stored);
  }, [workspaceId]);

  React.useEffect(() => {
    if (messages.length > 0) saveConversation(workspaceId, messages);
  }, [messages, workspaceId]);

  React.useEffect(() => {
    if (scrollRef.current) scrollRef.current.scrollTop = scrollRef.current.scrollHeight;
  }, [messages]);

  const chatMutation = useMutation({
    mutationFn: async (question: string) => {
      const response = await aiApi.askWorkspace({ workspaceId, question });
      return response;
    },
    onSuccess: (data: WorkspaceAnswerResponse, question: string) => {
      const userMessage: ChatMessage = {
        id: `user-${Date.now()}`,
        role: "user",
        content: question,
        timestamp: Date.now(),
      };
      const assistantMessage: ChatMessage = {
        id: `assistant-${Date.now()}`,
        role: "assistant",
        content: data.answer || "Tôi không tìm thấy câu trả lời phù hợp.",
        timestamp: Date.now(),
        sources: data.sources,
        relatedProjects: data.relatedProjects,
        relatedTasks: data.relatedTasks,
        relatedMembers: data.relatedMembers,
        relatedGoals: data.relatedGoals,
        relatedPages: data.relatedPages,
        suggestions: data.suggestions,
        confidence: data.confidence,
        processingTimeMs: data.processingTimeMs,
      };
      setMessages((prev) => [...prev, userMessage, assistantMessage]);
    },
    onError: () => {
      toast.error("Không thể gửi tin nhắn");
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
    if (e.key === "Enter" && !e.shiftKey) { e.preventDefault(); handleSend(); }
  };

  const handleCopy = async (content: string, id: string) => {
    await navigator.clipboard.writeText(content);
    setCopiedId(id);
    toast.success("Đã sao chép");
    setTimeout(() => setCopiedId(null), 2000);
  };

  const handleClear = () => {
    setMessages([]);
    localStorage.removeItem(`${STORAGE_KEY}-${workspaceId}`);
    toast.success("Đã xóa cuộc trò chuyện");
  };

  const handleQuickAction = (question: string) => {
    setInput(question);
    inputRef.current?.focus();
  };

  if (!open) return null;

  return (
    <div className="fixed bottom-24 right-6 w-[460px] h-[620px] bg-background border rounded-xl shadow-2xl z-50 flex flex-col animate-in slide-in-from-bottom-4 fade-in duration-300">
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
                <><Wifi className="w-3 h-3 text-green-500" /><span>Online</span></>
              ) : (
                <><WifiOff className="w-3 h-3 text-red-500" /><span>Offline</span></>
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
            <>
              <div className="text-center py-4">
                <div className="w-14 h-14 rounded-full bg-primary/10 flex items-center justify-center mx-auto mb-3">
                  <Bot className="w-7 h-7 text-primary" />
                </div>
                <h3 className="font-semibold mb-1">Workspace Assistant</h3>
                <p className="text-xs text-muted-foreground mb-3">
                  Hỏi tôi về workspace, dự án, team, deadline...
                </p>
                {onOpenDemo && (
                  <button
                    onClick={() => { onOpenDemo(); onClose(); }}
                    className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-full bg-gradient-to-r from-primary/10 to-violet-500/10 border border-primary/30 text-primary text-xs font-medium hover:from-primary/20 hover:to-violet-500/20 transition"
                  >
                    <Wand2 className="w-3.5 h-3.5" /> Thử Demo Mode
                  </button>
                )}
              </div>

              {/* Quick actions */}
              <div className="grid grid-cols-2 gap-1.5">
                {QUICK_ACTIONS.slice(0, 6).map((action) => (
                  <button
                    key={action.question}
                    onClick={() => handleQuickAction(action.question)}
                    className="flex items-center gap-1.5 px-3 py-2 rounded-lg border bg-card hover:bg-muted transition-colors text-xs text-left"
                  >
                    <action.icon className="w-3.5 h-3.5 text-primary flex-shrink-0" />
                    <span className="line-clamp-2">{action.label}</span>
                  </button>
                ))}
              </div>

              <div className="grid grid-cols-2 gap-1.5">
                {QUICK_ACTIONS.slice(6).map((action) => (
                  <button
                    key={action.question}
                    onClick={() => handleQuickAction(action.question)}
                    className="flex items-center gap-1.5 px-3 py-2 rounded-lg border bg-card hover:bg-muted transition-colors text-xs text-left"
                  >
                    <action.icon className="w-3.5 h-3.5 text-primary flex-shrink-0" />
                    <span className="line-clamp-2">{action.label}</span>
                  </button>
                ))}
              </div>
            </>
          )}

          {messages.map((message) => (
            <div
              key={message.id}
              className={cn("flex", message.role === "user" ? "justify-end" : "justify-start")}
            >
              <div
                className={cn(
                  "max-w-[88%] rounded-2xl px-4 py-3",
                  message.role === "user"
                    ? "bg-primary text-primary-foreground"
                    : "bg-muted"
                )}
              >
                {message.role === "assistant" && (
                  <div className="flex items-center gap-2 mb-2 text-[10px] text-muted-foreground">
                    <Bot className="w-3 h-3" />
                    <span>TaskFlow AI</span>
                    <span>•</span>
                    <span>{Math.round((message.processingTimeMs ?? 0) / 1000)}s</span>
                    <span>•</span>
                    {message.confidence && (
                      <span className={cn(
                        message.confidence >= 0.8 ? "text-green-600" :
                        message.confidence >= 0.5 ? "text-yellow-600" : "text-red-600"
                      )}>
                        {Math.round(message.confidence * 100)}%
                      </span>
                    )}
                  </div>
                )}

                <MarkdownRenderer content={message.content} />

                <RelatedItemsRenderer message={message} />

                {/* Actions */}
                <div className="flex items-center gap-1 mt-2 pt-2 border-t border-black/10 dark:border-white/10">
                  <button
                    onClick={() => handleCopy(message.content, message.id)}
                    className="p-1 rounded hover:bg-black/10 dark:hover:bg-white/10"
                  >
                    {copiedId === message.id
                      ? <Check className="w-3 h-3" />
                      : <Copy className="w-3 h-3" />
                    }
                  </button>
                  <button
                    onClick={() => {}}
                    className="p-1 rounded hover:bg-black/10 dark:hover:bg-white/10"
                  >
                    <RefreshCw className="w-3 h-3" />
                  </button>
                </div>
              </div>
            </div>
          ))}

          {chatMutation.isPending && (
            <div className="flex justify-start">
              <div className="bg-muted rounded-2xl px-4 py-3">
                <div className="flex items-center gap-2 mb-2">
                  <Bot className="w-4 h-4 text-primary" />
                  <span className="text-sm font-medium">Đang suy nghĩ...</span>
                </div>
                <TypingIndicator />
              </div>
            </div>
          )}
        </div>
      </ScrollArea>

      {/* Input */}
      <div className="p-4 border-t bg-muted/50 rounded-b-xl">
        <div className="flex items-end gap-2">
          <div className="flex-1 relative">
            <textarea
              ref={inputRef}
              value={input}
              onChange={(e) => setInput(e.target.value)}
              onKeyDown={handleKeyDown}
              placeholder="Hỏi về workspace..."
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
              Xóa
            </button>
            <span className="text-xs text-muted-foreground">
              {messages.length} tin nhắn
            </span>
          </div>
        )}
      </div>
    </div>
  );
}
