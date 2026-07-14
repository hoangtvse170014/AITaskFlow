"use client";

import * as React from "react";
import { useMutation } from "@tanstack/react-query";
import {
  Sparkles,
  Loader2,
  Wand2,
  CheckCircle2,
  XCircle,
  Clock,
  ListChecks,
  Users,
  Calendar,
  AlertTriangle,
  Activity,
  RotateCw,
  ChevronRight,
  Circle,
  Gauge,
  Briefcase,
  Hourglass,
  Target,
  RefreshCw,
} from "lucide-react";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { Input } from "@/components/ui/input";
import { Progress } from "@/components/ui/progress";
import { Separator } from "@/components/ui/separator";
import { ScrollArea } from "@/components/ui/scroll-area";
import { aiApi } from "@/lib/ai-api";
import { cn } from "@/lib/utils";
import { toast } from "react-hot-toast";
import type {
  DemoModeRequest,
  DemoModeResponse,
  DemoStageId,
  DemoStageResult,
  StageStatus,
} from "@/types/ai";

interface DemoModePanelProps {
  workspaceId?: string;
  className?: string;
  onSuccess?: (response: DemoModeResponse) => void;
}

interface StageView extends DemoStageResult {
  icon: React.ElementType;
  color: string;
}

const STAGE_DECORATIONS: Record<
  DemoStageId,
  { icon: React.ElementType; color: string }
> = {
  ANALYZING_REQUIREMENTS: { icon: Wand2,     color: "text-blue-500" },
  PLANNING_SPRINTS:       { icon: Calendar,  color: "text-indigo-500" },
  GENERATING_TASKS:       { icon: ListChecks, color: "text-purple-500" },
  ASSIGNING_MEMBERS:      { icon: Users,     color: "text-amber-500" },
  CREATING_ENTITIES:      { icon: Briefcase, color: "text-green-600" },
  REFRESHING_DASHBOARD:   { icon: RefreshCw, color: "text-cyan-500" },
  COMPLETED:              { icon: CheckCircle2, color: "text-emerald-600" },
};

const DEFAULT_IDEAS = [
  "Create a Grocery Management project for tracking inventory, suppliers, and orders.",
  "Build an Internal HR Onboarding system with employee records and training plans.",
  "Design a Customer Support portal with tickets, knowledge base, and SLAs.",
  "Plan a Marketing Campaign tracker with briefs, assets, and conversion analytics.",
];

