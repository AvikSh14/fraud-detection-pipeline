-- V1: create the alerts table — the core record for a transaction Flink flagged as
-- potentially fraudulent. This is the system of record api_service reads and writes.

CREATE TABLE alerts (
    id                    UUID PRIMARY KEY,
    transaction_reference VARCHAR(255) NOT NULL,
    risk_score            DOUBLE PRECISION NOT NULL,
    status                VARCHAR(20) NOT NULL DEFAULT 'NEW'
                               CHECK (status IN ('NEW', 'IN_REVIEW', 'CONFIRMED_FRAUD', 'FALSE_POSITIVE')),
    created_at            TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at            TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_alerts_status ON alerts (status);
