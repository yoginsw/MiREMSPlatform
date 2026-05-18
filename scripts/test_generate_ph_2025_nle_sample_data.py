#!/usr/bin/env python3
"""Tests for the Philippines 2025 NLE synthetic sample data generator."""
from __future__ import annotations

import csv
import json
from pathlib import Path

from generate_ph_2025_nle_sample_data import (
    GenerationConfig,
    build_dataset,
    write_dataset,
)


def rows(path: Path) -> list[dict[str, str]]:
    with path.open(newline="", encoding="utf-8") as handle:
        return list(csv.DictReader(handle))


def test_build_dataset_uses_tor_profile_and_generates_consistent_relationships() -> None:
    dataset = build_dataset(GenerationConfig(profile="small", precincts_per_barangay=2, voters_per_precinct=12))

    assert dataset.profile["source_document"] == "AES_TOR_2025NLE_final.pdf"
    assert dataset.profile["election"]["code"] == "PH-2025-NLE"
    assert dataset.profile["quantity_reference"]["automated_counting_machines"] == 110_000
    assert dataset.profile["quantity_reference"]["ccs_equipment"] == 2_200
    assert "ACM" in dataset.profile["systems"]
    assert "CCS" in dataset.profile["systems"]
    assert "EMS" in dataset.profile["systems"]

    expected_files = {
        "elections.csv",
        "jurisdictions.csv",
        "offices.csv",
        "parties.csv",
        "candidates.csv",
        "precincts.csv",
        "polling_centers.csv",
        "canvassing_centers.csv",
        "acm_units.csv",
        "ccs_units.csv",
        "ballot_styles.csv",
        "ballot_contests.csv",
        "voters.csv",
        "precinct_results.csv",
        "consolidated_results.csv",
        "transmission_events.csv",
        "operations_calendar.csv",
        "mirems_import_manifest.json",
    }
    assert set(dataset.tables) == expected_files

    manifest = dataset.tables["mirems_import_manifest.json"]
    assert manifest["bundle_id"] == "PH-2025-NLE-SYNTHETIC-SMALL"
    assert manifest["target_system"] == "MiREMS Platform"
    assert manifest["load_order"][:4] == ["elections.csv", "jurisdictions.csv", "offices.csv", "parties.csv"]
    assert manifest["resources"]["elections.csv"]["target_domain"] == "Election"
    assert manifest["resources"]["voters.csv"]["privacy_classification"] == "SYNTHETIC_PII"
    assert manifest["foreign_keys"]["precincts.csv"]["barangay_id"] == "jurisdictions.csv.jurisdiction_id"

    jurisdictions = dataset.tables["jurisdictions.csv"]
    assert {row["level"] for row in jurisdictions} >= {"REGION", "PROVINCE", "CITY", "MUNICIPALITY", "BARANGAY"}

    precincts = dataset.tables["precincts.csv"]
    acm_units = dataset.tables["acm_units.csv"]
    voters = dataset.tables["voters.csv"]
    assert len(precincts) == 24
    assert len(acm_units) == len(precincts)
    assert len(voters) == len(precincts) * 12
    assert all(row["registered_voters"] == "12" for row in precincts)
    assert all(row["is_synthetic"] == "true" for row in voters)

    precinct_ids = {row["precinct_id"] for row in precincts}
    assert {row["precinct_id"] for row in acm_units} == precinct_ids
    assert {row["precinct_id"] for row in dataset.tables["precinct_results.csv"]} == precinct_ids

    ballot_style_ids = {row["ballot_style_id"] for row in dataset.tables["ballot_styles.csv"]}
    assert {row["ballot_style_id"] for row in dataset.tables["ballot_contests.csv"]} <= ballot_style_ids

    contests = {row["office_code"] for row in dataset.tables["ballot_contests.csv"]}
    for required in ["PRESIDENT", "VICE_PRESIDENT", "SENATOR", "PARTY_LIST", "GOVERNOR", "MAYOR"]:
        assert required in contests

    calendar_codes = {row["milestone_code"] for row in dataset.tables["operations_calendar.csv"]}
    assert {"TEC_CERTIFICATION", "SOURCE_CODE_REVIEW", "MOCK_ELECTIONS", "ELECTION_DAY", "POST_ELECTION"} <= calendar_codes


def test_write_dataset_outputs_csv_files_and_profile_json(tmp_path: Path) -> None:
    dataset = build_dataset(GenerationConfig(profile="small", precincts_per_barangay=1, voters_per_precinct=5))
    write_dataset(dataset, tmp_path)

    profile = json.loads((tmp_path / "philippines-2025-nle-profile.json").read_text(encoding="utf-8"))
    assert profile["election"]["name"] == "2025 National and Local Elections"
    assert profile["sample_data_notice"].startswith("Synthetic")

    manifest = json.loads((tmp_path / "mirems_import_manifest.json").read_text(encoding="utf-8"))
    assert manifest["record_counts"]["voters.csv"] == 60
    assert manifest["load_order"][-1] == "operations_calendar.csv"
    assert manifest["resources"]["precinct_results.csv"]["target_domain"] == "VotingResult import staging"
    assert manifest["validation_rules"] == [
        "All generated voters must be synthetic.",
        "No ballots_cast value may exceed registered_voters.",
        "Every precinct must have one ACM assignment.",
        "Every ballot contest must reference an existing ballot style.",
    ]

    elections = rows(tmp_path / "elections.csv")
    assert elections == [
        {
            "election_id": "PH-2025-NLE",
            "name": "2025 National and Local Elections",
            "country": "PH",
            "authority": "COMELEC",
            "election_type": "NATIONAL_LOCAL",
            "election_date": "2025-05-09",
            "status": "SAMPLE",
        }
    ]

    precinct_results = rows(tmp_path / "precinct_results.csv")
    assert precinct_results
    assert all(int(row["ballots_cast"]) <= int(row["registered_voters"]) for row in precinct_results)
    assert all(row["source_system"] == "ACM" for row in precinct_results)

    consolidated = rows(tmp_path / "consolidated_results.csv")
    assert any(row["canvass_level"] == "CITY_MUNICIPAL" for row in consolidated)
    assert any(row["canvass_level"] == "PROVINCIAL" for row in consolidated)
