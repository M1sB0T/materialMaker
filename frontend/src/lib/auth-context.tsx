"use client";

import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useState,
  type ReactNode,
} from "react";
import { api, clearAuth, loadAuth, saveAuth, type AuthInfo } from "@/lib/api";
import type { LoginResponse } from "@/types";

interface AuthContextValue {
  auth: AuthInfo | null;
  /** 首次从 localStorage 恢复完成（避免 SSR 闪烁） */
  ready: boolean;
  loginOpen: boolean;
  openLogin: () => void;
  closeLogin: () => void;
  login: (username: string) => Promise<void>;
  logout: () => void;
}

const AuthContext = createContext<AuthContextValue | null>(null);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [auth, setAuth] = useState<AuthInfo | null>(null);
  const [ready, setReady] = useState(false);
  const [loginOpen, setLoginOpen] = useState(false);

  useEffect(() => {
    setAuth(loadAuth());
    setReady(true);
  }, []);

  const login = useCallback(async (username: string) => {
    const res = await api<LoginResponse>("/api/auth/login", {
      method: "POST",
      json: { username },
    });
    const next = { token: res.token, username: res.username };
    saveAuth(next);
    setAuth(next);
    setLoginOpen(false);
  }, []);

  const logout = useCallback(() => {
    // 尽力通知服务端吊销 token（失败不影响本地登出）
    api("/api/auth/logout", { method: "DELETE" }).catch(() => {});
    clearAuth();
    setAuth(null);
  }, []);

  const value = useMemo<AuthContextValue>(
    () => ({
      auth,
      ready,
      loginOpen,
      openLogin: () => setLoginOpen(true),
      closeLogin: () => setLoginOpen(false),
      login,
      logout,
    }),
    [auth, ready, loginOpen, login, logout]
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth(): AuthContextValue {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error("useAuth 必须在 AuthProvider 内使用");
  return ctx;
}
