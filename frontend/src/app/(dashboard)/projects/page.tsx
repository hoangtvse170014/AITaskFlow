"use client";

import * as React from "react";
import { useParams } from "next/navigation";
import { MainLayout } from "@/components/layout/MainLayout";
import { Header } from "@/components/layout/Header";
import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogFooter,
} from "@/components/ui/dialog";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Plus, FolderKanban, Loader2 } from "lucide-react";
import { useWorkspaceStore } from "@/store/workspaceStore";
import { useProjectStore } from "@/store/projectStore";
import { projectColors } from "@/lib/utils";
import { usePermissions } from "@/hooks/usePermissions";
import Link from "next/link";
import toast from "react-hot-toast";

export default function ProjectsPage() {
  const params = useParams();
  const { currentWorkspace, fetchWorkspaces } = useWorkspaceStore();
  const { projects, isLoading, fetchProjects, createProject } = useProjectStore();
  const { canCreateProjects } = usePermissions();
  const [isCreateOpen, setIsCreateOpen] = React.useState(false);
  const [isCreating, setIsCreating] = React.useState(false);
  const [newProject, setNewProject] = React.useState({
    name: "",
    key: "",
    description: "",
    color: projectColors[0],
  });

  const workspaceId = currentWorkspace?.id || (params.workspaceId as string);

  React.useEffect(() => {
    async function loadData() {
      await fetchWorkspaces();
    }
    loadData();
  }, [fetchWorkspaces]);

  React.useEffect(() => {
    if (workspaceId && currentWorkspace) {
      fetchProjects(workspaceId);
    }
  }, [workspaceId, currentWorkspace, fetchProjects]);

  const handleCreateProject = async () => {
    if (!newProject.name || !newProject.key) {
      toast.error("Please fill in all required fields");
      return;
    }

    if (newProject.key.length < 2 || newProject.key.length > 6) {
      toast.error("Project key must be 2-6 characters");
      return;
    }

    setIsCreating(true);
    try {
      await createProject(workspaceId, newProject);
      setIsCreateOpen(false);
      setNewProject({ name: "", key: "", description: "", color: projectColors[0] });
    } catch {
      // Error handled by store
    } finally {
      setIsCreating(false);
    }
  };

  return (
    <MainLayout>
      <Header
        title="Projects"
        subtitle={currentWorkspace?.name || "All Projects"}
        onCreateProject={canCreateProjects() ? () => setIsCreateOpen(true) : undefined}
      />
      <div className="p-6">
        {isLoading ? (
          <div className="flex items-center justify-center h-64">
            <Loader2 className="w-8 h-8 animate-spin text-muted-foreground" />
          </div>
        ) : projects.length === 0 && !canCreateProjects() ? (
          <Card className="border-dashed">
            <CardContent className="flex flex-col items-center justify-center py-16">
              <FolderKanban className="w-16 h-16 text-muted-foreground mb-4" />
              <h3 className="text-lg font-semibold mb-2">No projects yet</h3>
              <p className="text-muted-foreground text-center max-w-md">
                No projects have been created in this workspace yet.
              </p>
            </CardContent>
          </Card>
        ) : projects.length === 0 ? (
          <Card className="border-dashed">
            <CardContent className="flex flex-col items-center justify-center py-16">
              <FolderKanban className="w-16 h-16 text-muted-foreground mb-4" />
              <h3 className="text-lg font-semibold mb-2">No projects yet</h3>
              <p className="text-muted-foreground mb-4 text-center max-w-md">
                Create your first project to start organizing your tasks and collaborating with your team.
              </p>
              <Button onClick={() => setIsCreateOpen(true)}>
                <Plus className="w-4 h-4 mr-2" />
                Create Project
              </Button>
            </CardContent>
          </Card>
        ) : (
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
            {projects.map((project) => (
              <Link key={project.id} href={`/projects/${project.id}`}>
                <Card className="hover:border-primary/50 transition-all cursor-pointer h-full">
                  <CardContent className="p-6">
                    <div className="flex items-start gap-4">
                      <div
                        className="w-12 h-12 rounded-xl flex items-center justify-center text-white text-lg font-bold shrink-0"
                        style={{ backgroundColor: project.color }}
                      >
                        {project.key.charAt(0)}
                      </div>
                      <div className="flex-1 min-w-0">
                        <div className="flex items-center gap-2">
                          <h3 className="font-semibold truncate">{project.name}</h3>
                        </div>
                        <p className="text-sm text-muted-foreground mt-1">
                          {project.key}
                        </p>
                        {project.description && (
                          <p className="text-sm text-muted-foreground mt-2 line-clamp-2">
                            {project.description}
                          </p>
                        )}
                      </div>
                    </div>
                    <div className="mt-4 pt-4 border-t border-border flex items-center justify-between">
                      <div>
                        <p className="text-2xl font-bold">{project.taskCount}</p>
                        <p className="text-xs text-muted-foreground">Tasks</p>
                      </div>
                      <div className="text-right">
                        <p className="text-2xl font-bold text-green-500">
                          {project.completedTaskCount}
                        </p>
                        <p className="text-xs text-muted-foreground">Completed</p>
                      </div>
                      <div className="flex-1">
                        <div className="h-2 bg-muted rounded-full overflow-hidden">
                          <div
                            className="h-full bg-green-500 rounded-full"
                            style={{
                              width: `${
                                project.taskCount > 0
                                  ? (project.completedTaskCount / project.taskCount) * 100
                                  : 0
                              }%`,
                            }}
                          />
                        </div>
                      </div>
                    </div>
                  </CardContent>
                </Card>
              </Link>
            ))}

            {canCreateProjects() && (
              <Card
                className="border-dashed cursor-pointer hover:border-primary/50 transition-all"
                onClick={() => setIsCreateOpen(true)}
              >
                <CardContent className="flex flex-col items-center justify-center h-full py-12">
                  <div className="w-12 h-12 rounded-xl bg-muted flex items-center justify-center mb-3">
                    <Plus className="w-6 h-6 text-muted-foreground" />
                  </div>
                  <p className="font-medium">Create New Project</p>
                </CardContent>
              </Card>
            )}
          </div>
        )}
      </div>

      {/* Create Project Dialog */}
      <Dialog open={isCreateOpen} onOpenChange={setIsCreateOpen}>
        <DialogContent className="max-w-md">
          <DialogHeader>
            <DialogTitle>Create New Project</DialogTitle>
          </DialogHeader>
          <div className="space-y-4 py-4">
            <div className="space-y-2">
              <Label htmlFor="projectName">Project Name *</Label>
              <Input
                id="projectName"
                value={newProject.name}
                onChange={(e) =>
                  setNewProject({ ...newProject, name: e.target.value })
                }
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
              <Input
                id="projectDesc"
                value={newProject.description}
                onChange={(e) =>
                  setNewProject({ ...newProject, description: e.target.value })
                }
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
            <Button variant="outline" onClick={() => setIsCreateOpen(false)}>
              Cancel
            </Button>
            <Button onClick={handleCreateProject} disabled={isCreating}>
              {isCreating ? (
                <>
                  <Loader2 className="w-4 h-4 mr-2 animate-spin" />
                  Creating...
                </>
              ) : (
                "Create Project"
              )}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </MainLayout>
  );
}
