SQL_KEYWORDS = (
    "统计",
    "人数",
    "数量",
    "成功率",
    "转化率",
    "留存",
    "对比",
    "排名",
    "最近",
    "今天",
    "昨天",
    "本周",
    "奖励发放量",
)

RAG_KEYWORDS = (
    "规则",
    "说明",
    "限制",
    "条件",
    "如何",
    "为什么",
    "介绍",
    "文档",
    "FAQ",
    "faq",
    "使用方式",
)

HYBRID_KEYWORDS = (
    "根据",
    "结合",
    "分析",
    "是否正常",
    "原因",
    "对照规则",
)


def route_question(question: str) -> str:
    normalized = question.strip()
    has_sql = any(keyword in normalized for keyword in SQL_KEYWORDS)
    has_rag = any(keyword in normalized for keyword in RAG_KEYWORDS)
    has_hybrid = any(keyword in normalized for keyword in HYBRID_KEYWORDS)

    if (has_sql and has_rag) or (has_hybrid and (has_sql or has_rag)):
        return "hybrid"
    if has_rag:
        return "rag"
    return "sql"
