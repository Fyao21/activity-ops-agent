import { createContext, useCallback, useContext, useMemo, useState } from "react";
import { login as apiLogin } from "../../api/client";

const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const [user, setUser] = useState(() => {
    try {
      const stored = localStorage.getItem("auth_user");
      return stored ? JSON.parse(stored) : null;
    } catch {
      return null;
    }
  });

  const [token, setToken] = useState(() => localStorage.getItem("auth_token") || null);

  const login = useCallback(async (username, password) => {
    const res = await apiLogin(username, password);
    const result = res.data;
    if (!result) {
      throw new Error("服务器返回数据异常，请稍后重试");
    }
    const userData = {
      userId: result.userId,
      username: result.username || username,
      role: result.role || "OPERATOR",
    };
    const authToken = result.token;
    localStorage.setItem("auth_user", JSON.stringify(userData));
    localStorage.setItem("auth_token", authToken);
    setUser(userData);
    setToken(authToken);
    return userData;
  }, []);

  const logout = useCallback(() => {
    localStorage.removeItem("auth_user");
    localStorage.removeItem("auth_token");
    setUser(null);
    setToken(null);
  }, []);

  const isAuthenticated = !!token && !!user;

  const value = useMemo(
    () => ({ user, token, isAuthenticated, login, logout }),
    [user, token, isAuthenticated, login, logout]
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error("useAuth must be used within AuthProvider");
  return ctx;
}