export function DemoModePanel({ workspaceId, className, onSuccess }: DemoModePanelProps) {
  const [idea, setIdea] = React.useState("");
  const [teamSize, setTeamSize] = React.useState("5");
  const [weeksDeadline, setWeeksDeadline] = React.useState("8");
  const [tech, setTech] = React.useState("");
  const [result, setResult] = React.useState<DemoModeResponse | null>(null);

  // Local "virtual" stages let the UI animate before the server responds.
  const [localStages, setLocalStages] = React.useState<DemoStageResult[]>(() => buildInitialStages());

  const mutation = useMutation({
    mutationFn: async (req: DemoModeRequest) => aiApi.startDemo(req),
    onSuccess: (data) => {
      setResult(data);
      setLocalStages(data.stages ?? buildInitialStages());
      if (data.success) {
        if (data.idempotent) {
          toast.success("Project already existed (idempotent).");
        } else {
          toast.success(
            `Demo project "${data.projectName}" created with ${data.counts?.tasksCreated ?? 0} tasks!`
          );
        }
        onSuccess?.(data);
      } else {
        toast.error(data.error || "Demo Mode failed");
      }
    },
    onError: (err: any) => {
      toast.error(err?.message || "Demo Mode request failed");
      setLocalStages((prev) =>
        prev.map((s) =>
          s.status === "RUNNING" ? { ...s, status: "FAILED" as StageStatus } : s
        )
      );
    },
  });

  // Animate the "RUNNING" stage locally while waiting for the server.
  React.useEffect(() => {
    if (!mutation.isPending) return;
    let cancelled = false;
    setLocalStages(buildInitialStages());

    const tick = (stages: DemoStageResult[], cursor: number) => {
      if (cancelled) return;
      setLocalStages((prev) => {
        const next = [...prev];
        if (cursor >= next.length) return prev;
        const current = next[cursor];
        if (current && current.status === "PENDING") {
          next[cursor] = { ...current, status: "RUNNING" };
        }
        return next;
      });
    };

    let cursor = 0;
    tick(localStages, cursor);
    const interval = window.setInterval(() => {
      if (cancelled) return;
      cursor = Math.min(cursor + 1, STAGE_ORDER.length - 1);
      tick(localStages, cursor);
    }, 1800);

    return () => {
      cancelled = true;
      window.clearInterval(interval);
    };
  }, [mutation.isPending]);

  const isRunning = mutation.isPending;
  const stages = isRunning ? localStages : result?.stages ?? localStages;
  const completedCount = stages.filter((s) => s.status === "DONE").length;
  const progress = Math.round((completedCount / STAGE_ORDER.length) * 100);

  const onStart = () => {
    if (!workspaceId) {
      toast.error("No workspace selected");
      return;
    }
    if (idea.trim().length < 5) {
      toast.error("Please enter a project idea (at least 5 characters)");
      return;
    }
    const weeks = parseInt(weeksDeadline, 10);
    setResult(null);
    setLocalStages(buildInitialStages());

    const req: DemoModeRequest = {
      workspaceId,
      projectIdea: idea.trim(),
      teamSize: teamSize.trim() || "5",
      weeksDeadline: Number.isFinite(weeks) && weeks > 0 ? weeks : 8,
      technologyStack: tech.trim() || undefined,
    };
    mutation.mutate(req);
  };

  return (
    <Card className={cn("border-2 border-primary/20 shadow-lg", className)}>
      <CardHeader className="pb-3">
        <div className="flex items-center justify-between gap-2">
          <CardTitle className="flex items-center gap-2 text-xl">
            <Sparkles className="h-5 w-5 text-primary" />
            Demo Mode
          </CardTitle>
          <Badge variant="outline" className="text-xs">
            {STAGE_ORDER.length} stages
          </Badge>
        </div>
        <p className="text-sm text-muted-foreground pt-1">
          Mô tả ý tưởng dự án, AI sẽ tự động phân tích, lên kế hoạch, tạo task,
          phân công và tạo dự án thật trong workspace chỉ với một cú click.
        </p>
      </CardHeader>

      <CardContent className="space-y-5">
        {!result && !isRunning && (
          <InputForm
            idea={idea}
            setIdea={setIdea}
            teamSize={teamSize}
            setTeamSize={setTeamSize}
            weeksDeadline={weeksDeadline}
            setWeeksDeadline={setWeeksDeadline}
            tech={tech}
            setTech={setTech}
            disabled={!workspaceId}
            onStart={onStart}
            isRunning={isRunning}
            defaultIdeas={DEFAULT_IDEAS}
            onPickIdea={(v) => setIdea(v)}
          />
        )}

        {(isRunning || result) && (
          <ProgressPanel
            stages={decorateStages(stages)}
            progress={progress}
            isRunning={isRunning}
          />
        )}

        {result && (
          <ResultSummary
            result={result}
            onReset={() => {
              setResult(null);
              setIdea("");
              setLocalStages(buildInitialStages());
              mutation.reset();
            }}
          />
        )}
      </CardContent>
    </Card>
  );
}

// ---------------------------------------------------------------------------
// Sub-views
// ---------------------------------------------------------------------------

