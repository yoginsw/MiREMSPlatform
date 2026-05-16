-- KR extension scaffold tables. Detailed jurisdiction/type tables are added in later P6 goals.
CREATE TABLE kr_extension_metadata (
    id UUID PRIMARY KEY,
    extension_pack_id VARCHAR(32) NOT NULL DEFAULT 'kr',
    country_code CHAR(2) NOT NULL DEFAULT 'KR',
    schema_version INTEGER NOT NULL,
    enabled_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_kr_extension_metadata_pack CHECK (extension_pack_id = 'kr'),
    CONSTRAINT chk_kr_extension_metadata_country CHECK (country_code = 'KR')
);

CREATE TABLE kr_election_type_mappings (
    id UUID PRIMARY KEY,
    election_id UUID NOT NULL REFERENCES elections(id),
    extension_pack_id VARCHAR(32) NOT NULL DEFAULT 'kr',
    kr_election_type_code VARCHAR(64) NOT NULL,
    legal_basis TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_kr_election_type_mappings_election UNIQUE (election_id),
    CONSTRAINT chk_kr_election_type_mappings_pack CHECK (extension_pack_id = 'kr')
);

CREATE INDEX idx_kr_election_type_mappings_election_id ON kr_election_type_mappings (election_id);
CREATE INDEX idx_kr_election_type_mappings_type_code ON kr_election_type_mappings (kr_election_type_code);
