#!/usr/bin/env python3
"""Tests for validating MiREMS CSV sample import bundles."""
from __future__ import annotations

import csv
import json
from pathlib import Path

import pytest

from generate_ph_2025_nle_sample_data import GenerationConfig, build_dataset, write_dataset
from validate_sample_import_bundle import validate_bundle


def test_validate_bundle_accepts_generated_ph_2025_nle_dataset(tmp_path: Path) -> None:
    write_dataset(
        build_dataset(GenerationConfig(profile="small", precincts_per_barangay=1, voters_per_precinct=4)),
        tmp_path,
    )

    report = validate_bundle(tmp_path)

    assert report["bundle_id"] == "PH-2025-NLE-SYNTHETIC-SMALL"
    assert report["valid"] is True
    assert report["errors"] == []
    assert report["record_counts"]["precincts.csv"] == 12
    assert report["record_counts"]["voters.csv"] == 48
    assert report["checked_rules"] == [
        "manifest resources exist",
        "manifest record counts match CSV rows",
        "foreign-key references resolve",
        "synthetic voter flag enforced",
        "precinct ACM one-to-one assignment enforced",
        "ballots_cast <= registered_voters enforced",
    ]


def test_validate_bundle_rejects_missing_foreign_key_reference(tmp_path: Path) -> None:
    write_dataset(
        build_dataset(GenerationConfig(profile="small", precincts_per_barangay=1, voters_per_precinct=4)),
        tmp_path,
    )
    precinct_path = tmp_path / "precincts.csv"
    rows = _read_rows(precinct_path)
    rows[0]["barangay_id"] = "MISSING-BARANGAY"
    _write_rows(precinct_path, rows)

    report = validate_bundle(tmp_path)

    assert report["valid"] is False
    assert any("precincts.csv.barangay_id references missing jurisdictions.csv.jurisdiction_id" in error for error in report["errors"])


def test_validate_bundle_fails_when_manifest_resource_is_missing(tmp_path: Path) -> None:
    write_dataset(
        build_dataset(GenerationConfig(profile="small", precincts_per_barangay=1, voters_per_precinct=4)),
        tmp_path,
    )
    (tmp_path / "acm_units.csv").unlink()

    report = validate_bundle(tmp_path)

    assert report["valid"] is False
    assert "missing resource: acm_units.csv" in report["errors"]


def _read_rows(path: Path) -> list[dict[str, str]]:
    with path.open(newline="", encoding="utf-8") as handle:
        return list(csv.DictReader(handle))


def _write_rows(path: Path, rows: list[dict[str, str]]) -> None:
    with path.open("w", newline="", encoding="utf-8") as handle:
        writer = csv.DictWriter(handle, fieldnames=list(rows[0].keys()), lineterminator="\n")
        writer.writeheader()
        writer.writerows(rows)
