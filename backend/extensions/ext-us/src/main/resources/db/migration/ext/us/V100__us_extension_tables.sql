-- US extension scaffold tables. Detailed jurisdiction/type tables are added in later P7 goals.
CREATE TABLE us_extension_metadata (
    id UUID PRIMARY KEY,
    extension_pack_id VARCHAR(32) NOT NULL DEFAULT 'us',
    country_code CHAR(2) NOT NULL DEFAULT 'US',
    schema_version INTEGER NOT NULL,
    enabled_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_us_extension_metadata_pack CHECK (extension_pack_id = 'us'),
    CONSTRAINT chk_us_extension_metadata_country CHECK (country_code = 'US')
);

CREATE TABLE us_election_type_mappings (
    id UUID PRIMARY KEY,
    election_id UUID NOT NULL REFERENCES elections(id),
    extension_pack_id VARCHAR(32) NOT NULL DEFAULT 'us',
    us_election_type_code VARCHAR(64) NOT NULL,
    legal_basis TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_us_election_type_mappings_election UNIQUE (election_id),
    CONSTRAINT chk_us_election_type_mappings_pack CHECK (extension_pack_id = 'us')
);

CREATE INDEX idx_us_election_type_mappings_election_id ON us_election_type_mappings (election_id);
CREATE INDEX idx_us_election_type_mappings_type_code ON us_election_type_mappings (us_election_type_code);
