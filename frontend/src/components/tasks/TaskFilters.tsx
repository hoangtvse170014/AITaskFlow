"use client";

import * as React from "react";
import { Filter, X, Loader2 } from "lucide-react";
import { Button } from "@/components/ui/button";
import {
  Popover,
  PopoverContent,
  PopoverTrigger,
} from "@/components/ui/popover";
import { Checkbox } from "@/components/ui/checkbox";
import { Label } from "@/components/ui/label";
import { Avatar, AvatarFallback } from "@/components/ui/avatar";
import { ScrollArea } from "@/components/ui/scroll-area";
import { projectApi } from "@/lib/api";
import { useWorkspaceStore } from "@/store/workspaceStore";
import { toast } from "react-hot-toast";

interface ProjectMember {
  id: string;
  user: {
    id: string;
    email: string;
    fullName: string;
  };
  role: string;
}

interface TaskFiltersProps {
  projectId: string;
  filters: {
    status: string[];
    priority: string[];
    assignee: string[];
  };
  onFiltersChange: (filters: {
    status: string[];
    priority: string[];
    assignee: string[];
  }) => void;
}

const STATUS_OPTIONS = [
  { value: "TODO", label: "To Do" },
  { value: "IN_PROGRESS", label: "In Progress" },
  { value: "REVIEW", label: "Review" },
  { value: "DONE", label: "Done" },
];

const PRIORITY_OPTIONS = [
  { value: "LOW", label: "Low" },
  { value: "MEDIUM", label: "Medium" },
  { value: "HIGH", label: "High" },
  { value: "URGENT", label: "Urgent" },
];

export function TaskFilters({ projectId, filters, onFiltersChange }: TaskFiltersProps) {
  const { currentWorkspace } = useWorkspaceStore();
  const [members, setMembers] = React.useState<ProjectMember[]>([]);
  const [isLoading, setIsLoading] = React.useState(false);
  const [isOpen, setIsOpen] = React.useState(false);

  React.useEffect(() => {
    if (isOpen && projectId && currentWorkspace?.id) {
      fetchMembers();
    }
  }, [isOpen, projectId, currentWorkspace]);

  const fetchMembers = async () => {
    if (!currentWorkspace?.id) return;
    setIsLoading(true);
    try {
      const response = await projectApi.getMembers(currentWorkspace.id, projectId);
      if (response.data.success && response.data.data) {
        setMembers(response.data.data);
      }
    } catch {
      toast.error("Failed to fetch members");
    } finally {
      setIsLoading(false);
    }
  };

  const handleStatusToggle = (value: string) => {
    const newStatus = filters.status.includes(value)
      ? filters.status.filter((s) => s !== value)
      : [...filters.status, value];
    onFiltersChange({ ...filters, status: newStatus });
  };

  const handlePriorityToggle = (value: string) => {
    const newPriority = filters.priority.includes(value)
      ? filters.priority.filter((p) => p !== value)
      : [...filters.priority, value];
    onFiltersChange({ ...filters, priority: newPriority });
  };

  const handleAssigneeToggle = (value: string) => {
    const newAssignee = filters.assignee.includes(value)
      ? filters.assignee.filter((a) => a !== value)
      : [...filters.assignee, value];
    onFiltersChange({ ...filters, assignee: newAssignee });
  };

  const handleClearAll = () => {
    onFiltersChange({ status: [], priority: [], assignee: [] });
  };

  const activeFiltersCount =
    filters.status.length +
    filters.priority.length +
    filters.assignee.length;

  const getInitials = (name: string) => {
    return name
      .split(" ")
      .map((n) => n[0])
      .join("")
      .toUpperCase()
      .slice(0, 2);
  };

  return (
    <Popover open={isOpen} onOpenChange={setIsOpen}>
      <PopoverTrigger asChild>
        <Button variant="outline" size="sm" className="relative">
          <Filter className="w-4 h-4 mr-2" />
          Filter
          {activeFiltersCount > 0 && (
            <span className="ml-2 bg-primary text-primary-foreground text-xs px-1.5 py-0.5 rounded-full">
              {activeFiltersCount}
            </span>
          )}
        </Button>
      </PopoverTrigger>
      <PopoverContent className="w-80 p-0" align="end">
        <div className="flex items-center justify-between px-4 py-3 border-b">
          <h3 className="font-semibold text-sm">Filters</h3>
          {activeFiltersCount > 0 && (
            <Button
              variant="ghost"
              size="sm"
              className="text-xs h-auto py-1 px-2 text-muted-foreground"
              onClick={handleClearAll}
            >
              Clear all
            </Button>
          )}
        </div>

        <ScrollArea className="max-h-96">
          <div className="p-4 space-y-6">
            {/* Status Filter */}
            <div>
              <h4 className="text-sm font-medium mb-3">Status</h4>
              <div className="space-y-2">
                {STATUS_OPTIONS.map((option) => (
                  <div key={option.value} className="flex items-center gap-2">
                    <Checkbox
                      id={`status-${option.value}`}
                      checked={filters.status.includes(option.value)}
                      onCheckedChange={() => handleStatusToggle(option.value)}
                    />
                    <Label
                      htmlFor={`status-${option.value}`}
                      className="text-sm cursor-pointer"
                    >
                      {option.label}
                    </Label>
                  </div>
                ))}
              </div>
            </div>

            {/* Priority Filter */}
            <div>
              <h4 className="text-sm font-medium mb-3">Priority</h4>
              <div className="space-y-2">
                {PRIORITY_OPTIONS.map((option) => (
                  <div key={option.value} className="flex items-center gap-2">
                    <Checkbox
                      id={`priority-${option.value}`}
                      checked={filters.priority.includes(option.value)}
                      onCheckedChange={() => handlePriorityToggle(option.value)}
                    />
                    <Label
                      htmlFor={`priority-${option.value}`}
                      className="text-sm cursor-pointer flex items-center gap-2"
                    >
                      <span
                        className={`w-2 h-2 rounded-full ${
                          option.value === "LOW"
                            ? "bg-green-500"
                            : option.value === "MEDIUM"
                            ? "bg-yellow-500"
                            : option.value === "HIGH"
                            ? "bg-orange-500"
                            : "bg-red-500"
                        }`}
                      />
                      {option.label}
                    </Label>
                  </div>
                ))}
              </div>
            </div>

            {/* Assignee Filter */}
            <div>
              <h4 className="text-sm font-medium mb-3">Assignee</h4>
              {isLoading ? (
                <div className="flex items-center justify-center py-4">
                  <Loader2 className="w-4 h-4 animate-spin text-muted-foreground" />
                </div>
              ) : members.length === 0 ? (
                <p className="text-sm text-muted-foreground">No members in project</p>
              ) : (
                <div className="space-y-2">
                  {members.map((member) => (
                    <div key={member.id} className="flex items-center gap-2">
                      <Checkbox
                        id={`assignee-${member.user.id}`}
                        checked={filters.assignee.includes(member.user.id)}
                        onCheckedChange={() => handleAssigneeToggle(member.user.id)}
                      />
                      <Label
                        htmlFor={`assignee-${member.user.id}`}
                        className="text-sm cursor-pointer flex items-center gap-2"
                      >
                        <Avatar className="w-5 h-5">
                          <AvatarFallback className="text-[10px]">
                            {getInitials(member.user.fullName)}
                          </AvatarFallback>
                        </Avatar>
                        {member.user.fullName}
                      </Label>
                    </div>
                  ))}
                </div>
              )}
            </div>
          </div>
        </ScrollArea>
      </PopoverContent>
    </Popover>
  );
}
