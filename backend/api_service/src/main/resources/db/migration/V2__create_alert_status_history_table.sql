-- V2: append-only audit log of every status transition an alert goes through.
-- Rows are only ever inserted, never updated or deleted.

CREATE TABLE alert_status_history (
    id              UUID PRIMARY KEY,
    alert_id        UUID NOT NULL REFERENCES alerts (id),
    previous_status VARCHAR(20) NOT NULL
                        CHECK (previous_status IN ('NEW', 'IN_REVIEW', 'CONFIRMED_FRAUD', 'FALSE_POSITIVE')),
    new_status      VARCHAR(20) NOT NULL
                        CHECK (new_status IN ('NEW', 'IN_REVIEW', 'CONFIRMED_FRAUD', 'FALSE_POSITIVE')),
    changed_by      VARCHAR(255) NOT NULL,
    changed_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_alert_status_history_alert_id ON alert_status_history (alert_id);
