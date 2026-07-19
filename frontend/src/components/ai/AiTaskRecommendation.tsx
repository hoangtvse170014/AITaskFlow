"use client";

import * as React from "react";
import { useQuery } from "@tanstack/react-query";
import {
  Sparkles,
  RefreshCw,
  Loader2,
  AlertCircle,
  CheckCircle2,
  Award,
  Clock,
  AlertTriangle,
  X,
  ChevronDown,
  ChevronUp,
} from "lucide-react";
import { Card, CardContent } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Avatar, AvatarFallback } from "@/components/ui/avatar";
import { Progress } from "@/components/ui/progress";
import { ScrollArea } from "@/components/ui/scroll-area";
import { Badge } from "@/components/ui/badge";
import { aiApi } from "@/lib/ai-api";
import { toast } from "react-hot-toast";
import type { TaskAssignmentRequest, TaskAssignmentResponse, MemberRanking } from "@/types/ai";

interface AiTaskRecommendationProps {
  projectId: string;
  workspaceId: string;
  taskTitle: string;
  taskDescription?: string;
  taskPriority: string;
  taskDueDate?: string;
  onSelectMember: (memberId: string, memberName: string) => void;
  onClose: () => void;
}

function getInitials(name: string) {
  return name
    .split(" ")
    .map((n) => n[0])
    .join("")
    .toUpperCase()
    .slice(0, 2);
}

function LoadingSkeleton() {
  return (
    <div className="space-y-4 animate-pulse">
      <div className="flex items-center gap-3">
        <div className="w-12 h-12 rounded-full bg-muted" />
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
      <div className="space-y-2">
        <div className="h-8 bg-muted rounded" />
        <div className="h-8 bg-muted rounded" />
        <div className="h-8 bg-muted rounded" />
      </div>
    </div>
  );
}

function ConfidenceBar({ confidence }: { confidence: number }) {
  const percentage = Math.round(confidence * 100);
  const getColor = (value: number) => {
    if (value >= 0.8) return "bg-green-500";
    if (value >= 0.6) return "bg-blue-500";
    if (value >= 0.4) return "bg-yellow-500";
    return "bg-gray-500";
  };

  return (
    <div className="space-y-1">
      <div className="flex items-center justify-between text-xs">
        <span className="text-muted-foreground">Confidence</span>
        <span className="font-medium">{percentage}%</span>
      </div>
      <Progress value={percentage} className="h-2" />
    </div>
  );
}

function MemberRankingItem({
  ranking,
  index,
  isRecommended,
}: {
  ranking: MemberRanking;
  index: number;
  isRecommended: boolean;
}) {
  const [isExpanded, setIsExpanded] = React.useState(false);

  return (
    <div
      className={`border rounded-lg p-3 transition-colors ${
        isRecommended ? "border-primary bg-primary/5" : "border-border"
      }`}
    >
      <button
        onClick={() => setIsExpanded(!isExpanded)}
        className="w-full flex items-center gap-3"
      >
        <div className="flex items-center justify-center w-6 h-6 rounded-full bg-muted text-xs font-medium">
          {index + 1}
        </div>
        <Avatar className="w-8 h-8">
          <AvatarFallback className="text-xs">
            {getInitials(ranking.memberName)}
          </AvatarFallback>
        </Avatar>
        <div className="flex-1 text-left">
          <div className="flex items-center gap-2">
            <span className="font-medium text-sm">{ranking.memberName}</span>
            {isRecommended && (
              <Badge variant="secondary" className="text-xs">
                <CheckCircle2 className="w-3 h-3 mr-1" />
                Best Match
              </Badge>
            )}
          </div>
          <div className="flex items-center gap-2 text-xs text-muted-foreground">
            <span>{ranking.role}</span>
            <span>•</span>
            <span>{ranking.openTasks} open tasks</span>
          </div>
        </div>
        <div className="flex items-center gap-2">
          <div className="text-right">
            <span className="font-semibold text-sm">{ranking.score}</span>
            <span className="text-xs text-muted-foreground">/100</span>
          </div>
          {isExpanded ? (
            <ChevronUp className="w-4 h-4 text-muted-foreground" />
          ) : (
            <ChevronDown className="w-4 h-4 text-muted-foreground" />
          )}
        </div>
      </button>

      {isExpanded && (
        <div className="mt-3 pl-9 space-y-2 text-sm">
          <div className="flex items-center gap-4 text-xs text-muted-foreground">
            <span className="flex items-center gap-1">
              <Clock className="w-3 h-3" />
              {ranking.currentWorkload}
            </span>
            <span>{ranking.inProgressTasks} in progress</span>
          </div>
          <p className="text-muted-foreground italic">"{ranking.reason}"</p>
        </div>
      )}
    </div>
  );
}

