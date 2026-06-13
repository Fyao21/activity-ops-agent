import { Bot, Copy, User, Database, Shield, AlertTriangle, CheckCircle2 } from "lucide-react";

function renderLinkedText(text = "", isUser = false) {
  const parts = [];
  const linkPattern = /\[([^\]]+)\]\((https?:\/\/[^)\s]+|\/[^)\s]+)\)/g;
  let lastIndex = 0;
  let match;

  while ((match = linkPattern.exec(text)) !== null) {
    if (match.index > lastIndex) {
      parts.push(text.slice(lastIndex, match.index));
    }
    const href = match[2];
    const isExternal = href.startsWith("http");
    parts.push(
      <a
        key={`${href}-${match.index}`}
        href={href}
        target={isExternal ? "_blank" : undefined}
        rel={isExternal ? "noopener noreferrer" : undefined}
        className={isUser ? "ai-message-link is-user" : "ai-message-link"}
      >
        {match[1]}
      </a>
    );
    lastIndex = match.lastIndex;
  }
  if (lastIndex < text.length) parts.push(text.slice(lastIndex));
  return parts.length ? parts : text;
}

function RiskBadge({ level, reason }) {
  if (level === undefined || level === null) return null;
  const config = {
    0: { cls: "risk-safe", Icon: CheckCircle2, label: "安全" },
    1: { cls: "risk-safe", Icon: CheckCircle2, label: "安全" },
    2: { cls: "risk-low", Icon: Shield, label: "低风险" },
    3: { cls: "risk-low", Icon: Shield, label: "低风险" },
    4: { cls: "risk-low", Icon: Shield, label: "低风险" },
    5: { cls: "risk-mid", Icon: AlertTriangle, label: "中风险" },
    6: { cls: "risk-mid", Icon: AlertTriangle, label: "中风险" },
    7: { cls: "risk-high", Icon: AlertTriangle, label: "高风险" },
    8: { cls: "risk-high", Icon: AlertTriangle, label: "高风险" },
    9: { cls: "risk-high", Icon: AlertTriangle, label: "高风险" },
    10: { cls: "risk-high", Icon: AlertTriangle, label: "极高风险" },
  };
  const { cls, Icon, label } = config[Math.min(level, 10)] || config[0];
  return (
    <span className={`risk-badge ${cls}`} title={reason || ""}>
      <Icon className="h-3 w-3" />
      {label} ({level}/10)
      {reason && <span className="ml-1 opacity-70">— {reason}</span>}
    </span>
  );
}

function SqlBlock({ sql }) {
  if (!sql) return null;
  return (
    <details className="mt-3">
      <summary
        className="cursor-pointer text-xs flex items-center gap-1.5"
        style={{ fontWeight: 600, color: "var(--yuu-accent)" }}
      >
        <Database className="h-3.5 w-3.5" />
        查看生成的 SQL
      </summary>
      <pre className="sql-block mt-2">{sql}</pre>
    </details>
  );
}

function ResultTable({ data }) {
  if (!data || !Array.isArray(data) || data.length === 0) return null;
  const columns = Object.keys(data[0]);
  return (
    <div className="data-table-wrap mt-3">
      <table className="data-table">
        <thead>
          <tr>{columns.map((col) => <th key={col}>{col}</th>)}</tr>
        </thead>
        <tbody>
          {data.map((row, i) => (
            <tr key={i}>{columns.map((col) => <td key={col}>{String(row[col] ?? "-")}</td>)}</tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}

export default function Message({ message }) {
  const isUser = message.role === "user";

  const copyText = async () => {
    if (!navigator.clipboard || !message.text) return;
    await navigator.clipboard.writeText(message.text);
  };

  if (message.role === "system") {
    return (
      <div className="flex justify-center mb-3">
        <div
          className="rounded-full px-4 py-1.5 text-xs font-medium"
          style={{
            border: "1px solid var(--yuu-border)",
            background: "var(--yuu-panel)",
            backdropFilter: "blur(18px)",
            color: "var(--yuu-muted)",
          }}
        >
          {message.text}
        </div>
      </div>
    );
  }

  return (
    <div className={`ai-message-row ${isUser ? "is-user" : "is-assistant"}`}>
      {!isUser && (
        <div className="ai-message-avatar" aria-hidden="true">
          <Bot className="h-4 w-4" />
        </div>
      )}

      <article className={`ai-message-bubble ${isUser ? "is-user" : "is-assistant"}`}>
        {/* Risk indicator for assistant messages */}
        {!isUser && message.riskLevel !== undefined && (
          <div className="mb-2">
            <RiskBadge level={message.riskLevel} reason={message.riskReason} />
          </div>
        )}

        {message.isError ? (
          <div
            className="flex items-center gap-1.5 text-xs font-semibold"
            style={{ color: "var(--color-high)" }}
          >
            <AlertTriangle className="h-3.5 w-3.5" />
            {message.text}
          </div>
        ) : (
          <>
            {message.text && (
              <p>
                {renderLinkedText(message.text, isUser)}
                {message.streaming && <span className="streaming-cursor" />}
              </p>
            )}
            {message.streaming && !message.text && (
              <p style={{ color: "var(--yuu-muted)", fontStyle: "italic" }}>正在查询活动数据...</p>
            )}

            {/* SQL block */}
            {!isUser && message.sql && <SqlBlock sql={message.sql} />}

            {/* Data table */}
            {!isUser && message.data && <ResultTable data={message.data} />}
          </>
        )}

        <div className="ai-message-actions">
          <button type="button" onClick={copyText} className="ai-icon-button" aria-label="复制消息">
            <Copy className="h-3.5 w-3.5" />
          </button>
        </div>
      </article>

      {isUser && (
        <div className="ai-message-avatar is-user" aria-hidden="true">
          <User className="h-4 w-4" />
        </div>
      )}
    </div>
  );
}
