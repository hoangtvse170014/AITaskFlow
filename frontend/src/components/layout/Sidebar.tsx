"use client";

import * as React from "react";
import Link from "next/link";
import { usePathname } from "next/navigation";
import {
  LayoutDashboard,
  FolderKanban,
  Settings,
  ChevronLeft,
  ChevronRight,
  Plus,
  LogOut,
  Users,
  FileText,
  ChevronDown,
  ChevronRightIcon,
  MoreHorizontal,
  Star,
  Trash2,
  Pencil,
} from "lucide-react";
import { cn } from "@/lib/utils";
import { Button } from "@/components/ui/button";
import { Avatar } from "@/components/ui/avatar";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import { useWorkspaceStore } from "@/store/workspaceStore";
import { useAuthStore } from "@/store/authStore";
import { pageApi } from "@/lib/api";
import { usePageStore } from "@/store/pageStore";
import { usePermissions } from "@/hooks/usePermissions";
import toast from "react-hot-toast";

interface SidebarProps {
  collapsed: boolean;
  onToggle: () => void;
}

interface PageItem {
  id: string;
  title: string;
  icon: string | null;
  isFavorite: boolean;
  children: PageItem[];
}

interface PageTreeItemProps {
  page: PageItem;
  depth: number;
  currentWorkspaceId: string | undefined;
  onDelete: (pageId: string) => void;
  onRename: (pageId: string, newTitle: string) => void;
  onToggleFavorite: (pageId: string, isFavorite: boolean) => void;
}

function PageTreeItem({ page, depth, currentWorkspaceId, onDelete, onRename, onToggleFavorite }: PageTreeItemProps) {
  const pathname = usePathname();
  const [isExpanded, setIsExpanded] = React.useState(true);
  const hasChildren = page.children && page.children.length > 0;
  const isActive = pathname === `/workspace/${currentWorkspaceId}/pages/${page.id}`;

  return (
    <div>
      <div
        className={cn(
          "group flex items-center gap-1 px-2 py-1.5 rounded-md text-sm cursor-pointer transition-all",
          isActive
            ? "bg-primary/10 text-primary"
            : "text-muted-foreground hover:bg-accent hover:text-foreground"
        )}
        style={{ paddingLeft: `${depth * 12 + 8}px` }}
      >
        <button
          onClick={() => hasChildren && setIsExpanded(!isExpanded)}
          className={cn(
            "w-4 h-4 flex items-center justify-center rounded hover:bg-accent",
            !hasChildren && "invisible"
          )}
        >
          {isExpanded ? (
            <ChevronDown className="w-3 h-3" />
          ) : (
            <ChevronRightIcon className="w-3 h-3" />
          )}
        </button>
        
        <Link
          href={`/workspace/${currentWorkspaceId}/pages/${page.id}`}
          className="flex items-center gap-2 flex-1 min-w-0"
        >
          <span className="text-base shrink-0">{page.icon || "📄"}</span>
          <span className="truncate">{page.title}</span>
        </Link>

        <DropdownMenu>
          <DropdownMenuTrigger asChild>
            <Button
              variant="ghost"
              size="icon"
              className="h-6 w-6 opacity-0 group-hover:opacity-100"
              onClick={(e) => e.preventDefault()}
            >
              <MoreHorizontal className="w-4 h-4" />
            </Button>
          </DropdownMenuTrigger>
          <DropdownMenuContent align="start">
            <DropdownMenuItem onClick={() => onToggleFavorite(page.id, !page.isFavorite)}>
              <Star className={cn("w-4 h-4 mr-2", page.isFavorite && "fill-primary")} />
              {page.isFavorite ? "Remove from favorites" : "Add to favorites"}
            </DropdownMenuItem>
            <DropdownMenuItem onClick={() => {
              const newTitle = prompt("Enter new title:", page.title);
              if (newTitle && newTitle !== page.title) {
                onRename(page.id, newTitle);
              }
            }}>
              <Pencil className="w-4 h-4 mr-2" />
              Rename
            </DropdownMenuItem>
            <DropdownMenuSeparator />
            <DropdownMenuItem
              className="text-destructive focus:text-destructive"
              onClick={() => {
                if (confirm(`Delete "${page.title}"?`)) {
                  onDelete(page.id);
                }
              }}
            >
              <Trash2 className="w-4 h-4 mr-2" />
              Delete
            </DropdownMenuItem>
          </DropdownMenuContent>
        </DropdownMenu>
      </div>

      {hasChildren && isExpanded && (
        <div>
          {page.children.map((child) => (
            <PageTreeItem
              key={child.id}
              page={child}
              depth={depth + 1}
              currentWorkspaceId={currentWorkspaceId}
              onDelete={onDelete}
              onRename={onRename}
              onToggleFavorite={onToggleFavorite}
            />
          ))}
        </div>
      )}
    </div>
  );
}

