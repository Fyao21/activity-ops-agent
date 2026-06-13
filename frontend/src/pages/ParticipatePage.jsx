import { useState } from "react";
import { Users, Send, RotateCcw, CheckCircle2, AlertCircle } from "lucide-react";
import { participateActivity } from "../api/client";

const C = ["APP","H5","WEB"];

export default function ParticipatePage() {
  const [f, setF] = useState({ activityId:"", userId:"", channel:"APP" });
  const [res, setRes] = useState(null);
  const [loading, setLoading] = useState(false);
  const [err, setErr] = useState("");
  const [ok, setOk] = useState(false);

  const sub = async e => {
    e.preventDefault();
    if(!f.activityId||!f.userId){ setErr("活动ID和用户ID不能为空"); return; }
    setErr(""); setOk(false); setLoading(true); setRes(null);
    try{ const r = await participateActivity({activityId:+f.activityId,userId:+f.userId,channel:f.channel}); setRes(r); setOk(true); }
    catch(e){ setErr(e.message); setRes({error:e.message}); }
    finally{ setLoading(false); }
  };

  const reset = () => { setF({activityId:"",userId:"",channel:"APP"}); setRes(null); setErr(""); setOk(false); };

  return (
    <div className="yuu-page px-4 py-7">
      <div className="yuu-orb yuu-orb-a" />
      <div className="max-w-[1100px] mx-auto relative z-10">
        <div className="flex items-center gap-3 mb-6 animate-fade-in-up">
          <div className="yuu-icon-chip">
            <Users className="h-5 w-5" />
          </div>
          <h1 style={{fontSize:"1.25rem",fontWeight:700,color:"var(--yuu-text)"}}>用户参与</h1>
        </div>

        <div className="grid gap-5 md:grid-cols-3 animate-fade-in-up">
          <div className="md:col-span-2 yuu-card p-5">
            <form onSubmit={sub} className="flex flex-col gap-4">
              <div className="grid gap-4 sm:grid-cols-2">
                <div><label className="yuu-label">活动 ID</label><input type="number" value={f.activityId} onChange={e=>setF(f=>({...f,activityId:e.target.value}))} placeholder="例如：1" className="yuu-input"/></div>
                <div><label className="yuu-label">用户 ID</label><input type="number" value={f.userId} onChange={e=>setF(f=>({...f,userId:e.target.value}))} placeholder="例如：90001" className="yuu-input"/></div>
              </div>
              <div>
                <label className="yuu-label">渠道</label>
                <div className="flex gap-2">
                  {C.map(c=>(
                    <button key={c} type="button" onClick={()=>setF(f=>({...f,channel:c}))}
                      className={`yuu-seg-btn ${f.channel===c?"is-active":""}`}>{c}</button>
                  ))}
                </div>
              </div>
              {err && <div className="yuu-alert is-error"><AlertCircle className="h-3.5 w-3.5 flex-shrink-0"/>{err}</div>}
              {ok&&!err && <div className="yuu-alert is-success"><CheckCircle2 className="h-3.5 w-3.5 flex-shrink-0"/>参与成功</div>}
              <div className="flex gap-2">
                <button type="submit" disabled={loading} className="yuu-btn-primary flex-1"><Send className="h-4 w-4"/>{loading?"提交中":"提交"}</button>
                <button type="button" onClick={reset} className="yuu-btn-ghost"><RotateCcw className="h-4 w-4"/>重置</button>
              </div>
            </form>
          </div>
          <div className="yuu-card p-4">
            <p className="text-[10px] font-semibold uppercase tracking-wider mb-2" style={{color:"var(--yuu-muted)"}}>返回结果</p>
            <pre className="yuu-result-block" style={{minHeight:180,maxHeight:400}}>{res!=null?JSON.stringify(res,null,2):"// 响应"}</pre>
          </div>
        </div>
      </div>
    </div>
  );
}
