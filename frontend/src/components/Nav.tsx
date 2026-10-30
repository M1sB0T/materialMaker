"use client";

import Link from "next/link";
import { useAuth } from "@/lib/auth-context";

export function Nav() {
  const { auth, openLogin, logout } = useAuth();

  return (
    <header className="nav">
      <div className="nav-inner">
        <Link href="/" className="nav-brand" aria-label="回到模板列表首页">
          <span className="nav-logo" aria-hidden>
            🎨
          </span>
          素材工坊
        </Link>
        <div className="nav-user">
          {auth ? (
            <>
              <span className="nav-username" aria-label={`当前用户 ${auth.username}`}>
                {auth.username}
              </span>
              <button type="button" className="nav-logout" onClick={logout}>
                退出
              </button>
            </>
          ) : (
            <button type="button" className="nav-logout" onClick={openLogin}>
              登录
            </button>
          )}
        </div>
      </div>
    </header>
  );
}
