-- V4: history rows are immutable. UPDATE is rejected for every client (JPA, scripts, psql).
-- INSERT and DELETE stay allowed; DELETE is reserved for retention and will be restricted
-- by database roles later.

CREATE FUNCTION reject_alert_status_history_update()
RETURNS trigger
LANGUAGE plpgsql
AS $$
BEGIN
    RAISE EXCEPTION 'alert_status_history rows are immutable (id %)', OLD.id
        USING ERRCODE = 'restrict_violation';
END;
$$;

CREATE TRIGGER alert_status_history_block_update
    BEFORE UPDATE ON alert_status_history
    FOR EACH ROW
    EXECUTE FUNCTION reject_alert_status_history_update();
