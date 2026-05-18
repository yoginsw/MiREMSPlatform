#!/usr/bin/env python3
"""Import a validated MiREMS sample-data bundle into PostgreSQL.

The importer is manifest-driven and intentionally deterministic:
- validates the bundle before generating SQL;
- loads operational/geographic CSVs into sample_* staging tables;
- maps compatible records into core MiREMS domain persistence tables using UUIDv5;
- leaves aggregate precinct results in staging because they are not per-voting-session ballots.
"""

from __future__ import annotations

import argparse
import csv
import json
import subprocess
import tempfile
import uuid
from dataclasses import dataclass
from pathlib import Path
from typing import Iterable

from validate_sample_import_bundle import validate_bundle

MIREMS_SAMPLE_NAMESPACE = uuid.UUID("a0f97c6e-1a9f-47f8-bd5e-7dfb2e39b526")

STAGING_TABLES = {
    "jurisdictions.csv": "sample_jurisdictions",
    "offices.csv": "sample_offices",
    "parties.csv": "sample_parties",
    "polling_centers.csv": "sample_polling_centers",
    "canvassing_centers.csv": "sample_canvassing_centers",
    "precincts.csv": "sample_precincts",
    "acm_units.csv": "sample_acm_units",
    "ccs_units.csv": "sample_ccs_units",
    "precinct_results.csv": "sample_precinct_results",
    "operations_calendar.csv": "sample_operations_calendar",
}

CORE_DOMAIN_RESOURCES = {
    "elections.csv",
    "offices.csv",
    "candidates.csv",
    "ballot_styles.csv",
    "ballot_contests.csv",
    "voters.csv",
    "consolidated_results.csv",
    "transmission_events.csv",
}


@dataclass(frozen=True)
class ImportResource:
    csv_name: str
    target_domain: str
    target_table: str
    privacy_classification: str
    row_count: int


@dataclass(frozen=True)
class ImportPlan:
    bundle_id: str
    target_system: str
    source_profile: str
    resources: tuple[ImportResource, ...]

    @property
    def resources_by_csv(self) -> dict[str, ImportResource]:
        return {resource.csv_name: resource for resource in self.resources}


def deterministic_uuid(source_key: str) -> uuid.UUID:
    return uuid.uuid5(MIREMS_SAMPLE_NAMESPACE, source_key)


def build_import_plan(bundle_dir: Path) -> ImportPlan:
    manifest = _load_valid_manifest(bundle_dir)
    resources = []
    for csv_name in manifest["load_order"]:
        metadata = manifest["resources"][csv_name]
        resources.append(
            ImportResource(
                csv_name=csv_name,
                target_domain=metadata["target_domain"],
                target_table=metadata["target_table"],
                privacy_classification=metadata["privacy_classification"],
                row_count=manifest["record_counts"][csv_name],
            )
        )
    return ImportPlan(
        bundle_id=manifest["bundle_id"],
        target_system=manifest["target_system"],
        source_profile=manifest["source_profile"],
        resources=tuple(resources),
    )


def build_sql_script(bundle_dir: Path) -> str:
    manifest = _load_valid_manifest(bundle_dir)
    plan = build_import_plan(bundle_dir)
    tables = {resource.csv_name: _read_csv(bundle_dir / resource.csv_name) for resource in plan.resources}
    import_batch_id = deterministic_uuid(f"import-batch:{plan.bundle_id}")

    statements = [
        "BEGIN;",
        _upsert_import_batch(import_batch_id, plan, manifest),
        *_delete_existing_sample_data(import_batch_id, tables),
        *_insert_staging_tables(import_batch_id, plan, tables),
        *_insert_core_domain_tables(tables),
        "COMMIT;",
        "",
    ]
    return "\n".join(statement for statement in statements if statement)


def execute_sql_script(database_url: str, sql_script: str) -> None:
    with tempfile.NamedTemporaryFile("w", encoding="utf-8", suffix=".sql", delete=False) as sql_file:
        sql_file.write(sql_script)
        sql_path = sql_file.name
    subprocess.run(
        ["psql", database_url, "--set", "ON_ERROR_STOP=1", "--file", sql_path],
        check=True,
    )


