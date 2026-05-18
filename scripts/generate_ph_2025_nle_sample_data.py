#!/usr/bin/env python3
"""Generate Philippines 2025 NLE synthetic sample data for MiREMS.

The data model is derived from the COMELEC 2025 NLE AES Terms of Reference
(AES_TOR_2025NLE_final.pdf). The generator intentionally creates synthetic,
non-personal demo data; it does not reproduce actual COMELEC voters,
candidates, precinct statistics, or Annex J files.
"""
from __future__ import annotations

import argparse
import csv
import json
from dataclasses import dataclass
from pathlib import Path
from typing import Iterable


@dataclass(frozen=True)
class GenerationConfig:
    """Configurable size knobs for deterministic synthetic generation."""

    profile: str = "small"
    precincts_per_barangay: int = 2
    voters_per_precinct: int = 25


@dataclass(frozen=True)
class SampleDataset:
    """In-memory sample dataset containing profile metadata and CSV rows."""

    profile: dict[str, object]
    tables: dict[str, list[dict[str, object]]]


PH_2025_NLE_PROFILE: dict[str, object] = {
    "source_document": "AES_TOR_2025NLE_final.pdf",
    "source_document_title": (
        "2025 National and Local Elections Automation Project "
        "(OMR/OpScan/DRE with EMS and CCS) Terms of Reference"
    ),
    "sample_data_notice": (
        "Synthetic sample data generated from TOR structure only; not actual "
        "COMELEC voter, candidate, precinct, or Annex J data."
    ),
    "election": {
        "code": "PH-2025-NLE",
        "name": "2025 National and Local Elections",
        "country": "PH",
        "authority": "COMELEC",
        "type": "NATIONAL_LOCAL",
        "date": "2025-05-09",
    },
    "systems": ["EMS", "ACM", "CCS"],
    "jurisdiction_hierarchy": ["REGION", "PROVINCE", "CITY", "MUNICIPALITY", "BARANGAY", "PRECINCT"],
    "canvassing_hierarchy": ["ACM", "CITY_MUNICIPAL_CCS", "PROVINCIAL_CCS", "NATIONAL_CCS", "CENTRAL_SERVER"],
    "quantity_reference": {
        "automated_counting_machines": 110_000,
        "ccs_equipment": 2_200,
        "ballot_boxes": 104_345,
        "external_batteries": 104_345,
    },
    "tor_annex_references": [
        "Election Statistics",
        "Districting",
        "Number of Seats per Elective Position",
        "Jurisdictions",
        "Sample Data Files in MySQL and CSV",
        "Sample Reports and Statistics for EMS, ACM, and CCS",
    ],
}

OFFICES: list[dict[str, object]] = [
    {"office_code": "PRESIDENT", "name": "President", "scope": "NATIONAL", "seats": 1},
    {"office_code": "VICE_PRESIDENT", "name": "Vice-President", "scope": "NATIONAL", "seats": 1},
    {"office_code": "SENATOR", "name": "Senator", "scope": "NATIONAL", "seats": 12},
    {"office_code": "PARTY_LIST", "name": "Party-List Organization", "scope": "NATIONAL", "seats": 63},
    {"office_code": "GOVERNOR", "name": "Governor", "scope": "PROVINCE", "seats": 1},
    {"office_code": "VICE_GOVERNOR", "name": "Vice-Governor", "scope": "PROVINCE", "seats": 1},
    {"office_code": "BOARD_MEMBER", "name": "Provincial Board Member", "scope": "PROVINCE", "seats": 2},
    {"office_code": "HOUSE_REP", "name": "Member, House of Representatives", "scope": "DISTRICT", "seats": 1},
    {"office_code": "MAYOR", "name": "City/Municipal Mayor", "scope": "CITY_MUNICIPAL", "seats": 1},
    {"office_code": "VICE_MAYOR", "name": "City/Municipal Vice-Mayor", "scope": "CITY_MUNICIPAL", "seats": 1},
    {"office_code": "COUNCILOR", "name": "City/Municipal Councilor", "scope": "CITY_MUNICIPAL", "seats": 8},
]

