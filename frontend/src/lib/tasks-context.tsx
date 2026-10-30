"use client";

import {
  createContext,
  useContext,
  useEffect,
  useMemo,
  useRef,
  useSyncExternalStore,
  type ReactNode,
} from "react";
import { api } from "@/lib/api";
import { TasksStore } from "@/lib/tasksStore";
import { useAuth } from "@/lib/auth-context";
import type { TaskView } from "@/types";

const TasksContext = createContext<TasksStore | null>(null);

export function TasksProvider({ children }: { children: ReactNode }) {
  const { auth } = useAuth();
  const storeRef = useRef<TasksStore | null>(null);
  if (!storeRef.current) {
    storeRef.current = new TasksStore(() => api<TaskView[]>("/api/tasks"), 2000);
  }
  const store = storeRef.current;

  // 切换用户（登录/退出）时清空旧数据，避免上一个用户的数据串到当前界面
  const token = auth?.token ?? null;
  const username = auth?.username ?? null;
  useEffect(() => {
    store.reset();
  }, [store, token, username]);

  return <TasksContext.Provider value={store}>{children}</TasksContext.Provider>;
}

export function useTasksStore(): TasksStore {
  const store = useContext(TasksContext);
  if (!store) throw new Error("useTasksStore 必须在 TasksProvider 内使用");
  return store;
}

export function useTasksSnapshot() {
  const store = useTasksStore();
  return useSyncExternalStore(store.subscribe, store.getSnapshot, store.getSnapshot);
}
