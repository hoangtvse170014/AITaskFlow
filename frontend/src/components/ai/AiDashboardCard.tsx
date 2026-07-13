"use client";

import * as React from "react";
import { useQuery } from "@tanstack/react-query";
import {
  Sparkles,
  RefreshCw,
  AlertTriangle,
  CheckCircle2,
  Clock,
  TrendingUp,
  Loader2,
  AlertCircle,
  ChevronDown,
  ChevronUp,
} from "lucide-react";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { aiApi } from "@/lib/ai-api";
import type { ProjectAnalysisResponse } from "@/types/ai";

interface AiDashboardCardProps {
  projectId: string;
  projectName: string;
}

function HealthScoreGauge({ score }: { score: number }) {
  const getColor = (value: number) => {
    if (value >= 80) return "text-green-500";
    if (value >= 60) return "text-yellow-500";
    if (value >= 40) return "text-orange-500";
    return "text-red-500";
  };

  const getBgColor = (value: number) => {
    if (value >= 80) return "bg-green-500";
    if (value >= 60) return "bg-yellow-500";
    if (value >= 40) return "bg-orange-500";
    return "bg-red-500";
  };

  const circumference = 2 * Math.PI * 45;
  const strokeDashoffset = circumference - (score / 100) * circumference;

  return (
    <div className="relative w-24 h-24">
      <svg className="w-24 h-24 -rotate-90" viewBox="0 0 100 100">
        <circle
          cx="50"
          cy="50"
          r="45"
          fill="none"
          stroke="currentColor"
          strokeWidth="8"
          className="text-muted"
        />
        <circle
          cx="50"
          cy="50"
          r="45"
          fill="none"
          strokeWidth="8"
          strokeLinecap="round"
          className={getBgColor(score)}
          style={{
            strokeDasharray: circumference,
            strokeDashoffset: strokeDashoffset,
            transition: "stroke-dashoffset 0.5s ease-in-out",
          }}
        />
      </svg>
      <div className="absolute inset-0 flex items-center justify-center">
        <span className={`text-2xl font-bold ${getColor(score)}`}>{score}</span>
      </div>
    </div>
  );
}

function ConfidenceBadge({ confidence }: { confidence: number }) {
  const percentage = Math.round(confidence * 100);
  const getColor = (value: number) => {
    if (value >= 0.9) return "bg-green-500/10 text-green-600";
    if (value >= 0.7) return "bg-blue-500/10 text-blue-600";
    if (value >= 0.5) return "bg-yellow-500/10 text-yellow-600";
    return "bg-gray-500/10 text-gray-600";
  };

  return (
    <span className={`inline-flex items-center px-2 py-1 rounded-full text-xs font-medium ${getColor(confidence)}`}>
      {percentage}% confident
    </span>
  );
}

function SectionItem({
  icon: Icon,
  children,
  variant = "default",
}: {
  icon: React.ElementType;
  children: React.ReactNode;
  variant?: "default" | "warning" | "success";
}) {
  const iconColorClass = {
    default: "text-muted-foreground",
    warning: "text-orange-500",
    success: "text-green-500",
  }[variant];

  return (
    <li className="flex items-start gap-2 py-1">
      <Icon className={`w-4 h-4 mt-0.5 flex-shrink-0 ${iconColorClass}`} />
      <span className="text-sm text-muted-foreground">{children}</span>
    </li>
  );
}

function LoadingSkeleton() {
  return (
    <div className="space-y-4 animate-pulse">
      <div className="flex items-center gap-4">
        <div className="w-16 h-16 rounded-full bg-muted" />
        <div className="flex-1 space-y-2">
          <div className="h-4 bg-muted rounded w-3/4" />
          <div className="h-3 bg-muted rounded w-1/2" />
        </div>
      </div>
      <div className="space-y-2">
        <div className="h-3 bg-muted rounded w-full" />
        <div className="h-3 bg-muted rounded w-5/6" />
        <div className="h-3 bg-muted rounded w-4/6" />
      </div>
      <div className="grid grid-cols-2 gap-2">
        {[1, 2, 3, 4].map((i) => (
          <div key={i} className="h-12 bg-muted rounded" />
        ))}
      </div>
    </div>
  );
}

function ErrorState({
  error,
  onRetry,
}: {
  error: string | null;
  onRetry: () => void;
}) {
  return (
    <div className="flex flex-col items-center justify-center py-8 text-center">
      <AlertCircle className="w-12 h-12 text-destructive mb-3" />
      <p className="text-sm text-muted-foreground mb-4">
        {error || "Failed to load AI analysis"}
      </p>
      <Button onClick={onRetry} variant="outline" size="sm">
        <RefreshCw className="w-4 h-4 mr-2" />
        Try Again
      </Button>
    </div>
  );
}

