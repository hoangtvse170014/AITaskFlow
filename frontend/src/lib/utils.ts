import { clsx, type ClassValue } from "clsx";
import { twMerge } from "tailwind-merge";

export function cn(...inputs: ClassValue[]) {
  return twMerge(clsx(inputs));
}

export function getInitials(name: string): string {
  return name
    .split(" ")
    .map((n) => n[0])
    .join("")
    .toUpperCase()
    .slice(0, 2);
}

export function formatDate(date: string | Date): string {
  const d = new Date(date);
  return d.toLocaleDateString("en-US", {
    month: "short",
    day: "numeric",
    year: "numeric",
  });
}

export function formatRelativeDate(date: string | Date): string {
  const d = new Date(date);
  const now = new Date();
  const diffInMs = now.getTime() - d.getTime();
  const diffInDays = Math.floor(diffInMs / (1000 * 60 * 60 * 24));

  if (diffInDays === 0) {
    const diffInHours = Math.floor(diffInMs / (1000 * 60 * 60));
    if (diffInHours === 0) {
      const diffInMinutes = Math.floor(diffInMs / (1000 * 60));
      if (diffInMinutes < 1) return "just now";
      return `${diffInMinutes}m ago`;
    }
    return `${diffInHours}h ago`;
  }
  if (diffInDays === 1) return "yesterday";
  if (diffInDays < 7) return `${diffInDays}d ago`;
  return formatDate(date);
}

export function generateId(): string {
  return Math.random().toString(36).substring(2, 15);
}

export const priorityColors: Record<string, string> = {
  LOW: "text-green-500 bg-green-500/10",
  MEDIUM: "text-yellow-500 bg-yellow-500/10",
  HIGH: "text-orange-500 bg-orange-500/10",
  URGENT: "text-red-500 bg-red-500/10",
};

export const priorityDotColors: Record<string, string> = {
  LOW: "bg-green-500",
  MEDIUM: "bg-yellow-500",
  HIGH: "bg-orange-500",
  URGENT: "bg-red-500",
};

export const statusColors: Record<string, { bg: string; text: string; dot: string }> = {
  TODO: { bg: "bg-zinc-500/10", text: "text-zinc-400", dot: "bg-zinc-500" },
  IN_PROGRESS: { bg: "bg-blue-500/10", text: "text-blue-400", dot: "bg-blue-500" },
  REVIEW: { bg: "bg-purple-500/10", text: "text-purple-400", dot: "bg-purple-500" },
  DONE: { bg: "bg-green-500/10", text: "text-green-400", dot: "bg-green-500" },
};

export const statusLabels: Record<string, string> = {
  TODO: "To Do",
  IN_PROGRESS: "In Progress",
  REVIEW: "Review",
  DONE: "Done",
};

export const projectColors = [
  "#6366f1", // Indigo
  "#8b5cf6", // Violet
  "#ec4899", // Pink
  "#f43f5e", // Rose
  "#f97316", // Orange
  "#eab308", // Yellow
  "#22c55e", // Green
  "#14b8a6", // Teal
  "#06b6d4", // Cyan
  "#3b82f6", // Blue
];
