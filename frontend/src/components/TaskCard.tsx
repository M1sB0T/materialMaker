"use client";

import { formatTime, statusText, stepStatusText, type TasksSnapshot } from "@/lib/tasksStore";
import type { TaskView } from "@/types";

const STEP_DOT_CLASS: Record<string, string> = {
  PENDING: "step-dot-pending",
  RUNNING: "step-dot-running",
  SUCCESS: "step-dot-success",
  FAILED: "step-dot-failed",
};

export function TaskCard({ task }: { task: TaskView }) {
  return (
    <article className="task-card" aria-label={`任务 ${task.templateTitle} ${statusText(task.status)}`}>
      <div className="task-head">
        <h3 className="task-title">{task.templateTitle}</h3>
        <span className={`badge badge-${task.status.toLowerCase()}`}>{statusText(task.status)}</span>
        {task.status === "PROCESSING" && task.currentPhaseDisplay && (
          <span className="badge badge-processing">当前阶段：{task.currentPhaseDisplay}</span>
        )}
        <span className="task-time">创建于 {formatTime(task.createdAt)}</span>
      </div>

      <ul className="step-list" aria-label="处理步骤">
        {task.steps.map((step) => (
          <li key={step.step} className="step-item">
            <span className={`step-dot ${STEP_DOT_CLASS[step.status] ?? "step-dot-pending"}`} aria-hidden />
            <span>
              {step.stepDisplay}（{stepStatusText(step)}）
              {step.message && <span className="step-message">：{step.message}</span>}
            </span>
          </li>
        ))}
      </ul>

      {task.status === "FAILED" && task.failReason && (
        <p className="alert alert-error" role="alert">
          失败原因：{task.failReason}
        </p>
      )}

      {task.status === "SUCCESS" && task.resultUrl && (
        <div className="task-result">
          {/* eslint-disable-next-line @next/next/no-img-element */}
          <img src={task.resultUrl} alt={`任务 ${task.templateTitle} 的制作结果`} />
          <p className="result-caption">完成于 {formatTime(task.finishedAt)}</p>
        </div>
      )}
    </article>
  );
}

export function ConnectionBanner({ snapshot, onRetry }: { snapshot: TasksSnapshot; onRetry: () => void }) {
  if (!snapshot.connectionError) return null;
  return (
    <div className="alert alert-warning" role="alert">
      ⚠️ 任务状态查询暂时不可用，以下为最后一次获取到的状态，不会影响后台任务继续处理。
      <button type="button" className="btn btn-sm" style={{ marginLeft: 10 }} onClick={onRetry}>
        立即重试
      </button>
    </div>
  );
}
