from pathlib import Path

from import_sample_bundle import build_import_plan, build_sql_script, deterministic_uuid
from validate_sample_import_bundle import validate_bundle


BUNDLE_DIR = Path(__file__).resolve().parents[1] / "sample-data" / "ph-2025-nle"


def test_build_import_plan_follows_manifest_load_order_and_maps_domains():
    report = validate_bundle(BUNDLE_DIR)
    assert report["valid"] is True

    plan = build_import_plan(BUNDLE_DIR)

    assert plan.bundle_id == "PH-2025-NLE-SYNTHETIC-SMALL"
    assert [resource.csv_name for resource in plan.resources[:5]] == [
        "elections.csv",
        "jurisdictions.csv",
        "offices.csv",
        "parties.csv",
        "candidates.csv",
    ]
    assert plan.resources_by_csv["jurisdictions.csv"].target_table == "sample_jurisdictions"
    assert plan.resources_by_csv["elections.csv"].target_table == "elections"
    assert plan.resources_by_csv["voters.csv"].privacy_classification == "SYNTHETIC_PII"


def test_build_sql_script_loads_staging_before_core_domain_tables():
    sql = build_sql_script(BUNDLE_DIR)

    assert "INSERT INTO sample_import_batches" in sql
    assert sql.index("INSERT INTO sample_jurisdictions") < sql.index("INSERT INTO elections")
    assert sql.index("INSERT INTO sample_precincts") < sql.index("INSERT INTO voter_records")
    assert "ON CONFLICT (source_bundle_id) DO UPDATE" in sql
    assert "ON CONFLICT (id) DO UPDATE" in sql


def test_build_sql_script_maps_sample_identifiers_to_core_uuid_entities():
    sql = build_sql_script(BUNDLE_DIR)
    election_uuid = deterministic_uuid("election:PH-2025-NLE")
    president_contest_uuid = deterministic_uuid("contest:PH-2025-NLE:PRESIDENT")
    ballot_uuid = deterministic_uuid("ballot:PH-2025-NLE")

    assert str(election_uuid) in sql
    assert str(president_contest_uuid) in sql
    assert str(ballot_uuid) in sql
    assert f"'{ballot_uuid}', '{president_contest_uuid}', '1', 'President'" in sql
    assert "'PRESIDENTIAL'" in sql
    assert "'CANDIDATE_CHOICE'" in sql
    assert "'APPROVED'" in sql
    assert "'ACTIVE'" in sql


def test_build_sql_script_scopes_reimport_cleanup_to_sample_identifiers():
    sql = build_sql_script(BUNDLE_DIR)

    assert "DELETE FROM elections;" not in sql
    assert "DELETE FROM voter_records;" not in sql
    assert "WHERE id IN" in sql
    assert "WHERE import_batch_id =" in sql


def test_build_sql_script_refuses_invalid_bundle(tmp_path):
    empty_bundle = tmp_path / "empty"
    empty_bundle.mkdir()

    try:
        build_sql_script(empty_bundle)
    except ValueError as exc:
        assert "invalid sample import bundle" in str(exc)
    else:
        raise AssertionError("Expected invalid bundle to be rejected")
