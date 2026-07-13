"use client";

import * as React from "react";
import { useParams } from "next/navigation";
import { X, Calendar, User, Flag, Tag, Trash2, Loader2, Send } from "lucide-react";
import { formatDate, formatRelativeDate, priorityColors, statusColors, statusLabels } from "@/lib/utils";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Textarea } from "@/components/ui/textarea";
import { Avatar, AvatarFallback } from "@/components/ui/avatar";
import { Badge } from "@/components/ui/badge";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { Popover, PopoverContent, PopoverTrigger } from "@/components/ui/popover";
import { ScrollArea } from "@/components/ui/scroll-area";
import { SubTaskList } from "@/components/tasks/SubTaskList";
import { TimeTracker } from "@/components/tasks/TimeTracker";
import { useTaskStore } from "@/store/taskStore";
import { useWorkspaceStore } from "@/store/workspaceStore";
import { taskApi, projectApi, userApi } from "@/lib/api";
import { toast } from "react-hot-toast";
import type { Task, Comment, ActivityLog } from "@/types";

interface ProjectMember {
  id: string;
  user: {
    id: string;
    email: string;
    fullName: string;
  };
  role: string;
}

interface TaskModalProps {
  task: Task;
  projectId: string;
  onClose: () => void;
}

export function TaskModal({ task, projectId, onClose }: TaskModalProps) {
  const { updateTask, deleteTask } = useTaskStore();
  const { currentWorkspace } = useWorkspaceStore();
  const [isEditing, setIsEditing] = React.useState(false);
  const [isLoading, setIsLoading] = React.useState(false);
  const [editedTask, setEditedTask] = React.useState(task);
  const [comments, setComments] = React.useState<Comment[]>([]);
  const [activityLogs, setActivityLogs] = React.useState<ActivityLog[]>([]);
  const [newComment, setNewComment] = React.useState("");
  const [isAddingComment, setIsAddingComment] = React.useState(false);
  const [members, setMembers] = React.useState<ProjectMember[]>([]);
  const [isAssigneeOpen, setIsAssigneeOpen] = React.useState(false);
  const [searchQuery, setSearchQuery] = React.useState("");
  const [searchResults, setSearchResults] = React.useState<any[]>([]);
  const [isSearching, setIsSearching] = React.useState(false);

  React.useEffect(() => {
    setEditedTask(task);
    fetchComments();
    fetchActivity();
    fetchMembers();
  }, [task]);

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

  const handleSelectAssignee = (user: any) => {
    setEditedTask({ ...editedTask, assignee: user, assigneeId: user.id });
    setSearchQuery("");
    setSearchResults([]);
    setIsAssigneeOpen(false);
  };

  const handleRemoveAssignee = () => {
    setEditedTask({ ...editedTask, assignee: null, assigneeId: undefined });
  };

  const getInitials = (name: string) => {
    return name
      .split(" ")
      .map((n) => n[0])
      .join("")
      .toUpperCase()
      .slice(0, 2);
  };

  const fetchComments = async () => {
    try {
      const response = await taskApi.getComments(task.id);
      if (response.data.success) {
        setComments(response.data.data);
      }
    } catch (error) {
      console.error("Failed to fetch comments:", error);
    }
  };

  const fetchActivity = async () => {
    try {
      const response = await taskApi.getActivity(task.id);
      if (response.data.success) {
        setActivityLogs(response.data.data);
      }
    } catch (error) {
      console.error("Failed to fetch activity:", error);
    }
  };

  const handleSave = async () => {
    setIsLoading(true);
    try {
      await updateTask(projectId, task.id, editedTask);
      setIsEditing(false);
    } catch {
      // Error handled by store
    } finally {
      setIsLoading(false);
    }
  };

  const handleDelete = async () => {
    if (confirm("Are you sure you want to delete this task?")) {
      try {
        await deleteTask(projectId, task.id);
        onClose();
      } catch {
        // Error handled by store
      }
    }
  };

  const handleAddComment = async () => {
    if (!newComment.trim()) return;
    setIsAddingComment(true);
    try {
      const response = await taskApi.addComment(task.id, { content: newComment });
      if (response.data.success) {
        setComments([...comments, response.data.data]);
        setNewComment("");
      }
    } catch {
      // Error handled by interceptor
    } finally {
      setIsAddingComment(false);
    }
  };

  return (
    <Dialog open onOpenChange={onClose}>
      <DialogContent className="max-w-4xl max-h-[90vh] overflow-y-auto">
        <DialogHeader className="pb-4 border-b border-border">
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-2">
              <Badge variant="outline" className="font-mono">
                {task.taskKey}
              </Badge>
              <DialogTitle className="text-lg">
                {isEditing ? (
                  <Input
                    value={editedTask.title}
                    onChange={(e) => setEditedTask({ ...editedTask, title: e.target.value })}
                    className="text-lg font-semibold"
                  />
                ) : (
                  task.title
                )}
              </DialogTitle>
            </div>
            <div className="flex items-center gap-2">
              {isEditing ? (
                <>
                  <Button variant="outline" size="sm" onClick={() => setIsEditing(false)}>
                    Cancel
                  </Button>
                  <Button size="sm" onClick={handleSave} disabled={isLoading}>
                    {isLoading ? <Loader2 className="w-4 h-4 animate-spin" /> : "Save"}
                  </Button>
                </>
              ) : (
                <>
                  <Button variant="ghost" size="sm" onClick={handleDelete} className="text-destructive hover:text-destructive">
                    <Trash2 className="w-4 h-4" />
                  </Button>
                  <Button variant="ghost" size="sm" onClick={() => setIsEditing(true)}>
                    Edit
                  </Button>
                </>
              )}
              <button onClick={onClose} className="p-1 hover:bg-accent rounded">
                <X className="w-5 h-5" />
              </button>
            </div>
          </div>
        </DialogHeader>

        <div className="grid grid-cols-3 gap-6 pt-4">
          {/* Main Content */}
          <div className="col-span-2 space-y-6">
            {/* Description */}
            <div>
              <h4 className="text-sm font-semibold mb-2">Description</h4>
              {isEditing ? (
                <Textarea
                  value={editedTask.description || ""}
                  onChange={(e) => setEditedTask({ ...editedTask, description: e.target.value })}
                  placeholder="Add a description..."
                  className="min-h-[120px]"
                />
              ) : (
                <p className="text-sm text-muted-foreground whitespace-pre-wrap">
                  {task.description || "No description"}
                </p>
              )}
            </div>

            {/* Sub-tasks */}
            <div>
              <SubTaskList taskId={task.id} />
            </div>

            {/* Time Tracker */}
            <div>
              <TimeTracker taskId={task.id} />
            </div>

            {/* Comments & Activity */}
            <Tabs defaultValue="comments">
              <TabsList>
                <TabsTrigger value="comments">Comments</TabsTrigger>
                <TabsTrigger value="activity">Activity</TabsTrigger>
              </TabsList>
              <TabsContent value="comments" className="space-y-4">
                {comments.map((comment) => (
                  <div key={comment.id} className="flex gap-3">
                    <Avatar src={comment.author.avatarUrl} fallback={comment.author.fullName} size="sm" />
                    <div className="flex-1">
                      <div className="flex items-center gap-2">
                        <span className="font-medium text-sm">{comment.author.fullName}</span>
                        <span className="text-xs text-muted-foreground">
                          {formatRelativeDate(comment.createdAt)}
                        </span>
                      </div>
                      <p className="text-sm mt-1">{comment.content}</p>
                    </div>
                  </div>
                ))}
                <div className="flex gap-3 pt-4 border-t border-border">
                  <Avatar fallback="Me" size="sm" />
                  <div className="flex-1 flex gap-2">
                    <Input
                      value={newComment}
                      onChange={(e) => setNewComment(e.target.value)}
                      placeholder="Write a comment..."
                      onKeyDown={(e) => e.key === "Enter" && !e.shiftKey && handleAddComment()}
                    />
                    <Button size="icon" onClick={handleAddComment} disabled={isAddingComment}>
                      <Send className="w-4 h-4" />
                    </Button>
                  </div>
                </div>
              </TabsContent>
              <TabsContent value="activity">
                {activityLogs.length === 0 ? (
                  <p className="text-sm text-muted-foreground text-center py-4">No activity yet</p>
                ) : (
                  <div className="space-y-3">
                    {activityLogs.map((log) => (
                      <div key={log.id} className="flex items-start gap-3 text-sm">
                        <Avatar src={log.user.avatarUrl} fallback={log.user.fullName} size="sm" />
                        <div>
                          <p>
                            <span className="font-medium">{log.user.fullName}</span>
                            {" "}
                            {log.action.toLowerCase()} {log.fieldChanged || ""}
                            {log.oldValue && log.newValue && (
                              <>
                                {" from "}
                                <span className="text-muted-foreground">{log.oldValue}</span>
                                {" to "}
                                <span className="text-primary">{log.newValue}</span>
                              </>
                            )}
                          </p>
                          <p className="text-xs text-muted-foreground mt-1">
                            {formatRelativeDate(log.createdAt)}
                          </p>
                        </div>
                      </div>
                    ))}
                  </div>
                )}
              </TabsContent>
            </Tabs>
          </div>

          {/* Sidebar */}
          <div className="space-y-4">
            {/* Status */}
            <div>
              <label className="text-sm font-medium text-muted-foreground mb-1 block">Status</label>
              <Select
                value={isEditing ? editedTask.status : task.status}
                onValueChange={(value) => setEditedTask({ ...editedTask, status: value as any })}
                disabled={!isEditing}
              >
                <SelectTrigger>
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  {Object.entries(statusLabels).map(([value, label]) => (
                    <SelectItem key={value} value={value}>
                      <div className="flex items-center gap-2">
                        <span className={`w-2 h-2 rounded-full ${statusColors[value].dot}`} />
                        {label}
                      </div>
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>

            {/* Priority */}
            <div>
              <label className="text-sm font-medium text-muted-foreground mb-1 block">Priority</label>
              <Select
                value={isEditing ? editedTask.priority : task.priority}
                onValueChange={(value) => setEditedTask({ ...editedTask, priority: value as any })}
                disabled={!isEditing}
              >
                <SelectTrigger>
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  {["LOW", "MEDIUM", "HIGH", "URGENT"].map((p) => (
                    <SelectItem key={p} value={p}>
                      <div className="flex items-center gap-2">
                        <span className={`w-2 h-2 rounded-full ${
                          p === "LOW" ? "bg-green-500" :
                          p === "MEDIUM" ? "bg-yellow-500" :
                          p === "HIGH" ? "bg-orange-500" : "bg-red-500"
                        }`} />
                        {p.charAt(0) + p.slice(1).toLowerCase()}
                      </div>
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>

            {/* Assignee */}
            <div>
              <label className="text-sm font-medium text-muted-foreground mb-1 block">Assignee</label>
              {isEditing ? (
                <Popover open={isAssigneeOpen} onOpenChange={setIsAssigneeOpen}>
                  <PopoverTrigger asChild>
                    <Button variant="outline" role="combobox" className="w-full justify-start h-10">
                      {editedTask.assignee ? (
                        <div className="flex items-center gap-2">
                          <Avatar className="w-6 h-6">
                            <AvatarFallback className="text-xs">{getInitials(editedTask.assignee.fullName)}</AvatarFallback>
                          </Avatar>
                          <span className="text-sm">{editedTask.assignee.fullName}</span>
                        </div>
                      ) : (
                        <>
                          <User className="w-4 h-4 mr-2 text-muted-foreground" />
                          <span className="text-muted-foreground">Unassigned</span>
                        </>
                      )}
                    </Button>
                  </PopoverTrigger>
                  <PopoverContent className="w-[300px] p-0" align="start">
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
                              onClick={() => handleSelectAssignee(user)}
                            >
                              <Avatar className="w-6 h-6">
                                <AvatarFallback className="text-xs">{getInitials(user.fullName)}</AvatarFallback>
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
                              onClick={() => handleSelectAssignee(member.user)}
                            >
                              <Avatar className="w-6 h-6">
                                <AvatarFallback className="text-xs">{getInitials(member.user.fullName)}</AvatarFallback>
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
              ) : (
                <div className="flex items-center gap-2">
                  {task.assignee ? (
                    <Avatar className="w-8 h-8">
                      <AvatarFallback className="text-xs">{getInitials(task.assignee.fullName)}</AvatarFallback>
                    </Avatar>
                  ) : (
                    <div className="w-8 h-8 rounded-full bg-muted flex items-center justify-center">
                      <User className="w-4 h-4 text-muted-foreground" />
                    </div>
                  )}
                  <span className="text-sm">
                    {task.assignee?.fullName || "Unassigned"}
                  </span>
                </div>
              )}
            </div>

            {/* Reporter */}
            <div>
              <label className="text-sm font-medium text-muted-foreground mb-1 block">Reporter</label>
              <div className="flex items-center gap-2">
                <Avatar src={task.reporter.avatarUrl} fallback={task.reporter.fullName} size="sm" />
                <span className="text-sm">{task.reporter.fullName}</span>
              </div>
            </div>

            {/* Due Date */}
            <div>
              <label className="text-sm font-medium text-muted-foreground mb-1 block">Due Date</label>
              <div className={`flex items-center gap-2 ${task.overdue ? "text-red-500" : ""}`}>
                <Calendar className="w-4 h-4" />
                <span className="text-sm">{task.dueDate ? formatDate(task.dueDate) : "No due date"}</span>
              </div>
            </div>

            {/* Labels */}
            {task.labels && task.labels.length > 0 && (
              <div>
                <label className="text-sm font-medium text-muted-foreground mb-1 block">Labels</label>
                <div className="flex flex-wrap gap-1">
                  {task.labels.map((label, index) => (
                    <span
                      key={index}
                      className="px-2 py-1 text-xs rounded-full text-white"
                      style={{ backgroundColor: label.color }}
                    >
                      {label.name}
                    </span>
                  ))}
                </div>
              </div>
            )}

            {/* Timestamps */}
            <div className="pt-4 border-t border-border space-y-2">
              <p className="text-xs text-muted-foreground">
                Created {formatRelativeDate(task.createdAt)}
              </p>
              <p className="text-xs text-muted-foreground">
                Updated {formatRelativeDate(task.updatedAt)}
              </p>
            </div>
          </div>
        </div>
      </DialogContent>
    </Dialog>
  );
}
