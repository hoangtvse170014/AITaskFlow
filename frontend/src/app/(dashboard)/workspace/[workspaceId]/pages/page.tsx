"use client";

import * as React from "react";
import { useRouter } from "next/navigation";
import { MainLayout } from "@/components/layout/MainLayout";
import { Header } from "@/components/layout/Header";
import { Card, CardContent } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogFooter,
} from "@/components/ui/dialog";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import {
  Plus,
  Search,
  Star,
  StarOff,
  MoreHorizontal,
  Copy,
  Archive,
  Trash2,
  ChevronRight,
  ChevronDown,
  FileText,
  Archive as ArchiveIcon,
  Loader2,
} from "lucide-react";
import { useWorkspaceStore } from "@/store/workspaceStore";
import { usePageStore } from "@/store/pageStore";
import { cn } from "@/lib/utils";
import { formatDistanceToNow } from "date-fns";

interface PageItemProps {
  page: any;
  workspaceId: string;
  level?: number;
  onToggleFavorite: (pageId: string) => void;
  onDelete: (pageId: string) => void;
  onArchive: (pageId: string) => void;
  onDuplicate: (pageId: string) => void;
}

function PageItem({ page, workspaceId, level = 0, onToggleFavorite, onDelete, onArchive, onDuplicate }: PageItemProps) {
  const router = useRouter();
  const [isExpanded, setIsExpanded] = React.useState(true);
  const hasChildren = page.children && page.children.length > 0;

  return (
    <div>
      <div
        className={cn(
          "group flex items-center gap-2 px-3 py-2 rounded-lg hover:bg-accent cursor-pointer transition-colors"
        )}
        style={{ paddingLeft: `${level * 1.5 + 12}px` }}
      >
        {hasChildren ? (
          <button
            onClick={() => setIsExpanded(!isExpanded)}
            className="p-0.5 hover:bg-accent rounded"
          >
            {isExpanded ? (
              <ChevronDown className="w-4 h-4 text-muted-foreground" />
            ) : (
              <ChevronRight className="w-4 h-4 text-muted-foreground" />
            )}
          </button>
        ) : (
          <div className="w-5" />
        )}

        <div className="text-lg">{page.icon || "📄"}</div>

        <div
          className="flex-1 min-w-0"
          onClick={() => router.push(`/workspace/${workspaceId}/pages/${page.id}`)}
        >
          <p className="font-medium truncate">{page.title || "Untitled"}</p>
          <p className="text-xs text-muted-foreground">
            {formatDistanceToNow(new Date(page.updatedAt), { addSuffix: true })}
          </p>
        </div>

        <Button
          variant="ghost"
          size="icon"
          className="opacity-0 group-hover:opacity-100 h-8 w-8"
          onClick={(e) => {
            e.stopPropagation();
            onToggleFavorite(page.id);
          }}
        >
          {page.isFavorite ? (
            <Star className="w-4 h-4 fill-yellow-500 text-yellow-500" />
          ) : (
            <StarOff className="w-4 h-4 text-muted-foreground" />
          )}
        </Button>

        <DropdownMenu>
          <DropdownMenuTrigger asChild>
            <Button
              variant="ghost"
              size="icon"
              className="opacity-0 group-hover:opacity-100 h-8 w-8"
              onClick={(e) => e.stopPropagation()}
            >
              <MoreHorizontal className="w-4 h-4" />
            </Button>
          </DropdownMenuTrigger>
          <DropdownMenuContent align="end">
            <DropdownMenuItem onClick={() => router.push(`/workspace/${workspaceId}/pages/${page.id}`)}>
              <FileText className="w-4 h-4 mr-2" />
              Open
            </DropdownMenuItem>
            <DropdownMenuItem onClick={() => onDuplicate(page.id)}>
              <Copy className="w-4 h-4 mr-2" />
              Duplicate
            </DropdownMenuItem>
            <DropdownMenuSeparator />
            <DropdownMenuItem onClick={() => onArchive(page.id)}>
              <ArchiveIcon className="w-4 h-4 mr-2" />
              Archive
            </DropdownMenuItem>
            <DropdownMenuItem
              onClick={() => onDelete(page.id)}
              className="text-destructive focus:text-destructive"
            >
              <Trash2 className="w-4 h-4 mr-2" />
              Delete
            </DropdownMenuItem>
          </DropdownMenuContent>
        </DropdownMenu>
      </div>

      {hasChildren && isExpanded && (
        <div>
          {page.children.map((child: any) => (
            <PageItem
              key={child.id}
              page={child}
              workspaceId={workspaceId}
              level={level + 1}
              onToggleFavorite={onToggleFavorite}
              onDelete={onDelete}
              onArchive={onArchive}
              onDuplicate={onDuplicate}
            />
          ))}
        </div>
      )}
    </div>
  );
}

