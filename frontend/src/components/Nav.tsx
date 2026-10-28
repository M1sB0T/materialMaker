"use client";

import Link from "next/link";
import { useAuth } from "@/lib/auth-context";

export function Nav() {
  const { auth, openLogin, logout } = useAuth();

  return (
    <header className="nav">
      <div className="nav-inner">
        <Link href="/" className="nav-brand" aria-label="回到模板列表首页">
          🎨 素材工坊
        </Link>
        <nav className="nav-links" aria-label="主导航">
          <Link href="/">模板列表</Link>
          <Link href="/tasks">我的任务</Link>
        </nav>
        <div className="nav-user">
          {auth ? (
            <>
              <span className="nav-username" aria-label={`当前用户 ${auth.username}`}>
                👤 {auth.username}
              </span>
              <button type="button" className="btn btn-sm" onClick={logout}>
                退出
              </button>
            </>
          ) : (
            <button type="button" className="btn btn-sm btn-primary" onClick={openLogin}>
              登录
            </button>
          )}
        </div>
      </div>
    </header>
  );
}
