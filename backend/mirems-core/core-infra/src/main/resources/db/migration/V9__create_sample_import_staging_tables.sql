CREATE TABLE sample_import_batches (
    id UUID PRIMARY KEY,
    source_bundle_id VARCHAR(128) NOT NULL,
    target_system VARCHAR(255) NOT NULL,
    source_profile VARCHAR(255) NOT NULL,
    imported_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    manifest JSONB NOT NULL,
    CONSTRAINT uq_sample_import_batches_bundle_id UNIQUE (source_bundle_id)
);

CREATE INDEX idx_sample_import_batches_bundle_id ON sample_import_batches (source_bundle_id);

CREATE TABLE sample_jurisdictions (
    import_batch_id UUID NOT NULL REFERENCES sample_import_batches (id) ON DELETE CASCADE,
    jurisdiction_id VARCHAR(128) PRIMARY KEY,
    election_id VARCHAR(128) NOT NULL,
    level VARCHAR(64) NOT NULL,
    name VARCHAR(255) NOT NULL,
    parent_id VARCHAR(128),
    region_id VARCHAR(128),
    is_synthetic BOOLEAN NOT NULL
);

CREATE INDEX idx_sample_jurisdictions_parent_id ON sample_jurisdictions (parent_id);

CREATE TABLE sample_offices (
    import_batch_id UUID NOT NULL REFERENCES sample_import_batches (id) ON DELETE CASCADE,
    office_code VARCHAR(128) PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    scope VARCHAR(128) NOT NULL,
    seats INTEGER NOT NULL CHECK (seats > 0),
    election_id VARCHAR(128) NOT NULL
);

CREATE TABLE sample_parties (
    import_batch_id UUID NOT NULL REFERENCES sample_import_batches (id) ON DELETE CASCADE,
    party_id VARCHAR(128) PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    type VARCHAR(128) NOT NULL
);

CREATE TABLE sample_polling_centers (
    import_batch_id UUID NOT NULL REFERENCES sample_import_batches (id) ON DELETE CASCADE,
    polling_center_id VARCHAR(128) PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    barangay_id VARCHAR(128) NOT NULL REFERENCES sample_jurisdictions (jurisdiction_id),
    address TEXT NOT NULL,
    is_synthetic BOOLEAN NOT NULL
);

CREATE INDEX idx_sample_polling_centers_barangay_id ON sample_polling_centers (barangay_id);

CREATE TABLE sample_canvassing_centers (
    import_batch_id UUID NOT NULL REFERENCES sample_import_batches (id) ON DELETE CASCADE,
    canvassing_center_id VARCHAR(128) PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    canvass_level VARCHAR(128) NOT NULL,
    jurisdiction_id VARCHAR(128) NOT NULL REFERENCES sample_jurisdictions (jurisdiction_id),
    parent_canvassing_center_id VARCHAR(128)
);

CREATE INDEX idx_sample_canvassing_centers_jurisdiction_id ON sample_canvassing_centers (jurisdiction_id);

CREATE TABLE sample_precincts (
    import_batch_id UUID NOT NULL REFERENCES sample_import_batches (id) ON DELETE CASCADE,
    precinct_id VARCHAR(128) PRIMARY KEY,
    election_id VARCHAR(128) NOT NULL,
    barangay_id VARCHAR(128) NOT NULL REFERENCES sample_jurisdictions (jurisdiction_id),
    polling_center_id VARCHAR(128) NOT NULL REFERENCES sample_polling_centers (polling_center_id),
    clustered_precinct_number VARCHAR(128) NOT NULL,
    registered_voters INTEGER NOT NULL CHECK (registered_voters >= 0),
    ballot_style_id VARCHAR(128) NOT NULL
);

CREATE INDEX idx_sample_precincts_ballot_style_id ON sample_precincts (ballot_style_id);
CREATE INDEX idx_sample_precincts_polling_center_id ON sample_precincts (polling_center_id);

CREATE TABLE sample_acm_units (
    import_batch_id UUID NOT NULL REFERENCES sample_import_batches (id) ON DELETE CASCADE,
    acm_id VARCHAR(128) PRIMARY KEY,
    serial_number VARCHAR(128) NOT NULL,
    mac_address VARCHAR(64) NOT NULL,
    precinct_id VARCHAR(128) NOT NULL REFERENCES sample_precincts (precinct_id),
    status VARCHAR(64) NOT NULL
);

CREATE UNIQUE INDEX uq_sample_acm_units_precinct_id ON sample_acm_units (precinct_id);

CREATE TABLE sample_ccs_units (
    import_batch_id UUID NOT NULL REFERENCES sample_import_batches (id) ON DELETE CASCADE,
    ccs_id VARCHAR(128) PRIMARY KEY,
    serial_number VARCHAR(128) NOT NULL,
    canvassing_center_id VARCHAR(128) NOT NULL REFERENCES sample_canvassing_centers (canvassing_center_id),
    status VARCHAR(64) NOT NULL
);

CREATE TABLE sample_precinct_results (
    import_batch_id UUID NOT NULL REFERENCES sample_import_batches (id) ON DELETE CASCADE,
    result_id VARCHAR(128) PRIMARY KEY,
    election_id VARCHAR(128) NOT NULL,
    precinct_id VARCHAR(128) NOT NULL REFERENCES sample_precincts (precinct_id),
    source_system VARCHAR(128) NOT NULL,
    registered_voters INTEGER NOT NULL CHECK (registered_voters >= 0),
    ballots_cast INTEGER NOT NULL CHECK (ballots_cast >= 0),
    valid_ballots INTEGER NOT NULL CHECK (valid_ballots >= 0),
    rejected_ballots INTEGER NOT NULL CHECK (rejected_ballots >= 0),
    undervotes INTEGER NOT NULL CHECK (undervotes >= 0),
    overvotes INTEGER NOT NULL CHECK (overvotes >= 0),
    hash_posted VARCHAR(128) NOT NULL,
    CONSTRAINT chk_sample_precinct_results_cast_le_registered CHECK (ballots_cast <= registered_voters)
);

CREATE INDEX idx_sample_precinct_results_precinct_id ON sample_precinct_results (precinct_id);

CREATE TABLE sample_operations_calendar (
    import_batch_id UUID NOT NULL REFERENCES sample_import_batches (id) ON DELETE CASCADE,
    election_id VARCHAR(128) NOT NULL,
    milestone_code VARCHAR(128) NOT NULL,
    name VARCHAR(255) NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    PRIMARY KEY (election_id, milestone_code)
);
