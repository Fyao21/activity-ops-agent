import { Link } from "react-router-dom";
import { BarChart3, Bot, CalendarPlus, ListChecks, Sparkles, TrendingUp } from "lucide-react";
import LiquidGlassPanel from "../components/LiquidGlassPanel";
import { ROUTES } from "../shared/routes";

const QUICK_ACTIONS = [
  {
    to: ROUTES.AI_CHAT,
    icon: Bot,
    title: "AI 智能查询",
    desc: "用自然语言查询活动数据，AI 自动生成 SQL 并返回结果",
    color: "text-cyan-600",
    bg: "from-cyan-50 to-blue-50",
  },
  {
    to: ROUTES.ACTIVITIES,
    icon: ListChecks,
    title: "活动管理",
    desc: "查看、创建和管理所有运营活动，支持完整的生命周期管理",
    color: "text-indigo-600",
    bg: "from-indigo-50 to-violet-50",
  },
  {
    to: ROUTES.STATISTICS,
    icon: TrendingUp,
    title: "数据统计",
    desc: "活动参与、奖励发放等多维度统计分析，实时数据看板",
    color: "text-emerald-600",
    bg: "from-emerald-50 to-teal-50",
  },
];

const FEATURES = [
  { icon: Sparkles, title: "Text-to-SQL", desc: "自然语言转 SQL 查询，安全校验 + 风险评分" },
  { icon: BarChart3, title: "实时统计", desc: "Redis Stream 异步统计，秒级数据更新" },
  { icon: CalendarPlus, title: "活动运营", desc: "完整活动生命周期：创建、参与、奖励发放" },
];

export default function HomePage() {
  return (
    <main className="yuu-page flex flex-col items-center px-4 py-12 sm:py-20">
      {/* Hero */}
      <div className="text-center max-w-3xl animate-fade-in-up">
        <div className="inline-flex items-center gap-2 rounded-full border border-cyan-200 bg-cyan-50/60 px-4 py-1.5 text-sm font-medium text-cyan-700 backdrop-blur">
          <Sparkles className="h-4 w-4" />
          AI-Powered Activity Operations
        </div>
        <h1 className="mt-6 text-4xl sm:text-5xl lg:text-6xl font-extrabold tracking-tight text-[#07111f] leading-tight">
          活动运营
          <span className="bg-gradient-to-r from-cyan-500 to-blue-600 bg-clip-text text-transparent">数据分析平台</span>
        </h1>
        <p className="mt-5 text-lg text-[#53657d] leading-relaxed max-w-2xl mx-auto">
          基于 Spring Boot + Redis Stream + FastAPI + LangChain
          实现自然语言转 SQL 查询、异步统计更新与问答记录审计
        </p>
      </div>

      {/* Quick Actions */}
      <div className="mt-14 grid w-full max-w-5xl gap-5 sm:grid-cols-3">
        {QUICK_ACTIONS.map(({ to, icon: Icon, title, desc, color, bg }) => (
          <Link key={to} to={to} className="group block">
            <LiquidGlassPanel
              cornerRadius={24}
              displacementScale={50}
              blurAmount={0.05}
              saturation={130}
              aberrationIntensity={1}
              elasticity={0.12}
              mode="subtle"
              overLight
            >
              <div className={`flex flex-col gap-3 p-5 rounded-3xl bg-gradient-to-br ${bg}`}>
                <div className={`flex h-11 w-11 items-center justify-center rounded-2xl bg-white/70 backdrop-blur shadow-sm ${color}`}>
                  <Icon className="h-5 w-5" />
                </div>
                <div>
                  <h3 className="text-base font-bold text-[#07111f]">{title}</h3>
                  <p className="mt-1 text-sm text-[#53657d] leading-relaxed">{desc}</p>
                </div>
                <span className={`text-sm font-semibold ${color} group-hover:underline`}>
                  开始使用 →
                </span>
              </div>
            </LiquidGlassPanel>
          </Link>
        ))}
      </div>

      {/* Features */}
      <div className="mt-16 w-full max-w-4xl">
        <h2 className="text-center text-2xl font-bold text-[#07111f]">核心能力</h2>
        <div className="mt-8 grid gap-4 sm:grid-cols-3">
          {FEATURES.map(({ icon: Icon, title, desc }) => (
            <div key={title} className="liquid-native-card rounded-2xl p-5 flex flex-col items-center text-center gap-2">
              <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-cyan-50 text-cyan-600">
                <Icon className="h-5 w-5" />
              </div>
              <h3 className="font-bold text-[#07111f]">{title}</h3>
              <p className="text-sm text-[#53657d]">{desc}</p>
            </div>
          ))}
        </div>
      </div>
    </main>
  );
}
