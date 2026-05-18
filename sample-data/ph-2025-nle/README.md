# Philippines 2025 NLE Synthetic Sample Data

This directory contains MiREMS sample data generated from the structural cues in
`AES_TOR_2025NLE_final.pdf`:

- Election Management System (EMS)
- Automated Counting Machine (ACM)
- Consolidation and Canvassing System (CCS)
- National/local election contests
- Region/province/city/municipality/barangay/clustered-precinct hierarchy
- Polling and canvassing center assignments
- Synthetic voters, ballot styles, precinct returns, canvass summaries, and transmission events

## Important notice

The records are **synthetic**. They are not actual COMELEC voter records,
candidate lists, precinct statistics, Annex I jurisdiction files, or Annex J
MySQL/CSV sample files.

Use this dataset for local development, demos, integration tests, and load-test
fixtures only.

## Regenerate

```bash
python3 scripts/generate_ph_2025_nle_sample_data.py \
  --output-dir sample-data/ph-2025-nle \
  --profile small \
  --precincts-per-barangay 2 \
  --voters-per-precinct 25
```

## Verify

```bash
python3 -m pytest \
  scripts/test_generate_ph_2025_nle_sample_data.py \
  scripts/test_validate_sample_import_bundle.py \
  scripts/test_import_sample_bundle.py \
  -q
```

## Import manifest and dry-run validation

`mirems_import_manifest.json` defines the intended MiREMS load order, target
domain/table mapping, record counts, foreign-key contracts, and privacy
classification for each CSV.

Run the side-effect-free validator before using the bundle in a seed/import
pipeline:

```bash
python3 scripts/validate_sample_import_bundle.py sample-data/ph-2025-nle
```

The validator checks:

- all manifest resources exist
- manifest record counts match CSV rows
- declared foreign-key references resolve
- every voter row is marked synthetic
- every precinct has exactly one ACM assignment
- precinct ballots cast never exceed registered voters

## Import into a local PostgreSQL database

The import path is intentionally two-stage:

1. `validate_sample_import_bundle.py` performs side-effect-free validation.
2. `import_sample_bundle.py` loads operational/geographic resources into
   `sample_*` staging tables and maps compatible rows into MiREMS core domain
   tables with deterministic UUIDv5 identifiers.

Generate the SQL without touching a database:

```bash
python3 scripts/import_sample_bundle.py sample-data/ph-2025-nle --emit-sql > /tmp/mirems_sample_import.sql
```

Execute through `psql` when a local MiREMS PostgreSQL database is available:

```bash
python3 scripts/import_sample_bundle.py \
  sample-data/ph-2025-nle \
  --database-url postgresql://mirems:mirems@localhost:5432/mirems
```

The importer maps:

- `elections.csv` → `elections`
- `offices.csv` → `contests`
- `candidates.csv` → `candidates`
- `ballot_styles.csv` / `ballot_contests.csv` → `ballots`, `ballot_styles`, `ballot_contests`
- `voters.csv` → `voter_records` with synthetic external IDs only
- `consolidated_results.csv` → `tabulation_reports`
- `transmission_events.csv` → `audit_events`
- jurisdiction, equipment, center, precinct, operations, and precinct-result CSVs → `sample_*` staging tables
