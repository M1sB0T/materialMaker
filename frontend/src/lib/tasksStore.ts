import type { TaskStepView, TaskView } from "@/types";

export interface TasksSnapshot {
  tasks: TaskView[];
  /** 查询接口暂时不可用（网络/服务器异常）。注意：这不代表任务失败 */
  connectionError: boolean;
  /** 首次加载中 */
  loading: boolean;
}

/**
 * 与框架无关的任务跟踪 Store，便于对“多任务互不串扰、
 * 查询异常与任务失败区分、轮询自动启停”做单元测试。
 *
 * 设计要点：
 * - 查询失败时保留已有任务信息，仅置 connectionError，绝不改写任务状态
 * - 只有存在未完成任务（WAITING/PROCESSING）时才继续轮询
 * - 任务的 WAITING/PROCESSING/SUCCESS/FAILED 状态完全来自服务端数据
 */
export class TasksStore {
  private listeners = new Set<() => void>();
  private snapshot: TasksSnapshot = { tasks: [], connectionError: false, loading: false };
  private timer: ReturnType<typeof setInterval> | null = null;
  private inFlight = false;
  /** 每次自增，用于丢弃 reset 之前发出的过期请求结果 */
  private epoch = 0;

  constructor(
    /** 注入查询函数（默认走 /api/tasks），测试时可注入假实现 */
    private fetchTasks: () => Promise<TaskView[]>,
    private intervalMs = 2000
  ) {}

  subscribe = (listener: () => void): (() => void) => {
    this.listeners.add(listener);
    return () => this.listeners.delete(listener);
  };

  getSnapshot = (): TasksSnapshot => this.snapshot;

  /** 手动/定时刷新一次任务列表 */
  refresh = async (): Promise<void> => {
    if (this.inFlight) return;
    this.inFlight = true;
    const epoch = this.epoch;
    try {
      const tasks = (await this.fetchTasks()) ?? [];
      if (epoch !== this.epoch) return; // reset 之后到达的过期结果，丢弃
      this.snapshot = { tasks, connectionError: false, loading: false };
      this.emit();
    } catch {
      if (epoch !== this.epoch) return;
      // 查询异常：保留已有任务信息，不把任务标记为失败
      this.snapshot = { ...this.snapshot, connectionError: true, loading: false };
      this.emit();
    } finally {
      this.inFlight = false;
    }
  };

  /** 确保跟踪逻辑启动：先刷新一次，仍有未完成任务则开启轮询，全部结束后自动停止 */
  startTracking = (): void => {
    if (this.timer) return;
    void this.refresh().then(() => {
      if (!this.timer && this.hasUnfinished()) this.startPolling();
    });
  };

  private startPolling(): void {
    if (this.timer) return;
    this.timer = setInterval(() => {
      void this.refresh().then(() => {
        if (!this.hasUnfinished()) this.stopTracking();
      });
    }, this.intervalMs);
  }

  stopTracking = (): void => {
    if (this.timer) {
      clearInterval(this.timer);
      this.timer = null;
    }
  };

  /** 切换用户 / 退出登录时清空数据并停止轮询；丢弃在途请求的过期结果 */
  reset = (): void => {
    this.epoch++;
    this.stopTracking();
    this.snapshot = { tasks: [], connectionError: false, loading: false };
    this.emit();
  };

  hasUnfinished = (): boolean =>
    this.snapshot.tasks.some((t) => t.status === "WAITING" || t.status === "PROCESSING");

  private emit(): void {
    for (const listener of this.listeners) listener();
  }
}

/** 状态徽标文案（中文） */
export function statusText(status: TaskView["status"]): string {
  switch (status) {
    case "WAITING":
      return "等待处理";
    case "PROCESSING":
      return "处理中";
    case "SUCCESS":
      return "成功";
    case "FAILED":
      return "失败";
  }
}

export function stepStatusText(step: TaskStepView): string {
  switch (step.status) {
    case "PENDING":
      return "待执行";
    case "RUNNING":
      return "执行中";
    case "SUCCESS":
      return "已完成";
    case "FAILED":
      return "失败";
  }
}

export function formatTime(iso: string | null): string {
  if (!iso) return "-";
  const d = new Date(iso);
  if (Number.isNaN(d.getTime())) return iso;
  return d.toLocaleString("zh-CN", { hour12: false });
}
