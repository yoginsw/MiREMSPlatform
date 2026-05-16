CREATE TABLE us_jurisdictions (
    id UUID PRIMARY KEY,
    parent_jurisdiction_id UUID REFERENCES us_jurisdictions(id),
    fips_code VARCHAR(32) NOT NULL,
    precinct_code VARCHAR(64),
    name VARCHAR(128) NOT NULL,
    level VARCHAR(32) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_us_jurisdictions_level CHECK (level IN ('STATE', 'COUNTY', 'PRECINCT')),
    CONSTRAINT chk_us_jurisdictions_parent_required CHECK (
        (level = 'STATE' AND parent_jurisdiction_id IS NULL)
        OR (level IN ('COUNTY', 'PRECINCT') AND parent_jurisdiction_id IS NOT NULL)
    ),
    CONSTRAINT chk_us_jurisdictions_fips_shape CHECK (
        (level = 'STATE' AND fips_code ~ '^[0-9]{2}$' AND precinct_code IS NULL)
        OR (level = 'COUNTY' AND fips_code ~ '^[0-9]{5}$' AND precinct_code IS NULL)
        OR (level = 'PRECINCT' AND fips_code ~ '^[0-9]{5}$' AND precinct_code ~ '^[A-Za-z0-9][A-Za-z0-9_-]*$')
    )
);

CREATE OR REPLACE FUNCTION validate_us_jurisdiction_parent_level()
RETURNS TRIGGER AS $$
DECLARE
    parent_level VARCHAR(32);
    parent_fips VARCHAR(32);
BEGIN
    IF TG_OP = 'UPDATE' AND OLD.level <> NEW.level THEN
        RAISE EXCEPTION 'US jurisdiction level is immutable';
    END IF;

    IF TG_OP = 'UPDATE' AND OLD.fips_code <> NEW.fips_code THEN
        RAISE EXCEPTION 'US jurisdiction FIPS is immutable';
    END IF;

    IF NEW.level = 'STATE' THEN
        IF NEW.parent_jurisdiction_id IS NOT NULL THEN
            RAISE EXCEPTION 'STATE must not have a parent';
        END IF;
        RETURN NEW;
    END IF;

    SELECT level, fips_code INTO parent_level, parent_fips
    FROM us_jurisdictions
    WHERE id = NEW.parent_jurisdiction_id;

    IF NEW.level = 'COUNTY' THEN
        IF parent_level <> 'STATE' THEN
            RAISE EXCEPTION 'COUNTY parent must be STATE';
        END IF;
        IF substring(NEW.fips_code from 1 for 2) <> parent_fips THEN
            RAISE EXCEPTION 'county FIPS must start with parent state FIPS';
        END IF;
    END IF;

    IF NEW.level = 'PRECINCT' THEN
        IF parent_level <> 'COUNTY' THEN
            RAISE EXCEPTION 'PRECINCT parent must be COUNTY';
        END IF;
        IF NEW.fips_code <> parent_fips THEN
            RAISE EXCEPTION 'precinct mapping must start with parent county FIPS';
        END IF;
    END IF;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_us_jurisdictions_parent_level
BEFORE INSERT OR UPDATE OF parent_jurisdiction_id, level, fips_code ON us_jurisdictions
FOR EACH ROW
EXECUTE FUNCTION validate_us_jurisdiction_parent_level();

CREATE TABLE us_electoral_districts (
    id UUID PRIMARY KEY,
    jurisdiction_id UUID NOT NULL REFERENCES us_jurisdictions(id),
    district_code VARCHAR(64) NOT NULL UNIQUE,
    name VARCHAR(128) NOT NULL,
    district_type VARCHAR(64) NOT NULL,
    seat_count INTEGER NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_us_electoral_districts_type CHECK (district_type IN ('STATE_LEGISLATURE', 'LOCAL_AT_LARGE')),
    CONSTRAINT chk_us_electoral_districts_type_seat_count CHECK (
        (district_type = 'STATE_LEGISLATURE' AND seat_count = 1)
        OR (district_type = 'LOCAL_AT_LARGE' AND seat_count BETWEEN 2 AND 99)
    )
);

CREATE UNIQUE INDEX uq_us_jurisdictions_state_fips ON us_jurisdictions (fips_code) WHERE level = 'STATE';
CREATE UNIQUE INDEX uq_us_jurisdictions_county_fips ON us_jurisdictions (fips_code) WHERE level = 'COUNTY';
CREATE UNIQUE INDEX uq_us_jurisdictions_precinct_mapping ON us_jurisdictions (fips_code, precinct_code) WHERE level = 'PRECINCT';
CREATE INDEX idx_us_jurisdictions_parent ON us_jurisdictions (parent_jurisdiction_id);
CREATE INDEX idx_us_jurisdictions_fips_code ON us_jurisdictions (fips_code);
CREATE INDEX idx_us_jurisdictions_level ON us_jurisdictions (level);
CREATE INDEX idx_us_electoral_districts_jurisdiction ON us_electoral_districts (jurisdiction_id);
CREATE INDEX idx_us_electoral_districts_type ON us_electoral_districts (district_type);
