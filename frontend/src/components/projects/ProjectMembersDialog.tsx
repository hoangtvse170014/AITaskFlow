"use client";

import * as React from "react";
import { Loader2, UserPlus, X, AlertCircle } from "lucide-react";
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Avatar, AvatarFallback } from "@/components/ui/avatar";
import { Badge } from "@/components/ui/badge";
import { ScrollArea } from "@/components/ui/scroll-area";
import { userApi, projectApi } from "@/lib/api";
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

interface ProjectMembersDialogProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  projectId: string;
  isAdmin: boolean;
  onMembersChange?: () => void;
}

export function ProjectMembersDialog({
  open,
  onOpenChange,
  projectId,
  isAdmin,
  onMembersChange,
}: ProjectMembersDialogProps) {
  const { currentWorkspace } = useWorkspaceStore();
  const [members, setMembers] = React.useState<ProjectMember[]>([]);
  const [isLoading, setIsLoading] = React.useState(false);
  const [isSearching, setIsSearching] = React.useState(false);
  const [searchQuery, setSearchQuery] = React.useState("");
  const [searchResults, setSearchResults] = React.useState<any[]>([]);
  const [isAdding, setIsAdding] = React.useState(false);
  const [addingRole, setAddingRole] = React.useState("MEMBER");

  React.useEffect(() => {
    if (open && projectId) {
      fetchMembers();
    }
  }, [open, projectId]);

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

  const searchUsers = async (query: string) => {
    setIsSearching(true);
    try {
      const response = await userApi.search(query);
      if (response.data.success && response.data.data) {
        // Filter out existing members
        const existingIds = members.map((m) => m.user.id);
        setSearchResults(
          response.data.data.filter((u: any) => !existingIds.includes(u.id))
        );
      }
    } catch {
      toast.error("Failed to search users");
    } finally {
      setIsSearching(false);
    }
  };

  const handleAddMember = async (user: any) => {
    if (!currentWorkspace?.id) return;
    setIsAdding(true);
    try {
      await projectApi.addMember(currentWorkspace.id, projectId, {
        email: user.email,
        role: addingRole,
      });
      toast.success(`${user.fullName} added to project`);
      setSearchQuery("");
      setSearchResults([]);
      fetchMembers();
      onMembersChange?.();
    } catch (error: any) {
      toast.error(error?.response?.data?.message || "Failed to add member");
    } finally {
      setIsAdding(false);
    }
  };

  const handleRemoveMember = async (memberId: string) => {
    if (!currentWorkspace?.id) return;
    if (!confirm("Are you sure you want to remove this member?")) return;
    
    try {
      await projectApi.removeMember(currentWorkspace.id, projectId, memberId);
      toast.success("Member removed");
      fetchMembers();
      onMembersChange?.();
    } catch {
      toast.error("Failed to remove member");
    }
  };

  const getInitials = (name: string) => {
    return name
      .split(" ")
      .map((n) => n[0])
      .join("")
      .toUpperCase()
      .slice(0, 2);
  };

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="sm:max-w-md">
        <DialogHeader>
          <DialogTitle>Project Members</DialogTitle>
        </DialogHeader>

        <div className="space-y-4">
          {/* Search & Add Section */}
          {isAdmin && (
            <div className="space-y-2">
              <div className="relative">
                <Input
                  placeholder="Search users by email or name..."
                  value={searchQuery}
                  onChange={(e) => setSearchQuery(e.target.value)}
                />
                {isSearching && (
                  <Loader2 className="absolute right-3 top-1/2 -translate-y-1/2 w-4 h-4 animate-spin" />
                )}
              </div>

              {/* Search Results */}
              {searchResults.length > 0 && (
                <div className="border rounded-md bg-background max-h-48 overflow-y-auto">
                  {searchResults.map((user) => (
                    <div
                      key={user.id}
                      className="flex items-center justify-between p-2 hover:bg-muted/50"
                    >
                      <div className="flex items-center gap-2">
                        <Avatar className="w-8 h-8">
                          <AvatarFallback className="text-xs">
                            {getInitials(user.fullName)}
                          </AvatarFallback>
                        </Avatar>
                        <div>
                          <p className="text-sm font-medium">{user.fullName}</p>
                          <p className="text-xs text-muted-foreground">
                            {user.email}
                          </p>
                        </div>
                      </div>
                      <div className="flex items-center gap-2">
                        <select
                          className="text-xs border rounded px-1 py-0.5"
                          value={addingRole}
                          onChange={(e) => setAddingRole(e.target.value)}
                        >
                          <option value="MEMBER">Member</option>
                          <option value="ADMIN">Admin</option>
                        </select>
                        <Button
                          size="sm"
                          variant="ghost"
                          onClick={() => handleAddMember(user)}
                          disabled={isAdding}
                        >
                          <UserPlus className="w-4 h-4" />
                        </Button>
                      </div>
                    </div>
                  ))}
                </div>
              )}
            </div>
          )}

          {/* Members List */}
          <ScrollArea className="max-h-80">
            {isLoading ? (
              <div className="flex items-center justify-center py-8">
                <Loader2 className="w-6 h-6 animate-spin" />
              </div>
            ) : members.length === 0 ? (
              <div className="text-center py-8 text-muted-foreground">
                <AlertCircle className="w-8 h-8 mx-auto mb-2 opacity-50" />
                <p className="text-sm">No members yet</p>
              </div>
            ) : (
              <div className="space-y-2">
                {members.map((member) => (
                  <div
                    key={member.id}
                    className="flex items-center justify-between p-2 rounded-md hover:bg-muted/50"
                  >
                    <div className="flex items-center gap-3">
                      <Avatar className="w-8 h-8">
                        <AvatarFallback className="text-xs bg-primary/10 text-primary">
                          {getInitials(member.user.fullName)}
                        </AvatarFallback>
                      </Avatar>
                      <div>
                        <p className="text-sm font-medium">
                          {member.user.fullName}
                        </p>
                        <p className="text-xs text-muted-foreground">
                          {member.user.email}
                        </p>
                      </div>
                    </div>
                    <div className="flex items-center gap-2">
                      <Badge variant="outline" className="text-xs">
                        {member.role}
                      </Badge>
                      {isAdmin && (
                        <Button
                          size="sm"
                          variant="ghost"
                          className="h-8 w-8 p-0 text-muted-foreground hover:text-destructive"
                          onClick={() => handleRemoveMember(member.id)}
                        >
                          <X className="w-4 h-4" />
                        </Button>
                      )}
                    </div>
                  </div>
                ))}
              </div>
            )}
          </ScrollArea>
        </div>
      </DialogContent>
    </Dialog>
  );
}