def _load_valid_manifest(bundle_dir: Path) -> dict:
    report = validate_bundle(bundle_dir)
    if not report["valid"]:
        raise ValueError("invalid sample import bundle: " + "; ".join(report["errors"]))
    return json.loads((bundle_dir / "mirems_import_manifest.json").read_text(encoding="utf-8"))


def _read_csv(path: Path) -> list[dict[str, str]]:
    with path.open(newline="", encoding="utf-8") as handle:
        return list(csv.DictReader(handle))


def _upsert_import_batch(import_batch_id: uuid.UUID, plan: ImportPlan, manifest: dict) -> str:
    columns = ["id", "source_bundle_id", "target_system", "source_profile", "manifest"]
    values = [
        str(import_batch_id),
        plan.bundle_id,
        plan.target_system,
        plan.source_profile,
        json.dumps(manifest, ensure_ascii=False, sort_keys=True),
    ]
    return _insert_statement(
        "sample_import_batches",
        columns,
        [values],
        "ON CONFLICT (source_bundle_id) DO UPDATE SET "
        "target_system = EXCLUDED.target_system, "
        "source_profile = EXCLUDED.source_profile, "
        "imported_at = now(), "
        "manifest = EXCLUDED.manifest",
    )


def _delete_existing_sample_data(import_batch_id: uuid.UUID, tables: dict[str, list[dict[str, str]]]) -> list[str]:
    # Child-to-parent order for idempotent re-imports. Cleanup is scoped to the
    # deterministic identifiers generated from this bundle, never to whole tables.
    statements = [
        _delete_by_ids("audit_events", [deterministic_uuid(f"audit-event:{row['transmission_id']}") for row in tables["transmission_events.csv"]]),
        _delete_by_ids("tabulation_reports", [deterministic_uuid(f"tabulation-report:{row['canvass_id']}") for row in tables["consolidated_results.csv"]]),
        _delete_by_ids("voter_records", [deterministic_uuid(f"voter:{row['voter_id']}") for row in tables["voters.csv"]]),
        _delete_by_ids("ballot_styles", [deterministic_uuid(f"ballot-style:{row['ballot_style_id']}") for row in tables["ballot_styles.csv"]]),
        _delete_by_ids("ballot_contests", [deterministic_uuid(f"ballot:{row['election_id']}") for row in tables["elections.csv"]], column="ballot_id"),
        _delete_by_ids("ballots", [deterministic_uuid(f"ballot:{row['election_id']}") for row in tables["elections.csv"]]),
        _delete_by_ids("candidates", [deterministic_uuid(f"candidate:{row['candidate_id']}") for row in tables["candidates.csv"]]),
        _delete_by_ids("contests", [deterministic_uuid(f"contest:{row['election_id']}:{row['office_code']}") for row in tables["offices.csv"]]),
        _delete_by_ids("elections", [deterministic_uuid(f"election:{row['election_id']}") for row in tables["elections.csv"]]),
    ]
    staging_tables = [
        "sample_operations_calendar",
        "sample_precinct_results",
        "sample_ccs_units",
        "sample_acm_units",
        "sample_precincts",
        "sample_canvassing_centers",
        "sample_polling_centers",
        "sample_parties",
        "sample_offices",
        "sample_jurisdictions",
    ]
    statements.extend(f"DELETE FROM {table} WHERE import_batch_id = '{import_batch_id}';" for table in staging_tables)
    return [statement for statement in statements if statement]


def _delete_by_ids(table: str, ids: Iterable[uuid.UUID], column: str = "id") -> str:
    quoted_ids = ", ".join(_sql_literal(str(value)) for value in ids)
    if not quoted_ids:
        return ""
    return f"DELETE FROM {table} WHERE {column} IN ({quoted_ids});"


