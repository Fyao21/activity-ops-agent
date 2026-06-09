export const ROUTES = {
  HOME: "/",
  LOGIN: "/login",
  AI_CHAT: "/ai-chat",
  ACTIVITIES: "/activities",
  ACTIVITY_DETAIL: "/activities/:id",
  STATISTICS: "/statistics",
  REWARDS: "/rewards",
};

export const NAV_ITEMS = [
  { to: ROUTES.HOME, label: "首页", end: true },
  { to: ROUTES.ACTIVITIES, label: "活动管理" },
  { to: ROUTES.AI_CHAT, label: "AI 助手" },
  { to: ROUTES.STATISTICS, label: "数据统计" },
];
