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
const StatisticsPage = lazy(() => import("../pages/StatisticsPage.jsx"));

function PageFallback() {
  return (
    <div className="flex min-h-[calc(100vh-56px)] items-center justify-center text-sm text-gray-400">
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
      { path: ROUTES.HOME, element: withSuspense(<HomePage />) },
      { path: ROUTES.LOGIN, element: withSuspense(<LoginPage />) },
      { path: ROUTES.AI_CHAT, element: withSuspense(<AIChatPage />) },
      { path: ROUTES.ACTIVITIES, element: withSuspense(<ActivitiesPage />) },
      { path: ROUTES.ACTIVITY_DETAIL, element: withSuspense(<ActivityDetailPage />) },
      { path: ROUTES.STATISTICS, element: withSuspense(<StatisticsPage />) },
      { path: "*", element: <NotFound /> },
    ],
  },
]);
