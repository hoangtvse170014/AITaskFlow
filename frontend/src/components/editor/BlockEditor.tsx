"use client";

import * as React from "react";
import {
  DndContext,
  closestCenter,
  KeyboardSensor,
  PointerSensor,
  useSensor,
  useSensors,
  DragEndEvent,
} from "@dnd-kit/core";
import {
  arrayMove,
  SortableContext,
  sortableKeyboardCoordinates,
  verticalListSortingStrategy,
  useSortable,
} from "@dnd-kit/sortable";
import { CSS } from "@dnd-kit/utilities";
import { GripVertical, MoreHorizontal, Plus, Trash2, Type, Heading1, Heading2, Heading3, List, ListOrdered, CheckSquare, Quote, Code, Minus, ChevronDown, ChevronRight, Image, LayoutGrid } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { cn } from "@/lib/utils";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import { blockApi } from "@/lib/api";
import { AiToolbar } from "@/components/ai/AiToolbar";
import { AiSidePanel } from "@/components/ai/AiSidePanel";
import type { AiActionType } from "@/components/ai/AiToolbar";

interface BlockEditorProps {
  pageId: string;
  blocks: any[];
  onBlocksChange: (blocks: any[]) => void;
}

const BLOCK_TYPES = [
  { type: "TEXT", icon: Type, label: "Text", description: "Plain text block" },
  { type: "H1", icon: Heading1, label: "Heading 1", description: "Large heading" },
  { type: "H2", icon: Heading2, label: "Heading 2", description: "Medium heading" },
  { type: "H3", icon: Heading3, label: "Heading 3", description: "Small heading" },
  { type: "BULLET_LIST", icon: List, label: "Bullet List", description: "Bulleted list" },
  { type: "NUMBERED_LIST", icon: ListOrdered, label: "Numbered List", description: "Numbered list" },
  { type: "CHECKLIST", icon: CheckSquare, label: "Checklist", description: "Checkbox items" },
  { type: "QUOTE", icon: Quote, label: "Quote", description: "Quote block" },
  { type: "CODE", icon: Code, label: "Code Block", description: "Code snippet" },
  { type: "DIVIDER", icon: Minus, label: "Divider", description: "Horizontal line" },
  { type: "CALLOUT", icon: LayoutGrid, label: "Callout", description: "Highlight box" },
  { type: "TOGGLE", icon: ChevronDown, label: "Toggle", description: "Collapsible content" },
  { type: "IMAGE", icon: Image, label: "Image", description: "Embed an image" },
];

interface BlockItemProps {
  block: any;
  onUpdate: (content: string) => void;
  onDelete: () => void;
  onDuplicate: () => void;
  onAddBlock: (type: string, afterId: string) => void;
  isLast: boolean;
}