def _insert_staging_tables(import_batch_id: uuid.UUID, plan: ImportPlan, tables: dict[str, list[dict[str, str]]]) -> list[str]:
    statements = []
    for resource in plan.resources:
        staging_table = STAGING_TABLES.get(resource.csv_name)
        if staging_table is None:
            continue
        rows = tables[resource.csv_name]
        if not rows:
            continue
        columns = ["import_batch_id", *rows[0].keys()]
        values = [[str(import_batch_id), *(_normalize_staging_value(value) for value in row.values())] for row in rows]
        statements.append(_insert_statement(staging_table, columns, values, _staging_conflict_clause(staging_table)))
    return statements


def _insert_core_domain_tables(tables: dict[str, list[dict[str, str]]]) -> list[str]:
    statements = []
    elections = tables["elections.csv"]
    offices = tables["offices.csv"]
    parties = {row["party_id"]: row["name"] for row in tables["parties.csv"]}
    office_names = {row["office_code"]: row["name"] for row in offices}
    election_names = {row["election_id"]: row["name"] for row in elections}

    statements.append(_insert_elections(elections))
    statements.append(_insert_contests(offices))
    statements.append(_insert_candidates(tables["candidates.csv"], parties))
    statements.append(_insert_ballots(elections))
    style_to_election = {row["ballot_style_id"]: row["election_id"] for row in tables["ballot_styles.csv"]}
    statements.append(_insert_ballot_contests(tables["ballot_contests.csv"], office_names, style_to_election))
    statements.append(_insert_ballot_styles(tables["ballot_styles.csv"]))
    statements.append(_insert_voter_records(tables["voters.csv"], elections))
    statements.append(_insert_tabulation_reports(tables["consolidated_results.csv"]))
    statements.append(_insert_audit_events(tables["transmission_events.csv"], election_names))
    return [statement for statement in statements if statement]


def _insert_elections(rows: list[dict[str, str]]) -> str:
    values = [
        [
            str(deterministic_uuid(f"election:{row['election_id']}")),
            row["name"],
            _map_election_type(row["election_type"]),
            row["authority"],
            row["election_date"],
            row["country"],
            "ph-comelec-sample",
            _map_election_status(row["status"]),
        ]
        for row in rows
    ]
    return _insert_statement(
        "elections",
        ["id", "name", "election_type", "jurisdiction", "scheduled_date", "country_code", "extension_pack_id", "election_status"],
        values,
        "ON CONFLICT (id) DO UPDATE SET name = EXCLUDED.name, election_status = EXCLUDED.election_status",
    )


def _insert_contests(rows: list[dict[str, str]]) -> str:
    values = [
        [
            str(deterministic_uuid(f"contest:{row['election_id']}:{row['office_code']}")),
            str(deterministic_uuid(f"election:{row['election_id']}")),
            "CANDIDATE_CHOICE",
            row["name"],
            row["seats"],
            "1",
        ]
        for row in rows
    ]
    return _insert_statement(
        "contests",
        ["id", "election_id", "contest_type", "name", "seats", "vote_limit"],
        values,
        "ON CONFLICT (id) DO UPDATE SET name = EXCLUDED.name, seats = EXCLUDED.seats, vote_limit = EXCLUDED.vote_limit",
    )


def _insert_candidates(rows: list[dict[str, str]], parties: dict[str, str]) -> str:
    values = [
        [
            str(deterministic_uuid(f"candidate:{row['candidate_id']}")),
            str(deterministic_uuid(f"contest:{row['election_id']}:{row['office_code']}")),
            row["ballot_name"],
            parties[row["party_id"]],
            _map_candidate_status(row["status"]),
        ]
        for row in rows
    ]
    return _insert_statement(
        "candidates",
        ["id", "contest_id", "name", "party_affiliation", "candidate_status"],
        values,
        "ON CONFLICT (id) DO UPDATE SET name = EXCLUDED.name, party_affiliation = EXCLUDED.party_affiliation, candidate_status = EXCLUDED.candidate_status",
    )


