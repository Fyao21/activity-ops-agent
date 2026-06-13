import { createBrowserRouter } from "react-router-dom";
import { lazy, Suspense } from "react";

import AppShell from "./AppShell";
import NotFound from "./NotFound";
import { ROUTES } from "../shared/routes";

const AIChatPage = lazy(() => import("../pages/AIChatPage.jsx"));
const ActivitiesPage = lazy(() => import("../pages/ActivitiesPage.jsx"));
const ActivityDetailPage = lazy(() => import("../pages/ActivityDetailPage.jsx"));
const HomePage = lazy(() => import("../pages/HomePage.jsx"));
const LoginPage = lazy(() => import("../pages/LoginPage.jsx"));
const ParticipatePage = lazy(() => import("../pages/ParticipatePage.jsx"));
const RewardSendPage = lazy(() => import("../pages/RewardSendPage.jsx"));
const StatisticsPage = lazy(() => import("../pages/StatisticsPage.jsx"));

function PageFallback() {
  return (
    <div className="flex min-h-[calc(100vh-56px)] items-center justify-center text-sm" style={{ color: "var(--yuu-muted)" }}>
      加载中...
    </div>
  );
}

function withSuspense(element) {
  return <Suspense fallback={<PageFallback />}>{element}</Suspense>;
}

export const router = createBrowserRouter([
  {
    element: <AppShell />,
    errorElement: <NotFound />,
    children: [
      { path: ROUTES.HOME, element: withSuspense(<HomePage />), errorElement: <NotFound /> },
      { path: ROUTES.LOGIN, element: withSuspense(<LoginPage />), errorElement: <NotFound /> },
      { path: ROUTES.AI_CHAT, element: withSuspense(<AIChatPage />), errorElement: <NotFound /> },
      { path: ROUTES.ACTIVITIES, element: withSuspense(<ActivitiesPage />), errorElement: <NotFound /> },
      { path: ROUTES.ACTIVITY_DETAIL, element: withSuspense(<ActivityDetailPage />), errorElement: <NotFound /> },
      { path: ROUTES.PARTICIPATE, element: withSuspense(<ParticipatePage />), errorElement: <NotFound /> },
      { path: ROUTES.REWARDS, element: withSuspense(<RewardSendPage />), errorElement: <NotFound /> },
      { path: ROUTES.STATISTICS, element: withSuspense(<StatisticsPage />), errorElement: <NotFound /> },
      { path: "*", element: <NotFound /> },
    ],
  },
]);
