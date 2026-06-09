import { useState } from "react";
import { useNavigate, Navigate } from "react-router-dom";
import { LogIn, Loader2, Shield, Sparkles } from "lucide-react";
import LiquidGlassPanel from "../components/LiquidGlassPanel";
import { useAuth } from "../features/auth/AuthContext";

export default function LoginPage() {
  const { login, isAuthenticated } = useAuth();
  const navigate = useNavigate();
  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");

  if (isAuthenticated) {
    return <Navigate to="/" replace />;
  }

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!username.trim() || !password.trim()) {
      setError("请输入用户名和密码");
      return;
    }
    setError("");
    setLoading(true);
    try {
      await login(username.trim(), password);
      navigate("/", { replace: true });
    } catch (err) {
      setError(err.message || "登录失败，请检查用户名和密码");
    } finally {
      setLoading(false);
    }
  };

  return (
    <main className="yuu-page flex items-center justify-center px-4 py-20">
      <div className="w-full max-w-md animate-fade-in-up">
        <LiquidGlassPanel
          cornerRadius={36}
          displacementScale={70}
          blurAmount={0.0625}
          saturation={140}
          aberrationIntensity={2}
          elasticity={0.15}
          mode="prominent"
          overLight
        >
          <div className="p-8">
            {/* Logo */}
            <div className="flex flex-col items-center mb-6">
              <div className="ai-chat-mark mb-4">
                <Shield className="h-5 w-5" />
              </div>
              <h1 className="text-2xl font-extrabold text-[#07111f]">Activity Ops Agent</h1>
              <p className="mt-1 text-sm text-[#53657d]">登录以管理活动运营数据</p>
            </div>

            <form onSubmit={handleSubmit} className="flex flex-col gap-4">
              <div>
                <label className="block text-sm font-semibold text-[#07111f] mb-1.5">用户名</label>
                <input
                  type="text"
                  value={username}
                  onChange={(e) => setUsername(e.target.value)}
                  placeholder="请输入用户名"
                  className="w-full rounded-xl border border-white/70 bg-white/50 backdrop-blur px-4 py-2.5 text-sm text-[#07111f] placeholder:text-slate-400 focus:outline-none focus:border-cyan-400 focus:ring-2 focus:ring-cyan-100 transition"
                  autoComplete="username"
                  disabled={loading}
                />
              </div>
              <div>
                <label className="block text-sm font-semibold text-[#07111f] mb-1.5">密码</label>
                <input
                  type="password"
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                  placeholder="请输入密码"
                  className="w-full rounded-xl border border-white/70 bg-white/50 backdrop-blur px-4 py-2.5 text-sm text-[#07111f] placeholder:text-slate-400 focus:outline-none focus:border-cyan-400 focus:ring-2 focus:ring-cyan-100 transition"
                  autoComplete="current-password"
                  disabled={loading}
                />
              </div>

              {error && (
                <div className="rounded-xl bg-red-50/80 backdrop-blur border border-red-200 px-4 py-2.5 text-sm text-red-600">
                  {error}
                </div>
              )}

              <button
                type="submit"
                disabled={loading}
                className="liquid-action mt-2 flex items-center justify-center gap-2 rounded-xl border border-white/60 bg-gradient-to-r from-cyan-500 to-blue-600 py-3 text-sm font-bold text-white shadow-lg shadow-cyan-200/40 hover:shadow-xl hover:shadow-cyan-300/50 disabled:opacity-50 disabled:cursor-not-allowed transition-all"
              >
                {loading ? (
                  <Loader2 className="h-4 w-4 animate-spin" />
                ) : (
                  <LogIn className="h-4 w-4" />
                )}
                {loading ? "登录中..." : "登录"}
              </button>
            </form>

            <p className="mt-6 text-center text-xs text-[#94a3b8]">
              <Sparkles className="inline h-3 w-3 mr-1" />
              Admin / Operator 角色均可登录使用
            </p>
          </div>
        </LiquidGlassPanel>
      </div>
    </main>
  );
}
