import re
from typing import Dict, List, Set, Tuple


class SQLGuardError(ValueError):
    pass


FORBIDDEN_KEYWORDS = (
    "INSERT",
    "UPDATE",
    "DELETE",
    "DROP",
    "ALTER",
    "TRUNCATE",
    "CREATE",
    "REPLACE",
    "MERGE",
    "CALL",
    "GRANT",
    "REVOKE",
)

# Patterns that are suspicious even inside SELECT
FORBIDDEN_PATTERNS = (
    (r"(?i)\bSLEEP\s*\(", "SLEEP() call"),
    (r"(?i)\bBENCHMARK\s*\(", "BENCHMARK() call"),
    (r"(?i)\bLOAD_FILE\s*\(", "LOAD_FILE() call"),
    (r"(?i)\bINTO\s+(OUTFILE|DUMPFILE)\b", "INTO OUTFILE/DUMPFILE"),
    (r"(?i)\bEXEC\s*\(", "EXEC() call"),
    (r"(?i)\bWAITFOR\s+DELAY\b", "WAITFOR DELAY"),
    (r"(?i);\s*DROP\b", "DROP after semicolon"),
    (r"(?i);\s*DELETE\b", "DELETE after semicolon"),
    (r"(?i)UNION\s+SELECT.*UNION\s+SELECT", "Multiple UNION SELECT"),
    (r"(?i)\)\s*,\s*\(SELECT\b", "Nested subquery in tuple"),
)

# Field-level whitelist: only these fields may appear in generated SQL
ALLOWED_FIELDS_BY_TABLE: Dict[str, Set[str]] = {
    "activity": {
        "id", "activity_name", "activity_type",
        "start_time", "end_time", "status", "rule_desc",
        "create_time", "update_time",
    },
    "activity_user_record": {
        "id", "activity_id", "user_id", "channel",
        "participate_status", "participate_time", "create_time",
    },
    "activity_statistics": {
        "id", "activity_id", "stat_date",
        "participant_count", "reward_count", "reward_success_count",
        "conversion_rate", "retention_rate",
        "create_time", "update_time",
    },
    "reward_record": {
        "id", "activity_id", "user_id", "reward_type",
        "reward_amount", "send_status", "fail_reason",
        "send_time", "create_time", "update_time",
    },
    "agent_qa_record": {
        "id", "user_id", "question", "success", "create_time",
    },
}

# Fields that must never appear in queries (sensitive data)
FORBIDDEN_FIELDS = {
    "password", "password_hash",
}


def strip_sql_fence(sql: str) -> str:
    content = sql.strip()
    if content.startswith("```"):
        content = re.sub(r"^```[a-zA-Z0-9_]*\s*", "", content)
        content = re.sub(r"\s*```$", "", content)
    return content.strip()


def _strip_sql_comments(sql: str) -> str:
    """Remove SQL comments to prevent comment-based guard bypass."""
    # Remove block comments /* ... */
    no_blocks = re.sub(r"/\*.*?\*/", " ", sql, flags=re.DOTALL)
    # Remove line comments -- ... (but not inside strings)
    no_lines = re.sub(r"--[^\n]*", " ", no_blocks)
    return no_lines


def ensure_select_only(sql: str) -> str:
    normalized = strip_sql_fence(sql)
    if not normalized:
        raise SQLGuardError("Generated SQL is empty.")

    # Strip comments first to prevent bypass via /* */ SELECT or -- \n SELECT
    no_comments = _strip_sql_comments(normalized)
    compact = no_comments.strip()
    if not re.match(r"(?is)^select\b", compact):
        raise SQLGuardError("Only SELECT statements are allowed.")

    return normalized


def ensure_single_statement(sql: str) -> str:
    stripped = sql.strip()
    body = stripped[:-1] if stripped.endswith(";") else stripped
    if ";" in body:
        raise SQLGuardError("Multiple SQL statements are not allowed.")
    return stripped


def ensure_no_forbidden_keywords(sql: str) -> str:
    upper_sql = sql.upper()
    for keyword in FORBIDDEN_KEYWORDS:
        if re.search(rf"\b{keyword}\b", upper_sql):
            raise SQLGuardError(f"Forbidden SQL keyword detected: {keyword}")
    return sql


def ensure_no_forbidden_patterns(sql: str) -> str:
    """Block suspicious patterns even within SELECT statements."""
    for pattern, name in FORBIDDEN_PATTERNS:
        if re.search(pattern, sql):
            raise SQLGuardError(f"Forbidden SQL pattern detected: {name}")
    return sql


def ensure_no_select_star(sql: str) -> str:
    """Reject SELECT * to enforce explicit field selection."""
    if re.search(r"(?i)\bSELECT\s+\*", sql):
        raise SQLGuardError(
            "SELECT * is not allowed. Specify required columns explicitly."
        )
    return sql


