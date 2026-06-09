#!/usr/bin/env python
"""Text-to-SQL evaluation runner.

Reads eval_cases.json, runs each case through guard_sql() and optionally
through the LLM query chain, then reports pass/fail rates.

Usage:
    # Guard-only mode (fast, no LLM calls — tests SQL strings directly)
    python eval_runner.py --guard-only

    # Full mode (requires LLM + DB, slow)
    python eval_runner.py --full

    # Output detailed report
    python eval_runner.py --guard-only --verbose
"""

import argparse
import json
import logging
import re
import sys
from typing import Any, Dict, List, Optional

from sql_guard import SQLGuardError, guard_sql

logger = logging.getLogger(__name__)


def load_cases(path: str = "eval_cases.json") -> List[Dict[str, Any]]:
    with open(path, "r", encoding="utf-8") as f:
        cases = json.load(f)
    if not cases:
        raise ValueError("eval_cases.json is empty")
    return cases


def check_guard(
    case: Dict[str, Any],
    sql: Optional[str] = None,
) -> Dict[str, Any]:
    """Test guard_sql against a SQL string."""
    sql_to_test = sql or case.get("test_sql", "")
    result: Dict[str, Any] = {
        "case_id": case["id"],
        "question": case["question"],
        "guard_passed": True,
        "guard_error": None,
        "tested_sql": sql_to_test[:200] if sql_to_test else None,
    }

    if not sql_to_test:
        result["guard_passed"] = None  # skipped
        result["guard_error"] = "No SQL to test (require_full case)"
        return result

    try:
        guard_sql(sql_to_test)
    except SQLGuardError as e:
        result["guard_error"] = str(e)
        if case.get("expect_guard_rejection") or case.get("expect_refuse"):
            result["guard_passed"] = True  # Expected rejection
        else:
            result["guard_passed"] = False

    # If we expected guard rejection but it passed through -> FAIL
    if case.get("expect_guard_rejection") and result["guard_error"] is None:
        result["guard_passed"] = False
        result["guard_error"] = (
            "Expected SQLGuardError but guard allowed the SQL through"
        )

    return result


def run_guard_eval(cases: List[Dict[str, Any]]) -> Dict[str, Any]:
    """Run guard-only evaluation (no LLM required).

    Only tests cases that have explicit `test_sql` or expect guard rejection.
    Natural language cases are skipped (they need LLM to generate SQL).
    """
    results: List[Dict[str, Any]] = []
    passed = 0
    failed = 0
    skipped = 0

    for case in cases:
        r = check_guard(case)
        results.append(r)
        if r["guard_passed"] is None:
            skipped += 1
        elif r["guard_passed"]:
            passed += 1
        else:
            failed += 1

    evaluated = passed + failed
    return {
        "mode": "guard-only",
        "total": len(cases),
        "evaluated": evaluated,
        "skipped": skipped,
        "passed": passed,
        "failed": failed,
        "pass_rate": (
            f"{passed / evaluated * 100:.1f}%" if evaluated else "N/A"
        ),
        "results": results,
    }


