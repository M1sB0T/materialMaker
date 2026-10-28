"use client";

import { useEffect, useState, type FormEvent } from "react";
import { useAuth } from "@/lib/auth-context";

/** 登录对话框：输入用户名即可登录（演示用轻量身份方案） */
export function LoginDialog() {
  const { auth, loginOpen, closeLogin, login } = useAuth();
  const [username, setUsername] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  useEffect(() => {
    if (auth) setUsername(auth.username);
  }, [auth]);

  if (!loginOpen) return null;

  const onSubmit = async (e: FormEvent) => {
    e.preventDefault();
    if (!username.trim() || submitting) return;
    setSubmitting(true);
    setError(null);
    try {
      await login(username.trim());
    } catch (err) {
      setError(err instanceof Error ? err.message : "登录失败，请稍后再试");
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="dialog-backdrop" role="presentation">
      <div className="dialog" role="dialog" aria-modal="true" aria-labelledby="login-title">
        <h2 id="login-title">登录</h2>
        <p className="dialog-hint">输入用户名即可进入，首次登录将自动创建账号。</p>
        <form onSubmit={onSubmit}>
          <label className="field-label" htmlFor="login-username">
            用户名
          </label>
          <input
            id="login-username"
            className="text-input"
            value={username}
            onChange={(e) => setUsername(e.target.value)}
            placeholder="例如：demo"
            autoFocus
            maxLength={50}
          />
          {error && (
            <p className="error-text" role="alert">
              {error}
            </p>
          )}
          <div className="dialog-actions">
            <button type="button" className="btn" onClick={closeLogin}>
              取消
            </button>
            <button type="submit" className="btn btn-primary" disabled={submitting || !username.trim()}>
              {submitting ? "登录中…" : "登录"}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
