import { useCallback, useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { ArrowRight, Calendar, ListChecks, Plus, Search, Loader2, X } from "lucide-react";
import { listActivities, createActivity } from "../api/client";

const SM = { 0: { l: "未开始", c: "status-badge-pending" }, 1: { l: "进行中", c: "status-badge-active" }, 2: { l: "已结束", c: "status-badge-ended" } };
const TYPES = ["NEW_USER","RECALL","FLASH_SALE","MEMBER_ACTIVE","SALES_PROMOTION"];

export default function ActivitiesPage() {
  const [list, setList] = useState([]);
  const [total, setTotal] = useState(0);
  const [page, setPage] = useState(1);
  const [loading, setLoading] = useState(true);
  const [err, setErr] = useState("");
  const [search, setSearch] = useState("");
  const [show, setShow] = useState(false);
  const [form, setForm] = useState({ activityName:"", activityType:"NEW_USER", startTime:"", endTime:"", status:0, ruleDesc:"" });
  const [creating, setCreating] = useState(false);
  const [cErr, setCErr] = useState("");
  const ps = 10;

  const fetch = useCallback(async (p) => {
    setLoading(true); setErr("");
    try { const r = await listActivities(p, ps); setList(r.data?.records||[]); setTotal(r.data?.total||0); }
    catch(e){ setErr(e.message||"加载失败"); }
    finally{ setLoading(false); }
  },[]);

  useEffect(()=>{ fetch(page); },[page, fetch]);

  const filtered = search.trim() ? list.filter(a=>a.activityName?.toLowerCase().includes(search.toLowerCase())) : list;
  const tp = Math.ceil(total/ps);

  return (
    <div className="yuu-page px-4 py-6">
      <div className="yuu-orb yuu-orb-a" />
      <div className="max-w-[1100px] mx-auto relative z-10">
        {/* Header */}
        <div className="flex items-center justify-between gap-3 mb-5 animate-fade-in-up">
          <div className="flex items-center gap-3">
            <div className="yuu-icon-chip">
              <ListChecks className="h-5 w-5" />
            </div>
            <div>
              <h1 style={{ fontSize:"1.25rem", fontWeight:700, color:"var(--yuu-text)" }}>活动管理</h1>
              <p className="text-xs" style={{ color:"var(--yuu-muted)" }}>共 {total} 个活动</p>
            </div>
          </div>
          <button onClick={()=>setShow(v=>!v)} className="yuu-btn-primary"><Plus className="h-4 w-4"/>{show?"取消":"创建"}</button>
        </div>

        {/* Create form */}
        {show && (
          <div className="mb-5 yuu-card p-5 animate-fade-in-up">
            <form onSubmit={async e=>{
              e.preventDefault();
              if(!form.activityName.trim()){ setCErr("请输入活动名称"); return; }
              setCreating(true); setCErr("");
              try{ await createActivity({...form, startTime:form.startTime?form.startTime+":00":undefined, endTime:form.endTime?form.endTime+":00":undefined}); setShow(false); setForm({activityName:"",activityType:"NEW_USER",startTime:"",endTime:"",status:0,ruleDesc:""}); fetch(1); setPage(1); }
              catch(e){ setCErr(e.message||"创建失败"); }
              finally{ setCreating(false); }
            }} className="grid gap-3 sm:grid-cols-2">
              <div className="sm:col-span-2"><label className="yuu-label">名称 *</label><input value={form.activityName} onChange={e=>setForm(f=>({...f,activityName:e.target.value}))} placeholder="活动名称" className="yuu-input"/></div>
              <div><label className="yuu-label">类型</label><select value={form.activityType} onChange={e=>setForm(f=>({...f,activityType:e.target.value}))} className="yuu-select">{TYPES.map(t=><option key={t} value={t}>{t}</option>)}</select></div>
              <div><label className="yuu-label">状态</label><select value={form.status} onChange={e=>setForm(f=>({...f,status:+e.target.value}))} className="yuu-select"><option value={0}>未开始</option><option value={1}>进行中</option><option value={2}>已结束</option></select></div>
              <div><label className="yuu-label">开始</label><input type="datetime-local" value={form.startTime} onChange={e=>setForm(f=>({...f,startTime:e.target.value}))} className="yuu-input"/></div>
              <div><label className="yuu-label">结束</label><input type="datetime-local" value={form.endTime} onChange={e=>setForm(f=>({...f,endTime:e.target.value}))} className="yuu-input"/></div>
              <div className="sm:col-span-2"><label className="yuu-label">规则描述</label><textarea value={form.ruleDesc} onChange={e=>setForm(f=>({...f,ruleDesc:e.target.value}))} rows={2} className="yuu-input" style={{resize:"vertical"}}/></div>
              {cErr && <div className="yuu-alert is-error sm:col-span-2">{cErr}</div>}
              <div className="sm:col-span-2 flex gap-2"><button type="submit" disabled={creating} className="yuu-btn-primary"><Plus className="h-4 w-4"/>{creating?"创建中":"确认"}</button><button type="button" onClick={()=>{setShow(false);setCErr("");}} className="yuu-btn-ghost"><X className="h-4 w-4"/>取消</button></div>
            </form>
          </div>
        )}

        {err && <div className="yuu-alert is-error mb-4">{err}</div>}

        {/* Search */}
        <div className="relative mb-4" style={{maxWidth:320}}><Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4" style={{color:"var(--yuu-muted)"}}/><input placeholder="搜索活动..." value={search} onChange={e=>setSearch(e.target.value)} className="yuu-input" style={{paddingLeft:"2.5rem"}}/></div>

        {/* List */}
        {loading ? (
          <div className="space-y-2">{Array.from({length:4}).map((_,i)=><div key={i} className="skeleton rounded-xl" style={{height:64}}/>)}</div>
        ) : filtered.length > 0 ? (
          <>
            <div className="flex flex-col gap-2">
              {filtered.map(a=>{
                const s = SM[a.status]||SM[0];
                return (
                  <Link key={a.id} to={`/activities/${a.id}`} className="group animate-fade-in-up">
                    <div className="yuu-list-row">
                      <div className="w-2 h-2 rounded-full flex-shrink-0" style={{background:a.status===1?"var(--emerald)":a.status===2?"var(--yuu-muted)":"var(--amber)"}}/>
                      <div className="min-w-0 flex-1">
                        <div className="flex items-center gap-2">
                          <span className="text-sm font-semibold truncate" style={{color:"var(--yuu-text)"}}>{a.activityName}</span>
                          <span className={`status-badge flex-shrink-0 text-[10px] ${s.c}`}>{s.l}</span>
                        </div>
                        <p className="text-xs mt-0.5 truncate" style={{color:"var(--yuu-muted)"}}>{a.ruleDesc||"暂无描述"}</p>
                      </div>
                      <span className="hidden sm:flex items-center gap-1 text-[11px] flex-shrink-0" style={{color:"var(--yuu-muted)"}}><Calendar className="h-3 w-3"/>{a.startTime?.slice(0,10)||"—"}</span>
                      <ArrowRight className="h-3.5 w-3.5 flex-shrink-0 opacity-0 group-hover:opacity-100" style={{color:"var(--yuu-accent)"}}/>
                    </div>
                  </Link>
                );
              })}
            </div>
            {tp>1 && <div className="mt-6 flex justify-center gap-2"><button onClick={()=>setPage(p=>Math.max(1,p-1))} disabled={page<=1} className="yuu-btn-ghost" style={{opacity:page<=1?.4:1}}>上一页</button><span className="text-xs px-3 self-center" style={{color:"var(--yuu-muted)"}}>{page}/{tp}</span><button onClick={()=>setPage(p=>Math.min(tp,p+1))} disabled={page>=tp} className="yuu-btn-ghost" style={{opacity:page>=tp?.4:1}}>下一页</button></div>}
          </>
        ) : (
          <div className="flex flex-col items-center justify-center py-16" style={{color:"var(--yuu-muted)"}}><ListChecks className="h-10 w-10 mb-3 opacity-20"/><p className="text-sm">暂无活动</p></div>
        )}
      </div>
    </div>
  );
}
