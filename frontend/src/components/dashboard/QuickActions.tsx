"use client";

import * as React from "react";
import { Button } from "@/components/ui/button";
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogFooter,
} from "@/components/ui/dialog";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Textarea } from "@/components/ui/textarea";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import {
  Popover,
  PopoverContent,
  PopoverTrigger,
} from "@/components/ui/popover";
import { Avatar, AvatarFallback } from "@/components/ui/avatar";
import { ScrollArea } from "@/components/ui/scroll-area";
import {
  Plus,
  FolderKanban,
  Users,
  Sparkles,
  User,
  Loader2,
} from "lucide-react";
import { useWorkspaceStore } from "@/store/workspaceStore";
import { useProjectStore } from "@/store/projectStore";
import { projectColors } from "@/lib/utils";
import { userApi, projectApi } from "@/lib/api";
import { toast } from "react-hot-toast";

interface QuickActionsProps {
  onProjectCreated?: () => void;
  onTaskCreated?: () => void;
}

export function QuickActions({ onProjectCreated, onTaskCreated }: QuickActionsProps) {
  const { currentWorkspace } = useWorkspaceStore();
  const { createProject, projects } = useProjectStore();

  const [activeDialog, setActiveDialog] = React.useState<"project" | "task" | null>(null);

  // Project form
  const [newProject, setNewProject] = React.useState({
    name: "",
    key: "",
    description: "",
    color: projectColors[0],
  });
  const [isCreatingProject, setIsCreatingProject] = React.useState(false);

  // Task form
  const [newTask, setNewTask] = React.useState({
    title: "",
    description: "",
    priority: "MEDIUM",
    projectId: "",
    dueDate: "",
    assigneeId: "",
  });
  const [isCreatingTask, setIsCreatingTask] = React.useState(false);
  const [members, setMembers] = React.useState<any[]>([]);
  const [selectedProject, setSelectedProject] = React.useState<any>(null);
  const [selectedAssignee, setSelectedAssignee] = React.useState<any>(null);
  const [isAssigneeOpen, setIsAssigneeOpen] = React.useState(false);
  const [searchQuery, setSearchQuery] = React.useState("");
  const [searchResults, setSearchResults] = React.useState<any[]>([]);
  const [isSearching, setIsSearching] = React.useState(false);

  const handleCreateProject = async () => {
    if (!newProject.name || !newProject.key) {
      toast.error("Please fill in all required fields");
      return;
    }
    if (newProject.key.length < 2 || newProject.key.length > 6) {
      toast.error("Project key must be 2-6 characters");
      return;
    }

    setIsCreatingProject(true);
    try {
      await createProject(currentWorkspace?.id || "", newProject);
      toast.success("Project created successfully!");
      setActiveDialog(null);
      setNewProject({ name: "", key: "", description: "", color: projectColors[0] });
      onProjectCreated?.();
    } catch {
      // Error handled by store
    } finally {
      setIsCreatingProject(false);
    }
  };

  const fetchProjectMembers = async (projectId: string) => {
    try {
      const response = await projectApi.getMembers(currentWorkspace?.id || "", projectId);
      if (response.data.success && response.data.data) {
        setMembers(response.data.data.map((m: any) => m.user));
      }
    } catch {
      // Ignore
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

  React.useEffect(() => {
    if (searchQuery.length >= 2) {
      const debounce = setTimeout(() => searchUsers(searchQuery), 300);
      return () => clearTimeout(debounce);
    } else {
      setSearchResults([]);
    }
  }, [searchQuery]);

  const handleSelectProject = (project: any) => {
    setSelectedProject(project);
    setNewTask({ ...newTask, projectId: project.id });
    fetchProjectMembers(project.id);
  };

  const handleSelectAssignee = (user: any) => {
    setSelectedAssignee(user);
    setNewTask({ ...newTask, assigneeId: user.id });
    setSearchQuery("");
    setSearchResults([]);
    setIsAssigneeOpen(false);
  };

  const handleCreateTask = async () => {
    if (!newTask.title) {
      toast.error("Please enter a task title");
      return;
    }
    if (!newTask.projectId) {
      toast.error("Please select a project");
      return;
    }

    setIsCreatingTask(true);
    try {
      const { createTask } = await import("@/store/taskStore").then(m => m.useTaskStore.getState());
      await createTask(newTask.projectId, {
        title: newTask.title,
        description: newTask.description || undefined,
        status: "TODO",
        priority: newTask.priority as any,
        dueDate: newTask.dueDate ? new Date(newTask.dueDate).toISOString().split("T")[0] as any : undefined,
        assigneeId: newTask.assigneeId || undefined,
      });
      toast.success("Task created successfully!");
      setActiveDialog(null);
      setNewTask({ title: "", description: "", priority: "MEDIUM", projectId: "", dueDate: "", assigneeId: "" });
      setSelectedProject(null);
      setSelectedAssignee(null);
      onTaskCreated?.();
    } catch {
      // Error handled by store
    } finally {
      setIsCreatingTask(false);
    }
  };

  const getInitials = (name: string) => {
    return name.split(" ").map(n => n[0]).join("").toUpperCase().slice(0, 2);
  };

  return (
    <>
      {/* Quick Actions Bar */}
      <div className="bg-card border rounded-xl p-4 mb-6">
        <h3 className="text-sm font-medium text-muted-foreground mb-3">Quick Actions</h3>
        <div className="flex flex-wrap gap-3">
          <Button
            variant="outline"
            size="sm"
            onClick={() => setActiveDialog("project")}
            className="gap-2"
          >
            <FolderKanban className="w-4 h-4" />
            New Project
          </Button>
          <Button
            variant="outline"
            size="sm"
            onClick={() => setActiveDialog("task")}
            className="gap-2"
          >
            <Plus className="w-4 h-4" />
            New Task
          </Button>
          <Button
            variant="outline"
            size="sm"
            onClick={() => {
              window.location.href = "/members";
            }}
            className="gap-2"
          >
            <Users className="w-4 h-4" />
            Invite Member
          </Button>
        </div>
      </div>

      {/* Create Project Dialog */}
      <Dialog open={activeDialog === "project"} onOpenChange={(open) => !open && setActiveDialog(null)}>
        <DialogContent className="max-w-md">
          <DialogHeader>
            <DialogTitle className="flex items-center gap-2">
              <FolderKanban className="w-5 h-5" />
              Create New Project
            </DialogTitle>
          </DialogHeader>
          <div className="space-y-4 py-4">
            <div className="space-y-2">
              <Label htmlFor="projectName">Project Name *</Label>
              <Input
                id="projectName"
                value={newProject.name}
                onChange={(e) => setNewProject({ ...newProject, name: e.target.value })}
                placeholder="My Awesome Project"
              />
            </div>
            <div className="space-y-2">
              <Label htmlFor="projectKey">
                Project Key * <span className="text-muted-foreground">(2-6 letters)</span>
              </Label>
              <Input
                id="projectKey"
                value={newProject.key}
                onChange={(e) =>
                  setNewProject({
                    ...newProject,
                    key: e.target.value.toUpperCase().slice(0, 6),
                  })
                }
                placeholder="MAP"
                maxLength={6}
                className="font-mono"
              />
            </div>
            <div className="space-y-2">
              <Label htmlFor="projectDesc">Description</Label>
              <Textarea
                id="projectDesc"
                value={newProject.description}
                onChange={(e) => setNewProject({ ...newProject, description: e.target.value })}
                placeholder="Brief project description"
              />
            </div>
            <div className="space-y-2">
              <Label>Color</Label>
              <div className="flex flex-wrap gap-2">
                {projectColors.map((color) => (
                  <button
                    key={color}
                    onClick={() => setNewProject({ ...newProject, color })}
                    className={`w-8 h-8 rounded-lg transition-transform ${
                      newProject.color === color ? "scale-110 ring-2 ring-primary" : ""
                    }`}
                    style={{ backgroundColor: color }}
                  />
                ))}
              </div>
            </div>
          </div>
          <DialogFooter>
            <Button variant="outline" onClick={() => setActiveDialog(null)}>
              Cancel
            </Button>
            <Button onClick={handleCreateProject} disabled={isCreatingProject}>
              {isCreatingProject ? (
                <>
                  <Loader2 className="w-4 h-4 mr-2 animate-spin" />
                  Creating...
                </>
              ) : (
                <>
                  <FolderKanban className="w-4 h-4 mr-2" />
                  Create Project
                </>
              )}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      {/* Create Task Dialog */}
      <Dialog open={activeDialog === "task"} onOpenChange={(open) => !open && setActiveDialog(null)}>
        <DialogContent className="max-w-md">
          <DialogHeader>
            <DialogTitle className="flex items-center gap-2">
              <Plus className="w-5 h-5" />
              Create New Task
            </DialogTitle>
          </DialogHeader>
          <div className="space-y-4 py-4 max-h-[60vh] overflow-y-auto">
            {/* Project Selection */}
            <div className="space-y-2">
              <Label>Project *</Label>
              <Select
                value={selectedProject?.id || ""}
                onValueChange={(value) => {
                  const project = projects.find(p => p.id === value);
                  if (project) handleSelectProject(project);
                }}
              >
                <SelectTrigger>
                  <SelectValue placeholder="Select a project" />
                </SelectTrigger>
                <SelectContent>
                  {projects.map((project) => (
                    <SelectItem key={project.id} value={project.id}>
                      <div className="flex items-center gap-2">
                        <div
                          className="w-4 h-4 rounded"
                          style={{ backgroundColor: project.color }}
                        />
                        {project.name} ({project.key})
                      </div>
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>

            {/* Task Title */}
            <div className="space-y-2">
              <Label htmlFor="taskTitle">Title *</Label>
              <Input
                id="taskTitle"
                value={newTask.title}
                onChange={(e) => setNewTask({ ...newTask, title: e.target.value })}
                placeholder="Enter task title"
              />
            </div>

            {/* Task Description */}
            <div className="space-y-2">
              <Label htmlFor="taskDesc">Description</Label>
              <Textarea
                id="taskDesc"
                value={newTask.description}
                onChange={(e) => setNewTask({ ...newTask, description: e.target.value })}
                placeholder="Enter task description"
              />
            </div>

            {/* Priority & Due Date */}
            <div className="grid grid-cols-2 gap-4">
              <div className="space-y-2">
                <Label>Priority</Label>
                <Select
                  value={newTask.priority}
                  onValueChange={(value) => setNewTask({ ...newTask, priority: value })}
                >
                  <SelectTrigger>
                    <SelectValue />
                  </SelectTrigger>
                  <SelectContent>
                    <SelectItem value="LOW">Low</SelectItem>
                    <SelectItem value="MEDIUM">Medium</SelectItem>
                    <SelectItem value="HIGH">High</SelectItem>
                    <SelectItem value="URGENT">Urgent</SelectItem>
                  </SelectContent>
                </Select>
              </div>
              <div className="space-y-2">
                <Label htmlFor="dueDate">Due Date</Label>
                <Input
                  id="dueDate"
                  type="date"
                  value={newTask.dueDate}
                  onChange={(e) => setNewTask({ ...newTask, dueDate: e.target.value })}
                />
              </div>
            </div>

            {/* Assignee */}
            <div className="space-y-2">
              <Label>Assignee</Label>
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
                            onClick={() => handleSelectAssignee(user)}
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
                            onClick={() => handleSelectAssignee(member)}
                          >
                            <Avatar className="w-6 h-6">
                              <AvatarFallback className="text-xs">
                                {getInitials(member.fullName)}
                              </AvatarFallback>
                            </Avatar>
                            <div>
                              <p className="text-sm font-medium">{member.fullName}</p>
                              <p className="text-xs text-muted-foreground">{member.email}</p>
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
            </div>
          </div>
          <DialogFooter>
            <Button variant="outline" onClick={() => setActiveDialog(null)}>
              Cancel
            </Button>
            <Button onClick={handleCreateTask} disabled={isCreatingTask}>
              {isCreatingTask ? (
                <>
                  <Loader2 className="w-4 h-4 mr-2 animate-spin" />
                  Creating...
                </>
              ) : (
                <>
                  <Plus className="w-4 h-4 mr-2" />
                  Create Task
                </>
              )}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </>
  );
}
