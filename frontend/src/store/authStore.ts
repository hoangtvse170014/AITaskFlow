import { create } from "zustand";
import type { User, AuthResponse } from "@/types";
import { authApi } from "@/lib/api";
import toast from "react-hot-toast";

interface AuthState {
  user: User | null;
  isAuthenticated: boolean;
  isLoading: boolean;
  login: (email: string, password: string) => Promise<string | null>;
  register: (email: string, password: string, fullName: string) => Promise<string | null>;
  logout: () => void;
  checkAuth: () => Promise<void>;
  setUser: (user: User) => void;
}

export const useAuthStore = create<AuthState>((set) => ({
  user: null,
  isAuthenticated: false,
  isLoading: true,

  login: async (email: string, password: string) => {
    try {
      const response = await authApi.login({ email, password });
      if (response.data.success && response.data.data) {
        const { user, accessToken, refreshToken } = response.data.data;
        localStorage.setItem("accessToken", accessToken);
        localStorage.setItem("refreshToken", refreshToken);
        set({ user, isAuthenticated: true });
        toast.success("Welcome back!");
        // Return workspace ID for redirect
        return response.data.data.workspaceId || null;
      }
      return null;
    } catch (error) {
      throw error;
    }
  },

  register: async (email: string, password: string, fullName: string) => {
    try {
      const response = await authApi.register({ email, password, fullName });
      if (response.data.success && response.data.data) {
        const { user, accessToken, refreshToken } = response.data.data;
        localStorage.setItem("accessToken", accessToken);
        localStorage.setItem("refreshToken", refreshToken);
        set({ user, isAuthenticated: true });
        toast.success("Account created successfully!");
        // Return workspace ID for redirect
        return response.data.data.workspaceId || null;
      }
      return null;
    } catch (error) {
      throw error;
    }
  },

  logout: () => {
    localStorage.removeItem("accessToken");
    localStorage.removeItem("refreshToken");
    set({ user: null, isAuthenticated: false });
    toast.success("Logged out successfully");
  },

  checkAuth: async () => {
    const token = localStorage.getItem("accessToken");
    if (!token) {
      set({ isLoading: false, isAuthenticated: false, user: null });
      return;
    }

    try {
      const response = await authApi.me();
      if (response.data.success && response.data.data?.user) {
        set({ user: response.data.data.user, isAuthenticated: true, isLoading: false });
      } else {
        localStorage.removeItem("accessToken");
        localStorage.removeItem("refreshToken");
        set({ user: null, isAuthenticated: false, isLoading: false });
      }
    } catch {
      localStorage.removeItem("accessToken");
      localStorage.removeItem("refreshToken");
      set({ user: null, isAuthenticated: false, isLoading: false });
    }
  },

  setUser: (user: User) => {
    set({ user });
  },
}));