function InputForm(props: {
  idea: string;
  setIdea: (v: string) => void;
  teamSize: string;
  setTeamSize: (v: string) => void;
  weeksDeadline: string;
  setWeeksDeadline: (v: string) => void;
  tech: string;
  setTech: (v: string) => void;
  disabled: boolean;
  isRunning: boolean;
  defaultIdeas: string[];
  onPickIdea: (v: string) => void;
  onStart: () => void;
}) {
  return (
    <div className="space-y-3">
      <label className="text-sm font-medium">Ý tưởng dự án</label>
      <textarea
        value={props.idea}
        onChange={(e) => props.setIdea(e.target.value)}
        placeholder="Ví dụ: Create a Grocery Management project for tracking inventory, suppliers, and orders."
        className="flex min-h-[100px] w-full rounded-md border border-input bg-background px-3 py-2 text-sm shadow-sm placeholder:text-muted-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary focus-visible:ring-offset-2"
      />

      <div className="grid grid-cols-2 gap-3">
        <div>
          <label className="text-xs font-medium text-muted-foreground">Team size</label>
          <Input
            value={props.teamSize}
            onChange={(e) => props.setTeamSize(e.target.value)}
            placeholder="5"
            inputMode="numeric"
          />
        </div>
        <div>
          <label className="text-xs font-medium text-muted-foreground">Deadline (tuần)</label>
          <Input
            value={props.weeksDeadline}
            onChange={(e) => props.setWeeksDeadline(e.target.value)}
            placeholder="8"
            inputMode="numeric"
          />
        </div>
      </div>

      <div>
        <label className="text-xs font-medium text-muted-foreground">Tech stack (tuỳ chọn)</label>
        <Input
          value={props.tech}
          onChange={(e) => props.setTech(e.target.value)}
          placeholder="React, Spring Boot, PostgreSQL, ..."
        />
      </div>

      <div>
        <p className="text-xs font-medium text-muted-foreground mb-2">
          Hoặc chọn nhanh:
        </p>
        <div className="flex flex-wrap gap-2">
          {props.defaultIdeas.map((d) => (
            <button
              key={d}
              type="button"
              onClick={() => props.onPickIdea(d)}
              className="rounded-full border border-border bg-muted/40 px-3 py-1 text-xs hover:bg-muted transition"
            >
              {d.length > 48 ? d.substring(0, 48) + "…" : d}
            </button>
          ))}
        </div>
      </div>

      <Button
        type="button"
        size="lg"
        onClick={props.onStart}
        disabled={props.disabled || props.isRunning || props.idea.trim().length < 5}
        className="w-full"
      >
        {props.isRunning ? (
          <>
            <Loader2 className="mr-2 h-4 w-4 animate-spin" /> Đang chạy demo...
          </>
        ) : (
          <>
            <Sparkles className="mr-2 h-4 w-4" /> Bắt đầu Demo Mode
          </>
        )}
      </Button>
      {props.disabled && (
        <p className="text-xs text-amber-600">
          Vui lòng mở một workspace để chạy demo.
        </p>
      )}
    </div>
  );
}

function ProgressPanel({
  stages,
  progress,
  isRunning,
}: {
  stages: StageView[];
  progress: number;
  isRunning: boolean;
}) {
  return (
    <div className="space-y-4">
      <div className="space-y-1">
        <div className="flex items-center justify-between text-sm">
          <span className="font-medium">
            {isRunning ? "Đang thực thi..." : "Hoàn tất"}
          </span>
          <span className="text-muted-foreground">{progress}%</span>
        </div>
        <Progress value={progress} className="h-2" />
      </div>

      <ol className="space-y-2">
        {stages.map((s, i) => (
          <li
            key={s.stage}
            className={cn(
              "flex items-start gap-3 rounded-md border bg-card p-3 transition-colors",
              s.status === "RUNNING" && "border-primary/50 bg-primary/5",
              s.status === "DONE" && "border-emerald-300/50",
              s.status === "FAILED" && "border-red-400 bg-red-50",
              s.status === "SKIPPED" && "opacity-50"
            )}
          >
            <div className="mt-0.5">
              <StageIcon status={s.status} icon={s.icon} color={s.color} />
            </div>
            <div className="flex-1 min-w-0">
              <div className="flex items-center justify-between gap-2">
                <span className="text-sm font-medium">{s.label}</span>
                <StageStatusBadge status={s.status} duration={s.durationMs} />
              </div>
              {s.detail && (
                <p className="text-xs text-muted-foreground mt-0.5 break-words">
                  {s.detail}
                </p>
              )}
            </div>
            {i < stages.length - 1 && s.status === "DONE" && (
              <ChevronRight className="h-4 w-4 text-emerald-500/60" />
            )}
          </li>
        ))}
      </ol>
    </div>
  );
}

