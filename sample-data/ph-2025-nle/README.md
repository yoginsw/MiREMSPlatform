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
python3 -m pytest scripts/test_generate_ph_2025_nle_sample_data.py -q
```
