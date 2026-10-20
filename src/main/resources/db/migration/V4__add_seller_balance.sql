
-- ============================================================================
-- SELLER BALANCES
-- ============================================================================

CREATE TABLE seller_balances (
                                 seller_id UUID PRIMARY KEY,

                                 available_amount NUMERIC(19, 4) NOT NULL DEFAULT 0,
                                 pending_amount NUMERIC(19, 4) NOT NULL DEFAULT 0,
                                 reserved_amount NUMERIC(19, 4) NOT NULL DEFAULT 0,

                                 currency CHAR(3) NOT NULL DEFAULT 'MGA',

                                 created_at TIMESTAMPTZ(6) NOT NULL DEFAULT NOW(),
                                 updated_at TIMESTAMPTZ(6) NOT NULL DEFAULT NOW()

    -- No FK: seller belongs to another microservice.
);

CREATE TRIGGER trg_seller_balances_updated_at
    BEFORE UPDATE ON seller_balances
    FOR EACH ROW
    EXECUTE FUNCTION set_updated_at();
