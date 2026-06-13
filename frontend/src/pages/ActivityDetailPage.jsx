import { useEffect, useState } from "react";
import { useParams, Link } from "react-router-dom";
import { ArrowLeft, Calendar, Clock, ListChecks, Loader2, Tag } from "lucide-react";
import { getActivity } from "../api/client";

const SM = {0:{l:"未开始",c:"status-badge-pending"},1:{l:"进行中",c:"status-badge-active"},2:{l:"已结束",c:"status-badge-ended"}};

export default function ActivityDetailPage() {
  const { id } = useParams();
  const [a, setA] = useState(null);
  const [loading, setLoading] = useState(true);
  const [err, setErr] = useState("");

  useEffect(()=>{
    let cancel=false;
    (async()=>{
      setLoading(true); setErr("");
      try{ const r = await getActivity(id); if(!cancel) setA(r.data); }
      catch(e){ if(!cancel) setErr(e.message); }
      finally{ if(!cancel) setLoading(false); }
    })();
    return ()=>{ cancel=true; };
  },[id]);

  const s = a ? SM[a.status]||SM[0] : null;

  return (
    <div className="yuu-page px-4 py-7">
      <div className="yuu-orb yuu-orb-b" />
      <div className="max-w-[800px] mx-auto relative z-10">
        <Link to="/activities" className="yuu-btn-ghost mb-5 inline-flex items-center gap-1"><ArrowLeft className="h-4 w-4"/>返回</Link>

        {loading ? (
          <div className="flex justify-center py-16"><Loader2 className="h-5 w-5 animate-spin" style={{color:"var(--yuu-accent)"}}/></div>
        ) : err ? (
          <div className="yuu-alert is-error">{err}</div>
        ) : a ? (
          <div className="animate-fade-in-up">
            <div className="mb-6">
              <div className="flex items-center gap-2 mb-2">
                <span className="text-xs font-mono" style={{color:"var(--yuu-muted)"}}>#{a.id}</span>
                {s && <span className={`status-badge text-[10px] ${s.c}`}>{s.l}</span>}
              </div>
              <h1 style={{fontSize:"clamp(1.4rem,3vw,2rem)",fontWeight:800,color:"var(--yuu-text)"}}>{a.activityName}</h1>
            </div>

            <div className="grid gap-3 sm:grid-cols-2 mb-6">
              {[{i:Tag,l:"类型",v:a.activityType},{i:Calendar,l:"开始时间",v:a.startTime?.slice(0,16)},{i:Clock,l:"结束时间",v:a.endTime?.slice(0,16)},{i:Clock,l:"最后更新",v:a.updateTime?.slice(0,16)||"-"}].map(({i:I,l,v})=>(
                <div key={l} className="yuu-card flex items-center gap-3 px-4 py-3">
                  <I className="h-4 w-4 flex-shrink-0" style={{color:"var(--yuu-accent)"}}/>
                  <div className="min-w-0"><p className="text-[10px] uppercase tracking-wider" style={{color:"var(--yuu-muted)"}}>{l}</p><p className="text-sm font-semibold truncate" style={{color:"var(--yuu-text)"}}>{v||"未设置"}</p></div>
                </div>
              ))}
            </div>

            {a.ruleDesc && (
              <div className="yuu-card p-5">
                <h3 className="text-xs font-semibold mb-2 flex items-center gap-1.5" style={{color:"var(--yuu-text)"}}><ListChecks className="h-4 w-4" style={{color:"var(--yuu-accent)"}}/>活动规则</h3>
                <p className="text-sm leading-relaxed whitespace-pre-wrap" style={{color:"var(--yuu-muted)"}}>{a.ruleDesc}</p>
              </div>
            )}
          </div>
        ) : (
          <div className="flex flex-col items-center py-16" style={{color:"var(--yuu-muted)"}}><p className="text-sm">不存在</p></div>
        )}
      </div>
    </div>
  );
}
