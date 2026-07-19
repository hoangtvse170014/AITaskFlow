"use client";

import * as React from "react";
import { MainLayout } from "@/components/layout/MainLayout";
import { Header } from "@/components/layout/Header";
import { Card, CardContent } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Avatar } from "@/components/ui/avatar";
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogFooter,
} from "@/components/ui/dialog";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import { Badge } from "@/components/ui/badge";
import {
  Users,
  Mail,
  Shield,
  MoreVertical,
  UserMinus,
  Crown,
  UserCog,
  User,
  Plus,
  Eye,
} from "lucide-react";
import { useWorkspaceStore } from "@/store/workspaceStore";
import { workspaceApi } from "@/lib/api";
import { usePermissions } from "@/hooks/usePermissions";
import toast from "react-hot-toast";
import { format } from "date-fns";
import type { WorkspaceMember } from "@/types";

const ROLES = ["OWNER", "ADMIN", "MANAGER", "MEMBER", "GUEST"] as const;

export default function MembersPage() {
  const { currentWorkspace } = useWorkspaceStore();
  const { canInviteMembers, canManageMembers, canRemoveMembers, isOwner } = usePermissions();
  const [members, setMembers] = React.useState<WorkspaceMember[]>([]);
  const [isLoading, setIsLoading] = React.useState(true);
  const [isInviteOpen, setIsInviteOpen] = React.useState(false);
  const [inviteEmail, setInviteEmail] = React.useState("");
  const [inviteRole, setInviteRole] = React.useState("MEMBER");
  const [isInviting, setIsInviting] = React.useState(false);
  const [removingId, setRemovingId] = React.useState<string | null>(null);

  React.useEffect(() => {
    if (currentWorkspace) {
      fetchMembers();
    }
  }, [currentWorkspace]);

  const fetchMembers = async () => {
    if (!currentWorkspace) return;
    setIsLoading(true);
    try {
      const response = await workspaceApi.getMembers(currentWorkspace.id);
      if (response.data.success) {
        setMembers(response.data.data);
      }
    } catch (error) {
      console.error("Failed to fetch members:", error);
      toast.error("Failed to load members");
    } finally {
      setIsLoading(false);
    }
  };

  const handleInvite = async () => {
    if (!currentWorkspace || !inviteEmail) return;
    setIsInviting(true);
    try {
      await workspaceApi.inviteMember(currentWorkspace.id, {
        email: inviteEmail,
        role: inviteRole,
      });
      toast.success("Member added successfully");
      setIsInviteOpen(false);
      setInviteEmail("");
      setInviteRole("MEMBER");
      fetchMembers();
    } catch (error: any) {
      const message = error.response?.data?.message || "";
      if (message.includes("not found") || message.includes("User")) {
        toast.error("User not found. Please make sure they have registered an account first.");
      } else {
        toast.error(message || "Failed to add member");
      }
    } finally {
      setIsInviting(false);
    }
  };

  const handleRemoveMember = async (memberId: string) => {
    if (!currentWorkspace) return;
    setRemovingId(memberId);
    try {
      await workspaceApi.removeMember(currentWorkspace.id, memberId);
      toast.success("Member removed successfully");
      fetchMembers();
    } catch (error: any) {
      toast.error(error.response?.data?.message || "Failed to remove member");
    } finally {
      setRemovingId(null);
    }
  };

  const getRoleIcon = (role: string) => {
    switch (role) {
      case "OWNER":
        return <Crown className="w-4 h-4 text-yellow-500" />;
      case "ADMIN":
        return <Shield className="w-4 h-4 text-blue-500" />;
      case "MANAGER":
        return <UserCog className="w-4 h-4 text-purple-500" />;
      case "GUEST":
        return <Eye className="w-4 h-4 text-gray-400" />;
      default:
        return <User className="w-4 h-4 text-gray-500" />;
    }
  };

  const getRoleBadgeColor = (role: string) => {
    switch (role) {
      case "OWNER":
        return "bg-yellow-500/10 text-yellow-500 border-yellow-500/20";
      case "ADMIN":
        return "bg-blue-500/10 text-blue-500 border-blue-500/20";
      case "MANAGER":
        return "bg-purple-500/10 text-purple-500 border-purple-500/20";
      case "GUEST":
        return "bg-gray-500/10 text-gray-400 border-gray-500/20";
      default:
        return "bg-gray-500/10 text-gray-500 border-gray-500/20";
    }
  };

  const getRoleLabel = (role: string) => {
    switch (role) {
      case "OWNER":
        return "Owner";
      case "ADMIN":
        return "Admin";
      case "MANAGER":
        return "Manager";
      case "GUEST":
        return "Guest";
      default:
        return "Member";
    }
  };

  const getRoleDescription = (role: string) => {
    switch (role) {
      case "ADMIN":
        return "Can manage members and settings";
      case "MANAGER":
        return "Can manage projects and tasks";
      case "GUEST":
        return "View only access";
      default:
        return "Can create and edit content";
    }
  };

  const selectableRoles = ROLES.filter(r => r !== "OWNER");

  return (
    <MainLayout>
      <Header
        title="Members"
        subtitle={`${members.length} member${members.length !== 1 ? "s" : ""} in ${currentWorkspace?.name || "workspace"}`}
        actions={
          canInviteMembers() && (
            <Button onClick={() => setIsInviteOpen(true)}>
              <Plus className="w-4 h-4 mr-2" />
              Add Member
            </Button>
          )
        }
      />
      <div className="p-6">
        <div className="grid gap-4">
          {isLoading ? (
            <div className="text-center py-12 text-muted-foreground">
              <Users className="w-12 h-12 mx-auto mb-3 opacity-50 animate-pulse" />
              <p>Loading members...</p>
            </div>
          ) : members.length === 0 ? (
            <Card>
              <CardContent className="py-12 text-center text-muted-foreground">
                <Users className="w-12 h-12 mx-auto mb-3 opacity-50" />
                <p>No members yet</p>
                {canInviteMembers() && (
                  <Button
                    variant="link"
                    onClick={() => setIsInviteOpen(true)}
                    className="mt-2"
                  >
                    Invite the first member
                  </Button>
                )}
              </CardContent>
            </Card>
          ) : (
            members.map((member) => (
              <Card key={member.id} className="hover:bg-accent/50 transition-colors">
                <CardContent className="p-4">
                  <div className="flex items-center justify-between">
                    <div className="flex items-center gap-4">
                      <Avatar
                        src={member.user.avatarUrl}
                        fallback={member.user.fullName}
                        size="lg"
                      />
                      <div>
                        <div className="flex items-center gap-2">
                          <p className="font-medium">{member.user.fullName}</p>
                          {member.role === "OWNER" && (
                            <Badge className={getRoleBadgeColor(member.role)} variant="outline">
                              <Crown className="w-3 h-3 mr-1" />
                              Owner
                            </Badge>
                          )}
                        </div>
                        <p className="text-sm text-muted-foreground flex items-center gap-1">
                          <Mail className="w-3 h-3" />
                          {member.user.email}
                        </p>
                        {member.joinedAt && (
                          <p className="text-xs text-muted-foreground">
                            Joined {format(new Date(member.joinedAt), "MMM d, yyyy")}
                          </p>
                        )}
                      </div>
                    </div>
                    <div className="flex items-center gap-3">
                      <Badge className={getRoleBadgeColor(member.role)} variant="outline">
                        {getRoleIcon(member.role)}
                        <span className="ml-1">{getRoleLabel(member.role)}</span>
                      </Badge>
                      {canManageMembers() && member.role !== "OWNER" && (
                        <DropdownMenu>
                          <DropdownMenuTrigger asChild>
                            <Button variant="ghost" size="icon">
                              <MoreVertical className="w-4 h-4" />
                            </Button>
                          </DropdownMenuTrigger>
                          <DropdownMenuContent align="end">
                            <DropdownMenuItem>
                              <UserCog className="w-4 h-4 mr-2" />
                              Change Role
                            </DropdownMenuItem>
                            {canRemoveMembers() && (
                              <>
                                <DropdownMenuSeparator />
                                <DropdownMenuItem
                                  onClick={() => handleRemoveMember(member.id)}
                                  disabled={removingId === member.id}
                                  className="text-destructive focus:text-destructive"
                                >
                                  <UserMinus className="w-4 h-4 mr-2" />
                                  Remove from workspace
                                </DropdownMenuItem>
                              </>
                            )}
                          </DropdownMenuContent>
                        </DropdownMenu>
                      )}
                    </div>
                  </div>
                </CardContent>
              </Card>
            ))
          )}
        </div>
      </div>

      {/* Invite Dialog */}
      <Dialog open={isInviteOpen} onOpenChange={setIsInviteOpen}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>Add Member</DialogTitle>
          </DialogHeader>
          <div className="space-y-4 py-4">
            <p className="text-sm text-muted-foreground">
              Enter the email of an existing user to add them to this workspace.
            </p>
            <div className="space-y-2">
              <Label htmlFor="email">Email address</Label>
              <Input
                id="email"
                type="email"
                placeholder="colleague@company.com"
                value={inviteEmail}
                onChange={(e) => setInviteEmail(e.target.value)}
              />
            </div>
            <div className="space-y-2">
              <Label>Role</Label>
              <Select value={inviteRole} onValueChange={setInviteRole}>
                <SelectTrigger>
                  <SelectValue placeholder="Select role" />
                </SelectTrigger>
                <SelectContent>
                  {selectableRoles.map((role) => (
                    <SelectItem key={role} value={role}>
                      <div className="flex items-center">
                        {getRoleIcon(role)}
                        <span className="ml-2 mr-2">{getRoleLabel(role)}</span>
                        <span className="text-xs text-muted-foreground">
                          - {getRoleDescription(role)}
                        </span>
                      </div>
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>
          </div>
          <DialogFooter>
            <Button variant="outline" onClick={() => setIsInviteOpen(false)}>
              Cancel
            </Button>
            <Button onClick={handleInvite} disabled={!inviteEmail || isInviting}>
              {isInviting ? "Adding..." : "Add Member"}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </MainLayout>
  );
}
