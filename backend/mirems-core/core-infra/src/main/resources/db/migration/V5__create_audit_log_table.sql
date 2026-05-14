CREATE TABLE audit_events (
    id UUID PRIMARY KEY,
    event_type VARCHAR(255) NOT NULL,
    aggregate_id UUID NOT NULL,
    aggregate_type VARCHAR(255) NOT NULL,
    payload JSONB NOT NULL,
    actor_id VARCHAR(255) NOT NULL,
    occurred_at TIMESTAMP WITH TIME ZONE NOT NULL,
    source_ip VARCHAR(128)
);

CREATE INDEX idx_audit_events_event_type ON audit_events (event_type);
CREATE INDEX idx_audit_events_aggregate_id_occurred_at ON audit_events (aggregate_id, occurred_at);
CREATE INDEX idx_audit_events_actor_id_occurred_at ON audit_events (actor_id, occurred_at);
