CREATE TABLE ballots (
    id UUID PRIMARY KEY,
    election_id UUID NOT NULL REFERENCES elections (id),
    ballot_version INTEGER NOT NULL CHECK (ballot_version > 0),
    active BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE INDEX idx_ballots_election_id ON ballots (election_id);
CREATE INDEX idx_ballots_active ON ballots (active);

CREATE TABLE ballot_contests (
    ballot_id UUID NOT NULL REFERENCES ballots (id) ON DELETE CASCADE,
    contest_id UUID NOT NULL REFERENCES contests (id),
    display_order INTEGER NOT NULL CHECK (display_order > 0),
    presentation_title VARCHAR(255) NOT NULL,
    PRIMARY KEY (ballot_id, contest_id)
);

CREATE INDEX idx_ballot_contests_contest_id ON ballot_contests (contest_id);

CREATE TABLE ballot_styles (
    id UUID PRIMARY KEY,
    ballot_id UUID NOT NULL REFERENCES ballots (id) ON DELETE CASCADE,
    style_code VARCHAR(128) NOT NULL,
    district VARCHAR(255) NOT NULL,
    language CHAR(2) NOT NULL,
    accessibility_features JSONB NOT NULL DEFAULT '[]'::jsonb,
    CONSTRAINT uq_ballot_styles_style_code UNIQUE (style_code)
);

CREATE INDEX idx_ballot_styles_ballot_id ON ballot_styles (ballot_id);
CREATE INDEX idx_ballot_styles_district_language ON ballot_styles (district, language);
