"use client";

import * as React from "react";
import { Dialog, DialogContent } from "@/components/ui/dialog";
import { Input } from "@/components/ui/input";
import { Button } from "@/components/ui/button";
import { Avatar } from "@/components/ui/avatar";
import { Badge } from "@/components/ui/badge";
import {
  Search,
  Loader2,
  FileText,
  CheckSquare,
  FolderKanban,
  Users,
  Clock,
  X,
  ArrowRight,
} from "lucide-react";
import { api } from "@/lib/api";
import { useWorkspaceStore } from "@/store/workspaceStore";
import { useRouter } from "next/navigation";

interface SearchResult {
  tasks: TaskResult[];
  pages: PageResult[];
  projects: ProjectResult[];
  members: MemberResult[];
  query: string;
  totalCount: number;
}

interface TaskResult {
  id: string;
  title: string;
  status: string;
  priority: string;
  projectName: string;
  projectKey: string;
  taskKey: string;
}

interface PageResult {
  id: string;
  title: string;
  icon: string;
  slug: string;
}

interface ProjectResult {
  id: string;
  name: string;
  key: string;
  color: string;
}

interface MemberResult {
  id: string;
  fullName: string;
  email: string;
  avatarUrl: string;
}

interface RecentSearch {
  recentSearches: string[];
}

