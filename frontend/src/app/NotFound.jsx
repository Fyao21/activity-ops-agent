import { Link } from "react-router-dom";
import { Home, SearchX } from "lucide-react";

export default function NotFound() {
  return (
    <main className="yuu-page flex flex-col items-center justify-center px-4 py-20 text-center">
      <div
        className="flex h-20 w-20 items-center justify-center rounded-3xl mb-6"
        style={{
          border: "1px solid var(--yuu-border)",
          background: "var(--yuu-panel)",
          color: "var(--color-high)",
          backdropFilter: "blur(18px)",
        }}
      >
        <SearchX className="h-10 w-10" />
      </div>
      <h1
        style={{
          fontSize: "clamp(3rem, 8vw, 5rem)",
          fontWeight: 800,
          color: "var(--yuu-text)",
        }}
      >
        404
      </h1>
      <p className="mt-2 text-lg" style={{ color: "var(--yuu-muted)" }}>页面未找到</p>
      <p className="mt-1 text-sm" style={{ color: "var(--yuu-muted)", opacity: 0.7 }}>你访问的页面不存在或已被移除</p>
      <Link
        to="/"
        className="yuu-btn-primary mt-8"
      >
        <Home className="h-4 w-4" />
        返回首页
      </Link>
    </main>
  );
}
