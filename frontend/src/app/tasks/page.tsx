"use client";

import { Suspense, useEffect } from "react";
import Link from "next/link";
import { useSearchParams } from "next/navigation";
import { useAuth } from "@/lib/auth-context";
import { useTasksSnapshot, useTasksStore } from "@/lib/tasks-context";
import { ConnectionBanner, TaskCard } from "@/components/TaskCard";

export default function TasksPage() {
  return (
    <Suspense fallback={<p className="spinner-text">加载中…</p>}>
      <TasksPageInner />
    </Suspense>
  );
}

function TasksPageInner() {
  const { auth, ready, openLogin } = useAuth();
  const store = useTasksStore();
  const snapshot = useTasksSnapshot();
  const searchParams = useSearchParams();
  const focusId = searchParams.get("focus");

  // 挂载即恢复跟踪：刷新页面后重新拉取记录；有未完成任务则自动轮询，
  // 全部结束后轮询自动停止（清理逻辑在 store 与组件卸载时处理）
  useEffect(() => {
    if (!auth) return;
    store.startTracking();
    return () => store.stopTracking();
  }, [store, auth]);

  if (!ready) return <p className="spinner-text">加载中…</p>;

  if (!auth) {
    return (
      <div className="empty-state">
        <span className="empty-icon">🔐</span>
        <p>登录后可查看你的制作任务与历史记录</p>
        <button type="button" className="btn btn-primary" onClick={openLogin}>
          去登录
        </button>
      </div>
    );
  }

  const unfinished = snapshot.tasks.filter(
    (t) => t.status === "WAITING" || t.status === "PROCESSING"
  );
  const finished = snapshot.tasks.filter(
    (t) => t.status === "SUCCESS" || t.status === "FAILED"
  );
  const ordered = [...unfinished, ...finished];
  const focusTask = focusId ? ordered.find((t) => t.id === focusId) : null;
  const display = focusTask
    ? [focusTask, ...ordered.filter((t) => t.id !== focusId)]
    : ordered;

  return (
    <>
      <h1 className="page-title">我的任务</h1>
      <p className="page-subtitle">
        制作任务在后台继续执行，关闭页面不影响进度
        {unfinished.length > 0 && ` · ${unfinished.length} 个任务进行中`}
      </p>

      <ConnectionBanner snapshot={snapshot} onRetry={() => void store.refresh()} />

      {snapshot.loading && snapshot.tasks.length === 0 ? (
        <p className="spinner-text">任务加载中…</p>
      ) : display.length === 0 ? (
        <div className="empty-state">
          <span className="empty-icon">📭</span>
          还没有制作任务，
          <Link href="/">去挑一个模板试试</Link>
        </div>
      ) : (
        <div className="task-list">
          {display.map((task) => (
            <TaskCard key={task.id} task={task} />
          ))}
        </div>
      )}
    </>
  );
}
