"use client";

import * as React from "react";
import { useForm } from "react-hook-form";
import { Loader2, User, Sparkles } from "lucide-react";
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogFooter,
} from "@/components/ui/dialog";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Textarea } from "@/components/ui/textarea";
import { Label } from "@/components/ui/label";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { Popover, PopoverContent, PopoverTrigger } from "@/components/ui/popover";
import { Avatar, AvatarFallback } from "@/components/ui/avatar";
import { ScrollArea } from "@/components/ui/scroll-area";
import { useTaskStore } from "@/store/taskStore";
import { useWorkspaceStore } from "@/store/workspaceStore";
import { projectApi, userApi } from "@/lib/api";
import { AiTaskRecommendation } from "@/components/ai/AiTaskRecommendation";
import { toast } from "react-hot-toast";
import type { TaskStatus, TaskPriority } from "@/types";

interface ProjectMember {
  id: string;
  user: {
    id: string;
    email: string;
    fullName: string;
  };
  role: string;
}

interface CreateTaskDialogProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  projectId: string;
  defaultStatus?: TaskStatus;
}

interface TaskForm {
  title: string;
  description: string;
  status: TaskStatus;
  priority: TaskPriority;
  dueDate: string;
  assigneeId?: string;
}

export function CreateTaskDialog({
  open,
  onOpenChange,
  projectId,
  defaultStatus = "TODO",
}: CreateTaskDialogProps) {
  const { createTask } = useTaskStore();
  const { currentWorkspace } = useWorkspaceStore();
  const [isCreating, setIsCreating] = React.useState(false);
  const [members, setMembers] = React.useState<ProjectMember[]>([]);
  const [searchQuery, setSearchQuery] = React.useState("");
  const [searchResults, setSearchResults] = React.useState<any[]>([]);
  const [isSearching, setIsSearching] = React.useState(false);
  const [selectedAssignee, setSelectedAssignee] = React.useState<any>(null);
  const [isAssigneeOpen, setIsAssigneeOpen] = React.useState(false);
  const [showAiRecommendation, setShowAiRecommendation] = React.useState(false);

  const {
    register,
    handleSubmit,
    reset,
    setValue,
    watch,
    formState: { errors },
  } = useForm<TaskForm>({
    defaultValues: {
      title: "",
      description: "",
      status: defaultStatus,
      priority: "MEDIUM",
      dueDate: "",
    },
  });

  React.useEffect(() => {
    if (open) {
      setValue("status", defaultStatus);
      fetchMembers();
    }
  }, [open, defaultStatus, setValue]);

  React.useEffect(() => {
    if (searchQuery.length >= 2) {
      const debounce = setTimeout(() => {
        searchUsers(searchQuery);
      }, 300);
      return () => clearTimeout(debounce);
    } else {
      setSearchResults([]);
    }
  }, [searchQuery]);

  const fetchMembers = async () => {
    if (!currentWorkspace?.id) return;
    try {
      const response = await projectApi.getMembers(currentWorkspace.id, projectId);
      if (response.data.success && response.data.data) {
        setMembers(response.data.data);
      }
    } catch {
      // Ignore error
    }
  };

  const searchUsers = async (query: string) => {
    setIsSearching(true);
    try {
      const response = await userApi.search(query);
      if (response.data.success && response.data.data) {
        setSearchResults(response.data.data);
      }
    } catch {
      toast.error("Failed to search users");
    } finally {
      setIsSearching(false);
    }
  };

  const handleSelectMember = (user: any) => {
    setSelectedAssignee(user);
    setValue("assigneeId", user.id);
    setSearchQuery("");
    setSearchResults([]);
    setIsAssigneeOpen(false);
  };

  const handleRemoveAssignee = () => {
    setSelectedAssignee(null);
    setValue("assigneeId", undefined);
  };

  const getInitials = (name: string) => {
    return name
      .split(" ")
      .map((n) => n[0])
      .join("")
      .toUpperCase()
      .slice(0, 2);
  };

  const onSubmit = async (data: TaskForm) => {
    setIsCreating(true);
    try {
      await createTask(projectId, {
        title: data.title,
        description: data.description || undefined,
        status: data.status,
        priority: data.priority,
        dueDate: data.dueDate ? new Date(data.dueDate).toISOString().split("T")[0] as any : undefined,
        assigneeId: data.assigneeId || undefined,
      });
      reset();
      setSelectedAssignee(null);
      setShowAiRecommendation(false);
      onOpenChange(false);
    } catch {
      // Error handled by store
    } finally {
      setIsCreating(false);
    }
  };

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="max-w-md">
        <DialogHeader>
          <DialogTitle>Create New Task</DialogTitle>
        </DialogHeader>
        <form onSubmit={handleSubmit(onSubmit)} className="space-y-4 py-4">
          <div className="space-y-2">
            <Label htmlFor="title">Title *</Label>
            <Input
              id="title"
              placeholder="Enter task title"
              {...register("title", { required: "Title is required" })}
              className={errors.title ? "border-destructive" : ""}
            />
            {errors.title && (
              <p className="text-sm text-destructive">{errors.title.message}</p>
            )}
          </div>

          <div className="space-y-2">
            <Label htmlFor="description">Description</Label>
            <Textarea
              id="description"
              placeholder="Enter task description"
              {...register("description")}
            />
          </div>

          <div className="grid grid-cols-2 gap-4">
            <div className="space-y-2">
              <Label>Status</Label>
              <Select
                onValueChange={(value) => setValue("status", value as TaskStatus)}
                defaultValue={defaultStatus}
              >
                <SelectTrigger>
                  <SelectValue placeholder="Select status" />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="TODO">To Do</SelectItem>
                  <SelectItem value="IN_PROGRESS">In Progress</SelectItem>
                  <SelectItem value="REVIEW">Review</SelectItem>
                  <SelectItem value="DONE">Done</SelectItem>
                </SelectContent>
              </Select>
            </div>

            <div className="space-y-2">
              <Label>Priority</Label>
              <Select onValueChange={(value) => setValue("priority", value as TaskPriority)} defaultValue="MEDIUM">
                <SelectTrigger>
                  <SelectValue placeholder="Select priority" />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="LOW">Low</SelectItem>
                  <SelectItem value="MEDIUM">Medium</SelectItem>
                  <SelectItem value="HIGH">High</SelectItem>
                  <SelectItem value="URGENT">Urgent</SelectItem>
                </SelectContent>
              </Select>
            </div>
          </div>

          <div className="space-y-2">
            <Label htmlFor="assignee">Assignee</Label>
            <Popover open={isAssigneeOpen} onOpenChange={setIsAssigneeOpen}>
              <PopoverTrigger asChild>
                <Button
                  variant="outline"
                  role="combobox"
                  className="w-full justify-start text-left font-normal h-10"
                >
                  {selectedAssignee ? (
                    <div className="flex items-center gap-2">
                      <Avatar className="w-6 h-6">
                        <AvatarFallback className="text-xs">
                          {getInitials(selectedAssignee.fullName)}
                        </AvatarFallback>
                      </Avatar>
                      <span>{selectedAssignee.fullName}</span>
                    </div>
                  ) : (
                    <>
                      <User className="w-4 h-4 mr-2 text-muted-foreground" />
                      <span className="text-muted-foreground">Unassigned</span>
                    </>
                  )}
                </Button>
              </PopoverTrigger>
              <PopoverContent className="w-[400px] p-0" align="start">
                <div className="p-2">
                  <Input
                    placeholder="Search members..."
                    value={searchQuery}
                    onChange={(e) => setSearchQuery(e.target.value)}
                    autoFocus
                  />
                </div>
                <ScrollArea className="max-h-60">
                  {searchResults.length > 0 ? (
                    <div className="py-1">
                      {searchResults.map((user) => (
                        <button
                          key={user.id}
                          type="button"
                          className="w-full flex items-center gap-2 px-3 py-2 hover:bg-muted text-left"
                          onClick={() => handleSelectMember(user)}
                        >
                          <Avatar className="w-6 h-6">
                            <AvatarFallback className="text-xs">
                              {getInitials(user.fullName)}
                            </AvatarFallback>
                          </Avatar>
                          <div>
                            <p className="text-sm font-medium">{user.fullName}</p>
                            <p className="text-xs text-muted-foreground">{user.email}</p>
                          </div>
                        </button>
                      ))}
                    </div>
                  ) : members.length > 0 ? (
                    <div className="py-1">
                      {members.map((member) => (
                        <button
                          key={member.id}
                          type="button"
                          className="w-full flex items-center gap-2 px-3 py-2 hover:bg-muted text-left"
                          onClick={() => handleSelectMember(member.user)}
                        >
                          <Avatar className="w-6 h-6">
                            <AvatarFallback className="text-xs">
                              {getInitials(member.user.fullName)}
                            </AvatarFallback>
                          </Avatar>
                          <div>
                            <p className="text-sm font-medium">{member.user.fullName}</p>
                            <p className="text-xs text-muted-foreground">{member.user.email}</p>
                          </div>
                        </button>
                      ))}
                    </div>
                  ) : (
                    <div className="p-4 text-center text-muted-foreground text-sm">
                      No members found
                    </div>
                  )}
                </ScrollArea>
              </PopoverContent>
            </Popover>
            {selectedAssignee && (
              <Button
                type="button"
                variant="ghost"
                size="sm"
                className="text-xs text-muted-foreground"
                onClick={handleRemoveAssignee}
              >
                Clear assignment
              </Button>
            )}
          </div>

          {!showAiRecommendation ? (
            <Button
              type="button"
              variant="outline"
              size="sm"
              className="w-full"
              onClick={() => setShowAiRecommendation(true)}
            >
              <Sparkles className="w-4 h-4 mr-2" />
              Recommend with AI
            </Button>
          ) : (
            <AiTaskRecommendation
              projectId={projectId}
              workspaceId={currentWorkspace?.id || ""}
              taskTitle={watch("title") || ""}
              taskDescription={watch("description") || ""}
              taskPriority={watch("priority") || "MEDIUM"}
              taskDueDate={watch("dueDate") || undefined}
              onSelectMember={(memberId, memberName) => {
                const member = members.find(m => m.user.id === memberId);
                if (member) {
                  handleSelectMember(member.user);
                }
              }}
              onClose={() => setShowAiRecommendation(false)}
            />
          )}

          <div className="space-y-2">
            <Label htmlFor="dueDate">Due Date</Label>
            <Input id="dueDate" type="date" {...register("dueDate")} />
          </div>

          <DialogFooter>
            <Button
              type="button"
              variant="outline"
              onClick={() => {
                reset();
                setSelectedAssignee(null);
                setShowAiRecommendation(false);
                onOpenChange(false);
              }}
            >
              Cancel
            </Button>
            <Button type="submit" disabled={isCreating}>
              {isCreating ? (
                <>
                  <Loader2 className="w-4 h-4 mr-2 animate-spin" />
                  Creating...
                </>
              ) : (
                "Create Task"
              )}
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  );
}
