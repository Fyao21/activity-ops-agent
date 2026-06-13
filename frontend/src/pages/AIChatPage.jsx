import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import {
  Bot,
  Loader2,
  Menu,
  Plus,
  Send,
  Sparkles,
  Database,
  ShieldAlert,
  BarChart3,
  Users,
  Gift,
  Calendar,
} from "lucide-react";

import { agentQuery } from "../api/client";
import HistorySidebar from "../components/HistorySidebar";
import Message from "../components/Message";
import { useConversations } from "../features/chat/useConversations";
import { useAuth } from "../features/auth/AuthContext";
import "./ai-chat.css";

const TYPEWRITER_SPEED = 20;

function typewriteText(fullText, onUpdate, onDone) {
  let index = 0;
  const timer = setInterval(() => {
    index++;
    onUpdate(fullText.slice(0, index));
    if (index >= fullText.length) {
      clearInterval(timer);
      onDone();
    }
  }, TYPEWRITER_SPEED);
  return () => clearInterval(timer);
}

const QUICK_PROMPTS = [
  { icon: BarChart3, text: "帮我统计最近7天各活动的参与人数" },
  { icon: Users, text: "查询所有进行中活动及其参与情况" },
  { icon: Gift, text: "查看奖励发放成功和失败的数量" },
  { icon: Calendar, text: "列出本月创建的活动" },
  { icon: Database, text: "数据库有哪些表？展示所有可用的数据表" },
  { icon: ShieldAlert, text: "查询风险等级大于5的SQL历史记录" },
];

