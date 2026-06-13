import { useState } from "react";
import { useNavigate } from "react-router-dom";
import { ArrowRight, Bot, ListChecks, Send, Sparkles, TrendingUp, Zap } from "lucide-react";
import { ROUTES } from "../shared/routes";

export default function HomePage() {
  const navigate = useNavigate();
  const [q, setQ] = useState("");
  const go = () => navigate(q.trim() ? `${ROUTES.AI_CHAT}?q=${encodeURIComponent(q.trim())}` : ROUTES.AI_CHAT);

  return (
    <div className="yuu-page min-h-[calc(100vh-60px)] flex items-center justify-center px-4 py-6 overflow-hidden">
      <div className="yuu-orb yuu-orb-a" />
      <div className="yuu-orb yuu-orb-b" />

      <div className="relative z-10 w-full max-w-[720px] text-center mx-auto animate-fade-in-up">
        <span className="yuu-hero-badge inline-flex items-center gap-1.5 rounded-full border px-3 py-1 text-[11px] font-bold mb-5 tracking-wide">
          <Zap className="h-3 w-3" /> ACTIVITY OPS AGENT
        </span>

        <h1 style={{ fontSize: "clamp(2.2rem, 5vw, 3.6rem)", fontWeight: 800, lineHeight: 1.08, letterSpacing: "-0.02em" }}>
          <span style={{ color: "var(--yuu-text)" }}>用自然语言</span><br />
          <span style={{ background: "linear-gradient(135deg, #06b6d4, #2563eb)", WebkitBackgroundClip: "text", WebkitTextFillColor: "transparent" }}>
            查询活动数据
          </span>
        </h1>

        <p className="mt-4 text-base leading-relaxed mx-auto" style={{ color: "var(--yuu-muted)", maxWidth: 460 }}>
          输入问题，AI 自动生成 SQL 并执行查询，安全校验 + 风险评分
        </p>

        <div className="mt-8">
          <div className="yuu-card flex items-center gap-2 p-1.5 pl-4" style={{ borderRadius: 16 }}>
            <Bot className="h-5 w-5 flex-shrink-0" style={{ color: "var(--yuu-accent)" }} />
            <input
              type="text" placeholder="例如：最近7天各活动的参与人数和奖励发放情况"
              value={q} onChange={e => setQ(e.target.value)}
              onKeyDown={e => { if (e.key === "Enter") go(); }}
              className="flex-1 border-0 bg-transparent text-sm outline-none py-2"
              style={{ color: "var(--yuu-text)" }}
            />
            <button onClick={go} className="yuu-btn-primary flex-shrink-0" style={{ borderRadius: 12 }}>
              <Send className="h-4 w-4" /> <span className="hidden sm:inline ml-1">查询</span>
            </button>
          </div>
        </div>

        <div className="flex flex-wrap justify-center gap-3 mt-5">
          {[
            { to: ROUTES.ACTIVITIES, icon: ListChecks, label: "活动管理" },
            { to: ROUTES.STATISTICS, icon: TrendingUp, label: "数据看板" },
            { to: ROUTES.PARTICIPATE, icon: Sparkles, label: "用户参与" },
          ].map(({ to, icon: I, label }) => (
            <button key={to} onClick={() => navigate(to)}
              className="yuu-list-row group cursor-pointer px-4 py-2.5">
              <I className="h-4 w-4 flex-shrink-0" style={{ color: "var(--yuu-accent)" }} />
              <span className="text-sm font-semibold" style={{ color: "var(--yuu-text)" }}>{label}</span>
              <ArrowRight className="h-3.5 w-3.5 opacity-40 group-hover:opacity-100 transition-opacity flex-shrink-0" style={{ color: "var(--yuu-accent)" }} />
            </button>
          ))}
        </div>
      </div>
    </div>
  );
}
