"use client";

import { useEffect, useMemo, useState, type FormEvent } from "react";
import Link from "next/link";
import { useParams } from "next/navigation";
import { api, fileUrl } from "@/lib/api";
import { useAuth } from "@/lib/auth-context";
import { useTasksSnapshot, useTasksStore } from "@/lib/tasks-context";
import { formatTime, statusText } from "@/lib/tasksStore";
import { UploadSlot } from "@/components/UploadSlot";
import type { MaterialView, TaskView, TemplateView } from "@/types";

interface SubmitResponse {
  taskId: string;
  status: string;
}

/** History 网格中的单张任务卡：成功显结果图，处理中/失败显状态文案 */
function HistoryCard({ task }: { task: TaskView }) {
  const failed = task.status === "FAILED";
  const processing = task.status === "PROCESSING" || task.status === "WAITING";
  return (
    <Link
      href={`/tasks?focus=${task.id}`}
      className={`history-card ${failed ? "history-failed" : ""}`}
      aria-label={`查看任务详情：${statusText(task.status)}`}
    >
      {task.status === "SUCCESS" && task.resultUrl ? (
        // eslint-disable-next-line @next/next/no-img-element
        <img className="history-result" src={fileUrl(task.resultUrl)} alt={`任务结果：${task.templateTitle}`} />
      ) : (
        <span className={processing ? "history-processing" : ""}>
          {task.status === "WAITING" && "排队中…"}
          {task.status === "PROCESSING" && (task.currentPhaseDisplay ?? "制作中…")}
          {failed && (
            <>
              制作失败
              {task.failReason && <span className="history-reason">{task.failReason}</span>}
            </>
          )}
        </span>
      )}
      {task.createdAt && <span className="history-card-time">{formatTime(task.createdAt)}</span>}
    </Link>
  );
}

