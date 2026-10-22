CREATE TABLE seller_balance_history (
                                        id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                                        seller_id UUID NOT NULL,
                                        payout_id UUID NOT NULL ,
                                        balance NUMERIC(19,4) NOT NULL ,
                                        currency CHAR(3) NOT NULL DEFAULT 'MGA',
                                        recorded_at TIMESTAMPTZ(6) NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_seller_balance_history_time
    ON seller_balance_history (seller_id, recorded_at DESC);