export function AiTaskRecommendation({
  projectId,
  workspaceId,
  taskTitle,
  taskDescription,
  taskPriority,
  taskDueDate,
  onSelectMember,
  onClose,
}: AiTaskRecommendationProps) {
  const queryParams = React.useMemo(
    () => ({
      title: taskTitle,
      description: taskDescription,
      priority: taskPriority,
      dueDate: taskDueDate,
      projectId,
      workspaceId,
    }),
    [taskTitle, taskDescription, taskPriority, taskDueDate, projectId, workspaceId]
  );

  const {
    data: recommendation,
    isLoading,
    error,
    refetch,
    isFetching,
  } = useQuery<TaskAssignmentResponse, Error>({
    queryKey: ["ai-task-recommendation", queryParams],
    queryFn: () => aiApi.recommendAssignee(queryParams),
    staleTime: 0,
    retry: 1,
  });

  const handleUseRecommendation = () => {
    if (recommendation?.recommendedAssigneeId && recommendation?.recommendedAssignee) {
      onSelectMember(recommendation.recommendedAssigneeId, recommendation.recommendedAssignee);
    }
  };

  const recommendedMember = recommendation?.ranking?.[0];

  if (isLoading) {
    return (
      <Card className="border-dashed">
        <CardContent className="pt-4">
          <div className="flex items-center gap-2 mb-4">
            <Sparkles className="w-4 h-4 text-primary" />
            <span className="font-medium text-sm">AI Recommendation</span>
          </div>
          <LoadingSkeleton />
        </CardContent>
      </Card>
    );
  }

  if (error || recommendation?.error) {
    return (
      <Card className="border-dashed">
        <CardContent className="pt-4">
          <div className="flex items-center gap-2 mb-4">
            <Sparkles className="w-4 h-4 text-primary" />
            <span className="font-medium text-sm">AI Recommendation</span>
          </div>
          <div className="flex flex-col items-center justify-center py-6 text-center">
            <AlertCircle className="w-10 h-10 text-destructive mb-3" />
            <p className="text-sm text-muted-foreground mb-4">
              {recommendation?.error || error?.message || "Failed to get recommendation"}
            </p>
            <Button onClick={() => refetch()} variant="outline" size="sm">
              <RefreshCw className="w-4 h-4 mr-2" />
              Try Again
            </Button>
          </div>
        </CardContent>
      </Card>
    );
  }

  if (!recommendation) {
    return null;
  }

  return (
    <Card>
      <CardContent className="pt-4">
        <div className="flex items-center justify-between mb-4">
          <div className="flex items-center gap-2">
            <Sparkles className="w-4 h-4 text-primary" />
            <span className="font-medium text-sm">AI Recommendation</span>
          </div>
          <div className="flex items-center gap-1">
            <Button
              variant="ghost"
              size="sm"
              onClick={() => refetch()}
              disabled={isFetching}
              className="h-8 px-2"
            >
              {isFetching ? (
                <Loader2 className="w-4 h-4 animate-spin" />
              ) : (
                <RefreshCw className="w-4 h-4" />
              )}
            </Button>
            <Button variant="ghost" size="sm" onClick={onClose} className="h-8 px-2">
              <X className="w-4 h-4" />
            </Button>
          </div>
        </div>

        {recommendedMember && (
          <div className="space-y-4">
            <div className="flex items-start gap-3 p-3 rounded-lg bg-muted/50">
              <Avatar className="w-12 h-12">
                <AvatarFallback className="text-sm">
                  {getInitials(recommendedMember.memberName)}
                </AvatarFallback>
              </Avatar>
              <div className="flex-1 min-w-0">
                <div className="flex items-center gap-2 mb-1">
                  <span className="font-semibold">{recommendedMember.memberName}</span>
                  <Badge variant="outline" className="text-xs">
                    {recommendedMember.role}
                  </Badge>
                </div>
                <ConfidenceBar confidence={recommendation.confidence || 0} />
              </div>
            </div>

            {recommendation.reason && (
              <div className="space-y-1">
                <span className="text-xs font-medium text-muted-foreground">Reason</span>
                <p className="text-sm text-muted-foreground">{recommendation.reason}</p>
              </div>
            )}

            {recommendation.warnings && recommendation.warnings.length > 0 && (
              <div className="space-y-1">
                <span className="text-xs font-medium text-muted-foreground flex items-center gap-1">
                  <AlertTriangle className="w-3 h-3 text-orange-500" />
                  Warnings
                </span>
                <div className="space-y-1">
                  {recommendation.warnings.map((warning, index) => (
                    <p key={index} className="text-xs text-orange-600 dark:text-orange-400">
                      • {warning}
                    </p>
                  ))}
                </div>
              </div>
            )}

            {recommendation.ranking && recommendation.ranking.length > 1 && (
              <div className="space-y-2">
                <span className="text-xs font-medium text-muted-foreground flex items-center gap-1">
                  <Award className="w-3 h-3" />
                  Top 5 Ranking
                </span>
                <ScrollArea className="max-h-48">
                  <div className="space-y-2 pr-3">
                    {recommendation.ranking.slice(0, 5).map((ranking, index) => (
                      <MemberRankingItem
                        key={ranking.memberId}
                        ranking={ranking}
                        index={index}
                        isRecommended={index === 0}
                      />
                    ))}
                  </div>
                </ScrollArea>
              </div>
            )}

            <div className="flex gap-2">
              <Button
                onClick={handleUseRecommendation}
                className="flex-1"
                size="sm"
              >
                <CheckCircle2 className="w-4 h-4 mr-2" />
                Use Recommendation
              </Button>
              <Button onClick={onClose} variant="outline" size="sm">
                Close
              </Button>
            </div>
          </div>
        )}
      </CardContent>
    </Card>
  );
}
