export const ROUTES = {
  HOME: "/",
  LOGIN: "/login",
  AI_CHAT: "/ai-chat",
  ACTIVITIES: "/activities",
  ACTIVITY_DETAIL: "/activities/:id",
  PARTICIPATE: "/participate",
  REWARDS: "/rewards",
  STATISTICS: "/statistics",
};

export const NAV_ITEMS = [
  { to: ROUTES.HOME, label: "首页", end: true },
  { to: ROUTES.ACTIVITIES, label: "活动" },
  { to: ROUTES.PARTICIPATE, label: "参与" },
  { to: ROUTES.REWARDS, label: "奖励" },
  { to: ROUTES.AI_CHAT, label: "AI 助手" },
  { to: ROUTES.STATISTICS, label: "统计" },
];