PARTIES = [
    {"party_id": "PH-PARTY-001", "name": "Sample Democratic Coalition", "type": "POLITICAL_PARTY"},
    {"party_id": "PH-PARTY-002", "name": "Sample National Alliance", "type": "POLITICAL_PARTY"},
    {"party_id": "PH-PARTY-003", "name": "Sample Reform Movement", "type": "POLITICAL_PARTY"},
    {"party_id": "PH-PL-001", "name": "Sample Teachers Party-List", "type": "PARTY_LIST"},
    {"party_id": "PH-PL-002", "name": "Sample Health Workers Party-List", "type": "PARTY_LIST"},
]

OPERATIONS_CALENDAR = [
    {"milestone_code": "TEC_CERTIFICATION", "name": "TEC Certification", "start_date": "2024-08-01", "end_date": "2025-02-28"},
    {"milestone_code": "SOURCE_CODE_REVIEW", "name": "Source Code Review", "start_date": "2024-08-01", "end_date": "2024-09-30"},
    {"milestone_code": "FIELD_TEST", "name": "Field Test", "start_date": "2024-10-01", "end_date": "2024-10-31"},
    {"milestone_code": "MOCK_ELECTIONS", "name": "Mock Elections", "start_date": "2024-11-01", "end_date": "2024-11-30"},
    {"milestone_code": "EMS_DATA_LOADING", "name": "EMS installation and election data loading", "start_date": "2024-12-01", "end_date": "2024-12-31"},
    {"milestone_code": "BALLOT_FACE_CREATION", "name": "ACM/CCS configuration and ballot face creation", "start_date": "2025-01-01", "end_date": "2025-04-30"},
    {"milestone_code": "DEPLOYMENT", "name": "AES equipment deployment", "start_date": "2025-02-01", "end_date": "2025-04-30"},
    {"milestone_code": "FINAL_TESTING_SEALING", "name": "Final testing and sealing", "start_date": "2025-05-01", "end_date": "2025-05-08"},
    {"milestone_code": "ELECTION_DAY", "name": "Election Day with repair/maintenance assistance", "start_date": "2025-05-09", "end_date": "2025-05-09"},
    {"milestone_code": "POST_ELECTION", "name": "Post-election activities", "start_date": "2025-06-01", "end_date": "2025-12-31"},
]


