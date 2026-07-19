import { create } from "zustand";
import { pageApi, blockApi } from "@/lib/api";
import toast from "react-hot-toast";

interface Block {
  id: string;
  pageId: string;
  blockType: string;
  content: string | null;
  properties: string | null;
  orderIndex: number;
  isCollapsed: boolean;
  createdBy: any;
  updatedBy: any;
  createdAt: string;
  updatedAt: string;
}

interface BlockUpdateData {
  content?: string | null;
  properties?: string | null;
  orderIndex?: number;
  isCollapsed?: boolean;
  parentBlockId?: string | null;
}

interface Page {
  id: string;
  workspaceId: string;
  parentId: string | null;
  title: string;
  icon: string | null;
  coverUrl: string | null;
  slug: string | null;
  isPublic: boolean;
  isArchived: boolean;
  isFavorite: boolean;
  sidebarOrder: number;
  createdBy: any;
  updatedBy: any;
  createdAt: string;
  updatedAt: string;
  children: Page[];
  childCount: number;
  blockCount: number;
}

interface PageUpdateData {
  title?: string;
  icon?: string | null;
  coverUrl?: string | null;
  slug?: string | null;
  parentId?: string | null;
  isPublic?: boolean;
  isArchived?: boolean;
  sidebarOrder?: number;
}

interface PageDetail {
  page: Page;
  blocks: Block[];
  lastEditedAt: string;
  lastEditedBy: string;
}

interface PageState {
  pages: Page[];
  pageTree: Page[];
  currentPage: PageDetail | null;
  isLoading: boolean;
  isSaving: boolean;
  
  fetchPages: (workspaceId: string) => Promise<void>;
  fetchPageTree: (workspaceId: string) => Promise<void>;
  fetchPageById: (workspaceId: string, pageId: string) => Promise<void>;
  createPage: (workspaceId: string, data: { title: string; icon?: string; parentId?: string }) => Promise<Page>;
  updatePage: (workspaceId: string, pageId: string, data: PageUpdateData) => Promise<void>;
  deletePage: (workspaceId: string, pageId: string) => Promise<void>;
  archivePage: (workspaceId: string, pageId: string) => Promise<void>;
  restorePage: (workspaceId: string, pageId: string) => Promise<void>;
  duplicatePage: (workspaceId: string, pageId: string) => Promise<void>;
  toggleFavorite: (workspaceId: string, pageId: string) => Promise<void>;
  searchPages: (workspaceId: string, query: string) => Promise<any[]>;
  
  createBlock: (pageId: string, data: { blockType: string; content?: string; orderIndex?: number }) => Promise<Block>;
  updateBlock: (pageId: string, blockId: string, data: BlockUpdateData) => Promise<void>;
  deleteBlock: (pageId: string, blockId: string) => Promise<void>;
  reorderBlocks: (pageId: string, blockId: string, newIndex: number) => Promise<void>;
}

