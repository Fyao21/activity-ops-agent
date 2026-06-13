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
    <main className="yuu-page flex items-center justify-center px-6 sm:px-12 py-20 overflow-hidden">
      <div className="yuu-orb yuu-orb-a" aria-hidden="true" />
      <div className="yuu-orb yuu-orb-b" aria-hidden="true" />

      <div className="w-full max-w-md animate-fade-in-up" style={{ zIndex: 2 }}>
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
            <div className="flex flex-col items-center mb-8">
              <div className="ai-chat-mark mb-4">
                <Shield className="h-5 w-5" />
              </div>
              <h1
                style={{
                  fontSize: "clamp(1.6rem, 4vw, 2.2rem)",
                  fontWeight: 800,
                  color: "var(--yuu-text)",
                  letterSpacing: "-0.01em",
                }}
              >
                Activity Ops
              </h1>
              <p className="mt-1 text-sm" style={{ color: "var(--yuu-muted)" }}>
                登录以管理活动运营数据
              </p>
            </div>

            <form onSubmit={handleSubmit} className="flex flex-col gap-4">
              <div>
                <label className="yuu-label">用户名</label>
                <input
                  type="text"
                  value={username}
                  onChange={(e) => setUsername(e.target.value)}
                  placeholder="请输入用户名"
                  className="yuu-input"
                  autoComplete="username"
                  disabled={loading}
                />
              </div>
              <div>
                <label className="yuu-label">密码</label>
                <input
                  type="password"
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                  placeholder="请输入密码"
                  className="yuu-input"
                  autoComplete="current-password"
                  disabled={loading}
                />
              </div>

              {error && <div className="yuu-alert is-error">{error}</div>}

              <button
                type="submit"
                disabled={loading}
                className="yuu-btn-primary mt-2 w-full"
              >
                {loading ? (
                  <Loader2 className="h-4 w-4 animate-spin" />
                ) : (
                  <LogIn className="h-4 w-4" />
                )}
                {loading ? "登录中..." : "登录"}
              </button>
            </form>

            <p
              className="mt-6 text-center text-xs"
              style={{ color: "var(--yuu-muted)" }}
            >
              <Sparkles className="inline h-3 w-3 mr-1" />
              Admin / Operator 角色均可登录使用
            </p>
          </div>
        </LiquidGlassPanel>
      </div>
    </main>
  );
}
