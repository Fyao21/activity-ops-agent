import { useEffect, useState } from "react";
import { useParams, Link } from "react-router-dom";
import { ArrowLeft, Calendar, Clock, ListChecks, Loader2, Tag } from "lucide-react";
import { getActivity } from "../api/client";
import LiquidGlassPanel from "../components/LiquidGlassPanel";

const STATUS_MAP = {
  0: { label: "未开始", cls: "bg-slate-100 text-slate-600" },
  1: { label: "进行中", cls: "bg-emerald-100 text-emerald-700" },
  2: { label: "已结束", cls: "bg-rose-100 text-rose-700" },
};

export default function ActivityDetailPage() {
  const { id } = useParams();
  const [activity, setActivity] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  useEffect(() => {
    let cancelled = false;
    (async () => {
      setLoading(true);
      setError("");
      try {
        const res = await getActivity(id);
        if (!cancelled) setActivity(res.data);
      } catch (err) {
        if (!cancelled) setError(err.message || "加载活动详情失败");
      } finally {
        if (!cancelled) setLoading(false);
      }
    })();
    return () => { cancelled = true; };
  }, [id]);

  const status = activity ? STATUS_MAP[activity.status] || STATUS_MAP[0] : null;

  return (
    <main className="yuu-page px-4 py-8 sm:px-8">
      <div className="mx-auto max-w-3xl">
        <Link
          to="/activities"
          className="liquid-action mb-6 inline-flex items-center gap-1.5 rounded-xl border border-white/60 bg-white/50 px-4 py-2 text-sm font-semibold text-[#53657d] hover:text-[#07111f]"
        >
          <ArrowLeft className="h-4 w-4" />
          返回活动列表
        </Link>

        {loading ? (
          <div className="flex items-center justify-center py-20">
            <Loader2 className="h-6 w-6 animate-spin text-cyan-500" />
            <span className="ml-2 text-[#53657d]">加载中...</span>
          </div>
        ) : error ? (
          <div className="rounded-xl bg-red-50/80 backdrop-blur border border-red-200 px-4 py-3 text-sm text-red-600">
            {error}
          </div>
        ) : activity ? (
          <LiquidGlassPanel
            cornerRadius={28}
            displacementScale={60}
            blurAmount={0.05}
            saturation={130}
            aberrationIntensity={1.5}
            elasticity={0.13}
            mode="prominent"
            overLight
          >
            <div className="p-6 sm:p-8">
              <div className="flex flex-col sm:flex-row sm:items-start sm:justify-between gap-3 mb-6">
                <div>
                  <div className="flex items-center gap-2 mb-2">
                    <span className="text-xs font-mono text-[#94a3b8]">ID: {activity.id}</span>
                    {status && (
                      <span className={`inline-flex items-center gap-1 rounded-full px-2.5 py-1 text-xs font-semibold ${status.cls}`}>
                        {status.label}
                      </span>
                    )}
                  </div>
                  <h1 className="text-2xl sm:text-3xl font-extrabold text-[#07111f]">{activity.activityName}</h1>
                </div>
              </div>

              <div className="grid gap-4 sm:grid-cols-2 mb-6">
                <div className="flex items-center gap-3 p-4 rounded-xl bg-white/40 backdrop-blur border border-white/50">
                  <Tag className="h-5 w-5 text-cyan-600" />
                  <div>
                    <p className="text-xs text-[#94a3b8] font-semibold">活动类型</p>
                    <p className="text-sm text-[#07111f] font-bold">{activity.activityType || "未设置"}</p>
                  </div>
                </div>
                <div className="flex items-center gap-3 p-4 rounded-xl bg-white/40 backdrop-blur border border-white/50">
                  <Calendar className="h-5 w-5 text-cyan-600" />
                  <div>
                    <p className="text-xs text-[#94a3b8] font-semibold">开始时间</p>
                    <p className="text-sm text-[#07111f] font-bold">{activity.startTime?.slice(0, 16) || "未设置"}</p>
                  </div>
                </div>
                <div className="flex items-center gap-3 p-4 rounded-xl bg-white/40 backdrop-blur border border-white/50">
                  <Clock className="h-5 w-5 text-cyan-600" />
                  <div>
                    <p className="text-xs text-[#94a3b8] font-semibold">结束时间</p>
                    <p className="text-sm text-[#07111f] font-bold">{activity.endTime?.slice(0, 16) || "未设置"}</p>
                  </div>
                </div>
                <div className="flex items-center gap-3 p-4 rounded-xl bg-white/40 backdrop-blur border border-white/50">
                  <Clock className="h-5 w-5 text-cyan-600" />
                  <div>
                    <p className="text-xs text-[#94a3b8] font-semibold">最后更新</p>
                    <p className="text-sm text-[#07111f] font-bold">{activity.updateTime?.slice(0, 16) || "-"}</p>
                  </div>
                </div>
              </div>

              {activity.ruleDesc && (
                <div className="p-4 rounded-xl bg-white/40 backdrop-blur border border-white/50">
                  <h3 className="text-sm font-bold text-[#07111f] mb-2">
                    <ListChecks className="h-4 w-4 inline mr-1" />
                    活动规则
                  </h3>
                  <p className="text-sm text-[#53657d] leading-relaxed whitespace-pre-wrap">{activity.ruleDesc}</p>
                </div>
              )}
            </div>
          </LiquidGlassPanel>
        ) : (
          <div className="flex flex-col items-center justify-center py-20 text-[#94a3b8]">
            <p className="text-lg font-semibold">活动不存在</p>
          </div>
        )}
      </div>
    </main>
  );
}
