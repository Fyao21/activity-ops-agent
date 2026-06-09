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
          className="fixed inset-0 z-40 bg-black/20 backdrop-blur-sm md:hidden"
          onClick={() => setIsSidebarOpen(false)}
        />
      )}

      <aside
        className={`fixed inset-y-0 left-0 z-50 w-64 transform border-r border-white/60 bg-white/60 backdrop-blur-xl transition-transform duration-300 md:static md:flex md:translate-x-0 md:flex-col ${
          isSidebarOpen ? "translate-x-0" : "-translate-x-full"
        }`}
      >
        <div className="flex items-center justify-between border-b border-gray-100 px-4 py-3">
          <span className="text-sm font-semibold text-gray-700">查询历史</span>
          <button
            type="button"
            onClick={() => setIsSidebarOpen(false)}
            className="liquid-action rounded-full p-1.5 text-gray-400 hover:bg-gray-100 md:hidden"
            aria-label="关闭"
          >
            <X className="h-4 w-4" />
          </button>
        </div>

        <div className="flex-1 overflow-y-auto p-3">
          <button
            type="button"
            onClick={() => { onNewChat(); setIsSidebarOpen(false); }}
            className="liquid-action mb-3 flex w-full items-center justify-center gap-2 rounded-xl border border-gray-200 py-2 text-sm text-gray-500 transition hover:bg-gray-50"
          >
            <Plus className="h-4 w-4" />
            新建查询
          </button>

          {history.length === 0 && (
            <p className="mt-4 text-center text-xs text-gray-400">暂无历史记录</p>
          )}

          {history.map((item) => (
            <button
              key={item.id}
              type="button"
              onClick={() => { onSelectChat(item.id); setIsSidebarOpen(false); }}
              className="liquid-action mb-1 w-full rounded-xl px-3 py-2.5 text-left transition hover:bg-gray-50"
            >
              <div className="flex items-center gap-2">
                <MessageSquare className="h-3.5 w-3.5 text-cyan-500 flex-shrink-0" />
                <p className="truncate text-sm text-gray-800">{item.title}</p>
              </div>
              <p className="mt-0.5 text-xs text-gray-400 ml-[1.375rem]">
                {new Date(item.createdAt).toLocaleDateString("zh-CN")}
              </p>
            </button>
          ))}
        </div>
      </aside>
    </>
  );
}