def _insert_ballots(rows: list[dict[str, str]]) -> str:
    values = [
        [str(deterministic_uuid(f"ballot:{row['election_id']}")), str(deterministic_uuid(f"election:{row['election_id']}")), "1", "true"]
        for row in rows
    ]
    return _insert_statement(
        "ballots",
        ["id", "election_id", "ballot_version", "active"],
        values,
        "ON CONFLICT (id) DO UPDATE SET active = EXCLUDED.active",
    )


def _insert_ballot_contests(
    rows: list[dict[str, str]], office_names: dict[str, str], style_to_election: dict[str, str]
) -> str:
    by_office: dict[str, dict[str, str]] = {}
    for row in rows:
        key = row["office_code"]
        if key not in by_office or int(row["sequence"]) < int(by_office[key]["sequence"]):
            by_office[key] = row
    values = []
    for row in sorted(by_office.values(), key=lambda item: int(item["sequence"])):
        election_id = style_to_election[row["ballot_style_id"]]
        values.append(
            [
                str(deterministic_uuid(f"ballot:{election_id}")),
                str(deterministic_uuid(f"contest:{election_id}:{row['office_code']}")),
                row["sequence"],
                office_names[row["office_code"]],
            ]
        )
    return _insert_statement(
        "ballot_contests",
        ["ballot_id", "contest_id", "display_order", "presentation_title"],
        values,
        "ON CONFLICT (ballot_id, contest_id) DO UPDATE SET display_order = EXCLUDED.display_order, presentation_title = EXCLUDED.presentation_title",
    )


def _insert_ballot_styles(rows: list[dict[str, str]]) -> str:
    values = [
        [
            str(deterministic_uuid(f"ballot-style:{row['ballot_style_id']}")),
            str(deterministic_uuid(f"ballot:{row['election_id']}")),
            row["ballot_style_id"],
            row["jurisdiction_id"],
            row["language"][:2],
            json.dumps(["DRE"] if row["supports_dre"].lower() == "true" else []),
        ]
        for row in rows
    ]
    return _insert_statement(
        "ballot_styles",
        ["id", "ballot_id", "style_code", "district", "language", "accessibility_features"],
        values,
        "ON CONFLICT (id) DO UPDATE SET district = EXCLUDED.district, language = EXCLUDED.language, accessibility_features = EXCLUDED.accessibility_features",
    )


def _insert_voter_records(rows: list[dict[str, str]], elections: list[dict[str, str]]) -> str:
    election_ids = [str(deterministic_uuid(f"election:{row['election_id']}")) for row in elections]
    eligible_elections = json.dumps(election_ids)
    values = [
        [
            str(deterministic_uuid(f"voter:{row['voter_id']}")),
            f"SYNTHETIC:{row['voter_id']}",
            eligible_elections,
            _map_registration_status(row["eligibility_status"]),
        ]
        for row in rows
    ]
    return _insert_statement(
        "voter_records",
        ["id", "encrypted_external_voter_id", "eligible_elections", "registration_status"],
        values,
        "ON CONFLICT (id) DO UPDATE SET registration_status = EXCLUDED.registration_status, eligible_elections = EXCLUDED.eligible_elections",
    )


def _insert_tabulation_reports(rows: list[dict[str, str]]) -> str:
    values = []
    for row in rows:
        payload = {
            "sampleCanvassId": row["canvass_id"],
            "canvassLevel": row["canvass_level"],
            "expectedPrecincts": int(row["expected_precincts"]),
            "receivedPrecincts": int(row["received_precincts"]),
            "registeredVoters": int(row["registered_voters"]),
            "ballotsCast": int(row["ballots_cast"]),
            "validBallots": int(row["valid_ballots"]),
            "status": row["status"],
        }
        values.append(
            [
                str(deterministic_uuid(f"tabulation-report:{row['canvass_id']}")),
                str(deterministic_uuid(f"election:{row['election_id']}")),
                json.dumps([payload], sort_keys=True),
                "2025-05-09T23:59:00+08:00",
                "true",
            ]
        )
    return _insert_statement(
        "tabulation_reports",
        ["id", "election_id", "contest_tallies", "generated_at", "published"],
        values,
        "ON CONFLICT (id) DO UPDATE SET contest_tallies = EXCLUDED.contest_tallies, published = EXCLUDED.published",
    )