function CollapsibleSection({
  title,
  icon: Icon,
  items,
  defaultOpen = false,
}: {
  title: string;
  icon: React.ElementType;
  items: string[];
  defaultOpen?: boolean;
}) {
  const [isOpen, setIsOpen] = React.useState(defaultOpen);

  if (!items || items.length === 0) return null;

  return (
    <div className="border-t pt-3 mt-3">
      <button
        onClick={() => setIsOpen(!isOpen)}
        className="flex items-center justify-between w-full text-left"
      >
        <div className="flex items-center gap-2">
          <Icon className="w-4 h-4 text-muted-foreground" />
          <span className="font-medium text-sm">{title}</span>
          <span className="text-xs text-muted-foreground">({items.length})</span>
        </div>
        {isOpen ? (
          <ChevronUp className="w-4 h-4 text-muted-foreground" />
        ) : (
          <ChevronDown className="w-4 h-4 text-muted-foreground" />
        )}
      </button>
      {isOpen && (
        <ul className="mt-2 space-y-1 pl-6">
          {items.map((item, index) => (
            <SectionItem key={index} icon={AlertTriangle} variant="warning">
              {item}
            </SectionItem>
          ))}
        </ul>
      )}
    </div>
  );
}

export function AiDashboardCard({ projectId, projectName }: AiDashboardCardProps) {
  const [isExpanded, setIsExpanded] = React.useState(true);

  const {
    data: analysis,
    isLoading,
    error,
    refetch,
    isFetching,
  } = useQuery<ProjectAnalysisResponse, Error>({
    queryKey: ["ai-project-analysis", projectId],
    queryFn: () => aiApi.analyzeProject(projectId),
    staleTime: 5 * 60 * 1000,
    retry: 2,
    retryDelay: (attemptIndex) => Math.min(1000 * 2 ** attemptIndex, 30000),
  });

  const handleRetry = () => {
    refetch();
  };

  if (isLoading) {
    return (
      <Card className="border-dashed">
        <CardHeader className="pb-2">
          <div className="flex items-center gap-2">
            <Sparkles className="w-5 h-5 text-primary" />
            <CardTitle className="text-lg">AI Project Analysis</CardTitle>
          </div>
        </CardHeader>
        <CardContent>
          <LoadingSkeleton />
        </CardContent>
      </Card>
    );
  }

  if (error || analysis?.error) {
    return (
      <Card className="border-dashed">
        <CardHeader className="pb-2">
          <div className="flex items-center gap-2">
            <Sparkles className="w-5 h-5 text-primary" />
            <CardTitle className="text-lg">AI Project Analysis</CardTitle>
          </div>
        </CardHeader>
        <CardContent>
          <ErrorState
            error={analysis?.error || error?.message || null}
            onRetry={handleRetry}
          />
        </CardContent>
      </Card>
    );
  }

  if (!analysis) {
    return null;
  }

  return (
    <Card>
      <CardHeader className="pb-2">
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-2">
            <Sparkles className="w-5 h-5 text-primary" />
            <CardTitle className="text-lg">AI Project Analysis</CardTitle>
          </div>
          <Button
            variant="ghost"
            size="sm"
            onClick={handleRetry}
            disabled={isFetching}
          >
            {isFetching ? (
              <Loader2 className="w-4 h-4 animate-spin" />
            ) : (
              <RefreshCw className="w-4 h-4" />
            )}
          </Button>
        </div>
      </CardHeader>
      <CardContent>
        <div className="space-y-4">
          <div className="flex items-center gap-6">
            <HealthScoreGauge score={analysis.healthScore || 0} />
            <div className="flex-1">
              <div className="flex items-center gap-2 mb-2">
                <ConfidenceBadge confidence={analysis.confidence || 0} />
                {analysis.processingTimeMs && (
                  <span className="text-xs text-muted-foreground">
                    in {Math.round(analysis.processingTimeMs / 1000)}s
                  </span>
                )}
              </div>
              <p className="text-sm text-muted-foreground leading-relaxed">
                {analysis.summary}
              </p>
            </div>
          </div>

          {isExpanded && (
            <>
              <CollapsibleSection
                title="Risks"
                icon={AlertTriangle}
                items={analysis.risks || []}
                defaultOpen={true}
              />

              <CollapsibleSection
                title="Recommendations"
                icon={TrendingUp}
                items={analysis.recommendations || []}
                defaultOpen={true}
              />

              <CollapsibleSection
                title="Next Actions"
                icon={CheckCircle2}
                items={analysis.nextActions || []}
                defaultOpen={false}
              />
            </>
          )}

          <button
            onClick={() => setIsExpanded(!isExpanded)}
            className="flex items-center gap-1 text-sm text-muted-foreground hover:text-foreground transition-colors"
          >
            {isExpanded ? (
              <>
                <ChevronUp className="w-4 h-4" />
                Show less
              </>
            ) : (
              <>
                <ChevronDown className="w-4 h-4" />
                Show more details
              </>
            )}
          </button>
        </div>
      </CardContent>
    </Card>
  );
}
