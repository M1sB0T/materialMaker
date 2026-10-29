"use client";

import { useEffect, useMemo, useState } from "react";
import Link from "next/link";
import { api } from "@/lib/api";
import type { TemplateView } from "@/types";

export default function TemplateListPage() {
  const [templates, setTemplates] = useState<TemplateView[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [category, setCategory] = useState<string>("");

  const load = () => {
    setLoading(true);
    setError(null);
    api<TemplateView[]>("/api/templates")
      .then((list) => {
        setTemplates(list);
        setLoading(false);
      })
      .catch((err: unknown) => {
        setError(err instanceof Error ? err.message : "模板加载失败");
        setLoading(false);
      });
  };

  useEffect(() => {
    load();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const categories = useMemo(
    () => Array.from(new Set(templates.map((t) => t.category))),
    [templates]
  );
  const filtered = category ? templates.filter((t) => t.category === category) : templates;

  return (
    <>
      {/* 页签胶囊：与示例稿第一行对应，作为板块导航 */}
      <div className="pillbar" role="tablist" aria-label="板块切换">
        <span className="pill pill-active" aria-current="page">
          模板
        </span>
        <Link href="/tasks" className="pill">
          我的任务
        </Link>
      </div>

      {/* 分类文字行：All / Trending 风格 */}
      {!loading && !error && categories.length > 0 && (
        <div className="catbar" role="tablist" aria-label="按分类筛选模板">
          <button
            type="button"
            className={`cattab ${category === "" ? "cattab-active" : ""}`}
            onClick={() => setCategory("")}
          >
            全部
          </button>
          {categories.map((c) => (
            <button
              key={c}
              type="button"
              className={`cattab ${category === c ? "cattab-active" : ""}`}
              onClick={() => setCategory(c)}
            >
              {c}
            </button>
          ))}
        </div>
      )}

      {loading && <p className="spinner-text">模板加载中…</p>}
      {error && (
        <div className="alert alert-error" role="alert">
          {error}{" "}
          <button type="button" className="btn btn-sm" onClick={load}>
            重试
          </button>
        </div>
      )}

      {!loading && !error && (
        <>
          {filtered.length === 0 ? (
            <div className="empty-state">
              <span className="empty-icon">🗂️</span>
              该分类下暂无模板
            </div>
          ) : (
            <div className="template-grid">
              {filtered.map((t) => (
                <Link
                  key={t.id}
                  href={`/templates/${t.id}`}
                  className="template-card"
                  aria-label={`查看模板 ${t.title}`}
                >
                  {/* eslint-disable-next-line @next/next/no-img-element */}
                  <img className="template-cover" src={t.coverUrl} alt={`模板封面：${t.title}`} />
                  <div className="template-overlay">
                    <h3 className="template-title">{t.title}</h3>
                  </div>
                </Link>
              ))}
            </div>
          )}
        </>
      )}
    </>
  );
}
