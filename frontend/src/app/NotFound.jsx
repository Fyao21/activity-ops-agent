import { Link } from "react-router-dom";
import { Home, SearchX } from "lucide-react";

export default function NotFound() {
  return (
    <main className="yuu-page flex flex-col items-center justify-center px-4 py-20 text-center">
      <div className="flex h-20 w-20 items-center justify-center rounded-3xl bg-rose-50 text-rose-400 mb-6">
        <SearchX className="h-10 w-10" />
      </div>
      <h1 className="text-4xl font-extrabold text-[#07111f]">404</h1>
      <p className="mt-2 text-[#53657d] text-lg">页面未找到</p>
      <p className="mt-1 text-[#94a3b8] text-sm">你访问的页面不存在或已被移除</p>
      <Link
        to="/"
        className="liquid-action mt-8 inline-flex items-center gap-2 rounded-xl border border-white/60 bg-white/60 px-5 py-2.5 text-sm font-semibold text-[#53657d] hover:text-[#07111f]"
      >
        <Home className="h-4 w-4" />
        返回首页
      </Link>
    </main>
  );
}
