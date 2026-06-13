import { Plus, X, MessageSquare } from "lucide-react";

export default function HistorySidebar({
  history = [],
  onNewChat,
  onSelectChat,
  isSidebarOpen,
  setIsSidebarOpen,
}) {
  return (
    <>
      {/* Mobile overlay */}
      {isSidebarOpen && (
        <div
          className="yuu-mobile-scrim fixed inset-0 z-40 md:hidden"
          onClick={() => setIsSidebarOpen(false)}
        />
      )}

      <aside
        className={`fixed inset-y-0 left-0 z-50 w-64 transform transition-transform duration-300 md:static md:flex md:translate-x-0 md:flex-col ${
          isSidebarOpen ? "translate-x-0" : "-translate-x-full"
        }`}
        style={{
          borderRight: "1px solid var(--yuu-border)",
          background: "var(--yuu-panel)",
          backdropFilter: "blur(22px) saturate(168%)",
        }}
      >
        <div
          className="flex items-center justify-between px-4 py-3"
          style={{ borderBottom: "1px solid var(--yuu-border)" }}
        >
          <span
            className="text-sm font-semibold"
            style={{ color: "var(--yuu-text)" }}
          >
            查询历史
          </span>
          <button
            type="button"
            onClick={() => setIsSidebarOpen(false)}
            className="rounded-full p-1.5 md:hidden"
            style={{ color: "var(--yuu-muted)" }}
            aria-label="关闭"
          >
            <X className="h-4 w-4" />
          </button>
        </div>

        <div className="flex-1 overflow-y-auto p-3">
          <button
            type="button"
            onClick={() => { onNewChat(); setIsSidebarOpen(false); }}
            className="yuu-btn-ghost mb-3 w-full justify-center"
          >
            <Plus className="h-4 w-4" />
            新建查询
          </button>

          {history.length === 0 && (
            <p
              className="mt-4 text-center text-xs"
              style={{ color: "var(--yuu-muted)" }}
            >
              暂无历史记录
            </p>
          )}

          {history.map((item) => (
            <button
              key={item.id}
              type="button"
              onClick={() => { onSelectChat(item.id); setIsSidebarOpen(false); }}
              className="yuu-hover-lift mb-1 w-full rounded-xl px-3 py-2.5 text-left"
              style={{
                border: "1px solid var(--yuu-border)",
                background: "transparent",
                cursor: "pointer",
              }}
            >
              <div className="flex items-center gap-2">
                <MessageSquare
                  className="h-3.5 w-3.5 flex-shrink-0"
                  style={{ color: "var(--yuu-accent)" }}
                />
                <p
                  className="truncate text-sm"
                  style={{ color: "var(--yuu-text)", fontWeight: 500 }}
                >
                  {item.title}
                </p>
              </div>
              <p
                className="mt-0.5 text-xs ml-[1.375rem]"
                style={{ color: "var(--yuu-muted)" }}
              >
                {new Date(item.createdAt).toLocaleDateString("zh-CN")}
              </p>
            </button>
          ))}
        </div>
      </aside>
    </>
  );
}