function SortableBlock({ block, onUpdate, onDelete, onDuplicate, onAddBlock, isLast }: BlockItemProps) {
  const [isEditing, setIsEditing] = React.useState(false);
  const [content, setContent] = React.useState(block.content || "");
  const inputRef = React.useRef<HTMLTextAreaElement>(null);

  const {
    attributes,
    listeners,
    setNodeRef,
    transform,
    transition,
    isDragging,
  } = useSortable({ id: block.id });

  const style = {
    transform: CSS.Transform.toString(transform),
    transition,
  };

  React.useEffect(() => {
    setContent(block.content || "");
  }, [block.content]);

  const handleKeyDown = (e: React.KeyboardEvent) => {
    if (e.key === "Enter" && !e.shiftKey) {
      e.preventDefault();
      onAddBlock("TEXT", block.id);
    }
    if (e.key === "Backspace" && content === "" && !isLast) {
      e.preventDefault();
      onDelete();
    }
  };

  const getBlockStyle = () => {
    switch (block.blockType) {
      case "H1":
        return "text-3xl font-bold";
      case "H2":
        return "text-2xl font-semibold";
      case "H3":
        return "text-xl font-medium";
      case "QUOTE":
        return "border-l-4 border-primary pl-4 italic";
      case "CODE":
        return "bg-muted p-4 rounded-lg font-mono text-sm whitespace-pre-wrap";
      case "DIVIDER":
        return "h-px bg-border my-2";
      default:
        return "";
    }
  };

  const renderBlockContent = () => {
    if (block.blockType === "DIVIDER") {
      return <hr className="border-t border-border" />;
    }

    if (isEditing || content) {
      return (
        <textarea
          ref={inputRef}
          value={content}
          onChange={(e) => setContent(e.target.value)}
          onBlur={() => {
            setIsEditing(false);
            if (content !== block.content) {
              onUpdate(content);
            }
          }}
          onFocus={() => setIsEditing(true)}
          onKeyDown={handleKeyDown}
          placeholder="Type something..."
          className={cn(
            "w-full bg-transparent border-none outline-none resize-none min-h-[1.5rem]",
            getBlockStyle()
          )}
          rows={1}
        />
      );
    }

    return (
      <button
        onClick={() => {
          setIsEditing(true);
          setTimeout(() => inputRef.current?.focus(), 0);
        }}
        className="w-full text-left text-muted-foreground opacity-0 hover:opacity-100 transition-opacity"
      >
        Type something...
      </button>
    );
  };

  return (
    <div
      ref={setNodeRef}
      style={style}
      className={cn(
        "group relative flex items-start gap-2 py-1",
        isDragging && "opacity-50 bg-muted rounded-lg"
      )}
    >
      {/* Drag Handle */}
      <button
        {...attributes}
        {...listeners}
        className="opacity-0 group-hover:opacity-100 transition-opacity cursor-grab p-1 text-muted-foreground hover:text-foreground"
      >
        <GripVertical className="w-4 h-4" />
      </button>

      {/* Block Content */}
      <div className="flex-1 min-w-0">{renderBlockContent()}</div>

      {/* Block Menu */}
      <DropdownMenu>
        <DropdownMenuTrigger asChild>
          <Button
            variant="ghost"
            size="icon"
            className="opacity-0 group-hover:opacity-100 h-6 w-6"
          >
            <MoreHorizontal className="w-4 h-4" />
          </Button>
        </DropdownMenuTrigger>
        <DropdownMenuContent align="start">
          {BLOCK_TYPES.slice(0, 6).map((bt) => (
            <DropdownMenuItem key={bt.type} onClick={() => onAddBlock(bt.type, block.id)}>
              <bt.icon className="w-4 h-4 mr-2" />
              {bt.label}
            </DropdownMenuItem>
          ))}
          <DropdownMenuSeparator />
          <DropdownMenuItem onClick={onDuplicate}>
            <Plus className="w-4 h-4 mr-2" />
            Duplicate
          </DropdownMenuItem>
          <DropdownMenuItem onClick={onDelete} className="text-destructive focus:text-destructive">
            <Trash2 className="w-4 h-4 mr-2" />
            Delete
          </DropdownMenuItem>
        </DropdownMenuContent>
      </DropdownMenu>
    </div>
  );
}

