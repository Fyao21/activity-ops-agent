import { NavLink, Outlet } from "react-router-dom";
import { Sparkles, LogOut } from "lucide-react";
import { useAuth } from "../features/auth/AuthContext";
import { NAV_ITEMS } from "../shared/routes";

export default function AppShell() {
  const { logout, isAuthenticated, user } = useAuth();

  return (
    <div className="min-h-screen bg-white text-gray-900">
      <nav className="lab-nav sticky top-0 z-50 flex h-14 items-center justify-between px-5 sm:px-8">
        <NavLink
          to="/"
          className="lab-brand flex items-center gap-2 text-base font-semibold tracking-tight text-gray-900 transition-colors duration-150 hover:text-gray-500"
        >
          <Sparkles className="h-5 w-5 text-cyan-500" />
          ActivityOps
        </NavLink>

        <div className="flex items-center gap-0.5">
          {NAV_ITEMS.map(({ to, label, end }) => (
            <NavLink
              key={to}
              to={to}
              end={end}
              className={({ isActive }) =>
                `lab-nav-link liquid-action rounded-full px-3 py-1.5 text-sm font-medium transition-colors duration-150 sm:px-4 ${
                  isActive
                    ? "is-active bg-gray-100 text-gray-900"
                    : "text-gray-500 hover:bg-gray-50 hover:text-gray-900"
                }`
              }
            >
              {label}
            </NavLink>
          ))}

          {isAuthenticated ? (
            <div className="flex items-center gap-1 ml-2">
              <span className="hidden sm:inline text-xs text-[#94a3b8] font-medium px-2">
                {user?.username || user?.name || ""}
              </span>
              <button
                type="button"
                onClick={logout}
                className="liquid-action rounded-full px-3 py-1.5 text-sm font-medium text-gray-500 transition hover:text-gray-950 sm:px-4 flex items-center gap-1"
              >
                <LogOut className="h-3.5 w-3.5" />
                <span className="hidden sm:inline">退出</span>
              </button>
            </div>
          ) : (
            <NavLink
              to="/login"
              className="lab-nav-link liquid-action rounded-full px-4 py-1.5 text-sm font-medium text-gray-500 hover:bg-gray-50 hover:text-gray-900"
            >
              登录
            </NavLink>
          )}
        </div>
      </nav>

      <Outlet />
    </div>
  );
}
