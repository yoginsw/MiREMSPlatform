CREATE TABLE tabulation_reports (
    id UUID PRIMARY KEY,
    election_id UUID NOT NULL REFERENCES elections (id),
    contest_tallies JSONB NOT NULL,
    generated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    locked_at TIMESTAMP WITH TIME ZONE,
    hash CHAR(64),
    published BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE INDEX idx_tabulation_reports_election_id ON tabulation_reports (election_id);
CREATE INDEX idx_tabulation_reports_hash ON tabulation_reports (hash);
