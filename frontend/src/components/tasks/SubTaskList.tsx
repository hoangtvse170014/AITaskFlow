"use client";

import * as React from "react";
import { Plus, Check, Trash2, Loader2, MoreVertical } from "lucide-react";
import { Input } from "@/components/ui/input";
import { Button } from "@/components/ui/button";
import { Checkbox } from "@/components/ui/checkbox";
import { taskApi } from "@/lib/api";
import { toast } from "react-hot-toast";

interface SubTask {
  id: string;
  taskId: string;
  title: string;
  completed: boolean;
  position: number;
}

interface SubTaskListProps {
  taskId: string;
}

export function SubTaskList({ taskId }: SubTaskListProps) {
  const [subTasks, setSubTasks] = React.useState<SubTask[]>([]);
  const [isLoading, setIsLoading] = React.useState(true);
  const [newSubTaskTitle, setNewSubTaskTitle] = React.useState("");
  const [isAdding, setIsAdding] = React.useState(false);

  React.useEffect(() => {
    fetchSubTasks();
  }, [taskId]);

  const fetchSubTasks = async () => {
    setIsLoading(true);
    try {
      const response = await taskApi.getSubTasks(taskId);
      if (response.data.success && response.data.data) {
        setSubTasks(response.data.data);
      }
    } catch {
      toast.error("Failed to fetch sub-tasks");
    } finally {
      setIsLoading(false);
    }
  };

  const handleAddSubTask = async () => {
    if (!newSubTaskTitle.trim()) return;
    setIsAdding(true);
    try {
      const response = await taskApi.createSubTask(taskId, { title: newSubTaskTitle });
      if (response.data.success && response.data.data) {
        setSubTasks([...subTasks, response.data.data]);
        setNewSubTaskTitle("");
      }
    } catch {
      toast.error("Failed to add sub-task");
    } finally {
      setIsAdding(false);
    }
  };

  const handleToggle = async (subTask: SubTask) => {
    try {
      const response = await taskApi.toggleSubTask(taskId, subTask.id);
      if (response.data.success && response.data.data) {
        setSubTasks(subTasks.map(st => 
          st.id === subTask.id ? response.data.data : st
        ));
      }
    } catch {
      toast.error("Failed to toggle sub-task");
    }
  };

  const handleDelete = async (subTaskId: string) => {
    try {
      await taskApi.deleteSubTask(taskId, subTaskId);
      setSubTasks(subTasks.filter(st => st.id !== subTaskId));
    } catch {
      toast.error("Failed to delete sub-task");
    }
  };

  const completedCount = subTasks.filter(st => st.completed).length;

  return (
    <div className="space-y-3">
      <div className="flex items-center justify-between">
        <h4 className="text-sm font-semibold">
          Sub-tasks ({completedCount}/{subTasks.length})
        </h4>
      </div>

      {subTasks.length > 0 && (
        <div className="space-y-2">
          {subTasks.map((subTask) => (
            <div
              key={subTask.id}
              className="flex items-center gap-2 group"
            >
              <Checkbox
                checked={subTask.completed}
                onCheckedChange={() => handleToggle(subTask)}
                className="flex-shrink-0"
              />
              <span
                className={`flex-1 text-sm ${
                  subTask.completed ? "line-through text-muted-foreground" : ""
                }`}
              >
                {subTask.title}
              </span>
              <Button
                variant="ghost"
                size="sm"
                className="h-6 w-6 p-0 opacity-0 group-hover:opacity-100 transition-opacity"
                onClick={() => handleDelete(subTask.id)}
              >
                <Trash2 className="w-3 h-3 text-muted-foreground hover:text-destructive" />
              </Button>
            </div>
          ))}
        </div>
      )}

      {subTasks.length > 0 && (
        <div className="h-2 bg-muted rounded-full overflow-hidden">
          <div
            className="h-full bg-green-500 rounded-full transition-all"
            style={{
              width: `${
                subTasks.length > 0
                  ? (completedCount / subTasks.length) * 100
                  : 0
              }%`,
            }}
          />
        </div>
      )}

      <div className="flex items-center gap-2">
        <Input
          placeholder="Add a sub-task..."
          value={newSubTaskTitle}
          onChange={(e) => setNewSubTaskTitle(e.target.value)}
          onKeyDown={(e) => e.key === "Enter" && handleAddSubTask()}
          className="h-8 text-sm"
        />
        <Button
          size="sm"
          variant="ghost"
          onClick={handleAddSubTask}
          disabled={!newSubTaskTitle.trim() || isAdding}
          className="h-8"
        >
          {isAdding ? (
            <Loader2 className="w-4 h-4 animate-spin" />
          ) : (
            <Plus className="w-4 h-4" />
          )}
        </Button>
      </div>
    </div>
  );
}