export function Sidebar({ collapsed, onToggle }: SidebarProps) {
  const pathname = usePathname();
  const { currentWorkspace, workspaces, setCurrentWorkspace } = useWorkspaceStore();
  const { user, logout } = useAuthStore();
  const { updatePage, toggleFavorite: togglePageFavorite } = usePageStore();
  const { canViewProjects, canViewMembers, canViewPages, canCreatePages, canManageWorkspace } = usePermissions();
  
  const [favoritePages, setFavoritePages] = React.useState<PageItem[]>([]);
  const [allPages, setAllPages] = React.useState<PageItem[]>([]);
  const [isLoadingPages, setIsLoadingPages] = React.useState(false);
  const [showPages, setShowPages] = React.useState(false);
  const [pagesLoaded, setPagesLoaded] = React.useState(false);

  React.useEffect(() => {
    if (currentWorkspace) {
      setPagesLoaded(false);
      setShowPages(false);
      setAllPages([]);
      setFavoritePages([]);
    }
  }, [currentWorkspace?.id]);

  const fetchPages = React.useCallback(async () => {
    if (!currentWorkspace || pagesLoaded) return;
    setIsLoadingPages(true);
    try {
      const response = await pageApi.getTree(currentWorkspace.id);
      if (response.data.success) {
        const pages = response.data.data || [];
        setAllPages(pages);
        setFavoritePages(pages.filter((p: any) => p.isFavorite));
        setPagesLoaded(true);
      }
    } catch (error) {
      console.error("Failed to fetch pages:", error);
    } finally {
      setIsLoadingPages(false);
    }
  }, [currentWorkspace, pagesLoaded]);

  const handleTogglePages = () => {
    if (!showPages && !pagesLoaded) {
      fetchPages();
    }
    setShowPages(!showPages);
  };

  const handleDeletePage = async (pageId: string) => {
    if (!currentWorkspace) return;
    try {
      await usePageStore.getState().deletePage(currentWorkspace.id, pageId);
      toast.success("Page deleted");
      fetchPages();
    } catch (error) {
      toast.error("Failed to delete page");
    }
  };

  const handleRenamePage = async (pageId: string, newTitle: string) => {
    if (!currentWorkspace) return;
    try {
      await updatePage(currentWorkspace.id, pageId, { title: newTitle });
      toast.success("Page renamed");
      fetchPages();
    } catch (error) {
      toast.error("Failed to rename page");
    }
  };

  const handleToggleFavorite = async (pageId: string, isFavorite: boolean) => {
    if (!currentWorkspace) return;
    try {
      await togglePageFavorite(currentWorkspace.id, pageId);
      fetchPages();
    } catch (error) {
      toast.error("Failed to update favorite");
    }
  };

  const handleCreatePage = async () => {
    if (!currentWorkspace) return;
    try {
      const response = await pageApi.create(currentWorkspace.id, { title: "Untitled" });
      if (response.data.success && response.data.data) {
        window.location.href = `/workspace/${currentWorkspace.id}/pages/${response.data.data.id}`;
      }
    } catch (error) {
      toast.error("Failed to create page");
    }
  };

  const mainNavItems = [
    { href: "/dashboard", icon: LayoutDashboard, label: "Dashboard", show: true },
    { href: "/projects", icon: FolderKanban, label: "Projects", show: canViewProjects() },
    { href: "/members", icon: Users, label: "Members", show: canViewMembers() },
    { href: "/settings", icon: Settings, label: "Settings", show: canManageWorkspace() },
  ].filter(item => item.show);

  return (
    <aside
      className={cn(
        "fixed left-0 top-0 z-40 h-screen bg-card border-r border-border transition-all duration-300 flex flex-col",
        collapsed ? "w-16" : "w-64"
      )}
    >
      <div className="flex items-center h-16 px-4 border-b border-border">
        {!collapsed && (
          <Link href="/dashboard" className="flex items-center gap-2">
            <div className="w-8 h-8 rounded-lg bg-primary flex items-center justify-center">
              <span className="text-primary-foreground font-bold text-sm">TF</span>
            </div>
            <span className="font-semibold text-lg">TaskFlow</span>
          </Link>
        )}
        {collapsed && (
          <Link href="/dashboard" className="mx-auto">
            <div className="w-8 h-8 rounded-lg bg-primary flex items-center justify-center">
              <span className="text-primary-foreground font-bold text-sm">TF</span>
            </div>
          </Link>
        )}
      </div>

      {!collapsed && currentWorkspace && (
        <div className="px-3 py-3 border-b border-border">
          <DropdownMenu>
            <DropdownMenuTrigger asChild>
              <button className="w-full flex items-center gap-2 p-2 rounded-lg hover:bg-accent transition-colors text-left">
                <div
                  className="w-8 h-8 rounded-lg flex items-center justify-center text-white text-sm font-bold"
                  style={{ backgroundColor: "#6366f1" }}
                >
                  {currentWorkspace.name.charAt(0).toUpperCase()}
                </div>
                <div className="flex-1 min-w-0">
                  <p className="text-sm font-medium truncate">{currentWorkspace.name}</p>
                  <p className="text-xs text-muted-foreground truncate">
                    {currentWorkspace.role}
                  </p>
                </div>
              </button>
            </DropdownMenuTrigger>
            <DropdownMenuContent className="w-56">
              {workspaces.map((ws) => (
                <DropdownMenuItem
                  key={ws.id}
                  onClick={() => setCurrentWorkspace(ws)}
                  className={ws.id === currentWorkspace.id ? "bg-accent" : ""}
                >
                  <div
                    className="w-6 h-6 rounded flex items-center justify-center text-white text-xs font-bold mr-2"
                    style={{ backgroundColor: "#6366f1" }}
                  >
                    {ws.name.charAt(0).toUpperCase()}
                  </div>
                  <span className="truncate">{ws.name}</span>
                </DropdownMenuItem>
              ))}
              <DropdownMenuSeparator />
              <DropdownMenuItem>
                <Plus className="w-4 h-4 mr-2" />
                Create Workspace
              </DropdownMenuItem>
            </DropdownMenuContent>
          </DropdownMenu>
        </div>
      )}

      <nav className="flex-1 p-3 space-y-1 overflow-y-auto scrollbar-thin">
        {mainNavItems.map((item) => {
          const isActive = pathname.startsWith(item.href);
          return (
            <Link
              key={item.href}
              href={item.href}
              className={cn(
                "flex items-center gap-3 px-3 py-2.5 rounded-lg text-sm font-medium transition-all duration-200",
                isActive
                  ? "bg-primary/10 text-primary"
                  : "text-muted-foreground hover:bg-accent hover:text-foreground"
              )}
            >
              <item.icon className="w-5 h-5 shrink-0" />
              {!collapsed && <span>{item.label}</span>}
            </Link>
          );
        })}

        {canViewPages() && !collapsed && currentWorkspace && (
          <div className="pt-4">
            <div className="flex items-center justify-between px-3 mb-2">
              <button
                onClick={handleTogglePages}
                className="flex items-center gap-1 text-xs font-semibold text-muted-foreground uppercase tracking-wider hover:text-foreground"
              >
                {showPages ? <ChevronDown className="w-3 h-3" /> : <ChevronRightIcon className="w-3 h-3" />}
                Pages
              </button>
              {canCreatePages() && (
                <Button variant="ghost" size="icon" className="h-6 w-6" onClick={handleCreatePage}>
                  <Plus className="w-4 h-4" />
                </Button>
              )}
            </div>

            {showPages && (
              <>
                {isLoadingPages ? (
                  <div className="px-3 py-2 text-xs text-muted-foreground">Loading...</div>
                ) : (
                  <>
                    {favoritePages.length > 0 && (
                      <div className="mb-2">
                        <p className="px-3 text-xs text-muted-foreground mb-1">Favorites</p>
                        {favoritePages.slice(0, 5).map((page) => (
                          <PageTreeItem
                            key={page.id}
                            page={page}
                            depth={0}
                            currentWorkspaceId={currentWorkspace.id}
                            onDelete={handleDeletePage}
                            onRename={handleRenamePage}
                            onToggleFavorite={handleToggleFavorite}
                          />
                        ))}
                      </div>
                    )}

                    <div>
                      {allPages.filter(p => !p.isFavorite).map((page) => (
                        <PageTreeItem
                          key={page.id}
                          page={page}
                          depth={0}
                          currentWorkspaceId={currentWorkspace.id}
                          onDelete={handleDeletePage}
                          onRename={handleRenamePage}
                          onToggleFavorite={handleToggleFavorite}
                        />
                      ))}
                      {allPages.length === 0 && (
                        <div className="px-3 py-4 text-xs text-muted-foreground text-center">
                          No pages yet
                        </div>
                      )}
                    </div>
                  </>
                )}
              </>
            )}
          </div>
        )}
      </nav>

      <div className="p-3 border-t border-border">
        <Button
          variant="ghost"
          className={cn("w-full justify-start gap-3", !collapsed && "px-3")}
          onClick={onToggle}
        >
          {collapsed ? (
            <ChevronRight className="w-5 h-5" />
          ) : (
            <>
              <ChevronLeft className="w-5 h-5" />
              <span>Collapse</span>
            </>
          )}
        </Button>
      </div>

      <div className="p-3 border-t border-border">
        <DropdownMenu>
          <DropdownMenuTrigger asChild>
            <button className="w-full flex items-center gap-3 p-2 rounded-lg hover:bg-accent transition-colors">
              <Avatar src={user?.avatarUrl} fallback={user?.fullName} size="sm" />
              {!collapsed && (
                <div className="flex-1 text-left min-w-0">
                  <p className="text-sm font-medium truncate">{user?.fullName}</p>
                  <p className="text-xs text-muted-foreground truncate">{user?.email}</p>
                </div>
              )}
            </button>
          </DropdownMenuTrigger>
          <DropdownMenuContent align="end" className="w-56">
            <DropdownMenuItem asChild>
              <Link href="/settings">
                <Settings className="w-4 h-4 mr-2" />
                Settings
              </Link>
            </DropdownMenuItem>
            <DropdownMenuSeparator />
            <DropdownMenuItem onClick={logout} className="text-destructive focus:text-destructive">
              <LogOut className="w-4 h-4 mr-2" />
              Logout
            </DropdownMenuItem>
          </DropdownMenuContent>
        </DropdownMenu>
      </div>
    </aside>
  );
}
