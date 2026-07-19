"use client";

import * as React from "react";
import { cn } from "@/lib/utils";

interface ScrollAreaProps extends React.HTMLAttributes<HTMLDivElement> {
  children: React.ReactNode;
  className?: string;
}

const ScrollArea = React.forwardRef<HTMLDivElement, ScrollAreaProps>(
  ({ children, className, ...props }, ref) => {
    return (
      <div
        ref={ref}
        className={cn("overflow-y-auto", className)}
        style={{ 
          overflowY: 'auto',
          scrollbarWidth: 'thin',
          scrollbarColor: 'hsl(var(--muted)) transparent'
        }}
        {...props}
      >
        {children}
      </div>
    );
  }
);
ScrollArea.displayName = "ScrollArea";

export { ScrollArea };
