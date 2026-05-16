CREATE TABLE kr_election_types (
    code VARCHAR(64) PRIMARY KEY,
    core_election_type VARCHAR(64) NOT NULL,
    korean_label VARCHAR(64) NOT NULL,
    slug VARCHAR(96) NOT NULL UNIQUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

INSERT INTO kr_election_types (code, core_election_type, korean_label, slug) VALUES
    ('PRESIDENTIAL_ELECTION', 'PRESIDENTIAL', '대통령선거', 'presidential-election'),
    ('NATIONAL_ASSEMBLY_ELECTION', 'PARLIAMENTARY', '국회의원선거', 'national-assembly-election'),
    ('LOCAL_ELECTION', 'LOCAL', '지방선거', 'local-election'),
    ('SUPERINTENDENT_ELECTION', 'LOCAL', '교육감선거', 'superintendent-election'),
    ('BY_ELECTION', 'REGIONAL', '보궐선거', 'by-election');

CREATE TABLE kr_jurisdictions (
    id UUID PRIMARY KEY,
    parent_jurisdiction_id UUID REFERENCES kr_jurisdictions(id),
    administrative_code VARCHAR(32) NOT NULL UNIQUE,
    name VARCHAR(128) NOT NULL,
    level VARCHAR(32) NOT NULL,
    constituency_code VARCHAR(32) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_kr_jurisdictions_level CHECK (level IN ('SIDO', 'SIGUNGU', 'EUPMYEONDONG')),
    CONSTRAINT chk_kr_jurisdictions_parent_required CHECK (
        (level = 'SIDO' AND parent_jurisdiction_id IS NULL)
        OR (level IN ('SIGUNGU', 'EUPMYEONDONG') AND parent_jurisdiction_id IS NOT NULL)
    )
);

CREATE OR REPLACE FUNCTION validate_kr_jurisdiction_parent_level()
RETURNS TRIGGER AS $$
DECLARE
    parent_level VARCHAR(32);
BEGIN
    IF TG_OP = 'UPDATE' AND OLD.level <> NEW.level THEN
        RAISE EXCEPTION 'KR jurisdiction level is immutable';
    END IF;

    IF NEW.level = 'SIDO' THEN
        IF NEW.parent_jurisdiction_id IS NOT NULL THEN
            RAISE EXCEPTION 'SIDO must not have a parent';
        END IF;
        RETURN NEW;
    END IF;

    SELECT level INTO parent_level
    FROM kr_jurisdictions
    WHERE id = NEW.parent_jurisdiction_id;

    IF NEW.level = 'SIGUNGU' AND parent_level <> 'SIDO' THEN
        RAISE EXCEPTION 'SIGUNGU parent must be SIDO';
    END IF;

    IF NEW.level = 'EUPMYEONDONG' AND parent_level <> 'SIGUNGU' THEN
        RAISE EXCEPTION 'EUPMYEONDONG parent must be SIGUNGU';
    END IF;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_kr_jurisdictions_parent_level
BEFORE INSERT OR UPDATE OF parent_jurisdiction_id, level ON kr_jurisdictions
FOR EACH ROW
EXECUTE FUNCTION validate_kr_jurisdiction_parent_level();

CREATE INDEX idx_kr_jurisdictions_parent ON kr_jurisdictions (parent_jurisdiction_id);
CREATE INDEX idx_kr_jurisdictions_constituency_code ON kr_jurisdictions (constituency_code);
CREATE INDEX idx_kr_jurisdictions_level ON kr_jurisdictions (level);
