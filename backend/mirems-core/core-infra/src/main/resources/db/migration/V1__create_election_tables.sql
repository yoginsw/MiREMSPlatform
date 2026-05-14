CREATE TABLE elections (
    id UUID PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    election_type VARCHAR(64) NOT NULL,
    jurisdiction VARCHAR(255) NOT NULL,
    scheduled_date DATE NOT NULL,
    country_code CHAR(2) NOT NULL,
    extension_pack_id VARCHAR(255) NOT NULL,
    election_status VARCHAR(64) NOT NULL
);

CREATE INDEX idx_elections_status ON elections (election_status);
CREATE INDEX idx_elections_country_code ON elections (country_code);

CREATE TABLE contests (
    id UUID PRIMARY KEY,
    election_id UUID NOT NULL REFERENCES elections (id),
    contest_type VARCHAR(64) NOT NULL,
    name VARCHAR(255) NOT NULL,
    seats INTEGER NOT NULL CHECK (seats > 0),
    vote_limit INTEGER NOT NULL CHECK (vote_limit > 0),
    CONSTRAINT chk_contests_vote_limit CHECK (vote_limit <= seats)
);

CREATE INDEX idx_contests_election_id ON contests (election_id);

CREATE TABLE candidates (
    id UUID PRIMARY KEY,
    contest_id UUID NOT NULL REFERENCES contests (id),
    name VARCHAR(255) NOT NULL,
    party_affiliation VARCHAR(255) NOT NULL,
    candidate_status VARCHAR(64) NOT NULL
);

CREATE INDEX idx_candidates_contest_id ON candidates (contest_id);
CREATE INDEX idx_candidates_status ON candidates (candidate_status);
