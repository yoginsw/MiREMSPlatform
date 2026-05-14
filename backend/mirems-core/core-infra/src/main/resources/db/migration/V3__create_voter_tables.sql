CREATE TABLE voter_records (
    id UUID PRIMARY KEY,
    encrypted_external_voter_id TEXT NOT NULL,
    eligible_elections JSONB NOT NULL,
    registration_status VARCHAR(64) NOT NULL
);

CREATE INDEX idx_voter_records_registration_status ON voter_records (registration_status);

CREATE TABLE voting_sessions (
    id UUID PRIMARY KEY,
    voter_record_id UUID NOT NULL REFERENCES voter_records (id),
    election_id UUID NOT NULL REFERENCES elections (id),
    ballot_style_id UUID NOT NULL REFERENCES ballot_styles (id),
    started_at TIMESTAMP WITH TIME ZONE NOT NULL,
    completed_at TIMESTAMP WITH TIME ZONE,
    session_status VARCHAR(64) NOT NULL,
    device_id VARCHAR(255) NOT NULL
);

CREATE INDEX idx_voting_sessions_voter_record_id ON voting_sessions (voter_record_id);
CREATE INDEX idx_voting_sessions_election_id ON voting_sessions (election_id);
CREATE INDEX idx_voting_sessions_status ON voting_sessions (session_status);
CREATE UNIQUE INDEX uq_voting_sessions_non_spoiled_per_election
    ON voting_sessions (voter_record_id, election_id)
    WHERE session_status <> 'SPOILED';