def build_dataset(config: GenerationConfig | None = None) -> SampleDataset:
    """Build a deterministic synthetic Philippines 2025 NLE dataset."""

    config = config or GenerationConfig()
    if config.precincts_per_barangay < 1:
        raise ValueError("precincts_per_barangay must be at least 1")
    if config.voters_per_precinct < 1:
        raise ValueError("voters_per_precinct must be at least 1")

    profile = dict(PH_2025_NLE_PROFILE)
    profile["generator_profile"] = config.profile
    tables: dict[str, list[dict[str, object]]] = {}

    tables["elections.csv"] = [
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
    tables["offices.csv"] = [{**office, "election_id": "PH-2025-NLE"} for office in OFFICES]
    tables["parties.csv"] = PARTIES.copy()

    jurisdictions = _build_jurisdictions()
    tables["jurisdictions.csv"] = jurisdictions
    tables["polling_centers.csv"] = _build_polling_centers(jurisdictions)
    tables["canvassing_centers.csv"] = _build_canvassing_centers(jurisdictions)
    tables["precincts.csv"] = _build_precincts(jurisdictions, config)
    tables["acm_units.csv"] = _build_acm_units(tables["precincts.csv"])
    tables["ccs_units.csv"] = _build_ccs_units(tables["canvassing_centers.csv"])
    tables["ballot_styles.csv"] = _build_ballot_styles(tables["jurisdictions.csv"])
    tables["ballot_contests.csv"] = _build_ballot_contests(tables["ballot_styles.csv"])
    tables["candidates.csv"] = _build_candidates()
    tables["voters.csv"] = _build_voters(tables["precincts.csv"], config.voters_per_precinct)
    tables["precinct_results.csv"] = _build_precinct_results(tables["precincts.csv"])
    tables["consolidated_results.csv"] = _build_consolidated_results(tables["precinct_results.csv"])
    tables["transmission_events.csv"] = _build_transmission_events(tables["precincts.csv"])
    tables["operations_calendar.csv"] = [{"election_id": "PH-2025-NLE", **row} for row in OPERATIONS_CALENDAR]

    return SampleDataset(profile=profile, tables=tables)


def write_dataset(dataset: SampleDataset, output_dir: Path) -> None:
    """Write profile JSON and CSV tables to output_dir."""

    output_dir.mkdir(parents=True, exist_ok=True)
    (output_dir / "philippines-2025-nle-profile.json").write_text(
        json.dumps(dataset.profile, indent=2, ensure_ascii=False) + "\n",
        encoding="utf-8",
    )
    for filename, rows in dataset.tables.items():
        _write_csv(output_dir / filename, rows)


def _build_jurisdictions() -> list[dict[str, object]]:
    rows: list[dict[str, object]] = []
    regions = [
        ("PH-R01", "National Capital Sample Region", [
            ("PH-R01-P01", "Metro Sample Province", [
                ("PH-R01-P01-C01", "San Isidro Sample City", "CITY"),
                ("PH-R01-P01-M01", "Santa Maria Sample Municipality", "MUNICIPALITY"),
            ]),
            ("PH-R01-P02", "Laguna Sample Province", [
                ("PH-R01-P02-C01", "Bayani Sample City", "CITY"),
                ("PH-R01-P02-M01", "Malaya Sample Municipality", "MUNICIPALITY"),
            ]),
        ]),
        ("PH-R02", "Visayas Sample Region", [
            ("PH-R02-P01", "Cebu Sample Province", [
                ("PH-R02-P01-C01", "Lapu-Lapu Sample City", "CITY"),
                ("PH-R02-P01-M01", "Danao Sample Municipality", "MUNICIPALITY"),
            ]),
        ]),
    ]
    for region_id, region_name, provinces in regions:
        rows.append(_jurisdiction(region_id, "REGION", region_name, "", region_id))
        for province_id, province_name, locals_ in provinces:
            rows.append(_jurisdiction(province_id, "PROVINCE", province_name, region_id, region_id))
            for local_id, local_name, level in locals_:
                rows.append(_jurisdiction(local_id, level, local_name, province_id, region_id))
                for barangay_no in range(1, 3):
                    barangay_id = f"{local_id}-B{barangay_no:02d}"
                    rows.append(
                        _jurisdiction(
                            barangay_id,
                            "BARANGAY",
                            f"Barangay Sample {barangay_no:02d} - {local_name}",
                            local_id,
                            region_id,
                        )
                    )
    return rows


def _jurisdiction(jurisdiction_id: str, level: str, name: str, parent_id: str, region_id: str) -> dict[str, object]:
    return {
        "jurisdiction_id": jurisdiction_id,
        "election_id": "PH-2025-NLE",
        "level": level,
        "name": name,
        "parent_id": parent_id,
        "region_id": region_id,
        "is_synthetic": "true",
    }


def _build_polling_centers(jurisdictions: list[dict[str, object]]) -> list[dict[str, object]]:
    return [
        {
            "polling_center_id": f"PC-{row['jurisdiction_id']}",
            "name": f"{row['name']} Elementary School",
            "barangay_id": row["jurisdiction_id"],
            "address": f"Synthetic address for {row['name']}",
            "is_synthetic": "true",
        }
        for row in jurisdictions
        if row["level"] == "BARANGAY"
    ]


def _build_canvassing_centers(jurisdictions: list[dict[str, object]]) -> list[dict[str, object]]:
    rows = [
        {
            "canvassing_center_id": "CC-NATIONAL-001",
            "name": "National Board of Canvassers Sample Center",
            "canvass_level": "NATIONAL",
            "jurisdiction_id": "PH",
            "parent_canvassing_center_id": "",
        }
    ]
    for row in jurisdictions:
        if row["level"] in {"PROVINCE", "CITY", "MUNICIPALITY"}:
            level = "PROVINCIAL" if row["level"] == "PROVINCE" else "CITY_MUNICIPAL"
            parent = "CC-NATIONAL-001" if level == "PROVINCIAL" else f"CC-{row['parent_id']}"
            rows.append(
                {
                    "canvassing_center_id": f"CC-{row['jurisdiction_id']}",
                    "name": f"{row['name']} Board of Canvassers",
                    "canvass_level": level,
                    "jurisdiction_id": row["jurisdiction_id"],
                    "parent_canvassing_center_id": parent,
                }
            )
    return rows


def _build_precincts(jurisdictions: list[dict[str, object]], config: GenerationConfig) -> list[dict[str, object]]:
    precincts = []
    for barangay in [row for row in jurisdictions if row["level"] == "BARANGAY"]:
        for idx in range(1, config.precincts_per_barangay + 1):
            precinct_id = f"CP-{barangay['jurisdiction_id']}-{idx:03d}"
            precincts.append(
                {
                    "precinct_id": precinct_id,
                    "election_id": "PH-2025-NLE",
                    "barangay_id": barangay["jurisdiction_id"],
                    "polling_center_id": f"PC-{barangay['jurisdiction_id']}",
                    "clustered_precinct_number": f"{barangay['jurisdiction_id']}-{idx:03d}",
                    "registered_voters": str(config.voters_per_precinct),
                    "ballot_style_id": f"BS-{barangay['parent_id']}",
                }
            )
    return precincts


def _build_acm_units(precincts: list[dict[str, object]]) -> list[dict[str, object]]:
    return [
        {
            "acm_id": f"ACM-{idx:06d}",
            "serial_number": f"SYN-ACM-{idx:06d}",
            "mac_address": f"02:PH:25:{idx // 10000:02X}:{(idx // 100) % 256:02X}:{idx % 256:02X}",
            "precinct_id": precinct["precinct_id"],
            "status": "ASSIGNED",
        }
        for idx, precinct in enumerate(precincts, start=1)
    ]


def _build_ccs_units(canvassing_centers: list[dict[str, object]]) -> list[dict[str, object]]:
    return [
        {
            "ccs_id": f"CCS-{idx:05d}",
            "serial_number": f"SYN-CCS-{idx:05d}",
            "canvassing_center_id": center["canvassing_center_id"],
            "status": "ASSIGNED",
        }
        for idx, center in enumerate(canvassing_centers, start=1)
    ]


def _build_ballot_styles(jurisdictions: list[dict[str, object]]) -> list[dict[str, object]]:
    return [
        {
            "ballot_style_id": f"BS-{row['jurisdiction_id']}",
            "election_id": "PH-2025-NLE",
            "jurisdiction_id": row["jurisdiction_id"],
            "language": "en-PH",
            "supports_dre": "true",
        }
        for row in jurisdictions
        if row["level"] in {"CITY", "MUNICIPALITY"}
    ]


def _build_ballot_contests(ballot_styles: list[dict[str, object]]) -> list[dict[str, object]]:
    scoped_codes = ["PRESIDENT", "VICE_PRESIDENT", "SENATOR", "PARTY_LIST", "GOVERNOR", "VICE_GOVERNOR", "BOARD_MEMBER", "HOUSE_REP", "MAYOR", "VICE_MAYOR", "COUNCILOR"]
    rows = []
    for style in ballot_styles:
        for sequence, office_code in enumerate(scoped_codes, start=1):
            office = next(item for item in OFFICES if item["office_code"] == office_code)
            rows.append(
                {
                    "ballot_style_id": style["ballot_style_id"],
                    "contest_id": f"{style['ballot_style_id']}-{office_code}",
                    "office_code": office_code,
                    "sequence": sequence,
                    "seats": office["seats"],
                    "max_candidates_to_select": office["seats"],
                }
            )
    return rows


def _build_candidates() -> list[dict[str, object]]:
    rows = []
    counter = 1
    party_cycle = [party["party_id"] for party in PARTIES]
    for office in OFFICES:
        seats = int(office["seats"])
        candidate_count = max(seats + 2, 3)
        if office["office_code"] == "SENATOR":
            candidate_count = 24
        if office["office_code"] == "PARTY_LIST":
            candidate_count = 12
        for order in range(1, candidate_count + 1):
            rows.append(
                {
                    "candidate_id": f"PH-CAND-{counter:05d}",
                    "election_id": "PH-2025-NLE",
                    "office_code": office["office_code"],
                    "ballot_name": f"Synthetic {office['name']} Candidate {order:02d}",
                    "party_id": party_cycle[(counter - 1) % len(party_cycle)],
                    "ballot_order": order,
                    "status": "QUALIFIED_SAMPLE",
                    "is_synthetic": "true",
                }
            )
            counter += 1
    return rows


def _build_voters(precincts: list[dict[str, object]], voters_per_precinct: int) -> list[dict[str, object]]:
    rows = []
    counter = 1
    for precinct in precincts:
        for voter_no in range(1, voters_per_precinct + 1):
            rows.append(
                {
                    "voter_id": f"PH-SYN-VOTER-{counter:08d}",
                    "precinct_id": precinct["precinct_id"],
                    "display_name": f"Synthetic Voter {counter:08d}",
                    "eligibility_status": "ELIGIBLE",
                    "is_synthetic": "true",
                    "synthetic_sequence": voter_no,
                }
            )
            counter += 1
    return rows


def _build_precinct_results(precincts: list[dict[str, object]]) -> list[dict[str, object]]:
    rows = []
    for idx, precinct in enumerate(precincts, start=1):
        registered = int(precinct["registered_voters"])
        ballots_cast = min(registered, max(1, round(registered * (0.62 + (idx % 5) * 0.03))))
        rejected = idx % 3
        rows.append(
            {
                "result_id": f"ER-{idx:06d}",
                "election_id": "PH-2025-NLE",
                "precinct_id": precinct["precinct_id"],
                "source_system": "ACM",
                "registered_voters": registered,
                "ballots_cast": ballots_cast,
                "valid_ballots": ballots_cast - rejected,
                "rejected_ballots": rejected,
                "undervotes": idx % 4,
                "overvotes": idx % 2,
                "hash_posted": f"sample-hash-{idx:064d}"[:76],
            }
        )
    return rows


def _build_consolidated_results(precinct_results: list[dict[str, object]]) -> list[dict[str, object]]:
    total_registered = sum(int(row["registered_voters"]) for row in precinct_results)
    total_cast = sum(int(row["ballots_cast"]) for row in precinct_results)
    total_valid = sum(int(row["valid_ballots"]) for row in precinct_results)
    return [
        {
            "canvass_id": "CANV-CM-001",
            "election_id": "PH-2025-NLE",
            "canvass_level": "CITY_MUNICIPAL",
            "expected_precincts": len(precinct_results),
            "received_precincts": len(precinct_results),
            "registered_voters": total_registered,
            "ballots_cast": total_cast,
            "valid_ballots": total_valid,
            "status": "COMPLETE_SAMPLE",
        },
        {
            "canvass_id": "CANV-PROV-001",
            "election_id": "PH-2025-NLE",
            "canvass_level": "PROVINCIAL",
            "expected_precincts": len(precinct_results),
            "received_precincts": len(precinct_results),
            "registered_voters": total_registered,
            "ballots_cast": total_cast,
            "valid_ballots": total_valid,
            "status": "COMPLETE_SAMPLE",
        },
        {
            "canvass_id": "CANV-NATL-001",
            "election_id": "PH-2025-NLE",
            "canvass_level": "NATIONAL",
            "expected_precincts": len(precinct_results),
            "received_precincts": len(precinct_results),
            "registered_voters": total_registered,
            "ballots_cast": total_cast,
            "valid_ballots": total_valid,
            "status": "COMPLETE_SAMPLE",
        },
    ]


def _build_transmission_events(precincts: list[dict[str, object]]) -> list[dict[str, object]]:
    rows = []
    for idx, precinct in enumerate(precincts, start=1):
        rows.append(
            {
                "transmission_id": f"TX-{idx:06d}",
                "election_id": "PH-2025-NLE",
                "precinct_id": precinct["precinct_id"],
                "source_system": "ACM",
                "destination_system": "CITY_MUNICIPAL_CCS",
                "status": "RECEIVED",
                "attempt": 1,
                "transmitted_at": f"2025-05-09T{18 + (idx % 4):02d}:{(idx * 7) % 60:02d}:00+08:00",
            }
        )
    return rows


def _write_csv(path: Path, rows: list[dict[str, object]]) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    if not rows:
        path.write_text("", encoding="utf-8")
        return
    fieldnames = list(rows[0].keys())
    with path.open("w", newline="", encoding="utf-8") as handle:
        writer = csv.DictWriter(handle, fieldnames=fieldnames, lineterminator="\n")
        writer.writeheader()
        writer.writerows(rows)


def _parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--output-dir", type=Path, default=Path("sample-data/ph-2025-nle"))
    parser.add_argument("--profile", default="small", choices=["small", "medium", "scale-smoke"])
    parser.add_argument("--precincts-per-barangay", type=int, default=2)
    parser.add_argument("--voters-per-precinct", type=int, default=25)
    return parser.parse_args()


def main() -> int:
    args = _parse_args()
    dataset = build_dataset(
        GenerationConfig(
            profile=args.profile,
            precincts_per_barangay=args.precincts_per_barangay,
            voters_per_precinct=args.voters_per_precinct,
        )
    )
    write_dataset(dataset, args.output_dir)
    print(f"Wrote Philippines 2025 NLE synthetic sample data to {args.output_dir}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
