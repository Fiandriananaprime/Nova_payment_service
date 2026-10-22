CREATE TABLE idempotency_keys (
                                  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                                  seller_id UUID NOT NULL,
                                  idempotency_key UUID NOT NULL,
                                  request_hash VARCHAR(64) NOT NULL,
                                  payout_id UUID,
                                  status VARCHAR(20) NOT NULL DEFAULT 'processing',
                                  response_status INTEGER,
                                  response_body JSONB,
                                  created_at TIMESTAMPTZ(6) NOT NULL DEFAULT NOW(),
                                  expires_at TIMESTAMPTZ(6),
                                  CONSTRAINT uq_idempotency_seller_key UNIQUE (seller_id, idempotency_key),
                                  CONSTRAINT chk_idempotency_status CHECK (
                                      status IN ('processing', 'completed', 'failed')
                                      ),
                                  CONSTRAINT chk_idempotency_response_status CHECK (
                                      response_status IS NULL OR response_status BETWEEN 100 AND 599
                                      )
);

CREATE INDEX idx_idempotency_expires_at
    ON idempotency_keys (expires_at)
    WHERE expires_at IS NOT NULL;

CREATE INDEX idx_idempotency_payout_id
    ON idempotency_keys (payout_id)
    WHERE payout_id IS NOT NULL;