/**
 * 前端通过 Next.js 中间层代理访问 Java 后端：
 * 浏览器始终请求同源 /api/** 与 /files/**，由服务端转发到 Java，
 * 避免 CORS 问题；API_ORIGIN 可用环境变量覆盖（容器中为 http://backend:8080）。
 */
const apiOrigin = process.env.API_ORIGIN || "http://localhost:8080";

/** @type {import('next').NextConfig} */
const nextConfig = {
  output: "standalone",
  async rewrites() {
    return [
      { source: "/api/:path*", destination: `${apiOrigin}/api/:path*` },
      { source: "/files/:path*", destination: `${apiOrigin}/files/:path*` },
      { source: "/demo/:path*", destination: `${apiOrigin}/demo/:path*` },
    ];
  },
};

export default nextConfig;