function StageIcon({
  status,
  icon: Icon,
  color,
}: {
  status: StageStatus;
  icon: React.ElementType;
  color: string;
}) {
  if (status === "DONE")
    return <CheckCircle2 className="h-5 w-5 text-emerald-500" />;
  if (status === "FAILED")
    return <XCircle className="h-5 w-5 text-red-500" />;
  if (status === "SKIPPED")
    return <Circle className="h-5 w-5 text-muted-foreground/40" />;
  if (status === "RUNNING")
    return <Loader2 className={cn("h-5 w-5 animate-spin", color)} />;
  return <Icon className={cn("h-5 w-5 opacity-60", color)} />;
}

function StageStatusBadge({
  status,
  duration,
}: {
  status: StageStatus;
  duration?: number;
}) {
  const label = (() => {
    switch (status) {
      case "DONE":    return "Hoàn tất";
      case "RUNNING": return "Đang chạy";
      case "FAILED":  return "Lỗi";
      case "SKIPPED": return "Bỏ qua";
      default:        return "Chờ";
    }
  })();
  const color = (() => {
    switch (status) {
      case "DONE":    return "bg-emerald-100 text-emerald-700";
      case "RUNNING": return "bg-blue-100 text-blue-700";
      case "FAILED":  return "bg-red-100 text-red-700";
      case "SKIPPED": return "bg-gray-100 text-gray-600";
      default:        return "bg-muted text-muted-foreground";
    }
  })();
  return (
    <div className="flex items-center gap-2">
      {typeof duration === "number" && status === "DONE" && (
        <Badge variant="outline" className="text-xs font-normal">
          <Clock className="h-3 w-3 mr-1" />
          {(duration / 1000).toFixed(1)}s
        </Badge>
      )}
      <Badge className={cn("text-xs", color)}>{label}</Badge>
    </div>
  );
}

