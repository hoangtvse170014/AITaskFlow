"use client";

import * as React from "react";
import { Clock, Play, Pause, Square, Trash2, Loader2 } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { toast } from "react-hot-toast";

interface TimeEntry {
  id: string;
  taskId: string;
  userId: string;
  startTime: string;
  endTime: string | null;
  duration: number; // in seconds
  description: string;
}

interface TimeTrackerProps {
  taskId: string;
}

export function TimeTracker({ taskId }: TimeTrackerProps) {
  const [isRunning, setIsRunning] = React.useState(false);
  const [elapsedTime, setElapsedTime] = React.useState(0);
  const [startTime, setStartTime] = React.useState<Date | null>(null);
  const [entries, setEntries] = React.useState<TimeEntry[]>([]);
  const [isLoading, setIsLoading] = React.useState(false);
  const [manualHours, setManualHours] = React.useState("");
  const [manualMinutes, setManualMinutes] = React.useState("");

  React.useEffect(() => {
    // Load entries from localStorage
    const savedEntries = localStorage.getItem(`time-entries-${taskId}`);
    if (savedEntries) {
      setEntries(JSON.parse(savedEntries));
    }
  }, [taskId]);

  React.useEffect(() => {
    let interval: NodeJS.Timeout;
    if (isRunning && startTime) {
      interval = setInterval(() => {
        const now = new Date();
        setElapsedTime(Math.floor((now.getTime() - startTime.getTime()) / 1000));
      }, 1000);
    }
    return () => clearInterval(interval);
  }, [isRunning, startTime]);

  const formatTime = (seconds: number) => {
    const hrs = Math.floor(seconds / 3600);
    const mins = Math.floor((seconds % 3600) / 60);
    const secs = seconds % 60;
    return `${hrs.toString().padStart(2, "0")}:${mins.toString().padStart(2, "0")}:${secs.toString().padStart(2, "0")}`;
  };

  const handleStart = () => {
    setIsRunning(true);
    setStartTime(new Date());
  };

  const handlePause = () => {
    setIsRunning(false);
  };

  const handleStop = () => {
    if (startTime) {
      const endTime = new Date();
      const duration = Math.floor((endTime.getTime() - startTime.getTime()) / 1000);
      
      const newEntry: TimeEntry = {
        id: Date.now().toString(),
        taskId,
        userId: "current-user",
        startTime: startTime.toISOString(),
        endTime: endTime.toISOString(),
        duration,
        description: "",
      };
      
      const updatedEntries = [newEntry, ...entries];
      setEntries(updatedEntries);
      localStorage.setItem(`time-entries-${taskId}`, JSON.stringify(updatedEntries));
      
      toast.success("Time entry saved");
    }
    
    setIsRunning(false);
    setElapsedTime(0);
    setStartTime(null);
  };

  const handleDelete = (id: string) => {
    const updatedEntries = entries.filter(e => e.id !== id);
    setEntries(updatedEntries);
    localStorage.setItem(`time-entries-${taskId}`, JSON.stringify(updatedEntries));
  };

  const handleAddManual = () => {
    const hours = parseInt(manualHours) || 0;
    const minutes = parseInt(manualMinutes) || 0;
    const duration = hours * 3600 + minutes * 60;
    
    if (duration <= 0) {
      toast.error("Please enter valid time");
      return;
    }
    
    const newEntry: TimeEntry = {
      id: Date.now().toString(),
      taskId,
      userId: "current-user",
      startTime: new Date().toISOString(),
      endTime: new Date().toISOString(),
      duration,
      description: "",
    };
    
    const updatedEntries = [newEntry, ...entries];
    setEntries(updatedEntries);
    localStorage.setItem(`time-entries-${taskId}`, JSON.stringify(updatedEntries));
    
    setManualHours("");
    setManualMinutes("");
    toast.success("Time entry added");
  };

  const totalTime = entries.reduce((sum, e) => sum + e.duration, 0);

  return (
    <div className="space-y-4">
      <div className="flex items-center justify-between">
        <h4 className="text-sm font-semibold flex items-center gap-2">
          <Clock className="w-4 h-4" />
          Time Tracker
        </h4>
        <span className="text-sm text-muted-foreground">
          Total: {formatTime(totalTime)}
        </span>
      </div>

      {/* Timer Display */}
      <Card className="bg-muted/50">
        <CardContent className="pt-4">
          <div className="text-center">
            <p className="text-4xl font-mono font-bold mb-4">
              {formatTime(elapsedTime)}
            </p>
            <div className="flex items-center justify-center gap-2">
              {!isRunning ? (
                <Button onClick={handleStart} size="sm" className="bg-green-600 hover:bg-green-700">
                  <Play className="w-4 h-4 mr-1" />
                  Start
                </Button>
              ) : (
                <>
                  <Button onClick={handlePause} size="sm" variant="outline">
                    <Pause className="w-4 h-4 mr-1" />
                    Pause
                  </Button>
                  <Button onClick={handleStop} size="sm" className="bg-red-600 hover:bg-red-700">
                    <Square className="w-4 h-4 mr-1" />
                    Stop
                  </Button>
                </>
              )}
            </div>
          </div>
        </CardContent>
      </Card>

      {/* Manual Entry */}
      <div className="flex items-end gap-2">
        <div className="flex-1">
          <label className="text-xs text-muted-foreground mb-1 block">Add manual time</label>
          <div className="flex gap-2">
            <Input
              type="number"
              placeholder="Hours"
              value={manualHours}
              onChange={(e) => setManualHours(e.target.value)}
              className="w-20"
              min="0"
            />
            <Input
              type="number"
              placeholder="Minutes"
              value={manualMinutes}
              onChange={(e) => setManualMinutes(e.target.value)}
              className="w-20"
              min="0"
            />
            <Button onClick={handleAddManual} size="sm">
              Add
            </Button>
          </div>
        </div>
      </div>

      {/* Time Entries */}
      {entries.length > 0 && (
        <div className="space-y-2">
          <h5 className="text-xs font-medium text-muted-foreground">Recent entries</h5>
          {entries.slice(0, 5).map((entry) => (
            <div key={entry.id} className="flex items-center justify-between py-2 border-b">
              <div>
                <p className="text-sm">{formatTime(entry.duration)}</p>
                <p className="text-xs text-muted-foreground">
                  {new Date(entry.startTime).toLocaleDateString()}
                </p>
              </div>
              <Button
                variant="ghost"
                size="sm"
                className="h-6 w-6 p-0 text-muted-foreground hover:text-destructive"
                onClick={() => handleDelete(entry.id)}
              >
                <Trash2 className="w-3 h-3" />
              </Button>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
