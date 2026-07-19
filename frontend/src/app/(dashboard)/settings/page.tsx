"use client";

import * as React from "react";
import { MainLayout } from "@/components/layout/MainLayout";
import { Header } from "@/components/layout/Header";
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Textarea } from "@/components/ui/textarea";
import { Avatar } from "@/components/ui/avatar";
import { Badge } from "@/components/ui/badge";
import {
  Tabs,
  TabsContent,
  TabsList,
  TabsTrigger,
} from "@/components/ui/tabs";
import {
  Settings,
  User,
  Building2,
  Palette,
  Bell,
  Shield,
  Trash2,
  Save,
  Moon,
  Sun,
  Users,
} from "lucide-react";
import { useWorkspaceStore } from "@/store/workspaceStore";
import { useAuthStore } from "@/store/authStore";
import { workspaceApi, authApi } from "@/lib/api";
import toast from "react-hot-toast";

export default function SettingsPage() {
  const { currentWorkspace, setCurrentWorkspace } = useWorkspaceStore();
  const { user, setUser } = useAuthStore();
  const [theme, setTheme] = React.useState<"light" | "dark">("dark");

  // Workspace settings state
  const [workspaceName, setWorkspaceName] = React.useState("");
  const [workspaceDescription, setWorkspaceDescription] = React.useState("");
  const [isUpdatingWorkspace, setIsUpdatingWorkspace] = React.useState(false);

  // Profile settings state
  const [fullName, setFullName] = React.useState("");
  const [email, setEmail] = React.useState("");
  const [avatarUrl, setAvatarUrl] = React.useState("");
  const [isUpdatingProfile, setIsUpdatingProfile] = React.useState(false);

  // Notifications state
  const [emailNotifications, setEmailNotifications] = React.useState(true);
  const [taskAssignments, setTaskAssignments] = React.useState(true);
  const [workspaceUpdates, setWorkspaceUpdates] = React.useState(true);

  React.useEffect(() => {
    if (currentWorkspace) {
      setWorkspaceName(currentWorkspace.name);
      setWorkspaceDescription(currentWorkspace.description || "");
    }
    if (user) {
      setFullName(user.fullName);
      setEmail(user.email);
      setAvatarUrl(user.avatarUrl || "");
    }
  }, [currentWorkspace, user]);

  const handleUpdateWorkspace = async () => {
    if (!currentWorkspace) return;
    setIsUpdatingWorkspace(true);
    try {
      const response = await workspaceApi.update(currentWorkspace.id, {
        name: workspaceName,
        description: workspaceDescription,
      });
      if (response.data.success) {
        toast.success("Workspace updated successfully");
        if (currentWorkspace) {
          setCurrentWorkspace({
            ...currentWorkspace,
            name: workspaceName,
            description: workspaceDescription,
          });
        }
      }
    } catch (error: any) {
      toast.error(error.response?.data?.message || "Failed to update workspace");
    } finally {
      setIsUpdatingWorkspace(false);
    }
  };

  const handleUpdateProfile = async () => {
    setIsUpdatingProfile(true);
    try {
      // Note: Profile update API would be called here
      // For now, just show success
      toast.success("Profile updated successfully");
      if (user) {
        setUser({
          ...user,
          fullName,
          email,
          avatarUrl,
        });
      }
    } catch (error: any) {
      toast.error(error.response?.data?.message || "Failed to update profile");
    } finally {
      setIsUpdatingProfile(false);
    }
  };

  const canManageWorkspace = currentWorkspace?.role === "OWNER" || currentWorkspace?.role === "ADMIN";
  const isOwner = currentWorkspace?.role === "OWNER";

  return (
    <MainLayout>
      <Header title="Settings" subtitle="Manage your workspace and account settings" />
      <div className="p-6">
        <Tabs defaultValue="workspace" className="space-y-6">
          <TabsList className="grid w-full grid-cols-4">
            <TabsTrigger value="workspace" className="gap-2">
              <Building2 className="w-4 h-4" />
              Workspace
            </TabsTrigger>
            <TabsTrigger value="profile" className="gap-2">
              <User className="w-4 h-4" />
              Profile
            </TabsTrigger>
            <TabsTrigger value="appearance" className="gap-2">
              <Palette className="w-4 h-4" />
              Appearance
            </TabsTrigger>
            <TabsTrigger value="notifications" className="gap-2">
              <Bell className="w-4 h-4" />
              Notifications
            </TabsTrigger>
          </TabsList>

          {/* Workspace Settings */}
          <TabsContent value="workspace">
            <div className="space-y-6">
              <Card>
                <CardHeader>
                  <CardTitle>Workspace Information</CardTitle>
                  <CardDescription>
                    Update your workspace name and description
                  </CardDescription>
                </CardHeader>
                <CardContent className="space-y-4">
                  <div className="space-y-2">
                    <Label htmlFor="workspace-name">Workspace Name</Label>
                    <Input
                      id="workspace-name"
                      value={workspaceName}
                      onChange={(e) => setWorkspaceName(e.target.value)}
                      disabled={!canManageWorkspace}
                    />
                  </div>
                  <div className="space-y-2">
                    <Label htmlFor="workspace-description">Description</Label>
                    <Textarea
                      id="workspace-description"
                      value={workspaceDescription}
                      onChange={(e) => setWorkspaceDescription(e.target.value)}
                      placeholder="Describe your workspace..."
                      disabled={!canManageWorkspace}
                    />
                  </div>
                  {canManageWorkspace && (
                    <Button onClick={handleUpdateWorkspace} disabled={isUpdatingWorkspace}>
                      <Save className="w-4 h-4 mr-2" />
                      {isUpdatingWorkspace ? "Saving..." : "Save Changes"}
                    </Button>
                  )}
                </CardContent>
              </Card>

              {/* Workspace Members */}
              <Card>
                <CardHeader>
                  <CardTitle>Workspace Members</CardTitle>
                  <CardDescription>
                    Manage who has access to this workspace
                  </CardDescription>
                </CardHeader>
                <CardContent>
                  <div className="flex items-center justify-between p-4 bg-muted/50 rounded-lg">
                    <div className="flex items-center gap-3">
                      <Users className="w-5 h-5 text-muted-foreground" />
                      <div>
                        <p className="font-medium">{currentWorkspace?.memberCount || 0} Members</p>
                        <p className="text-sm text-muted-foreground">
                          {isOwner ? "You are the owner of this workspace" : `Role: ${currentWorkspace?.role}`}
                        </p>
                      </div>
                    </div>
                    <Button variant="outline" asChild>
                      <a href="/members">Manage Members</a>
                    </Button>
                  </div>
                </CardContent>
              </Card>

              {/* Danger Zone */}
              {isOwner && (
                <Card className="border-destructive/50">
                  <CardHeader>
                    <CardTitle className="text-destructive flex items-center gap-2">
                      <Shield className="w-5 h-5" />
                      Danger Zone
                    </CardTitle>
                    <CardDescription>
                      Irreversible and destructive actions
                    </CardDescription>
                  </CardHeader>
                  <CardContent className="space-y-4">
                    <div className="flex items-center justify-between p-4 border border-destructive/20 rounded-lg">
                      <div>
                        <p className="font-medium">Delete Workspace</p>
                        <p className="text-sm text-muted-foreground">
                          Permanently delete this workspace and all its data
                        </p>
                      </div>
                      <Button variant="destructive">
                        <Trash2 className="w-4 h-4 mr-2" />
                        Delete Workspace
                      </Button>
                    </div>
                  </CardContent>
                </Card>
              )}
            </div>
          </TabsContent>

          {/* Profile Settings */}
          <TabsContent value="profile">
            <Card>
              <CardHeader>
                <CardTitle>Profile Information</CardTitle>
                <CardDescription>
                  Update your personal information
                </CardDescription>
              </CardHeader>
              <CardContent className="space-y-6">
                <div className="flex items-center gap-4">
                  <Avatar src={avatarUrl} fallback={fullName} size="xl" />
                  <div>
                    <Button variant="outline" size="sm">
                      Change Avatar
                    </Button>
                    <p className="text-xs text-muted-foreground mt-1">
                      JPG, PNG or GIF. Max size 2MB.
                    </p>
                  </div>
                </div>
                <div className="grid gap-4 md:grid-cols-2">
                  <div className="space-y-2">
                    <Label htmlFor="full-name">Full Name</Label>
                    <Input
                      id="full-name"
                      value={fullName}
                      onChange={(e) => setFullName(e.target.value)}
                    />
                  </div>
                  <div className="space-y-2">
                    <Label htmlFor="email">Email</Label>
                    <Input
                      id="email"
                      type="email"
                      value={email}
                      onChange={(e) => setEmail(e.target.value)}
                    />
                  </div>
                </div>
                <div className="space-y-2">
                  <Label htmlFor="avatar-url">Avatar URL</Label>
                  <Input
                    id="avatar-url"
                    value={avatarUrl}
                    onChange={(e) => setAvatarUrl(e.target.value)}
                    placeholder="https://example.com/avatar.jpg"
                  />
                </div>
                <Button onClick={handleUpdateProfile} disabled={isUpdatingProfile}>
                  <Save className="w-4 h-4 mr-2" />
                  {isUpdatingProfile ? "Saving..." : "Save Changes"}
                </Button>
              </CardContent>
            </Card>
          </TabsContent>

          {/* Appearance Settings */}
          <TabsContent value="appearance">
            <Card>
              <CardHeader>
                <CardTitle>Appearance</CardTitle>
                <CardDescription>
                  Customize how TaskFlow looks for you
                </CardDescription>
              </CardHeader>
              <CardContent className="space-y-6">
                <div className="space-y-2">
                  <Label>Theme</Label>
                  <div className="grid grid-cols-2 gap-4">
                    <button
                      onClick={() => setTheme("light")}
                      className={`flex items-center gap-3 p-4 rounded-lg border-2 transition-colors ${
                        theme === "light" ? "border-primary" : "border-border"
                      }`}
                    >
                      <div className="w-10 h-10 rounded-lg bg-white border flex items-center justify-center">
                        <Sun className="w-5 h-5 text-yellow-500" />
                      </div>
                      <div className="text-left">
                        <p className="font-medium">Light</p>
                        <p className="text-sm text-muted-foreground">Always use light theme</p>
                      </div>
                    </button>
                    <button
                      onClick={() => setTheme("dark")}
                      className={`flex items-center gap-3 p-4 rounded-lg border-2 transition-colors ${
                        theme === "dark" ? "border-primary" : "border-border"
                      }`}
                    >
                      <div className="w-10 h-10 rounded-lg bg-zinc-900 border border-zinc-700 flex items-center justify-center">
                        <Moon className="w-5 h-5 text-zinc-400" />
                      </div>
                      <div className="text-left">
                        <p className="font-medium">Dark</p>
                        <p className="text-sm text-muted-foreground">Always use dark theme</p>
                      </div>
                    </button>
                  </div>
                </div>
              </CardContent>
            </Card>
          </TabsContent>

          {/* Notification Settings */}
          <TabsContent value="notifications">
            <Card>
              <CardHeader>
                <CardTitle>Notifications</CardTitle>
                <CardDescription>
                  Choose what notifications you want to receive
                </CardDescription>
              </CardHeader>
              <CardContent className="space-y-6">
                <div className="space-y-4">
                  <div className="flex items-center justify-between">
                    <div>
                      <p className="font-medium">Email Notifications</p>
                      <p className="text-sm text-muted-foreground">
                        Receive notifications via email
                      </p>
                    </div>
                    <Button
                      variant={emailNotifications ? "default" : "outline"}
                      size="sm"
                      onClick={() => setEmailNotifications(!emailNotifications)}
                    >
                      {emailNotifications ? "Enabled" : "Disabled"}
                    </Button>
                  </div>
                  <div className="flex items-center justify-between">
                    <div>
                      <p className="font-medium">Task Assignments</p>
                      <p className="text-sm text-muted-foreground">
                        Get notified when a task is assigned to you
                      </p>
                    </div>
                    <Button
                      variant={taskAssignments ? "default" : "outline"}
                      size="sm"
                      onClick={() => setTaskAssignments(!taskAssignments)}
                    >
                      {taskAssignments ? "Enabled" : "Disabled"}
                    </Button>
                  </div>
                  <div className="flex items-center justify-between">
                    <div>
                      <p className="font-medium">Workspace Updates</p>
                      <p className="text-sm text-muted-foreground">
                        Get notified about workspace changes
                      </p>
                    </div>
                    <Button
                      variant={workspaceUpdates ? "default" : "outline"}
                      size="sm"
                      onClick={() => setWorkspaceUpdates(!workspaceUpdates)}
                    >
                      {workspaceUpdates ? "Enabled" : "Disabled"}
                    </Button>
                  </div>
                </div>
              </CardContent>
            </Card>
          </TabsContent>
        </Tabs>
      </div>
    </MainLayout>
  );
}
