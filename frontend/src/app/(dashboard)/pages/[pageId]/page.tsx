"use client";

import * as React from "react";
import { useParams, useRouter } from "next/navigation";
import { MainLayout } from "@/components/layout/MainLayout";
import { Header } from "@/components/layout/Header";
import { BlockEditor } from "@/components/editor/BlockEditor";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import {
  MoreHorizontal,
  Star,
  StarOff,
  Copy,
  Archive,
  Trash2,
  ArrowLeft,
  Heart,
  Clock,
} from "lucide-react";
import { useWorkspaceStore } from "@/store/workspaceStore";
import { usePageStore } from "@/store/pageStore";
import toast from "react-hot-toast";
import { formatDistanceToNow } from "date-fns";

export default function PageViewPage() {
  const params = useParams();
  const router = useRouter();
  const workspaceId = params.workspaceId as string;
  const pageId = params.pageId as string;
  
  const { currentWorkspace } = useWorkspaceStore();
  const {
    currentPage,
    isLoading,
    fetchPageById,
    updatePage,
    deletePage,
    archivePage,
    duplicatePage,
    toggleFavorite,
  } = usePageStore();

  const [title, setTitle] = React.useState("");
  const [isEditingTitle, setIsEditingTitle] = React.useState(false);

  React.useEffect(() => {
    if (workspaceId && pageId) {
      fetchPageById(workspaceId, pageId);
    }
  }, [workspaceId, pageId]);

  React.useEffect(() => {
    if (currentPage?.page) {
      setTitle(currentPage.page.title);
    }
  }, [currentPage]);

  const handleTitleChange = async () => {
    if (title !== currentPage?.page.title && title.trim()) {
      await updatePage(workspaceId, pageId, { title: title.trim() });
    }
    setIsEditingTitle(false);
  };

  const handleToggleFavorite = async () => {
    await toggleFavorite(workspaceId, pageId);
  };

  const handleDuplicate = async () => {
    await duplicatePage(workspaceId, pageId);
    toast.success("Page duplicated");
  };

  const handleArchive = async () => {
    await archivePage(workspaceId, pageId);
    router.push(`/workspace/${workspaceId}/pages`);
  };

  const handleDelete = async () => {
    if (confirm("Are you sure you want to delete this page? This action cannot be undone.")) {
      await deletePage(workspaceId, pageId);
      router.push(`/workspace/${workspaceId}/pages`);
    }
  };

  const handleBlocksChange = (blocks: any[]) => {
    // Update local state optimistically
    usePageStore.setState((state) => ({
      currentPage: state.currentPage ? { ...state.currentPage, blocks } : null,
    }));
  };

  if (isLoading || !currentPage) {
    return (
      <MainLayout>
        <div className="flex items-center justify-center h-screen">
          <div className="animate-pulse text-muted-foreground">Loading page...</div>
        </div>
      </MainLayout>
    );
  }

  return (
    <MainLayout>
      {/* Cover Image */}
      {currentPage.page.coverUrl && (
        <div
          className="h-48 w-full bg-cover bg-center"
          style={{ backgroundImage: `url(${currentPage.page.coverUrl})` }}
        />
      )}

      <div className="max-w-4xl mx-auto px-4 py-8">
        {/* Back button */}
        <Button
          variant="ghost"
          size="sm"
          onClick={() => router.push(`/workspace/${workspaceId}/pages`)}
          className="mb-4"
        >
          <ArrowLeft className="w-4 h-4 mr-2" />
          Back to pages
        </Button>

        {/* Page Header */}
        <div className="flex items-start gap-4 mb-8">
          {/* Icon */}
          {currentPage.page.icon && (
            <div className="text-5xl">{currentPage.page.icon}</div>
          )}

          {/* Title */}
          <div className="flex-1">
            {isEditingTitle ? (
              <Input
                value={title}
                onChange={(e) => setTitle(e.target.value)}
                onBlur={handleTitleChange}
                onKeyDown={(e) => {
                  if (e.key === "Enter") handleTitleChange();
                  if (e.key === "Escape") {
                    setTitle(currentPage.page.title);
                    setIsEditingTitle(false);
                  }
                }}
                className="text-4xl font-bold border-none bg-transparent px-0 focus-visible:ring-0"
                autoFocus
              />
            ) : (
              <h1
                onClick={() => setIsEditingTitle(true)}
                className="text-4xl font-bold cursor-pointer hover:bg-accent/50 rounded px-2 -mx-2"
              >
                {currentPage.page.title || "Untitled"}
              </h1>
            )}

            {/* Meta info */}
            <div className="flex items-center gap-4 mt-2 text-sm text-muted-foreground">
              <span className="flex items-center gap-1">
                <Clock className="w-3 h-3" />
                {formatDistanceToNow(new Date(currentPage.page.updatedAt), { addSuffix: true })}
              </span>
              {currentPage.lastEditedBy && (
                <span>by {currentPage.lastEditedBy}</span>
              )}
            </div>
          </div>

          {/* Actions */}
          <div className="flex items-center gap-2">
            <Button
              variant="ghost"
              size="icon"
              onClick={handleToggleFavorite}
              title={currentPage.page.isFavorite ? "Remove from favorites" : "Add to favorites"}
            >
              {currentPage.page.isFavorite ? (
                <Star className="w-4 h-4 fill-yellow-500 text-yellow-500" />
              ) : (
                <StarOff className="w-4 h-4" />
              )}
            </Button>

            <DropdownMenu>
              <DropdownMenuTrigger asChild>
                <Button variant="ghost" size="icon">
                  <MoreHorizontal className="w-4 h-4" />
                </Button>
              </DropdownMenuTrigger>
              <DropdownMenuContent align="end">
                <DropdownMenuItem onClick={handleDuplicate}>
                  <Copy className="w-4 h-4 mr-2" />
                  Duplicate
                </DropdownMenuItem>
                <DropdownMenuItem onClick={handleArchive}>
                  <Archive className="w-4 h-4 mr-2" />
                  Archive
                </DropdownMenuItem>
                <DropdownMenuSeparator />
                <DropdownMenuItem onClick={handleDelete} className="text-destructive focus:text-destructive">
                  <Trash2 className="w-4 h-4 mr-2" />
                  Delete
                </DropdownMenuItem>
              </DropdownMenuContent>
            </DropdownMenu>
          </div>
        </div>

        {/* Block Editor */}
        <BlockEditor
          pageId={pageId}
          blocks={currentPage.blocks || []}
          onBlocksChange={handleBlocksChange}
        />
      </div>
    </MainLayout>
  );
}
