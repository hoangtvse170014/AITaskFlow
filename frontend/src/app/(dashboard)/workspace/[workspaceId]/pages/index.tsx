"use client";

import { redirect } from "next/navigation";
import { useWorkspaceStore } from "@/store/workspaceStore";

export default function WorkspacePagesIndex() {
  const { currentWorkspace } = useWorkspaceStore();
  
  if (currentWorkspace) {
    redirect(`/workspace/${currentWorkspace.id}/pages`);
  }
  
  redirect("/dashboard");
}
