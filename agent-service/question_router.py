SQL_KEYWORDS = (
    "统计",
    "数量",
    "人数",
    "次数",
    "正确率",
    "错误率",
    "错题数",
    "提问次数",
    "答题",
    "错误最多",
    "错题最多",
    "排名",
    "对比",
    "最近",
    "今天",
    "昨天",
    "本周",
    "平均",
    "活跃度",
    "每门课程",
)

RAG_KEYWORDS = (
    "是什么",
    "什么是",
    "解释",
    "原理",
    "规则",
    "说明",
    "知识点",
    "总结",
    "复习",
    "资料",
    "文档",
    "为什么",
    "如何",
    "怎么",
)

HYBRID_KEYWORDS = (
    "结合",
    "根据",
    "分析",
    "建议",
    "薄弱点",
    "是否正常",
    "原因",
    "复习计划",
    "掌握情况",
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
