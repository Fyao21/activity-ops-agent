import re


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


def strip_sql_fence(sql: str) -> str:
    content = sql.strip()
    if content.startswith("```"):
        content = re.sub(r"^```[a-zA-Z0-9_]*\s*", "", content)
        content = re.sub(r"\s*```$", "", content)
    return content.strip()


def ensure_select_only(sql: str) -> str:
    normalized = strip_sql_fence(sql)
    if not normalized:
        raise SQLGuardError("Generated SQL is empty.")

    compact = normalized.lstrip()
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


def ensure_no_password_access(sql: str) -> str:
    normalized = re.sub(r"\s+", " ", sql.strip(), flags=re.MULTILINE)
    if re.search(r"(?i)\bsys_user\s*\.\s*password\b", normalized):
        raise SQLGuardError("Querying sys_user.password is forbidden.")
    if re.search(r"(?i)\bpassword\b", normalized):
        raise SQLGuardError("Querying password fields is forbidden.")
    return sql


def ensure_limit(sql: str, default_limit: int = 100) -> str:
    trimmed = sql.strip()
    suffix = ";" if trimmed.endswith(";") else ""
    body = trimmed[:-1].rstrip() if suffix else trimmed

    if re.search(r"(?i)\blimit\s+\d+(\s*,\s*\d+)?\b", body):
        return f"{body}{suffix}"

    return f"{body} LIMIT {default_limit}{suffix}"


def guard_sql(sql: str, default_limit: int = 100) -> str:
    guarded = ensure_select_only(sql)
    guarded = ensure_single_statement(guarded)
    guarded = ensure_no_forbidden_keywords(guarded)
    guarded = ensure_no_password_access(guarded)
    guarded = ensure_limit(guarded, default_limit=default_limit)
    return guarded
