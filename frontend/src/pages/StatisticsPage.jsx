import { useCallback, useEffect, useState } from "react";
import {
  TrendingUp,
  Users,
  Gift,
  Activity,
  Loader2,
  Calendar,
  BarChart3,
} from "lucide-react";
import { queryStatistics } from "../api/client";
import LiquidGlassPanel from "../components/LiquidGlassPanel";

function StatCard({ icon: Icon, title, value, subtitle, color, bg }) {
  return (
    <LiquidGlassPanel
      cornerRadius={20}
      displacementScale={40}
      blurAmount={0.05}
      saturation={120}
      aberrationIntensity={1}
      elasticity={0.1}
      mode="subtle"
      overLight
    >
      <div className="p-5 flex items-start gap-4">
        <div className={`flex h-11 w-11 items-center justify-center rounded-2xl ${bg} ${color} flex-shrink-0`}>
          <Icon className="h-5 w-5" />
        </div>
        <div>
          <p className="text-sm font-semibold text-[#53657d]">{title}</p>
          <p className="mt-1 text-2xl font-extrabold text-[#07111f]">{value}</p>
          {subtitle && <p className="mt-0.5 text-xs text-[#94a3b8]">{subtitle}</p>}
        </div>
      </div>
    </LiquidGlassPanel>
  );
}

export default function StatisticsPage() {
  const [stats, setStats] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  const fetchStats = useCallback(async () => {
    setLoading(true);
    setError("");
    try {
      const res = await queryStatistics({});
      setStats(res.data || []);
    } catch (err) {
      setError(err.message || "加载统计数据失败");
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchStats();
  }, [fetchStats]);

  // Calculate aggregate values (use ?? since 0 is a valid count)
  const totalParticipants = stats.reduce((sum, s) => sum + (s.participantCount ?? 0), 0);
  const totalRewards = stats.reduce((sum, s) => sum + (s.rewardCount ?? 0), 0);
  const totalRewardSuccess = stats.reduce((sum, s) => sum + (s.rewardSuccessCount ?? 0), 0);
  const uniqueActivityCount = new Set(stats.map((s) => s.activityId).filter(Boolean)).size;

  return (
    <main className="yuu-page px-4 py-8 sm:px-8">
      <div className="mx-auto max-w-6xl">
        {/* Header */}
        <div className="mb-8 animate-fade-in-up">
          <h1 className="text-3xl font-extrabold text-[#07111f] flex items-center gap-3">
            <BarChart3 className="h-7 w-7 text-cyan-600" />
            数据统计
          </h1>
          <p className="mt-1 text-[#53657d]">活动参与与奖励发放数据概览</p>
        </div>

        {error && (
          <div className="mb-6 rounded-xl bg-red-50/80 backdrop-blur border border-red-200 px-4 py-3 text-sm text-red-600">
            {error}
          </div>
        )}

        {/* Summary cards */}
        <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4 mb-8">
          <StatCard
            icon={Activity}
            title="活动总数"
            value={uniqueActivityCount}
            subtitle="当前统计范围"
            color="text-cyan-600"
            bg="bg-cyan-50/80"
          />
          <StatCard
            icon={Users}
            title="总参与人次"
            value={totalParticipants}
            subtitle="所有活动累计"
            color="text-indigo-600"
            bg="bg-indigo-50/80"
          />
          <StatCard
            icon={Gift}
            title="总奖励发放"
            value={totalRewards}
            subtitle="成功 + 失败合计"
            color="text-emerald-600"
            bg="bg-emerald-50/80"
          />
          <StatCard
            icon={TrendingUp}
            title="成功率"
            value={totalRewards > 0
              ? `${Math.round(totalRewardSuccess / totalRewards * 100)}%`
              : "N/A"}
            subtitle="奖励发放成功率"
            color="text-amber-600"
            bg="bg-amber-50/80"
          />
        </div>

        {/* Detail table */}
        {loading ? (
          <div className="flex items-center justify-center py-16">
            <Loader2 className="h-6 w-6 animate-spin text-cyan-500" />
            <span className="ml-2 text-[#53657d]">加载中...</span>
          </div>
        ) : stats.length > 0 ? (
          <div className="data-table-wrap animate-fade-in-up">
            <table className="data-table">
              <thead>
                <tr>
                  <th>活动 ID</th>
                  <th>参与人数</th>
                  <th>奖励发放数</th>
                  <th>奖励成功数</th>
                  <th>奖励失败数</th>
                  <th>统计日期</th>
                  <th>更新时间</th>
                </tr>
              </thead>
              <tbody>
                {stats.map((row, i) => (
                  <tr key={i}>
                    <td>
                      <span className="font-mono text-cyan-700 font-semibold">#{row.activityId}</span>
                    </td>
                    <td>{row.participantCount != null ? row.participantCount : "-"}</td>
                    <td>{row.rewardCount != null ? row.rewardCount : "-"}</td>
                    <td>
                      <span className="text-emerald-600 font-semibold">
                        {row.rewardSuccessCount != null ? row.rewardSuccessCount : "-"}
                      </span>
                    </td>
                    <td>
                      <span className="text-rose-600 font-semibold">
                        {row.rewardCount != null && row.rewardSuccessCount != null
                          ? row.rewardCount - row.rewardSuccessCount
                          : "-"}
                      </span>
                    </td>
                    <td className="text-xs text-[#94a3b8]">{row.statDate ?? "-"}</td>
                    <td className="text-xs text-[#94a3b8]">{row.updateTime?.slice(0, 16) ?? "-"}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        ) : (
          <div className="flex flex-col items-center justify-center py-16 text-[#94a3b8]">
            <Calendar className="h-12 w-12 mb-4 opacity-30" />
            <p className="text-lg font-semibold">暂无统计数据</p>
            <p className="text-sm mt-1">活动产生数据后会自动同步</p>
          </div>
        )}

        {/* Refresh button */}
        <div className="mt-6 text-center">
          <button
            onClick={fetchStats}
            disabled={loading}
            className="liquid-action inline-flex items-center gap-2 rounded-xl border border-white/60 bg-white/60 px-5 py-2.5 text-sm font-semibold text-[#53657d] hover:text-[#07111f] disabled:opacity-50"
          >
            {loading ? <Loader2 className="h-4 w-4 animate-spin" /> : <TrendingUp className="h-4 w-4" />}
            刷新数据
          </button>
        </div>
      </div>
    </main>
  );
}