export default function TemplateDetailPage() {
  const params = useParams<{ id: string }>();
  const templateId = params?.id ?? "";
  const { auth, openLogin, ready } = useAuth();
  const tasksStore = useTasksStore();
  const tasksSnapshot = useTasksSnapshot();

  const [template, setTemplate] = useState<TemplateView | null>(null);
  const [loading, setLoading] = useState(true);
  const [loadError, setLoadError] = useState<string | null>(null);
  const [slots, setSlots] = useState<(MaterialView | null)[]>([]);
  const [submitting, setSubmitting] = useState(false);
  const [submitError, setSubmitError] = useState<string | null>(null);
  const [submittedTaskId, setSubmittedTaskId] = useState<string | null>(null);

  const load = () => {
    setLoading(true);
    setLoadError(null);
    api<TemplateView>(`/api/templates/${templateId}`)
      .then((t) => {
        setTemplate(t);
        setSlots(Array.from({ length: t.requiredImageCount }, () => null));
        setLoading(false);
      })
      .catch((err: unknown) => {
        setLoadError(err instanceof Error ? err.message : "模板加载失败");
        setLoading(false);
      });
  };

  useEffect(() => {
    if (templateId) load();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [templateId]);

  // 登录后加载该用户的任务列表（供右侧 History 展示），有未完成任务时自动轮询
  useEffect(() => {
    if (!auth) return;
    tasksStore.startTracking();
    return () => tasksStore.stopTracking();
  }, [tasksStore, auth, templateId]);

  const allFilled = useMemo(() => slots.length > 0 && slots.every((s) => s !== null), [slots]);

  // 右侧 History：只展示当前模板的任务，新提交的排最前
  const historyTasks = useMemo(() => {
    const list = tasksSnapshot.tasks.filter((t) => t.templateId === templateId);
    return [...list].reverse();
  }, [tasksSnapshot.tasks, templateId]);

  const onSubmit = async (e: FormEvent) => {
    e.preventDefault();
    if (!auth) {
      openLogin();
      return;
    }
    if (!template || submitting) return;
    if (!allFilled) {
      setSubmitError("请先按要求上传全部素材图片");
      return;
    }
    setSubmitting(true);
    setSubmitError(null);
    setSubmittedTaskId(null);
    try {
      const res = await api<SubmitResponse>("/api/tasks", {
        method: "POST",
        json: { templateId: template.id, materialIds: slots.map((s) => s!.id) },
      });
      // 受理成功：留在本页，右侧 History 出现新卡片并随轮询实时更新
      setSubmittedTaskId(res.taskId);
      tasksStore.startTracking();
    } catch (err) {
      setSubmitError(err instanceof Error ? err.message : "提交失败，请稍后再试");
    } finally {
      setSubmitting(false);
    }
  };

  const leftPanel = (t: TemplateView) => (
    <form className="panel" onSubmit={onSubmit}>
      <Link href="/" className="panel-back" aria-label="返回模板列表">
        <span aria-hidden>‹</span> {t.title}
      </Link>

      {/* eslint-disable-next-line @next/next/no-img-element */}
      <img className="detail-cover" src={t.coverUrl} alt={`模板封面：${t.title}`} />

      <p className="upload-label">上传素材（{t.requiredImageCount} 张）</p>
      <div className="slot-grid">
        {slots.map((slot, i) => (
          <UploadSlot
            key={i}
            index={i}
            templateId={t.id}
            acceptedMimeTypes={t.acceptedMimeTypes}
            maxFileSizeBytes={t.maxFileSizeBytes}
            material={slot}
            onChange={(m) =>
              setSlots((prev) => prev.map((s, j) => (j === i ? m : s)))
            }
          />
        ))}
      </div>

      <ul className="detail-requirements">
        <li>支持类型：{t.acceptedMimeTypes.join("、")}</li>
        <li>大小限制：不超过 {(t.maxFileSizeBytes / 1024 / 1024).toFixed(1)}MB</li>
      </ul>

      {submitError && (
        <p className="alert alert-error" role="alert">
          {submitError}
        </p>
      )}
      {submittedTaskId && (
        <p className="alert alert-info submit-banner" role="status">
          任务已受理（ID：{submittedTaskId.slice(0, 8)}…），可在右侧查看进度
        </p>
      )}

      {!ready ? null : !auth ? (
        <button type="button" className="btn-cta" onClick={openLogin}>
          登录后开始制作
        </button>
      ) : (
        <button
          type="submit"
          className="btn-cta"
          disabled={submitting || !allFilled}
          aria-disabled={submitting || !allFilled}
        >
          {submitting ? "提交中…" : !allFilled ? `上传全部 ${t.requiredImageCount} 张图片后开始制作` : "立即制作"}
        </button>
      )}
    </form>
  );

  const historyPanel = (
    <section className="panel" aria-label="本模板的制作历史">
      <div className="history-head">
        <h2 className="history-title">制作记录</h2>
        <button
          type="button"
          className="history-refresh"
          onClick={() => void tasksStore.refresh()}
          disabled={tasksSnapshot.connectionError && tasksSnapshot.tasks.length === 0}
          aria-label="刷新制作记录"
        >
          ⟳
        </button>
      </div>

      {tasksSnapshot.connectionError && (
        <div className="alert alert-warning" role="alert">
          制作记录查询暂时不可用，不影响后台任务继续处理。
          <button
            type="button"
            className="btn btn-sm"
            style={{ marginLeft: 10 }}
            onClick={() => void tasksStore.refresh()}
          >
            重试
          </button>
        </div>
      )}

      {!auth ? (
        <div className="history-empty">登录后可在这里查看你的制作记录</div>
      ) : tasksSnapshot.loading && historyTasks.length === 0 ? (
        <div className="history-empty">加载中…</div>
      ) : historyTasks.length === 0 ? (
        <div className="history-empty">还没有制作记录，上传素材开始第一次制作吧</div>
      ) : (
        <div className="history-grid">
          {historyTasks.map((task) => (
            <HistoryCard key={task.id} task={task} />
          ))}
        </div>
      )}
    </section>
  );

  if (loading) return <p className="spinner-text">模板加载中…</p>;

  if (loadError || !template) {
    return (
      <div className="alert alert-error" role="alert">
        {loadError ?? "模板不存在"}
        <div style={{ marginTop: 12 }}>
          <button type="button" className="btn btn-sm" onClick={load}>
            重试
          </button>{" "}
          <Link href="/" className="btn btn-sm">
            返回模板列表
          </Link>
        </div>
      </div>
    );
  }

  return (
    <div className="detail-layout">
      {leftPanel(template)}
      {historyPanel}
    </div>
  );
}
