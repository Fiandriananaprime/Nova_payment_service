ALTER TABLE idempotency_keys
    DROP CONSTRAINT IF EXISTS chk_idempotency_status;

UPDATE idempotency_keys
SET status = UPPER(status);

ALTER TABLE idempotency_keys
    ALTER COLUMN status SET DEFAULT 'PROCESSING';

ALTER TABLE idempotency_keys
    ADD CONSTRAINT chk_idempotency_status
        CHECK (status IN ('PROCESSING', 'COMPLETED', 'FAILED'));
