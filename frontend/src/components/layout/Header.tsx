"use client";

import * as React from "react";
import { Bell, Plus, Search } from "lucide-react";
import { Button } from "@/components/ui/button";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import { Avatar } from "@/components/ui/avatar";
import { useAuthStore } from "@/store/authStore";
import { NotificationDropdown } from "./NotificationDropdown";
import { SearchDialog } from "@/components/search/SearchDialog";
import Link from "next/link";

interface HeaderProps {
  title?: string;
  subtitle?: string;
  onCreateProject?: () => void;
  actions?: React.ReactNode;
}

export function Header({ title, subtitle, onCreateProject, actions }: HeaderProps) {
  const { user } = useAuthStore();
  const [isSearchOpen, setIsSearchOpen] = React.useState(false);

  React.useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      if ((e.ctrlKey || e.metaKey) && e.key === "k") {
        e.preventDefault();
        setIsSearchOpen(true);
      }
    };
    document.addEventListener("keydown", handleKeyDown);
    return () => document.removeEventListener("keydown", handleKeyDown);
  }, []);

  return (
    <>
      <header className="sticky top-0 z-30 bg-background/95 backdrop-blur supports-[backdrop-filter]:bg-background/60 border-b border-border">
        <div className="flex items-center justify-between h-16 px-6">
          <div>
            {title && <h1 className="text-xl font-semibold">{title}</h1>}
            {subtitle && <p className="text-sm text-muted-foreground">{subtitle}</p>}
          </div>

          <div className="flex items-center gap-4">
            <Button
              variant="outline"
              className="hidden sm:flex items-center gap-2 text-muted-foreground"
              onClick={() => setIsSearchOpen(true)}
            >
              <Search className="w-4 h-4" />
              <span className="text-sm">Search...</span>
              <kbd className="hidden lg:inline-flex h-5 px-1.5 items-center gap-1 rounded border bg-muted text-[10px] font-medium text-muted-foreground">
                Ctrl+K
              </kbd>
            </Button>

            {actions && <div className="flex items-center gap-2">{actions}</div>}
            
            {onCreateProject && (
              <Button onClick={onCreateProject} size="sm">
                <Plus className="w-4 h-4 mr-2" />
                New Project
              </Button>
            )}

            <NotificationDropdown />

            <DropdownMenu>
              <DropdownMenuTrigger asChild>
                <button className="flex items-center gap-2">
                  <Avatar src={user?.avatarUrl} fallback={user?.fullName} size="sm" />
                </button>
              </DropdownMenuTrigger>
              <DropdownMenuContent align="end" className="w-56">
                <div className="px-2 py-1.5">
                  <p className="text-sm font-medium">{user?.fullName}</p>
                  <p className="text-xs text-muted-foreground">{user?.email}</p>
                </div>
                <DropdownMenuItem asChild>
                  <Link href="/settings">Settings</Link>
                </DropdownMenuItem>
              </DropdownMenuContent>
            </DropdownMenu>
          </div>
        </div>
      </header>

      <SearchDialog open={isSearchOpen} onOpenChange={setIsSearchOpen} />
    </>
  );
}