function ResultSummary({
  result,
  onReset,
}: {
  result: DemoModeResponse;
  onReset: () => void;
}) {
  if (!result.success) {
    return (
      <div className="rounded-lg border border-red-300 bg-red-50 p-4 space-y-2">
        <div className="flex items-start gap-2">
          <AlertTriangle className="h-5 w-5 text-red-500 mt-0.5" />
          <div>
            <h4 className="font-medium text-red-700">Demo Mode thất bại</h4>
            <p className="text-sm text-red-600">{result.error}</p>
            {result.currentStage && (
              <p className="text-xs text-muted-foreground mt-1">
                Stage: {result.currentStage}
              </p>
            )}
          </div>
        </div>
        <Button size="sm" variant="outline" onClick={onReset}>
          <RotateCw className="mr-2 h-3 w-3" /> Thử lại
        </Button>
      </div>
    );
  }

  return (
    <div className="space-y-4">
      <div className="rounded-lg border border-emerald-300 bg-emerald-50 p-4 space-y-2">
        <div className="flex items-start gap-2">
          <CheckCircle2 className="h-5 w-5 text-emerald-600 mt-0.5" />
          <div className="flex-1">
            <h4 className="font-medium text-emerald-700">
              {result.idempotent ? "Đã trả về project tồn tại" : "Tạo dự án thành công"}
            </h4>
            <p className="text-sm text-emerald-700/80">
              Project:{" "}
              <span className="font-mono font-medium">{result.projectName}</span>{" "}
              {result.projectId && (
                <span className="text-xs text-muted-foreground">
                  ({result.projectId.substring(0, 8)}…)
                </span>
              )}
            </p>
          </div>
        </div>
      </div>

      {result.counts && <CountsGrid counts={result.counts} />}

      {result.timeline && <TimelineCard timeline={result.timeline} />}

      {result.workload && result.workload.length > 0 && (
        <WorkloadTable workload={result.workload} />
      )}

      {result.risks && result.risks.length > 0 && (
        <RisksList risks={result.risks} />
      )}

      {result.steps && result.steps.length > 0 && (
        <details className="rounded-md border border-border bg-muted/30 p-3">
          <summary className="cursor-pointer text-sm font-medium">
            Steps log ({result.steps.length})
          </summary>
          <ScrollArea className="h-[160px] mt-2">
            <ul className="space-y-1 text-xs text-muted-foreground">
              {result.steps.map((s, i) => (
                <li key={i} className="font-mono">{s}</li>
              ))}
            </ul>
          </ScrollArea>
        </details>
      )}

      <div className="flex justify-between items-center gap-2 pt-2">
        <div className="text-xs text-muted-foreground">
          {result.processingTimeMs ? (result.processingTimeMs / 1000).toFixed(1) : "-"}s •{" "}
          refreshSignals: {result.refreshSignals?.join(", ") || "dashboard"}
        </div>
        <Button size="sm" variant="outline" onClick={onReset}>
          <RotateCw className="mr-2 h-3 w-3" /> Chạy demo khác
        </Button>
      </div>
    </div>
  );
}

function CountsGrid({ counts }: { counts: NonNullable<DemoModeResponse["counts"]> }) {
  const items = [
    { label: "Sprints",      value: counts.sprintsCreated,    icon: Calendar,   color: "text-indigo-500" },
    { label: "Tasks",        value: counts.tasksCreated,      icon: ListChecks, color: "text-purple-500" },
    { label: "Subtasks",     value: counts.subtasksCreated,   icon: Target,     color: "text-blue-500" },
    { label: "Assignments",  value: counts.assignmentsApplied, icon: Users,     color: "text-amber-500" },
    { label: "Risks",        value: counts.risksGenerated,    icon: AlertTriangle, color: "text-red-500" },
  ];
  return (
    <div className="grid grid-cols-2 sm:grid-cols-5 gap-2">
      {items.map((it) => {
        const Icon = it.icon;
        return (
          <div key={it.label} className="rounded-md border bg-card p-3 flex flex-col items-start gap-1">
            <Icon className={cn("h-4 w-4", it.color)} />
            <span className="text-2xl font-bold">{it.value}</span>
            <span className="text-xs text-muted-foreground">{it.label}</span>
          </div>
        );
      })}
    </div>
  );
}

function TimelineCard({ timeline }: { timeline: NonNullable<DemoModeResponse["timeline"]> }) {
  return (
    <div className="rounded-md border bg-card p-3 space-y-1">
      <h5 className="text-sm font-medium flex items-center gap-2">
        <Hourglass className="h-4 w-4 text-indigo-500" /> Timeline
      </h5>
      <div className="grid grid-cols-2 sm:grid-cols-4 gap-2 text-xs">
        <Field label="Tuần"        value={timeline.totalWeeks ?? "-"} icon={Calendar} />
        <Field label="Giờ ước tính" value={timeline.totalEstimatedHours ?? "-"} icon={Clock} />
        <Field label="Story points" value={timeline.totalStoryPoints ?? "-"} icon={Gauge} />
        <Field label="Start → End"  value={timeline.startDate ? `${timeline.startDate} → ${timeline.endDate ?? "?"}` : "ngay"} icon={Activity} />
      </div>
    </div>
  );
}

