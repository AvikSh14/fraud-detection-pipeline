-- V3: optimistic locking for alerts. Existing rows start at version 0.
ALTER TABLE alerts ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
