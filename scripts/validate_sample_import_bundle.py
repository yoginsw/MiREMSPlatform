#!/usr/bin/env python3
"""Validate MiREMS CSV sample import bundles before loading them.

This validator checks the generated `mirems_import_manifest.json`, CSV record
counts, declared foreign-key references, and election-specific safety rules. It
is intentionally side-effect free: no database writes, no network calls.
"""
from __future__ import annotations

import argparse
import csv
import json
from pathlib import Path
from typing import Any

CHECKED_RULES = [
    "manifest resources exist",
    "manifest record counts match CSV rows",
    "foreign-key references resolve",
    "synthetic voter flag enforced",
    "precinct ACM one-to-one assignment enforced",
    "ballots_cast <= registered_voters enforced",
]


def validate_bundle(bundle_dir: Path) -> dict[str, Any]:
    """Validate a generated sample-data bundle and return a report."""

    bundle_dir = Path(bundle_dir)
    errors: list[str] = []
    manifest_path = bundle_dir / "mirems_import_manifest.json"
    if not manifest_path.exists():
        return _report("UNKNOWN", False, ["missing resource: mirems_import_manifest.json"], {}, CHECKED_RULES)

    manifest = json.loads(manifest_path.read_text(encoding="utf-8"))
    bundle_id = str(manifest.get("bundle_id", "UNKNOWN"))
    load_order = list(manifest.get("load_order", []))
    record_counts = dict(manifest.get("record_counts", {}))

    tables: dict[str, list[dict[str, str]]] = {}
    for filename in load_order:
        path = bundle_dir / filename
        if not path.exists():
            errors.append(f"missing resource: {filename}")
            continue
        rows = _read_csv(path)
        tables[filename] = rows
        expected = record_counts.get(filename)
        if expected is not None and int(expected) != len(rows):
            errors.append(f"record count mismatch for {filename}: manifest={expected}, actual={len(rows)}")

    _validate_foreign_keys(manifest, tables, errors)
    _validate_voters(tables, errors)
    _validate_acm_assignments(tables, errors)
    _validate_precinct_results(tables, errors)

    return _report(bundle_id, not errors, errors, {name: len(rows) for name, rows in tables.items()}, CHECKED_RULES)


def _validate_foreign_keys(manifest: dict[str, Any], tables: dict[str, list[dict[str, str]]], errors: list[str]) -> None:
    foreign_keys = manifest.get("foreign_keys", {})
    for source_file, mapping in foreign_keys.items():
        source_rows = tables.get(source_file)
        if source_rows is None:
            continue
        for source_column, target in mapping.items():
            target_file, target_column = str(target).rsplit(".", 1)
            target_rows = tables.get(target_file)
            if target_rows is None:
                continue
            allowed = {row[target_column] for row in target_rows if row.get(target_column)}
            for row in source_rows:
                value = row.get(source_column, "")
                if not value:
                    continue
                if source_file == target_file and source_column == "parent_id":
                    # Region root rows intentionally have no parent; non-root parents must resolve.
                    pass
                if value not in allowed:
                    errors.append(f"{source_file}.{source_column} references missing {target}: {value}")
                    break


def _validate_voters(tables: dict[str, list[dict[str, str]]], errors: list[str]) -> None:
    for row in tables.get("voters.csv", []):
        if row.get("is_synthetic") != "true":
            errors.append(f"voters.csv contains non-synthetic row: {row.get('voter_id', '<unknown>')}")
            return


def _validate_acm_assignments(tables: dict[str, list[dict[str, str]]], errors: list[str]) -> None:
    precinct_ids = {row["precinct_id"] for row in tables.get("precincts.csv", [])}
    acm_precinct_ids = [row.get("precinct_id", "") for row in tables.get("acm_units.csv", [])]
    if set(acm_precinct_ids) != precinct_ids or len(acm_precinct_ids) != len(set(acm_precinct_ids)):
        errors.append("every precinct must have exactly one ACM assignment")


def _validate_precinct_results(tables: dict[str, list[dict[str, str]]], errors: list[str]) -> None:
    for row in tables.get("precinct_results.csv", []):
        if int(row.get("ballots_cast", "0")) > int(row.get("registered_voters", "0")):
            errors.append(f"precinct_results.csv ballots_cast exceeds registered_voters: {row.get('result_id')}")
            return


def _read_csv(path: Path) -> list[dict[str, str]]:
    with path.open(newline="", encoding="utf-8") as handle:
        return list(csv.DictReader(handle))


def _report(
    bundle_id: str,
    valid: bool,
    errors: list[str],
    record_counts: dict[str, int],
    checked_rules: list[str],
) -> dict[str, Any]:
    return {
        "bundle_id": bundle_id,
        "valid": valid,
        "errors": errors,
        "record_counts": record_counts,
        "checked_rules": checked_rules,
    }


def _parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("bundle_dir", type=Path, nargs="?", default=Path("sample-data/ph-2025-nle"))
    return parser.parse_args()


def main() -> int:
    args = _parse_args()
    report = validate_bundle(args.bundle_dir)
    print(json.dumps(report, indent=2, ensure_ascii=False))
    return 0 if report["valid"] else 1


if __name__ == "__main__":
    raise SystemExit(main())
