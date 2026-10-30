import type { Metadata } from "next";
import "./globals.css";
import { AuthProvider } from "@/lib/auth-context";
import { TasksProvider } from "@/lib/tasks-context";
import { Nav } from "@/components/Nav";
import { LoginDialog } from "@/components/LoginDialog";

export const metadata: Metadata = {
  title: "素材工坊 - 模板制作",
  description: "浏览模板、上传素材、提交制作任务并查看结果（演示项目）",
};

export default function RootLayout({ children }: { children: React.ReactNode }) {
  return (
    <html lang="zh-CN">
      <body>
        <AuthProvider>
          <TasksProvider>
            <Nav />
            <main className="page">{children}</main>
            <LoginDialog />
          </TasksProvider>
        </AuthProvider>
      </body>
    </html>
  );
}
