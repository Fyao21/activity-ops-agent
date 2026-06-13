import { NavLink, Outlet } from "react-router-dom";
import { Sparkles, LogOut, Moon, Sun, Activity } from "lucide-react";
import { useAuth } from "../features/auth/AuthContext";
import { useTheme } from "../features/theme/ThemeContext";
import { NAV_ITEMS } from "../shared/routes";

export default function AppShell() {
  const { logout, isAuthenticated, user } = useAuth();
  const { toggleTheme, isDark } = useTheme();

  return (
    <div className="min-h-screen" style={{ background: "var(--yuu-bg)", color: "var(--yuu-text)" }}>
      <nav className="lab-nav sticky top-0 z-50">
        <NavLink
          to="/"
          className="lab-brand"
        >
          <div className="lab-brand-icon">
            <Activity className="h-4 w-4" />
          </div>
          ActivityOps
        </NavLink>

        <div className="lab-nav-links">
          {NAV_ITEMS.map(({ to, label, end }) => (
            <NavLink
              key={to}
              to={to}
              end={end}
              className={({ isActive }) =>
                `lab-nav-link ${isActive ? "is-active" : ""}`
              }
            >
              {label}
            </NavLink>
          ))}
        </div>

        <div className="lab-nav-user">
          <button
            type="button"
            onClick={toggleTheme}
            className="theme-toggle"
            aria-label={isDark ? "切换到亮色模式" : "切换到暗色模式"}
          >
            {isDark ? <Sun className="h-4 w-4" /> : <Moon className="h-4 w-4" />}
          </button>

          {isAuthenticated ? (
            <>
              <span className="hidden sm:inline text-xs font-semibold" style={{ color: "var(--yuu-muted)" }}>
                {user?.username || user?.name || ""}
              </span>
              <button
                type="button"
                onClick={logout}
                className="lab-logout-btn"
              >
                <LogOut className="h-3.5 w-3.5" />
                <span className="hidden sm:inline">退出</span>
              </button>
            </>
          ) : (
            <NavLink to="/login" className="lab-login-link">
              <Sparkles className="h-3.5 w-3.5" />
              登录
            </NavLink>
          )}
        </div>
      </nav>

      <Outlet />
    </div>
  );
}