export default function PagesPage() {
  const router = useRouter();
  const { currentWorkspace } = useWorkspaceStore();
  const {
    pageTree,
    pages,
    isLoading,
    fetchPageTree,
    createPage,
    deletePage,
    archivePage,
    duplicatePage,
    toggleFavorite,
  } = usePageStore();

  const [searchQuery, setSearchQuery] = React.useState("");
  const [isCreateOpen, setIsCreateOpen] = React.useState(false);
  const [newPageTitle, setNewPageTitle] = React.useState("");
  const [isCreating, setIsCreating] = React.useState(false);

  React.useEffect(() => {
    if (currentWorkspace) {
      fetchPageTree(currentWorkspace.id);
    }
  }, [currentWorkspace]);

  const handleCreatePage = async () => {
    if (!currentWorkspace || !newPageTitle.trim()) return;
    setIsCreating(true);
    try {
      const newPage = await createPage(currentWorkspace.id, { title: newPageTitle.trim() });
      if (newPage) {
        setIsCreateOpen(false);
        setNewPageTitle("");
        router.push(`/workspace/${currentWorkspace.id}/pages/${newPage.id}`);
      }
    } finally {
      setIsCreating(false);
    }
  };

  const handleToggleFavorite = async (pageId: string) => {
    if (currentWorkspace) {
      await toggleFavorite(currentWorkspace.id, pageId);
      fetchPageTree(currentWorkspace.id);
    }
  };

  const handleDelete = async (pageId: string) => {
    if (!currentWorkspace) return;
    if (confirm("Are you sure you want to delete this page?")) {
      await deletePage(currentWorkspace.id, pageId);
      fetchPageTree(currentWorkspace.id);
    }
  };

  const handleArchive = async (pageId: string) => {
    if (currentWorkspace) {
      await archivePage(currentWorkspace.id, pageId);
      fetchPageTree(currentWorkspace.id);
    }
  };

  const handleDuplicate = async (pageId: string) => {
    if (currentWorkspace) {
      await duplicatePage(currentWorkspace.id, pageId);
      fetchPageTree(currentWorkspace.id);
    }
  };

  const filteredPages = searchQuery
    ? pageTree.filter(
        (p) =>
          p.title.toLowerCase().includes(searchQuery.toLowerCase()) ||
          (p.children && p.children.some((c) => c.title.toLowerCase().includes(searchQuery.toLowerCase())))
      )
    : pageTree;

  return (
    <MainLayout>
      <Header
        title="Pages"
        subtitle={`${pages.length} pages in ${currentWorkspace?.name || "workspace"}`}
        actions={
          <Button onClick={() => setIsCreateOpen(true)}>
            <Plus className="w-4 h-4 mr-2" />
            New Page
          </Button>
        }
      />

      <div className="p-6">
        {/* Search */}
        <div className="relative mb-6">
          <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-muted-foreground" />
          <Input
            placeholder="Search pages..."
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            className="pl-10"
          />
        </div>

        {/* Quick Actions */}
        <div className="flex items-center gap-4 mb-6">
          <Button variant="outline" size="sm" onClick={() => router.push("/projects")}>
            <FileText className="w-4 h-4 mr-2" />
            Projects
          </Button>
          <Button variant="outline" size="sm" onClick={() => router.push("/workspace/archived")}>
            <ArchiveIcon className="w-4 h-4 mr-2" />
            Archived
          </Button>
        </div>

        {/* Pages List */}
        {isLoading ? (
          <div className="flex items-center justify-center py-12">
            <Loader2 className="w-6 h-6 animate-spin text-muted-foreground" />
          </div>
        ) : filteredPages.length === 0 ? (
          <Card>
            <CardContent className="py-12 text-center text-muted-foreground">
              <FileText className="w-12 h-12 mx-auto mb-4 opacity-50" />
              <p className="mb-4">
                {searchQuery ? "No pages match your search" : "No pages yet"}
              </p>
              {!searchQuery && (
                <Button onClick={() => setIsCreateOpen(true)}>
                  <Plus className="w-4 h-4 mr-2" />
                  Create your first page
                </Button>
              )}
            </CardContent>
          </Card>
        ) : (
          <div className="space-y-1">
            {filteredPages.map((page) => (
              <PageItem
                key={page.id}
                page={page}
                workspaceId={currentWorkspace?.id || ""}
                onToggleFavorite={handleToggleFavorite}
                onDelete={handleDelete}
                onArchive={handleArchive}
                onDuplicate={handleDuplicate}
              />
            ))}
          </div>
        )}
      </div>

      {/* Create Page Dialog */}
      <Dialog open={isCreateOpen} onOpenChange={setIsCreateOpen}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>Create a new page</DialogTitle>
          </DialogHeader>
          <div className="py-4">
            <Input
              placeholder="Page title"
              value={newPageTitle}
              onChange={(e) => setNewPageTitle(e.target.value)}
              onKeyDown={(e) => {
                if (e.key === "Enter") handleCreatePage();
              }}
              autoFocus
            />
          </div>
          <DialogFooter>
            <Button variant="outline" onClick={() => setIsCreateOpen(false)}>
              Cancel
            </Button>
            <Button onClick={handleCreatePage} disabled={!newPageTitle.trim() || isCreating}>
              {isCreating ? "Creating..." : "Create"}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </MainLayout>
  );
}