def run_full_eval(cases: List[Dict[str, Any]]) -> Dict[str, Any]:
    """Run full evaluation with LLM SQL generation.

    NOTE: Requires OPENAI_API_KEY and database to be configured.
    """
    from agent import ActivitySQLAgent

    agent = ActivitySQLAgent()
    results: List[Dict[str, Any]] = []
    passed = 0
    failed = 0
    refused = 0

    for case in cases:
        r: Dict[str, Any] = {
            "case_id": case["id"],
            "question": case["question"],
            "expected_intent": case.get("expected_intent"),
        }

        try:
            output = agent.query(
                case["question"], user_id=None
            )
            sql = output.get("generated_sql", "")
            result_data = output.get("query_result", [])
            refused_flag = sql == ""

            r.update({
                "generated_sql": sql,
                "success": output.get("success"),
                "risk_level": output.get("risk_level", 0),
                "risk_reason": output.get("risk_reason", ""),
                "row_count": (
                    len(result_data) if isinstance(result_data, list) else 0
                ),
            })

            # Evaluate
            case_passed = True
            failures: List[str] = []

            # Case expects refusal
            if case.get("expect_refuse"):
                if not refused_flag:
                    failures.append("Expected refusal but SQL was generated")
                    case_passed = False
                else:
                    refused += 1

            # Case expects guard rejection
            if case.get("expect_guard_rejection"):
                if output.get("success"):
                    failures.append(
                        "Expected guard rejection but query succeeded"
                    )
                    case_passed = False
                else:
                    case_passed = True  # Got rejected, that's correct

            # Check tables
            if case.get("must_include_tables") and sql:
                for table in case["must_include_tables"]:
                    if not re.search(
                        rf"\b{re.escape(table)}\b", sql, re.IGNORECASE
                    ):
                        failures.append(f"Missing table: {table}")
                        case_passed = False

            # Check fields
            if case.get("must_include_fields") and sql:
                for field in case["must_include_fields"]:
                    if not re.search(
                        rf"\b{re.escape(field)}\b", sql, re.IGNORECASE
                    ):
                        failures.append(f"Missing field: {field}")
                        case_passed = False

            # Check forbidden fields
            if case.get("forbidden_fields") and sql:
                for field in case["forbidden_fields"]:
                    if re.search(
                        rf"\b{re.escape(field)}\b", sql, re.IGNORECASE
                    ):
                        failures.append(
                            f"Forbidden field found: {field}"
                        )
                        case_passed = False

            # Check result row count bounds
            actual_rows = r.get("row_count", 0)
            if "min_row_count" in case and isinstance(result_data, list):
                if actual_rows < case["min_row_count"]:
                    if not case.get("allow_approximate_sql"):
                        failures.append(
                            f"Row count {actual_rows} < min {case['min_row_count']}"
                        )
                        case_passed = False
            if "max_row_count" in case and isinstance(result_data, list):
                if actual_rows > case["max_row_count"]:
                    failures.append(
                        f"Row count {actual_rows} > max {case['max_row_count']}"
                    )
                    case_passed = False

            r["case_passed"] = case_passed
            r["failure_reasons"] = failures

            if case_passed:
                passed += 1
            else:
                failed += 1

        except Exception as exc:
            r.update({
                "case_passed": False,
                "failure_reasons": [str(exc)],
                "success": False,
            })
            failed += 1

        results.append(r)

    total_evaluated = passed + failed
    return {
        "mode": "full",
        "total": len(cases),
        "evaluated": total_evaluated,
        "passed": passed,
        "failed": failed,
        "refused": refused,
        "pass_rate": (
            f"{passed / total_evaluated * 100:.1f}%"
            if total_evaluated else "N/A"
        ),
        "results": results,
    }


def print_report(report: Dict[str, Any], verbose: bool = False) -> None:
    print("=" * 60)
    print(f"  Text-to-SQL Evaluation Report ({report['mode']})")
    print("=" * 60)
    print(f"  Total cases : {report['total']}")
    if "skipped" in report:
        print(f"  Skipped     : {report['skipped']} (NL questions, needs --full)")
    print(f"  Evaluated   : {report.get('evaluated', report['total'])}")
    print(f"  Passed      : {report['passed']}")
    print(f"  Failed      : {report['failed']}")
    if "refused" in report:
        print(f"  Refused     : {report['refused']}")
    print(f"  Pass rate   : {report['pass_rate']}")
    print("-" * 60)

    if verbose:
        for r in report["results"]:
            guard_result = r.get("guard_passed")
            if guard_result is None:
                print(f"  [SKIP] {r['case_id']}: {r['question'][:60]}")
                continue

            case_passed = r.get("case_passed", guard_result)
            status = "PASS" if case_passed else "FAIL"
            print(f"  [{status}] {r['case_id']}: {r['question'][:60]}")
            failures = r.get("failure_reasons", [])
            for f in failures:
                print(f"          -> {f}")
            if "guard_error" in r and r["guard_error"]:
                gs = "REJECTED" if r.get("guard_passed") else "UNEXPECTED"
                print(f"          guard: {gs} - {r['guard_error'][:80]}")
            if "generated_sql" in r and r["generated_sql"]:
                print(f"          SQL: {r['generated_sql'][:120]}")
            if "risk_level" in r and r["risk_level"]:
                print(f"          risk={r['risk_level']}: {r.get('risk_reason', '')}")
    print("=" * 60)


def main() -> None:
    parser = argparse.ArgumentParser(
        description="Text-to-SQL evaluation runner"
    )
    parser.add_argument(
        "--guard-only",
        action="store_true",
        help="Run guard-only evaluation (fast, no LLM)",
    )
    parser.add_argument(
        "--full",
        action="store_true",
        help="Run full evaluation with LLM SQL generation (slow)",
    )
    parser.add_argument(
        "--verbose", "-v",
        action="store_true",
        help="Print per-case details",
    )
    parser.add_argument(
        "--cases",
        default="eval_cases.json",
        help="Path to eval cases file (default: eval_cases.json)",
    )

    args = parser.parse_args()

    if not args.guard_only and not args.full:
        parser.print_help()
        sys.exit(1)

    logging.basicConfig(
        level=logging.WARNING,
        format="%(asctime)s [%(levelname)s] %(name)s - %(message)s",
    )

    cases = load_cases(args.cases)

    if args.full:
        report = run_full_eval(cases)
    else:
        report = run_guard_eval(cases)

    print_report(report, verbose=args.verbose)

    # Exit code reflects pass rate
    evaluated = report.get("evaluated", 0)
    if evaluated > 0 and report["passed"] < evaluated:
        sys.exit(1)


if __name__ == "__main__":
    main()