def _insert_audit_events(rows: list[dict[str, str]], election_names: dict[str, str]) -> str:
    values = []
    for row in rows:
        payload = {
            "sampleTransmissionId": row["transmission_id"],
            "electionName": election_names.get(row["election_id"], row["election_id"]),
            "precinctId": row["precinct_id"],
            "sourceSystem": row["source_system"],
            "destinationSystem": row["destination_system"],
            "status": row["status"],
            "attempt": int(row["attempt"]),
        }
        values.append(
            [
                str(deterministic_uuid(f"audit-event:{row['transmission_id']}")),
                "SAMPLE_TRANSMISSION_" + row["status"],
                str(deterministic_uuid(f"precinct:{row['precinct_id']}")),
                "SamplePrecinct",
                json.dumps(payload, sort_keys=True),
                "sample-import",
                row["transmitted_at"],
                None,
            ]
        )
    return _insert_statement(
        "audit_events",
        ["id", "event_type", "aggregate_id", "aggregate_type", "payload", "actor_id", "occurred_at", "source_ip"],
        values,
        "ON CONFLICT (id) DO UPDATE SET payload = EXCLUDED.payload, occurred_at = EXCLUDED.occurred_at",
    )


def _insert_statement(table: str, columns: list[str], values: Iterable[list[object]], conflict_clause: str) -> str:
    value_rows = ["(" + ", ".join(_sql_literal(value) for value in row) + ")" for row in values]
    if not value_rows:
        return ""
    return (
        f"INSERT INTO {table} ({', '.join(columns)}) VALUES\n"
        + ",\n".join(value_rows)
        + f"\n{conflict_clause};"
    )


def _staging_conflict_clause(table: str) -> str:
    primary_keys = {
        "sample_jurisdictions": "jurisdiction_id",
        "sample_offices": "office_code",
        "sample_parties": "party_id",
        "sample_polling_centers": "polling_center_id",
        "sample_canvassing_centers": "canvassing_center_id",
        "sample_precincts": "precinct_id",
        "sample_acm_units": "acm_id",
        "sample_ccs_units": "ccs_id",
        "sample_precinct_results": "result_id",
        "sample_operations_calendar": "election_id, milestone_code",
    }
    return f"ON CONFLICT ({primary_keys[table]}) DO NOTHING"


def _normalize_staging_value(value: str) -> object:
    if value == "":
        return None
    if value.lower() == "true":
        return "true"
    if value.lower() == "false":
        return "false"
    return value


def _sql_literal(value: object) -> str:
    if value is None:
        return "NULL"
    if isinstance(value, uuid.UUID):
        value = str(value)
    text = str(value)
    if text in {"true", "false"}:
        return text
    return "'" + text.replace("'", "''") + "'"


def _map_election_type(value: str) -> str:
    return "PRESIDENTIAL" if value == "NATIONAL_LOCAL" else value


def _map_election_status(value: str) -> str:
    return "DRAFT" if value == "SAMPLE" else value


def _map_candidate_status(value: str) -> str:
    return "APPROVED" if value == "QUALIFIED_SAMPLE" else value


def _map_registration_status(value: str) -> str:
    return "ACTIVE" if value == "ELIGIBLE" else "PENDING"


def main() -> None:
    parser = argparse.ArgumentParser(description="Import a MiREMS sample-data bundle into PostgreSQL.")
    parser.add_argument("bundle_dir", type=Path, nargs="?", default=Path("sample-data/ph-2025-nle"))
    parser.add_argument("--emit-sql", action="store_true", help="Print the generated SQL and do not execute it.")
    parser.add_argument("--database-url", help="PostgreSQL URL passed to psql for execution.")
    args = parser.parse_args()

    sql_script = build_sql_script(args.bundle_dir)
    if args.emit_sql or not args.database_url:
        print(sql_script)
        return
    execute_sql_script(args.database_url, sql_script)


if __name__ == "__main__":
    main()
