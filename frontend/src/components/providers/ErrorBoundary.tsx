"use client";

import * as React from "react";

interface ErrorBoundaryProps {
  children: React.ReactNode;
}

interface ErrorBoundaryState {
  hasError: boolean;
  error: Error | null;
}

export class ErrorBoundary extends React.Component<ErrorBoundaryProps, ErrorBoundaryState> {
  constructor(props: ErrorBoundaryProps) {
    super(props);
    this.state = { hasError: false, error: null };
  }

  static getDerivedStateFromError(error: Error): ErrorBoundaryState {
    return { hasError: true, error };
  }

  componentDidCatch(error: Error, info: React.ErrorInfo): void {
    if (process.env.NODE_ENV === "production") {
      // Replace with real telemetry hook (Sentry, Datadog) when available.
      console.error("Unhandled error:", error, info.componentStack);
    }
  }

  private handleReset = () => {
    this.setState({ hasError: false, error: null });
  };

  private handleReload = () => {
    if (typeof window !== "undefined") {
      window.location.reload();
    }
  };

  render() {
    if (this.state.hasError) {
      return (
        <div className="min-h-screen flex items-center justify-center bg-background p-6">
          <div className="max-w-md text-center space-y-4">
            <h1 className="text-2xl font-semibold">Something went wrong</h1>
            <p className="text-sm text-muted-foreground">
              {this.state.error?.message || "An unexpected error occurred."}
            </p>
            <div className="flex items-center justify-center gap-2">
              <button
                onClick={this.handleReset}
                className="px-4 py-2 rounded-md border bg-card hover:bg-accent text-sm"
              >
                Try again
              </button>
              <button
                onClick={this.handleReload}
                className="px-4 py-2 rounded-md bg-primary text-primary-foreground text-sm"
              >
                Reload page
              </button>
            </div>
          </div>
        </div>
      );
    }

    return this.props.children;
  }
}
