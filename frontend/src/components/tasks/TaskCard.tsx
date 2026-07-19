"use client";

import * as React from "react";
import { useSortable } from "@dnd-kit/sortable";
import { CSS } from "@dnd-kit/utilities";
import { Calendar, MessageSquare, CheckSquare } from "lucide-react";
import { cn, formatDate, priorityColors, priorityDotColors } from "@/lib/utils";
import { Avatar } from "@/components/ui/avatar";
import { Badge } from "@/components/ui/badge";
import type { Task } from "@/types";

interface TaskCardProps {
  task: Task;
  onClick?: () => void;
  isDragging?: boolean;
}

export function TaskCard({ task, onClick, isDragging }: TaskCardProps) {
  const {
    attributes,
    listeners,
    setNodeRef,
    transform,
    transition,
    isDragging: isSortableDragging,
  } = useSortable({ id: task.id });

  const style = {
    transform: CSS.Transform.toString(transform),
    transition,
  };

  return (
    <div
      ref={setNodeRef}
      style={style}
      {...attributes}
      {...listeners}
      onClick={onClick}
      className={cn(
        "group bg-card border border-border rounded-lg p-3 cursor-pointer transition-all duration-200",
        "hover:border-primary/50 hover:shadow-md",
        "active:cursor-grabbing",
        (isDragging || isSortableDragging) && "opacity-50 shadow-lg scale-[1.02]"
      )}
    >
      {/* Labels */}
      {task.labels && task.labels.length > 0 && (
        <div className="flex flex-wrap gap-1 mb-2">
          {task.labels.slice(0, 3).map((label, index) => (
            <span
              key={index}
              className="px-2 py-0.5 text-xs rounded-full text-white"
              style={{ backgroundColor: label.color }}
            >
              {label.name}
            </span>
          ))}
          {task.labels.length > 3 && (
            <span className="px-2 py-0.5 text-xs rounded-full bg-muted text-muted-foreground">
              +{task.labels.length - 3}
            </span>
          )}
        </div>
      )}

      {/* Title */}
      <p className="text-sm font-medium line-clamp-2 mb-2">{task.title}</p>

      {/* Meta */}
      <div className="flex items-center justify-between mt-3">
        <div className="flex items-center gap-2">
          {/* Priority */}
          <div className="flex items-center gap-1" title={`Priority: ${task.priority}`}>
            <span className={cn("w-2 h-2 rounded-full", priorityDotColors[task.priority])} />
          </div>

          {/* Comments */}
          {task.totalChecklistItems > 0 && (
            <div className="flex items-center gap-1 text-muted-foreground" title="Checklist">
              <CheckSquare className="w-3.5 h-3.5" />
              <span className="text-xs">
                {task.completedChecklistItems}/{task.totalChecklistItems}
              </span>
            </div>
          )}

          {/* Due Date */}
          {task.dueDate && (
            <div
              className={cn(
                "flex items-center gap-1",
                task.overdue ? "text-red-500" : "text-muted-foreground"
              )}
              title={`Due: ${formatDate(task.dueDate)}`}
            >
              <Calendar className="w-3.5 h-3.5" />
              <span className="text-xs">{formatDate(task.dueDate)}</span>
            </div>
          )}
        </div>

        {/* Assignee */}
        {task.assignee && (
          <Avatar src={task.assignee.avatarUrl} fallback={task.assignee.fullName} size="sm" />
        )}
      </div>
    </div>
  );
}