export function SearchDialog({ open, onOpenChange }: { open: boolean; onOpenChange: (open: boolean) => void }) {
  const router = useRouter();
  const { currentWorkspace } = useWorkspaceStore();
  const [query, setQuery] = React.useState("");
  const [results, setResults] = React.useState<SearchResult | null>(null);
  const [recentSearches, setRecentSearches] = React.useState<string[]>([]);
  const [isLoading, setIsLoading] = React.useState(false);
  const [selectedIndex, setSelectedIndex] = React.useState(0);
  const inputRef = React.useRef<HTMLInputElement>(null);

  React.useEffect(() => {
    if (open) {
      setQuery("");
      setResults(null);
      fetchRecentSearches();
      setTimeout(() => inputRef.current?.focus(), 100);
    }
  }, [open]);

  React.useEffect(() => {
    if (query.length >= 2) {
      const debounce = setTimeout(() => performSearch(), 300);
      return () => clearTimeout(debounce);
    } else {
      setResults(null);
    }
  }, [query]);

  const fetchRecentSearches = async () => {
    try {
      const response = await api.get<{ success: boolean; data: RecentSearch }>("/search/recent?limit=10");
      if (response.data.success) {
        setRecentSearches(response.data.data.recentSearches || []);
      }
    } catch (error) {
      console.error("Failed to fetch recent searches:", error);
    }
  };

  const performSearch = async () => {
    if (!currentWorkspace || !query.trim()) return;
    setIsLoading(true);
    try {
      const response = await api.get<{ success: boolean; data: SearchResult }>(
        `/search?workspaceId=${currentWorkspace.id}&q=${encodeURIComponent(query)}`
      );
      if (response.data.success) {
        setResults(response.data.data);
        setSelectedIndex(0);
      }
    } catch (error) {
      console.error("Search failed:", error);
    } finally {
      setIsLoading(false);
    }
  };

  const getAllResults = () => {
    if (!results) return [];
    const all: Array<{ type: string; id: string; title: string; subtitle?: string; icon: React.ReactNode }> = [];
    
    results.tasks?.forEach((task) => {
      all.push({
        type: "task",
        id: task.id,
        title: task.title,
        subtitle: `${task.taskKey} - ${task.projectName}`,
        icon: <CheckSquare className="w-4 h-4 text-green-500" />,
      });
    });
    
    results.pages?.forEach((page) => {
      all.push({
        type: "page",
        id: page.id,
        title: page.title,
        subtitle: page.icon ? `${page.icon} Page` : "Page",
        icon: <FileText className="w-4 h-4 text-blue-500" />,
      });
    });
    
    results.projects?.forEach((project) => {
      all.push({
        type: "project",
        id: project.id,
        title: project.name,
        subtitle: project.key,
        icon: <FolderKanban className="w-4 h-4 text-purple-500" />,
      });
    });
    
    results.members?.forEach((member) => {
      all.push({
        type: "member",
        id: member.id,
        title: member.fullName,
        subtitle: member.email,
        icon: <Users className="w-4 h-4 text-orange-500" />,
      });
    });
    
    return all;
  };

  const handleKeyDown = (e: React.KeyboardEvent) => {
    const items = getAllResults();
    if (e.key === "ArrowDown") {
      e.preventDefault();
      setSelectedIndex((prev) => Math.min(prev + 1, items.length - 1));
    } else if (e.key === "ArrowUp") {
      e.preventDefault();
      setSelectedIndex((prev) => Math.max(prev - 1, 0));
    } else if (e.key === "Enter" && items.length > 0) {
      e.preventDefault();
      navigateToItem(items[selectedIndex]);
    }
  };

  const navigateToItem = (item: typeof getAllResults extends () => infer R ? R extends (infer T)[] ? T : never : never) => {
    onOpenChange(false);
    switch (item.type) {
      case "task":
        router.push(`/projects/${item.id}`);
        break;
      case "page":
        router.push(`/workspace/${currentWorkspace?.id}/pages/${item.id}`);
        break;
      case "project":
        router.push(`/projects/${item.id}`);
        break;
      case "member":
        router.push(`/members`);
        break;
    }
  };

  const clearRecentSearches = async () => {
    try {
      await api.delete("/search/history");
      setRecentSearches([]);
    } catch (error) {
      console.error("Failed to clear search history:", error);
    }
  };

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="sm:max-w-2xl p-0 gap-0 overflow-hidden">
        <div className="flex items-center border-b px-4">
          <Search className="w-5 h-5 text-muted-foreground shrink-0" />
          <Input
            ref={inputRef}
            placeholder="Search tasks, pages, projects, members..."
            value={query}
            onChange={(e) => setQuery(e.target.value)}
            onKeyDown={handleKeyDown}
            className="border-0 focus-visible:ring-0 text-base h-14"
          />
          {isLoading && <Loader2 className="w-5 h-5 animate-spin text-muted-foreground shrink-0" />}
          {query && !isLoading && (
            <Button variant="ghost" size="icon" onClick={() => setQuery("")} className="shrink-0">
              <X className="w-4 h-4" />
            </Button>
          )}
        </div>

        <div className="max-h-[400px] overflow-y-auto">
          {query.length < 2 && !results && (
            <div className="p-4">
              {recentSearches.length > 0 && (
                <div className="space-y-2">
                  <div className="flex items-center justify-between">
                    <h4 className="text-sm font-medium text-muted-foreground">Recent Searches</h4>
                    <Button variant="ghost" size="sm" onClick={clearRecentSearches} className="h-auto p-1 text-xs">
                      Clear
                    </Button>
                  </div>
                  {recentSearches.map((search, index) => (
                    <button
                      key={index}
                      onClick={() => setQuery(search)}
                      className="flex items-center gap-3 w-full p-2 rounded-lg hover:bg-accent transition-colors"
                    >
                      <Clock className="w-4 h-4 text-muted-foreground" />
                      <span className="text-sm">{search}</span>
                      <ArrowRight className="w-4 h-4 text-muted-foreground ml-auto" />
                    </button>
                  ))}
                </div>
              )}
              
              <div className="mt-6 space-y-2">
                <h4 className="text-sm font-medium text-muted-foreground">Quick Tips</h4>
                <div className="text-sm text-muted-foreground space-y-1">
                  <p>Press <kbd className="px-1.5 py-0.5 bg-muted rounded text-xs">Ctrl+K</kbd> to open search</p>
                  <p>Use arrow keys to navigate results</p>
                  <p>Press <kbd className="px-1.5 py-0.5 bg-muted rounded text-xs">Enter</kbd> to select</p>
                </div>
              </div>
            </div>
          )}

          {results && results.totalCount === 0 && (
            <div className="p-8 text-center text-muted-foreground">
              <Search className="w-12 h-12 mx-auto mb-3 opacity-50" />
              <p>No results found for "{query}"</p>
              <p className="text-sm mt-1">Try a different search term</p>
            </div>
          )}

          {results && results.totalCount > 0 && (
            <div className="p-2">
              {results.tasks && results.tasks.length > 0 && (
                <div className="mb-4">
                  <h4 className="text-xs font-medium text-muted-foreground uppercase px-2 mb-1">
                    Tasks ({results.tasks.length})
                  </h4>
                  {results.tasks.map((task, index) => (
                    <button
                      key={task.id}
                      onClick={() => navigateToItem({ type: "task", id: task.id, title: task.title, subtitle: `${task.taskKey} - ${task.projectName}`, icon: <CheckSquare className="w-4 h-4 text-green-500" /> })}
                      className={`flex items-center gap-3 w-full p-2 rounded-lg transition-colors ${
                        selectedIndex === index ? "bg-accent" : "hover:bg-accent"
                      }`}
                    >
                      <CheckSquare className="w-4 h-4 text-green-500 shrink-0" />
                      <div className="flex-1 min-w-0 text-left">
                        <p className="text-sm font-medium truncate">{task.title}</p>
                        <p className="text-xs text-muted-foreground truncate">{task.taskKey} - {task.projectName}</p>
                      </div>
                      <Badge variant="outline" className="text-xs shrink-0">
                        {task.status}
                      </Badge>
                    </button>
                  ))}
                </div>
              )}

              {results.pages && results.pages.length > 0 && (
                <div className="mb-4">
                  <h4 className="text-xs font-medium text-muted-foreground uppercase px-2 mb-1">
                    Pages ({results.pages.length})
                  </h4>
                  {results.pages.map((page, index) => (
                    <button
                      key={page.id}
                      onClick={() => navigateToItem({ type: "page", id: page.id, title: page.title, subtitle: page.icon, icon: <FileText className="w-4 h-4 text-blue-500" /> })}
                      className={`flex items-center gap-3 w-full p-2 rounded-lg transition-colors ${
                        selectedIndex === index + (results.tasks?.length || 0) ? "bg-accent" : "hover:bg-accent"
                      }`}
                    >
                      <FileText className="w-4 h-4 text-blue-500 shrink-0" />
                      <div className="flex-1 min-w-0 text-left">
                        <p className="text-sm font-medium truncate">{page.icon && <span className="mr-1">{page.icon}</span>}{page.title}</p>
                      </div>
                    </button>
                  ))}
                </div>
              )}

              {results.projects && results.projects.length > 0 && (
                <div className="mb-4">
                  <h4 className="text-xs font-medium text-muted-foreground uppercase px-2 mb-1">
                    Projects ({results.projects.length})
                  </h4>
                  {results.projects.map((project, index) => (
                    <button
                      key={project.id}
                      onClick={() => navigateToItem({ type: "project", id: project.id, title: project.name, subtitle: project.key, icon: <FolderKanban className="w-4 h-4 text-purple-500" /> })}
                      className={`flex items-center gap-3 w-full p-2 rounded-lg transition-colors ${
                        selectedIndex === index + (results.tasks?.length || 0) + (results.pages?.length || 0) ? "bg-accent" : "hover:bg-accent"
                      }`}
                    >
                      <FolderKanban className="w-4 h-4 shrink-0" style={{ color: project.color }} />
                      <div className="flex-1 min-w-0 text-left">
                        <p className="text-sm font-medium truncate">{project.name}</p>
                        <p className="text-xs text-muted-foreground">{project.key}</p>
                      </div>
                    </button>
                  ))}
                </div>
              )}

              {results.members && results.members.length > 0 && (
                <div>
                  <h4 className="text-xs font-medium text-muted-foreground uppercase px-2 mb-1">
                    Members ({results.members.length})
                  </h4>
                  {results.members.map((member, index) => (
                    <button
                      key={member.id}
                      onClick={() => navigateToItem({ type: "member", id: member.id, title: member.fullName, subtitle: member.email, icon: <Users className="w-4 h-4 text-orange-500" /> })}
                      className={`flex items-center gap-3 w-full p-2 rounded-lg transition-colors ${
                        selectedIndex === index + (results.tasks?.length || 0) + (results.pages?.length || 0) + (results.projects?.length || 0) ? "bg-accent" : "hover:bg-accent"
                      }`}
                    >
                      <Avatar src={member.avatarUrl} fallback={member.fullName} size="sm" />
                      <div className="flex-1 min-w-0 text-left">
                        <p className="text-sm font-medium truncate">{member.fullName}</p>
                        <p className="text-xs text-muted-foreground truncate">{member.email}</p>
                      </div>
                    </button>
                  ))}
                </div>
              )}
            </div>
          )}
        </div>

        <div className="border-t px-4 py-2 flex items-center justify-between text-xs text-muted-foreground">
          <div className="flex items-center gap-4">
            <span><kbd className="px-1 py-0.5 bg-muted rounded">↑↓</kbd> Navigate</span>
            <span><kbd className="px-1 py-0.5 bg-muted rounded">↵</kbd> Select</span>
            <span><kbd className="px-1 py-0.5 bg-muted rounded">Esc</kbd> Close</span>
          </div>
          {results && (
            <span>{results.totalCount} result{results.totalCount !== 1 ? "s" : ""}</span>
          )}
        </div>
      </DialogContent>
    </Dialog>
  );
}