export const usePageStore = create<PageState>((set, get) => ({
  pages: [],
  pageTree: [],
  currentPage: null,
  isLoading: false,
  isSaving: false,

  fetchPages: async (workspaceId: string) => {
    set({ isLoading: true });
    try {
      const response = await pageApi.getAll(workspaceId);
      if (response.data.success) {
        set({ pages: response.data.data });
      }
    } catch (error) {
      console.error("Failed to fetch pages:", error);
    } finally {
      set({ isLoading: false });
    }
  },

  fetchPageTree: async (workspaceId: string) => {
    try {
      const response = await pageApi.getTree(workspaceId);
      if (response.data.success) {
        set({ pageTree: response.data.data });
      }
    } catch (error) {
      console.error("Failed to fetch page tree:", error);
    }
  },

  fetchPageById: async (workspaceId: string, pageId: string) => {
    set({ isLoading: true });
    try {
      const response = await pageApi.getById(workspaceId, pageId);
      if (response.data.success) {
        set({ currentPage: response.data.data });
      }
    } catch (error) {
      console.error("Failed to fetch page:", error);
      toast.error("Failed to load page");
    } finally {
      set({ isLoading: false });
    }
  },

  createPage: async (workspaceId: string, data: { title: string; icon?: string; parentId?: string }) => {
    try {
      const response = await pageApi.create(workspaceId, data);
      if (response.data.success) {
        const newPage = response.data.data;
        set((state) => ({ pages: [...state.pages, newPage] }));
        toast.success("Page created");
        return newPage;
      }
    } catch (error: any) {
      toast.error(error.response?.data?.message || "Failed to create page");
    }
  },

  updatePage: async (workspaceId: string, pageId: string, data: PageUpdateData) => {
    set({ isSaving: true });
    try {
      const response = await pageApi.update(workspaceId, pageId, data);
      if (response.data.success) {
        set((state) => ({
          currentPage: state.currentPage ? { ...state.currentPage, page: response.data.data } : null,
          pages: state.pages.map((p) => (p.id === pageId ? { ...p, ...response.data.data } : p)),
        }));
      }
    } catch (error: any) {
      toast.error(error.response?.data?.message || "Failed to update page");
    } finally {
      set({ isSaving: false });
    }
  },

  deletePage: async (workspaceId: string, pageId: string) => {
    try {
      await pageApi.delete(workspaceId, pageId);
      set((state) => ({
        pages: state.pages.filter((p) => p.id !== pageId),
        currentPage: state.currentPage?.page.id === pageId ? null : state.currentPage,
      }));
      toast.success("Page deleted");
    } catch (error: any) {
      toast.error(error.response?.data?.message || "Failed to delete page");
    }
  },

  archivePage: async (workspaceId: string, pageId: string) => {
    try {
      const response = await pageApi.archive(workspaceId, pageId);
      if (response.data.success) {
        set((state) => ({
          pages: state.pages.map((p) => (p.id === pageId ? { ...p, ...response.data.data } : p)),
        }));
        toast.success("Page archived");
      }
    } catch (error: any) {
      toast.error(error.response?.data?.message || "Failed to archive page");
    }
  },

  restorePage: async (workspaceId: string, pageId: string) => {
    try {
      const response = await pageApi.restore(workspaceId, pageId);
      if (response.data.success) {
        set((state) => ({
          pages: state.pages.map((p) => (p.id === pageId ? { ...p, ...response.data.data } : p)),
        }));
        toast.success("Page restored");
      }
    } catch (error: any) {
      toast.error(error.response?.data?.message || "Failed to restore page");
    }
  },

  duplicatePage: async (workspaceId: string, pageId: string) => {
    try {
      const response = await pageApi.duplicate(workspaceId, pageId);
      if (response.data.success) {
        set((state) => ({ pages: [...state.pages, response.data.data] }));
        toast.success("Page duplicated");
      }
    } catch (error: any) {
      toast.error(error.response?.data?.message || "Failed to duplicate page");
    }
  },

  toggleFavorite: async (workspaceId: string, pageId: string) => {
    try {
      const response = await pageApi.toggleFavorite(workspaceId, pageId);
      if (response.data.success) {
        set((state) => ({
          pages: state.pages.map((p) => (p.id === pageId ? { ...p, isFavorite: response.data.data.isFavorite } : p)),
        }));
      }
    } catch (error: any) {
      toast.error(error.response?.data?.message || "Failed to toggle favorite");
    }
  },

  searchPages: async (workspaceId: string, query: string) => {
    try {
      const response = await pageApi.search(workspaceId, query);
      if (response.data.success) {
        return response.data.data.pages || [];
      }
      return [];
    } catch (error) {
      console.error("Failed to search pages:", error);
      return [];
    }
  },

  createBlock: async (pageId: string, data: { blockType: string; content?: string; orderIndex?: number }) => {
    try {
      const response = await blockApi.create(pageId, data);
      if (response.data.success) {
        const newBlock = response.data.data;
        set((state) => ({
          currentPage: state.currentPage
            ? { ...state.currentPage, blocks: [...state.currentPage.blocks, newBlock] }
            : null,
        }));
        return newBlock;
      }
    } catch (error: any) {
      toast.error(error.response?.data?.message || "Failed to create block");
    }
  },

  updateBlock: async (pageId: string, blockId: string, data: BlockUpdateData) => {
    try {
      const response = await blockApi.update(pageId, blockId, data);
      if (response.data.success) {
        set((state) => ({
          currentPage: state.currentPage
            ? {
                ...state.currentPage,
                blocks: state.currentPage.blocks.map((b) =>
                  b.id === blockId ? { ...b, ...response.data.data } : b
                ),
              }
            : null,
        }));
      }
    } catch (error: any) {
      toast.error(error.response?.data?.message || "Failed to update block");
    }
  },

  deleteBlock: async (pageId: string, blockId: string) => {
    try {
      await blockApi.delete(pageId, blockId);
      set((state) => ({
        currentPage: state.currentPage
          ? { ...state.currentPage, blocks: state.currentPage.blocks.filter((b) => b.id !== blockId) }
          : null,
      }));
    } catch (error: any) {
      toast.error(error.response?.data?.message || "Failed to delete block");
    }
  },

  reorderBlocks: async (pageId: string, blockId: string, newIndex: number) => {
    try {
      await blockApi.reorder(pageId, { blockId, newIndex });
      const { currentPage } = get();
      if (currentPage) {
        const blocks = [...currentPage.blocks];
        const blockIndex = blocks.findIndex((b) => b.id === blockId);
        if (blockIndex !== -1) {
          const [block] = blocks.splice(blockIndex, 1);
          blocks.splice(newIndex, 0, block);
          blocks.forEach((b, i) => (b.orderIndex = i));
          set({ currentPage: { ...currentPage, blocks } });
        }
      }
    } catch (error: any) {
      toast.error(error.response?.data?.message || "Failed to reorder blocks");
    }
  },
}));
