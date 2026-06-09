import { useCallback, useEffect, useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import {
  Calendar,
  Clock,
  ListChecks,
  Plus,
  Search,
  ArrowRight,
  Loader2,
} from "lucide-react";
import { listActivities } from "../api/client";
import LiquidGlassPanel from "../components/LiquidGlassPanel";

const STATUS_MAP = {
  0: { label: "未开始", cls: "bg-slate-100 text-slate-600" },
  1: { label: "进行中", cls: "bg-emerald-100 text-emerald-700" },
  2: { label: "已结束", cls: "bg-rose-100 text-rose-700" },
};

function ActivityCard({ activity }) {
  const status = STATUS_MAP[activity.status] || STATUS_MAP[0];
  return (
    <Link to={`/activities/${activity.id}`} className="group block animate-fade-in-up">
      <LiquidGlassPanel
        cornerRadius={20}
        displacementScale={40}
        blurAmount={0.04}
        saturation={120}
        aberrationIntensity={1}
        elasticity={0.1}
        mode="subtle"
        overLight
      >
        <div className="p-5">
          <div className="flex items-start justify-between gap-3">
            <div>
              <h3 className="text-base font-bold text-[#07111f] group-hover:text-cyan-700 transition-colors">
                {activity.activityName}
              </h3>
              <p className="mt-1 text-sm text-[#53657d] line-clamp-2">
                {activity.ruleDesc || "暂无规则描述"}
              </p>
            </div>
            <span className={`inline-flex items-center gap-1 rounded-full px-2.5 py-1 text-xs font-semibold flex-shrink-0 ${status.cls}`}>
              {status.label}
            </span>
          </div>

          <div className="mt-4 flex items-center gap-4 text-xs text-[#94a3b8]">
            {activity.startTime && (
              <span className="flex items-center gap-1">
                <Calendar className="h-3 w-3" />
                {activity.startTime?.slice(0, 10)}
              </span>
            )}
            {activity.endTime && (
              <span className="flex items-center gap-1">
                <Clock className="h-3 w-3" />
                {activity.endTime?.slice(0, 10)}
              </span>
            )}
            <span className="flex items-center gap-1 ml-auto text-cyan-600 font-semibold opacity-0 group-hover:opacity-100 transition-opacity">
              详情 <ArrowRight className="h-3 w-3" />
            </span>
          </div>
        </div>
      </LiquidGlassPanel>
    </Link>
  );
}

export default function ActivitiesPage() {
  const navigate = useNavigate();
  const [activities, setActivities] = useState([]);
  const [total, setTotal] = useState(0);
  const [page, setPage] = useState(1);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [search, setSearch] = useState("");
  const pageSize = 12;

  const fetchActivities = useCallback(async (p) => {
    setLoading(true);
    setError("");
    try {
      const res = await listActivities(p, pageSize);
      setActivities(res.data?.records || []);
      setTotal(res.data?.total || 0);
    } catch (err) {
      setError(err.message || "加载活动列表失败");
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchActivities(page);
  }, [page, fetchActivities]);

  const filtered = search.trim()
    ? activities.filter((a) => a.activityName?.toLowerCase().includes(search.toLowerCase()))
    : activities;

  const totalPages = Math.ceil(total / pageSize);

  return (
    <main className="yuu-page px-4 py-8 sm:px-8">
      {/* Header */}
      <div className="mx-auto max-w-6xl">
        <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4 mb-8 animate-fade-in-up">
          <div>
            <h1 className="text-3xl font-extrabold text-[#07111f] flex items-center gap-3">
              <ListChecks className="h-7 w-7 text-cyan-600" />
              活动管理
            </h1>
            <p className="mt-1 text-[#53657d]">管理和查看所有运营活动</p>
          </div>
          <button
            onClick={() => navigate("/ai-chat", { state: { prompt: "帮我创建一个新活动" } })}
            className="liquid-action inline-flex items-center gap-2 rounded-xl border border-white/60 bg-gradient-to-r from-cyan-500 to-blue-600 px-5 py-2.5 text-sm font-bold text-white shadow-lg shadow-cyan-200/40"
          >
            <Plus className="h-4 w-4" />
            创建活动
          </button>
        </div>

        {error && (
          <div className="mb-4 rounded-xl bg-red-50/80 backdrop-blur border border-red-200 px-4 py-3 text-sm text-red-600">
            {error}
          </div>
        )}

        {/* Search */}
        <div className="mb-6 animate-fade-in-up">
          <div className="relative max-w-md">
            <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-slate-400" />
            <input
              type="text"
              placeholder="搜索活动名称..."
              value={search}
              onChange={(e) => setSearch(e.target.value)}
              className="w-full rounded-xl border border-white/70 bg-white/50 backdrop-blur pl-10 pr-4 py-2.5 text-sm text-[#07111f] placeholder:text-slate-400 focus:outline-none focus:border-cyan-400 focus:ring-2 focus:ring-cyan-100 transition"
            />
          </div>
        </div>

        {/* Activity grid */}
        {loading ? (
          <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
            {Array.from({ length: 6 }).map((_, i) => (
              <div key={i} className="liquid-native-card rounded-2xl p-5 h-40 skeleton" />
            ))}
          </div>
        ) : filtered.length > 0 ? (
          <>
            <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
              {filtered.map((a) => (
                <ActivityCard key={a.id} activity={a} />
              ))}
            </div>

            {/* Pagination */}
            {totalPages > 1 && (
              <div className="mt-8 flex items-center justify-center gap-2">
                <button
                  onClick={() => setPage((p) => Math.max(1, p - 1))}
                  disabled={page <= 1}
                  className="liquid-action rounded-xl border border-white/60 bg-white/50 px-4 py-2 text-sm font-semibold text-[#07111f] disabled:opacity-40"
                >
                  上一页
                </button>
                <span className="text-sm text-[#53657d] px-3">
                  {page} / {totalPages}
                </span>
                <button
                  onClick={() => setPage((p) => Math.min(totalPages, p + 1))}
                  disabled={page >= totalPages}
                  className="liquid-action rounded-xl border border-white/60 bg-white/50 px-4 py-2 text-sm font-semibold text-[#07111f] disabled:opacity-40"
                >
                  下一页
                </button>
              </div>
            )}
          </>
        ) : (
          <div className="flex flex-col items-center justify-center py-20 text-[#94a3b8]">
            <ListChecks className="h-12 w-12 mb-4 opacity-30" />
            <p className="text-lg font-semibold">暂无活动</p>
            <p className="text-sm mt-1">点击"创建活动"开始第一个活动</p>
          </div>
        )}
      </div>
    </main>
  );
}
