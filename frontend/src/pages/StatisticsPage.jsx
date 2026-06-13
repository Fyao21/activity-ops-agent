import { useCallback, useEffect, useState } from "react";
import { TrendingUp, Users, Gift, Activity, Loader2, BarChart3, RotateCcw } from "lucide-react";
import { queryStatistics } from "../api/client";

export default function StatisticsPage() {
  const [stats, setStats] = useState([]);
  const [loading, setLoading] = useState(true);
  const [err, setErr] = useState("");

  const load = useCallback(async () => {
    setLoading(true); setErr("");
    try { const r = await queryStatistics({}); setStats(r.data||[]); }
    catch(e){ setErr(e.message); }
    finally{ setLoading(false); }
  },[]);

  useEffect(()=>{ load(); },[load]);

  const ac = new Set(stats.map(s=>s.activityId).filter(Boolean)).size;
  const tp = stats.reduce((s,r)=>s+(r.participantCount??0),0);
  const tr = stats.reduce((s,r)=>s+(r.rewardCount??0),0);
  const ts = stats.reduce((s,r)=>s+(r.rewardSuccessCount??0),0);
  const rate = tr>0?`${Math.round(ts/tr*100)}%`:"—";

  const cards = [
    { icon:Activity, label:"活动总数", value:ac, tone:"cyan" },
    { icon:Users, label:"参与人次", value:tp, tone:"violet" },
    { icon:Gift, label:"奖励发放", value:tr, tone:"emerald" },
    { icon:TrendingUp, label:"成功率", value:rate, tone:"amber" },
  ];

  return (
    <div className="yuu-page px-4 py-6">
      <div className="yuu-orb yuu-orb-a" />
      <div className="max-w-[1100px] mx-auto relative z-10">
        <div className="flex items-center justify-between mb-6 animate-fade-in-up">
          <div className="flex items-center gap-3">
            <div className="yuu-icon-chip">
              <BarChart3 className="h-5 w-5" />
            </div>
            <div>
              <h1 style={{ fontSize:"1.25rem", fontWeight:700, color:"var(--yuu-text)" }}>数据统计</h1>
              <p className="text-xs" style={{ color:"var(--yuu-muted)" }}>活动数据看板</p>
            </div>
          </div>
          <button onClick={load} disabled={loading} className="yuu-btn-ghost"><RotateCcw className="h-4 w-4"/><span className="hidden sm:inline ml-1">刷新</span></button>
        </div>

        {err && <div className="yuu-alert is-error mb-4">{err}</div>}

        <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4 mb-8 animate-fade-in-up">
          {cards.map(({icon:I,label,value,tone})=>(
            <div key={label} className="yuu-card yuu-card-accent p-4 flex items-center gap-4" data-tone={tone}>
              <div className="yuu-icon-chip" data-tone={tone}>
                <I className="h-5 w-5" />
              </div>
              <div className="min-w-0">
                <p className="text-[10px] font-semibold uppercase tracking-wider" style={{color:"var(--yuu-muted)"}}>{label}</p>
                <p className="text-2xl font-extrabold tracking-tight" style={{color:"var(--yuu-text)"}}>{loading?"—":value}</p>
              </div>
            </div>
          ))}
        </div>

        {loading ? (
          <div className="flex justify-center py-12"><Loader2 className="h-5 w-5 animate-spin" style={{color:"var(--yuu-accent)"}}/></div>
        ) : stats.length > 0 ? (
          <div className="data-table-wrap animate-fade-in-up">
            <table className="data-table">
              <thead><tr>{["活动ID","参与人数","奖励发放","成功数","失败数","统计日期","更新时间"].map(h=><th key={h}>{h}</th>)}</tr></thead>
              <tbody>
                {stats.map((r,i)=>(
                  <tr key={i}>
                    <td><span className="font-mono font-semibold" style={{color:"var(--yuu-accent)"}}>#{r.activityId}</span></td>
                    <td>{r.participantCount??"-"}</td>
                    <td>{r.rewardCount??"-"}</td>
                    <td><span style={{color:"var(--emerald)",fontWeight:600}}>{r.rewardSuccessCount??"-"}</span></td>
                    <td><span style={{color:"var(--rose)",fontWeight:600}}>{r.rewardCount!=null&&r.rewardSuccessCount!=null?r.rewardCount-r.rewardSuccessCount:"-"}</span></td>
                    <td className="text-xs" style={{color:"var(--yuu-muted)"}}>{r.statDate??"-"}</td>
                    <td className="text-xs hidden sm:table-cell" style={{color:"var(--yuu-muted)"}}>{r.updateTime?.slice(0,16)??"-"}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        ) : (
          <div className="flex flex-col items-center py-12" style={{color:"var(--yuu-muted)"}}><p className="text-sm">暂无数据</p></div>
        )}
      </div>
    </div>
  );
}
