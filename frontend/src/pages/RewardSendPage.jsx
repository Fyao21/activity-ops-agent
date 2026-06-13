import { useState } from "react";
import { Gift, Send, RotateCcw, CheckCircle2, AlertCircle } from "lucide-react";
import { sendReward } from "../api/client";

const T = ["COUPON","POINT","CASH"];
const L = {COUPON:"优惠券",POINT:"积分",CASH:"现金"};

export default function RewardSendPage() {
  const [f, setF] = useState({ activityId:"", userId:"", rewardType:"COUPON", rewardAmount:"" });
  const [res, setRes] = useState(null);
  const [loading, setLoading] = useState(false);
  const [err, setErr] = useState("");
  const [ok, setOk] = useState(false);

  const sub = async e => {
    e.preventDefault();
    if(!f.activityId||!f.userId||!f.rewardAmount){ setErr("请完整填写"); return; }
    if(+f.rewardAmount<=0){ setErr("金额须>0"); return; }
    setErr(""); setOk(false); setLoading(true); setRes(null);
    try{ const r = await sendReward({activityId:+f.activityId,userId:+f.userId,rewardType:f.rewardType,rewardAmount:+f.rewardAmount}); setRes(r); setOk(true); }
    catch(e){ setErr(e.message); setRes({error:e.message}); }
    finally{ setLoading(false); }
  };

  const reset = () => { setF({activityId:"",userId:"",rewardType:"COUPON",rewardAmount:""}); setRes(null); setErr(""); setOk(false); };

  return (
    <div className="yuu-page px-4 py-7">
      <div className="yuu-orb yuu-orb-a" />
      <div className="max-w-[1100px] mx-auto relative z-10">
        <div className="flex items-center gap-3 mb-6 animate-fade-in-up">
          <div className="yuu-icon-chip" data-tone="emerald">
            <Gift className="h-5 w-5" />
          </div>
          <h1 style={{fontSize:"1.25rem",fontWeight:700,color:"var(--yuu-text)"}}>奖励发放</h1>
        </div>

        <div className="grid gap-5 md:grid-cols-3 animate-fade-in-up">
          <div className="md:col-span-2 yuu-card p-5">
            <form onSubmit={sub} className="flex flex-col gap-4">
              <div className="grid gap-4 sm:grid-cols-2">
                <div><label className="yuu-label">活动 ID</label><input type="number" value={f.activityId} onChange={e=>setF(f=>({...f,activityId:e.target.value}))} placeholder="例如：1" className="yuu-input"/></div>
                <div><label className="yuu-label">用户 ID</label><input type="number" value={f.userId} onChange={e=>setF(f=>({...f,userId:e.target.value}))} placeholder="例如：90001" className="yuu-input"/></div>
              </div>
              <div>
                <label className="yuu-label">奖励类型</label>
                <div className="flex gap-2">
                  {T.map(t=>(
                    <button key={t} type="button" onClick={()=>setF(f=>({...f,rewardType:t}))}
                      className={`yuu-seg-btn ${f.rewardType===t?"is-active":""}`}>{L[t]}</button>
                  ))}
                </div>
              </div>
              <div><label className="yuu-label">金额</label><input type="number" min="0.01" step="0.01" value={f.rewardAmount} onChange={e=>setF(f=>({...f,rewardAmount:e.target.value}))} placeholder="例如：10.00" className="yuu-input"/></div>
              {err && <div className="yuu-alert is-error"><AlertCircle className="h-3.5 w-3.5 flex-shrink-0"/>{err}</div>}
              {ok&&!err && <div className="yuu-alert is-success"><CheckCircle2 className="h-3.5 w-3.5 flex-shrink-0"/>发放成功</div>}
              <div className="flex gap-2">
                <button type="submit" disabled={loading} className="yuu-btn-primary flex-1"><Gift className="h-4 w-4"/>{loading?"发放中":"发放"}</button>
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
