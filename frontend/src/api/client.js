const BASE_URL = "/api";

async function request(url, options = {}) {
  const token = localStorage.getItem("auth_token");
  const headers = {
    "Content-Type": "application/json",
    ...(token ? { Authorization: `Bearer ${token}` } : {}),
    ...options.headers,
  };

  const res = await fetch(`${BASE_URL}${url}`, {
    ...options,
    headers,
  });

  // Handle 401 by clearing token and redirecting to login; abort the request
  if (res.status === 401) {
    localStorage.removeItem("auth_token");
    localStorage.removeItem("auth_user");
    if (window.location.pathname !== "/login") {
      window.location.href = "/login";
    }
    throw new Error("登录已过期，请重新登录");
  }

  let data;
  try {
    data = await res.json();
  } catch {
    // Response body is not JSON (e.g. proxy HTML error page)
    data = {};
  }

  // Check both HTTP-level and business-level errors (code != 0 means business error)
  if (!res.ok || (data.code !== undefined && data.code !== 0)) {
    throw new Error(data.message || `请求失败 (${res.status})`);
  }

  return data;
}

// ===== Auth =====
export async function login(username, password) {
  return request("/auth/login", {
    method: "POST",
    body: JSON.stringify({ username, password }),
  });
}

// ===== Activities =====
export async function listActivities(page = 1, pageSize = 10) {
  return request(`/activity/list?page=${page}&pageSize=${pageSize}`);
}

export async function getActivity(id) {
  return request(`/activity/${id}`);
}

export async function createActivity(data) {
  return request("/activity/create", {
    method: "POST",
    body: JSON.stringify(data),
  });
}

export async function updateActivity(data) {
  return request("/activity/update", {
    method: "PUT",
    body: JSON.stringify(data),
  });
}

export async function participateActivity(data) {
  return request("/activity/participate", {
    method: "POST",
    body: JSON.stringify(data),
  });
}

// ===== Rewards =====
export async function sendReward(data) {
  return request("/reward/send", {
    method: "POST",
    body: JSON.stringify(data),
  });
}

// ===== Statistics =====
export async function queryStatistics(params) {
  const searchParams = new URLSearchParams();
  if (params.activityId) searchParams.set("activityId", params.activityId);
  if (params.startDate) searchParams.set("startDate", params.startDate);
  if (params.endDate) searchParams.set("endDate", params.endDate);
  return request(`/statistics/activity?${searchParams.toString()}`);
}

// ===== Agent (AI Query) =====
export async function agentQuery(question, userId) {
  return request("/agent/query", {
    method: "POST",
    body: JSON.stringify({
      question,
      userId: userId || null,
    }),
  });
}

// ===== Health =====
export async function healthCheck() {
  return request("/health");
}