function WorkloadTable({ workload }: { workload: NonNullable<DemoModeResponse["workload"]> }) {
  if (workload.length === 0) return null;
  const sorted = [...workload].sort((a, b) => (b.taskCount ?? 0) - (a.taskCount ?? 0));
  return (
    <div className="rounded-md border bg-card p-3 space-y-2">
      <h5 className="text-sm font-medium flex items-center gap-2">
        <Users className="h-4 w-4 text-amber-500" /> Workload sau khi phân công
      </h5>
      <div className="space-y-1.5">
        {sorted.map((w) => {
          const pct = Math.min(100, Math.max(0, Math.round(w.workloadPercentage ?? 0)));
          return (
            <div key={(w.memberId ?? w.memberName ?? Math.random().toString())} className="flex items-center gap-2 text-xs">
              <span className="w-28 truncate font-medium">{w.memberName ?? w.memberId}</span>
              <div className="flex-1 h-2 bg-muted rounded">
                <div
                  className={cn(
                    "h-2 rounded",
                    pct > 100 ? "bg-red-500"
                      : pct > 80 ? "bg-amber-500"
                      : "bg-emerald-500"
                  )}
                  style={{ width: `${Math.min(100, pct)}%` }}
                />
              </div>
              <span className="w-12 text-right tabular-nums text-muted-foreground">
                {pct}% · {w.taskCount ?? 0}
              </span>
            </div>
          );
        })}
      </div>
    </div>
  );
}

function RisksList({ risks }: { risks: string[] }) {
  return (
    <div className="rounded-md border bg-card p-3 space-y-2">
      <h5 className="text-sm font-medium flex items-center gap-2">
        <AlertTriangle className="h-4 w-4 text-red-500" /> Risks ({risks.length})
      </h5>
      <ul className="space-y-1 text-sm">
        {risks.slice(0, 8).map((r, i) => (
          <li key={i} className="text-muted-foreground before:content-['—'] before:mr-2 before:text-red-400">
            {r}
          </li>
        ))}
        {risks.length > 8 && (
          <li className="text-xs italic text-muted-foreground">+ {risks.length - 8} risks khác</li>
        )}
      </ul>
    </div>
  );
}

function Field({
  label, value, icon: Icon,
}: {
  label: string; value: React.ReactNode; icon: React.ElementType;
}) {
  return (
    <div className="flex flex-col">
      <span className="text-muted-foreground flex items-center gap-1">
        <Icon className="h-3 w-3" /> {label}
      </span>
      <span className="font-medium">{value}</span>
    </div>
  );
}

// ---------------------------------------------------------------------------
// Helpers
// ---------------------------------------------------------------------------

const STAGE_ORDER: DemoStageId[] = [
  "ANALYZING_REQUIREMENTS",
  "PLANNING_SPRINTS",
  "GENERATING_TASKS",
  "ASSIGNING_MEMBERS",
  "CREATING_ENTITIES",
  "REFRESHING_DASHBOARD",
];

function buildInitialStages(): DemoStageResult[] {
  return STAGE_ORDER.map((id) => ({
    stage: id,
    label: STAGE_DECORATIONS[id] ? defaultLabelFor(id) : id,
    status: "PENDING" as StageStatus,
  }));
}

function defaultLabelFor(id: DemoStageId): string {
  switch (id) {
    case "ANALYZING_REQUIREMENTS": return "Analyzing requirements...";
    case "PLANNING_SPRINTS":       return "Planning sprints...";
    case "GENERATING_TASKS":       return "Generating tasks...";
    case "ASSIGNING_MEMBERS":      return "Assigning members...";
    case "CREATING_ENTITIES":      return "Creating project...";
    case "REFRESHING_DASHBOARD":   return "Refreshing dashboard...";
    default:                       return id;
  }
}

function decorateStages(stages: DemoStageResult[]): StageView[] {
  return stages.map((s) => {
    const deco = STAGE_DECORATIONS[s.stage] ?? STAGE_DECORATIONS.ANALYZING_REQUIREMENTS;
    return { ...s, icon: deco.icon, color: deco.color };
  });
}