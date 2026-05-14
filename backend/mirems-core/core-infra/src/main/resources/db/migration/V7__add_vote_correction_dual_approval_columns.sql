ALTER TABLE vote_corrections
    ADD COLUMN first_approved_by VARCHAR(255),
    ADD COLUMN first_approved_at TIMESTAMP WITH TIME ZONE,
    ADD COLUMN second_approved_by VARCHAR(255),
    ADD COLUMN second_approved_at TIMESTAMP WITH TIME ZONE;

CREATE INDEX idx_vote_corrections_first_approved_by ON vote_corrections (first_approved_by);
CREATE INDEX idx_vote_corrections_second_approved_by ON vote_corrections (second_approved_by);