export default function AIChatPage() {
  const { user } = useAuth();
  const {
    activeConversation,
    addMessage,
    conversations,
    replaceConversation,
    selectConversation,
    startNewConversation,
  } = useConversations();

  const [input, setInput] = useState("");
  const [isSidebarOpen, setIsSidebarOpen] = useState(false);
  const [isLoading, setIsLoading] = useState(false);
  const [isTypewriting, setIsTypewriting] = useState(false);
  const [statusText, setStatusText] = useState("");
  const messagesEndRef = useRef(null);
  const typewriterCancelRef = useRef(null);
  const inputRef = useRef("");
  const busyRef = useRef(false);

  // Keep inputRef in sync so handleSend can read it without depending on input state
  inputRef.current = input;
  busyRef.current = isLoading || isTypewriting;

  const messages = useMemo(() => activeConversation?.messages ?? [], [activeConversation]);

  useEffect(() => {
    messagesEndRef.current?.scrollIntoView({ behavior: "smooth" });
  }, [messages, isLoading]);

  useEffect(() => {
    return () => {
      typewriterCancelRef.current?.();
      typewriterCancelRef.current = null;
    };
  }, []);

  const appendAssistant = useCallback(
    (conversationId, assistantMessage) => {
      replaceConversation(conversationId, (current) => ({
        ...current,
        messages: [...current.messages, assistantMessage],
      }));
    },
    [replaceConversation]
  );

  const updateLastMessage = useCallback(
    (conversationId, updater) => {
      replaceConversation(conversationId, (current) => {
        const msgs = [...current.messages];
        if (msgs.length > 0) {
          msgs[msgs.length - 1] = updater(msgs[msgs.length - 1]);
        }
        return { ...current, messages: msgs };
      });
    },
    [replaceConversation]
  );

  const handleSend = useCallback(async () => {
    const text = inputRef.current.trim();
    if (!text || busyRef.current) return;

    setInput("");
    setStatusText("AI Agent 正在分析你的问题...");
    const userMessage = { role: "user", text };
    const conversation = addMessage(userMessage);
    setIsLoading(true);

    try {
      // Add placeholder for streaming response
      appendAssistant(conversation.id, {
        role: "assistant",
        text: "",
        streaming: true,
      });

      if (!user?.userId) {
        throw new Error("请先登录后再使用 AI 查询功能");
      }
      const res = await agentQuery(text, user.userId);
      setStatusText("正在生成回答...");

      const result = res.data;
      const answer = result?.answer || "查询完成，但未返回文字结果。";
      const sql = result?.generatedSql || result?.generated_sql || "";
      const queryData = result?.queryResult || result?.query_result || [];
      const riskLevel = result?.riskLevel ?? result?.risk_level ?? 0;
      const riskReason = result?.riskReason || result?.risk_reason || "";

      setIsLoading(false);
      setIsTypewriting(true);

      // Build final assistant message
      const finalMessage = {
        role: "assistant",
        text: answer,
        streaming: false,
        sql,
        data: queryData,
        riskLevel,
        riskReason,
      };

      // Typewriter effect for the answer text only
      typewriterCancelRef.current?.();
      typewriterCancelRef.current = typewriteText(
        answer,
        (partial) => {
          updateLastMessage(conversation.id, (msg) => ({
            ...msg,
            text: partial,
          }));
        },
        () => {
          updateLastMessage(conversation.id, () => ({
            ...finalMessage,
            streaming: false,
          }));
          setStatusText("");
          setIsTypewriting(false);
          typewriterCancelRef.current = null;
        }
      );
    } catch (error) {
      setIsTypewriting(false);
      if (conversation?.id) {
        updateLastMessage(conversation.id, (msg) => ({
          ...msg,
          text: `查询失败：${error.message}`,
          streaming: false,
          isError: true,
        }));
      }
    } finally {
      setIsLoading(false);
      setStatusText("");
    }
  }, [addMessage, appendAssistant, updateLastMessage, user]);

  const handleQuickPrompt = useCallback((prompt) => {
    setInput(prompt);
  }, []);

  return (
    <main className="ai-chat-page relative flex h-[calc(100vh-60px)]">
      <div className="ai-chat-ambient" aria-hidden="true" />

      <HistorySidebar
        history={conversations}
        onSelectChat={selectConversation}
        onNewChat={startNewConversation}
        isSidebarOpen={isSidebarOpen}
        setIsSidebarOpen={setIsSidebarOpen}
      />

      <div className="ai-chat-main relative flex flex-1 flex-col overflow-hidden">
        {/* Top bar (mobile) */}
        <div className="ai-chat-topbar md:hidden">
          <button
            type="button"
            onClick={() => setIsSidebarOpen(true)}
            aria-label="打开历史侧栏"
            className="rounded-xl p-2"
            style={{ color: "var(--yuu-muted)" }}
          >
            <Menu className="h-5 w-5" />
          </button>
          <div
            className="flex items-center gap-2 text-sm"
            style={{ fontWeight: 600, color: "var(--yuu-text)" }}
          >
            <Bot className="h-4 w-4" style={{ color: "var(--yuu-accent)" }} />
            AI 分析助手
          </div>
          <button
            type="button"
            onClick={startNewConversation}
            aria-label="新建查询"
            className="rounded-xl p-2"
            style={{ color: "var(--yuu-muted)" }}
          >
            <Plus className="h-5 w-5" />
          </button>
        </div>

        {/* Messages area */}
        <div className="ai-chat-scroll flex-1 overflow-y-auto px-4 py-6">
          {messages.length === 0 && (
            <div className="ai-chat-empty mx-auto">
              <div className="ai-chat-empty-inner">
                <div className="ai-chat-mark">
                  <Sparkles className="h-5 w-5" />
                </div>
                <h1>Activity Ops AI</h1>
                <p>
                  用自然语言查询活动运营数据。AI 会自动将你的问题转换为 SQL，
                  经过安全校验后执行并返回结果。
                  支持活动查询、参与统计、奖励分析等多维度数据洞察。
                </p>

                {/* Quick prompt chips */}
                <div className="ai-chat-guide-grid">
                  {QUICK_PROMPTS.map(({ icon: Icon, text }) => (
                    <button
                      key={text}
                      type="button"
                      onClick={() => handleQuickPrompt(text)}
                      className="ai-guide-chip"
                    >
                      <div className="flex items-center gap-1.5">
                        <Icon className="h-3.5 w-3.5" style={{ color: "var(--yuu-accent)" }} />
                        <span>{text}</span>
                      </div>
                    </button>
                  ))}
                </div>
              </div>
            </div>
          )}

          {messages.map((message, index) => (
            <Message
              key={`${message.role}-${index}-${message.createdAt || ""}`}
              message={message}
            />
          ))}

          {isLoading && (
            <div className="mb-4 flex">
              <div className="ai-thinking">
                <Loader2 className="h-4 w-4 animate-spin" style={{ color: "var(--yuu-accent)" }} />
                <span>{statusText || "AI 正在处理..."}</span>
              </div>
            </div>
          )}

          <div ref={messagesEndRef} />
        </div>

        {/* Composer */}
        <div className="ai-chat-composer-wrap">
          <div className="ai-chat-status-row">
            <span>{statusText || "Enter 发送，Shift+Enter 换行"}</span>
            <span className="hidden sm:inline" style={{ color: "var(--yuu-accent)", fontWeight: 600 }}>
              Text-to-SQL · 安全校验 · 活动数据洞察
            </span>
          </div>

          <div className="ai-composer-glass">
            <div className="ai-composer">
              <textarea
                rows="1"
                className="ai-composer-input"
                placeholder="例如：帮我查一下最近一个月各活动的参与人数和奖励发放情况"
                value={input}
                onChange={(event) => setInput(event.target.value)}
                onKeyDown={(event) => {
                  if (event.key === "Enter" && !event.shiftKey) {
                    event.preventDefault();
                    handleSend();
                  }
                }}
              />

              <button
                type="button"
                onClick={handleSend}
                disabled={isLoading || isTypewriting || !input.trim()}
                className="ai-send-button"
                aria-label="发送查询"
              >
                {isLoading ? (
                  <Loader2 className="h-4 w-4 animate-spin" />
                ) : (
                  <Send className="h-4 w-4" />
                )}
              </button>
            </div>
          </div>
        </div>
      </div>
    </main>
  );
}