export function BlockEditor({ pageId, blocks, onBlocksChange }: BlockEditorProps) {
  const sensors = useSensors(
    useSensor(PointerSensor),
    useSensor(KeyboardSensor, {
      coordinateGetter: sortableKeyboardCoordinates,
    })
  );

  const [aiPanelOpen, setAiPanelOpen] = React.useState(false);
  const [aiLoading, setAiLoading] = React.useState(false);

  const handleDragEnd = async (event: DragEndEvent) => {
    const { active, over } = event;
    if (over && active.id !== over.id) {
      const oldIndex = blocks.findIndex((b) => b.id === active.id);
      const newIndex = blocks.findIndex((b) => b.id === over.id);
      const newBlocks = arrayMove(blocks, oldIndex, newIndex);
      onBlocksChange(newBlocks);
      await blockApi.reorder(pageId, { blockId: active.id as string, newIndex });
    }
  };

  const handleAddBlock = async (type: string, afterId: string) => {
    try {
      const response = await blockApi.create(pageId, { blockType: type });
      if (response.data.success) {
        const newBlock = response.data.data;
        const afterIndex = blocks.findIndex((b) => b.id === afterId);
        const newBlocks = [...blocks];
        newBlocks.splice(afterIndex + 1, 0, newBlock);
        onBlocksChange(newBlocks);
      }
    } catch (error) {
      console.error("Failed to add block:", error);
    }
  };

  const handleUpdateBlock = async (blockId: string, content: string) => {
    try {
      await blockApi.update(pageId, blockId, { content });
      onBlocksChange(blocks.map((b) => (b.id === blockId ? { ...b, content } : b)));
    } catch (error) {
      console.error("Failed to update block:", error);
    }
  };

  const handleDeleteBlock = async (blockId: string) => {
    try {
      await blockApi.delete(pageId, blockId);
      onBlocksChange(blocks.filter((b) => b.id !== blockId));
    } catch (error) {
      console.error("Failed to delete block:", error);
    }
  };

  const handleDuplicateBlock = async (blockId: string) => {
    try {
      const response = await blockApi.duplicate(pageId, [blockId]);
      if (response.data.success && response.data.data.length > 0) {
        const duplicated = response.data.data[0];
        const blockIndex = blocks.findIndex((b) => b.id === blockId);
        const newBlocks = [...blocks];
        newBlocks.splice(blockIndex + 1, 0, duplicated);
        onBlocksChange(newBlocks);
      }
    } catch (error) {
      console.error("Failed to duplicate block:", error);
    }
  };

  const handleAddBlockAtEnd = async (blockData?: { blockType: string; content?: string }) => {
    try {
      const blockType = blockData?.blockType || "TEXT";
      const response = await blockApi.create(pageId, { blockType });
      if (response.data.success) {
        const newBlock = { ...response.data.data, content: blockData?.content || "" };
        const newBlocks = [...blocks, newBlock];
        onBlocksChange(newBlocks);
        // Update the block with content if provided
        if (blockData?.content) {
          await blockApi.update(pageId, newBlock.id, { content: blockData.content });
        }
      }
    } catch (error) {
      console.error("Failed to add block:", error);
    }
  };

  return (
    <div className="max-w-3xl mx-auto py-8">
      {/* AI Toolbar */}
      <AiToolbar
        onAction={(action) => {
          setAiPanelOpen(true);
        }}
        isLoading={aiLoading}
      />

      {/* AI Side Panel */}
      <AiSidePanel
        pageId={pageId}
        open={aiPanelOpen}
        onClose={() => setAiPanelOpen(false)}
        onInsert={(content, type) => {
          // Add content as a new block
          const newBlock = {
            id: `temp-${Date.now()}`,
            blockType: type === "actions" ? "CHECKLIST" : "TEXT",
            content: content,
          };
          handleAddBlockAtEnd(newBlock);
        }}
      />

      {/* Slash Command Menu */}
      <div className="mb-4">
        <DropdownMenu>
          <DropdownMenuTrigger asChild>
            <Button variant="outline" size="sm">
              <Plus className="w-4 h-4 mr-2" />
              Add block
            </Button>
          </DropdownMenuTrigger>
          <DropdownMenuContent className="w-64">
            <div className="px-2 py-1.5 text-xs text-muted-foreground font-medium">
              Basic blocks
            </div>
            {BLOCK_TYPES.map((bt) => (
              <DropdownMenuItem key={bt.type} onClick={() => handleAddBlock(bt.type, blocks[blocks.length - 1]?.id || "")}>
                <bt.icon className="w-4 h-4 mr-2 text-muted-foreground" />
                <div>
                  <div className="text-sm">{bt.label}</div>
                  <div className="text-xs text-muted-foreground">{bt.description}</div>
                </div>
              </DropdownMenuItem>
            ))}
          </DropdownMenuContent>
        </DropdownMenu>
      </div>

      {/* Block List */}
      <DndContext
        sensors={sensors}
        collisionDetection={closestCenter}
        onDragEnd={handleDragEnd}
      >
        <SortableContext items={blocks.map((b) => b.id)} strategy={verticalListSortingStrategy}>
          <div className="space-y-1">
            {blocks.map((block, index) => (
              <SortableBlock
                key={block.id}
                block={block}
                onUpdate={(content) => handleUpdateBlock(block.id, content)}
                onDelete={() => handleDeleteBlock(block.id)}
                onDuplicate={() => handleDuplicateBlock(block.id)}
                onAddBlock={handleAddBlock}
                isLast={index === blocks.length - 1}
              />
            ))}
          </div>
        </SortableContext>
      </DndContext>

      {/* Empty State */}
      {blocks.length === 0 && (
        <div className="text-center py-12 text-muted-foreground">
          <Type className="w-12 h-12 mx-auto mb-4 opacity-50" />
          <p className="mb-4">Start writing or add a block</p>
          <DropdownMenu>
            <DropdownMenuTrigger asChild>
              <Button variant="outline">
                <Plus className="w-4 h-4 mr-2" />
                Add your first block
              </Button>
            </DropdownMenuTrigger>
            <DropdownMenuContent className="w-64">
              {BLOCK_TYPES.map((bt) => (
                <DropdownMenuItem key={bt.type} onClick={() => handleAddBlock(bt.type, "")}>
                  <bt.icon className="w-4 h-4 mr-2 text-muted-foreground" />
                  {bt.label}
                </DropdownMenuItem>
              ))}
            </DropdownMenuContent>
          </DropdownMenu>
        </div>
      )}
    </div>
  );
}
