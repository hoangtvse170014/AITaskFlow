"use client";

import * as React from "react";
import { Sidebar } from "./Sidebar";

interface MainLayoutProps {
  children: React.ReactNode;
}

export function MainLayout({ children }: MainLayoutProps) {
  const [sidebarCollapsed, setSidebarCollapsed] = React.useState(false);

  return (
    <div className="min-h-screen bg-background">
      <Sidebar
        collapsed={sidebarCollapsed}
        onToggle={() => setSidebarCollapsed(!sidebarCollapsed)}
      />
      <main
        className={`transition-all duration-300 ${
          sidebarCollapsed ? "ml-16" : "ml-64"
        }`}
      >
        {children}
      </main>
    </div>
  );
}
