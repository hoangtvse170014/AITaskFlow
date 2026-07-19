"use client";

import * as React from "react";
import { Search, X } from "lucide-react";
import { Input } from "@/components/ui/input";

interface TaskSearchProps {
  searchQuery: string;
  onSearchChange: (query: string) => void;
}

export function TaskSearch({ searchQuery, onSearchChange }: TaskSearchProps) {
  return (
    <div className="relative">
      <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-muted-foreground" />
      <Input
        placeholder="Search tasks..."
        value={searchQuery}
        onChange={(e) => onSearchChange(e.target.value)}
        className="pl-9 w-64"
      />
      {searchQuery && (
        <button
          onClick={() => onSearchChange("")}
          className="absolute right-3 top-1/2 -translate-y-1/2 text-muted-foreground hover:text-foreground"
        >
          <X className="w-4 h-4" />
        </button>
      )}
    </div>
  );
}
