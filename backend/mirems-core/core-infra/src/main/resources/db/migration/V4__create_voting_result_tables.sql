CREATE TABLE voting_results (
    id UUID PRIMARY KEY,
    session_id UUID NOT NULL REFERENCES voting_sessions (id),
    contest_id UUID NOT NULL REFERENCES contests (id),
    selected_candidate_ids JSONB NOT NULL,
    cast_at TIMESTAMP WITH TIME ZONE NOT NULL,
    hash CHAR(64) NOT NULL
);

CREATE INDEX idx_voting_results_session_id ON voting_results (session_id);
CREATE INDEX idx_voting_results_contest_id ON voting_results (contest_id);
CREATE INDEX idx_voting_results_cast_at ON voting_results (cast_at);

CREATE TABLE vote_corrections (
    id UUID PRIMARY KEY,
    original_voting_result_id UUID NOT NULL REFERENCES voting_results (id),
    corrected_candidate_ids JSONB NOT NULL,
    reason TEXT NOT NULL,
    requested_by VARCHAR(255) NOT NULL,
    requested_at TIMESTAMP WITH TIME ZONE NOT NULL,
    correction_status VARCHAR(64) NOT NULL
);

CREATE INDEX idx_vote_corrections_original_result_id ON vote_corrections (original_voting_result_id);
CREATE INDEX idx_vote_corrections_status ON vote_corrections (correction_status);