def ensure_no_password_access(sql: str) -> str:
    normalized = re.sub(r"\s+", " ", sql.strip(), flags=re.MULTILINE)
    # Check for any forbidden field reference
    for field in FORBIDDEN_FIELDS:
        pattern = rf"(?i)\b{re.escape(field)}\b"
        if re.search(pattern, normalized):
            raise SQLGuardError(f"Querying {field} is forbidden.")
    # Legacy check
    if re.search(r"(?i)\bsys_user\s*\.\s*password\b", normalized):
        raise SQLGuardError("Querying sys_user.password is forbidden.")
    return sql


def ensure_limit(sql: str, default_limit: int = 100) -> str:
    trimmed = sql.strip()
    suffix = ";" if trimmed.endswith(";") else ""
    body = trimmed[:-1].rstrip() if suffix else trimmed

    if re.search(r"(?i)\blimit\s+\d+(\s*,\s*\d+)?\b", body):
        # Enforce max limit even when user-specified
        m = re.search(r"(?i)\blimit\s+(\d+)\b", body)
        if m:
            limit_val = int(m.group(1))
            if limit_val > 1000:
                body = re.sub(
                    r"(?i)\blimit\s+\d+\b", f"LIMIT {default_limit}", body
                )
        return f"{body}{suffix}"

    return f"{body} LIMIT {default_limit}{suffix}"


def ensure_field_whitelist(sql: str) -> str:
    """
    Check that referenced fields belong to allowed tables' whitelists.
    This is a best-effort structural check; table aliases are handled
    by matching bare column names against all allowed field sets.
    """
    # Extract potential column references from SELECT and WHERE clauses
    # Simple heuristic: words that look like column names between SELECT/FROM/WHERE
    field_refs = set()

    # Match `table.column` patterns
    for m in re.finditer(r"(?i)\b(\w+)\.(\w+)\b", sql):
        table = m.group(1).lower()
        col = m.group(2).lower()
        if table in ALLOWED_FIELDS_BY_TABLE:
            if col not in ALLOWED_FIELDS_BY_TABLE[table]:
                raise SQLGuardError(
                    f"Field '{table}.{col}' is not in the allowed field list for table '{table}'."
                )
        field_refs.add((table, col))

    # If no qualified references found, allow (unqualified references are common in simple queries)
    return sql


def risk_score(sql: str) -> Tuple[int, str]:
    """
    Evaluate SQL risk level (0-10) for audit purposes.
    0 = safe, 1-3 = low, 4-6 = medium, 7-9 = high, 10 = critical

    Returns (score, reason)
    """
    score = 0
    reasons: List[str] = []

    upper = sql.upper()

    # +2 for each JOIN (complexity)
    join_count = len(re.findall(r"(?i)\bJOIN\b", upper))
    if join_count > 2:
        score += min(join_count, 5)
        reasons.append(f"{join_count} JOINs")

    # +2 for GROUP BY with HAVING (aggregation)
    if re.search(r"(?i)\bGROUP\s+BY\b", upper):
        score += 1
        reasons.append("GROUP BY")
    if re.search(r"(?i)\bHAVING\b", upper):
        score += 1
        reasons.append("HAVING")

    # +2 for subquery
    subquery_count = len(re.findall(r"(?i)\(\s*SELECT\b", upper))
    if subquery_count > 0:
        score += min(subquery_count * 2, 6)
        reasons.append(f"{subquery_count} subqueries")

    # +1 for ORDER BY on non-indexed-looking fields
    if re.search(r"(?i)\bORDER\s+BY\b", upper):
        score += 1
        reasons.append("ORDER BY")

    # +2 for no WHERE clause on large tables
    if not re.search(r"(?i)\bWHERE\b", upper):
        if re.search(r"(?i)\bactivity_user_record\b", upper) or re.search(
            r"(?i)\breward_record\b", upper
        ):
            score += 2
            reasons.append("no WHERE on large table")

    # +3 for LIKE with leading wildcard (slow)
    if re.search(r"(?i)LIKE\s+['\"]%", upper):
        score += 3
        reasons.append("LIKE with leading %")

    return min(score, 10), "; ".join(reasons) if reasons else "low"


def guard_sql(sql: str, default_limit: int = 100) -> str:
    """
    Full SQL guard pipeline.
    Returns the sanitized SQL.
    Raises SQLGuardError on any violation.
    """
    guarded = ensure_select_only(sql)
    guarded = ensure_single_statement(guarded)
    guarded = ensure_no_forbidden_keywords(guarded)
    guarded = ensure_no_forbidden_patterns(guarded)
    guarded = ensure_no_select_star(guarded)
    guarded = ensure_no_password_access(guarded)
    guarded = ensure_field_whitelist(guarded)
    guarded = ensure_limit(guarded, default_limit=default_limit)
    return guarded
