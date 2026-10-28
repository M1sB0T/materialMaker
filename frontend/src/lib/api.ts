import type { MaterialView } from "@/types";

/** 认证信息保存在 localStorage，刷新后自动恢复登录态 */
export interface AuthInfo {
  token: string;
  username: string;
}

const AUTH_KEY = "materialmaker.auth";

export class ApiError extends Error {
  status: number;

  constructor(status: number, message: string) {
    super(message);
    this.status = status;
  }
}

export function loadAuth(): AuthInfo | null {
  if (typeof window === "undefined") return null;
  const raw = window.localStorage.getItem(AUTH_KEY);
  if (!raw) return null;
  try {
    const parsed = JSON.parse(raw) as AuthInfo;
    return parsed && parsed.token ? parsed : null;
  } catch {
    return null;
  }
}

export function saveAuth(auth: AuthInfo): void {
  window.localStorage.setItem(AUTH_KEY, JSON.stringify(auth));
}

export function clearAuth(): void {
  window.localStorage.removeItem(AUTH_KEY);
}

/** 统一请求封装：自动携带 X-Auth-Token，非 2xx 抛 ApiError（保留后端 message） */
export async function api<T>(
  path: string,
  init?: RequestInit & { json?: unknown }
): Promise<T> {
  const auth = loadAuth();
  const headers = new Headers(init?.headers);
  if (auth) headers.set("X-Auth-Token", auth.token);
  let body = init?.body;
  if (init?.json !== undefined) {
    headers.set("Content-Type", "application/json");
    body = JSON.stringify(init.json);
  }
  const res = await fetch(path, { ...init, headers, body, cache: "no-store" });
  if (!res.ok) {
    let message = `请求失败（${res.status}）`;
    try {
      const data = (await res.json()) as { message?: string };
      if (data?.message) message = data.message;
    } catch {
      // 忽略非 JSON 响应体
    }
    throw new ApiError(res.status, message);
  }
  return (await res.json()) as T;
}

export function uploadMaterial(templateId: string, file: File): Promise<MaterialView> {
  const form = new FormData();
  form.append("templateId", templateId);
  form.append("file", file);
  return api<MaterialView>("/api/materials", { method: "POST", body: form });
}
