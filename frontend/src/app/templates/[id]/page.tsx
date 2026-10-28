"use client";

import { useEffect, useMemo, useState, type FormEvent } from "react";
import Link from "next/link";
import { useParams, useRouter } from "next/navigation";
import { api } from "@/lib/api";
import { useAuth } from "@/lib/auth-context";
import { useTasksStore } from "@/lib/tasks-context";
import { UploadSlot } from "@/components/UploadSlot";
import type { MaterialView, TemplateView } from "@/types";

interface SubmitResponse {
  taskId: string;
  status: string;
}

export default function TemplateDetailPage() {
  const params = useParams<{ id: string }>();
  const templateId = params?.id ?? "";
  const router = useRouter();
  const { auth, openLogin, ready } = useAuth();
  const tasksStore = useTasksStore();

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

  const allFilled = useMemo(() => slots.length > 0 && slots.every((s) => s !== null), [slots]);

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
      // 受理成功：任务进入后台处理，立即开始跟踪状态
      setSubmittedTaskId(res.taskId);
      tasksStore.startTracking();
      router.push("/tasks?focus=" + res.taskId);
    } catch (err) {
      setSubmitError(err instanceof Error ? err.message : "提交失败，请稍后再试");
    } finally {
      setSubmitting(false);
    }
  };

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
    <>
      <h1 className="page-title">{template.title}</h1>
      <p className="page-subtitle">{template.category} · {template.description}</p>

      <div className="detail-layout">
        <div>
          {/* eslint-disable-next-line @next/next/no-img-element */}
          <img className="detail-cover" src={template.coverUrl} alt={`模板封面：${template.title}`} />
          <ul className="detail-requirements" style={{ marginTop: 12 }}>
            <li>📎 需要素材：{template.requiredImageCount} 张图片</li>
            <li>🏷️ 支持类型：{template.acceptedMimeTypes.join("、")}</li>
            <li>📦 大小限制：不超过 {(template.maxFileSizeBytes / 1024 / 1024).toFixed(1)}MB</li>
          </ul>
        </div>

        <form className="card" onSubmit={onSubmit}>
          <h2>上传素材</h2>
          <div className="slot-grid">
            {slots.map((slot, i) => (
              <UploadSlot
                key={i}
                index={i}
                templateId={template.id}
                acceptedMimeTypes={template.acceptedMimeTypes}
                maxFileSizeBytes={template.maxFileSizeBytes}
                material={slot}
                onChange={(m) =>
                  setSlots((prev) => prev.map((s, j) => (j === i ? m : s)))
                }
              />
            ))}
          </div>

          {submitError && (
            <p className="alert alert-error" role="alert">
              {submitError}
            </p>
          )}
          {submittedTaskId && (
            <p className="alert alert-info submit-banner" role="status">
              任务已受理（ID：{submittedTaskId.slice(0, 8)}…），正在跳转到任务页…
            </p>
          )}

          {!ready ? null : !auth ? (
            <button type="button" className="btn btn-primary btn-block" onClick={openLogin}>
              登录后提交制作任务
            </button>
          ) : (
            <button
              type="submit"
              className="btn btn-primary btn-block"
              disabled={submitting || !allFilled}
              aria-disabled={submitting || !allFilled}
            >
              {submitting ? "提交中…" : !allFilled ? `请上传全部 ${template.requiredImageCount} 张图片` : "提交制作任务"}
            </button>
          )}
        </form>
      </div>
    </>
  );
}
