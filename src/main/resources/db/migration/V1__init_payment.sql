-- ============================================================================
-- NovaMarket - Payment Service
-- PostgreSQL schema
-- ============================================================================

CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- ============================================================================
-- ENUMS
-- ============================================================================

CREATE TYPE payment_method_type AS ENUM (
    'card',
    'mvola',
    'orange_money'
);

CREATE TYPE transaction_method AS ENUM (
    'mvola',
    'orange_money',
    'card',
    'cod'
);

CREATE TYPE transaction_status AS ENUM (
    'completed',
    'pending',
    'failed',
    'refunded'
);

CREATE TYPE refund_status AS ENUM (
    'requested',
    'processed',
    'failed'
);

CREATE TYPE payout_method_type AS ENUM (
    'mvola',
    'orange_money',
    'bank_account'
);

CREATE TYPE payout_status AS ENUM (
    'pending',
    'processing',
    'completed',
    'failed',
    'cancelled'
);

-- ============================================================================
-- PAYMENT METHODS
-- ============================================================================

CREATE TABLE payment_methods (
                                 id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    -- Owned by auth/user service.
    -- No FK because this is a different microservice/database.
                                 user_id UUID NOT NULL,

                                 type payment_method_type NOT NULL,

    -- Payment provider information
                                 provider VARCHAR(100),
                                 provider_payment_method_id VARCHAR(255),

    -- User-facing metadata
                                 label VARCHAR(100),

    -- Card display information
                                 brand VARCHAR(50),
                                 last4 VARCHAR(4),
                                 exp_month SMALLINT,
                                 exp_year SMALLINT,

    -- Mainly used for mobile money
                                 phone VARCHAR(30),

                                 is_default BOOLEAN NOT NULL DEFAULT FALSE,
                                 verified BOOLEAN NOT NULL DEFAULT FALSE,

                                 created_at TIMESTAMPTZ(6) NOT NULL DEFAULT NOW(),
                                 updated_at TIMESTAMPTZ(6) NOT NULL DEFAULT NOW(),

                                 CONSTRAINT chk_payment_method_card_data
                                     CHECK (
                                         type <> 'card'
                                             OR (
                                             brand IS NOT NULL
                                                 AND last4 IS NOT NULL
                                                 AND exp_month IS NOT NULL
                                                 AND exp_year IS NOT NULL
                                             )
                                         ),

                                 CONSTRAINT chk_payment_method_last4
                                     CHECK (
                                         last4 IS NULL
                                             OR last4 ~ '^[0-9]{4}$'
),

    CONSTRAINT chk_payment_method_exp_month
        CHECK (
            exp_month IS NULL
            OR exp_month BETWEEN 1 AND 12
        ),

    CONSTRAINT chk_payment_method_exp_year
        CHECK (
            exp_year IS NULL
            OR exp_year BETWEEN 2000 AND 2500
        ),

    CONSTRAINT chk_payment_method_mobile_phone
        CHECK (
            type NOT IN ('mvola', 'orange_money')
            OR phone IS NOT NULL
        ),

    CONSTRAINT chk_payment_method_card_no_phone
        CHECK (
            type <> 'card'
            OR phone IS NULL
        ),

    CONSTRAINT chk_payment_method_card_provider
        CHECK (
            type <> 'card'
            OR provider_payment_method_id IS NOT NULL
        )
);

CREATE INDEX idx_payment_methods_user
    ON payment_methods (user_id, created_at DESC);

CREATE INDEX idx_payment_methods_user_type
    ON payment_methods (user_id, type);

CREATE UNIQUE INDEX uq_payment_methods_provider_reference
    ON payment_methods (provider, provider_payment_method_id)
    WHERE provider_payment_method_id IS NOT NULL;

-- Only one default payment method per user
CREATE UNIQUE INDEX uq_payment_methods_user_default
    ON payment_methods (user_id)
    WHERE is_default = TRUE;

-- ============================================================================
-- TRANSACTIONS
-- ============================================================================

CREATE TABLE transactions (
                              id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    -- Owned by order service, therefore no FK.
                              order_id UUID NOT NULL,

    -- Owned by auth/user service, therefore no FK.
                              buyer_id UUID NOT NULL,
                              seller_id UUID NOT NULL,

                              amount NUMERIC(19, 4) NOT NULL,
                              method transaction_method NOT NULL,

    -- Marketplace commission
                              commission NUMERIC(19, 4) NOT NULL DEFAULT 0,

                              status transaction_status NOT NULL DEFAULT 'pending',

    -- References the PaymentMethod in this service.
    -- No Prisma/SQL FK is required, but the value is still a UUID.
                              payment_method_id UUID,

    -- Reference returned/generated by the PSP.
                              provider_reference VARCHAR(255),

                              failure_reason TEXT,

                              created_at TIMESTAMPTZ(6) NOT NULL DEFAULT NOW(),
                              updated_at TIMESTAMPTZ(6) NOT NULL DEFAULT NOW(),

                              CONSTRAINT chk_transaction_amount
                                  CHECK (amount > 0),

                              CONSTRAINT chk_transaction_commission
                                  CHECK (commission >= 0),

                              CONSTRAINT chk_transaction_commission_not_greater_than_amount
                                  CHECK (commission <= amount),

                              CONSTRAINT chk_transaction_payment_method
                                  CHECK (
                                      method = 'cod'
                                          OR payment_method_id IS NOT NULL
                                      )
);

CREATE INDEX idx_transactions_order
    ON transactions (order_id);

CREATE INDEX idx_transactions_payment_method
    ON transactions (payment_method_id);

CREATE INDEX idx_transactions_seller_status_date
    ON transactions (seller_id, status, created_at DESC);

CREATE INDEX idx_transactions_buyer_date
    ON transactions (buyer_id, created_at DESC);

CREATE INDEX idx_transactions_status_date
    ON transactions (status, created_at DESC);

CREATE INDEX idx_transactions_provider_reference
    ON transactions (provider_reference);

-- ============================================================================
-- REFUNDS
-- ============================================================================

CREATE TABLE refunds (
                         id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

                         transaction_id UUID NOT NULL,

                         amount NUMERIC(19, 4) NOT NULL,

                         reason TEXT,

                         status refund_status NOT NULL DEFAULT 'requested',

                         created_at TIMESTAMPTZ(6) NOT NULL DEFAULT NOW(),
                         processed_at TIMESTAMPTZ(6),

                         CONSTRAINT fk_refunds_transaction
                             FOREIGN KEY (transaction_id)
                                 REFERENCES transactions(id)
                                 ON DELETE RESTRICT,

                         CONSTRAINT chk_refund_amount
                             CHECK (amount > 0),

                         CONSTRAINT chk_refund_processed_at
                             CHECK (
                                 status <> 'processed'
                                     OR processed_at IS NOT NULL
                                 )
);

CREATE INDEX idx_refunds_transaction
    ON refunds (transaction_id, created_at DESC);

CREATE INDEX idx_refunds_status_date
    ON refunds (status, created_at DESC);

-- ============================================================================
-- SELLER PAYOUT METHODS
-- ============================================================================

CREATE TABLE seller_payout_methods (
                                       id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    -- Owned by auth/user/store service.
    -- No FK because it belongs to another microservice.
                                       seller_id UUID NOT NULL,

                                       type payout_method_type NOT NULL,

                                       label VARCHAR(100),

                                       account_name VARCHAR(255) NOT NULL,

    -- Sensitive data.
    -- Must be encrypted at the application layer before insertion.
                                       account_encrypted TEXT NOT NULL,

    -- Safe display value, e.g. ******4242
                                       masked_account VARCHAR(100) NOT NULL,

                                       is_default BOOLEAN NOT NULL DEFAULT FALSE,
                                       verified BOOLEAN NOT NULL DEFAULT FALSE,

                                       created_at TIMESTAMPTZ(6) NOT NULL DEFAULT NOW(),
                                       updated_at TIMESTAMPTZ(6) NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_seller_payout_methods_seller
    ON seller_payout_methods (seller_id, created_at DESC);

CREATE INDEX idx_seller_payout_methods_seller_type
    ON seller_payout_methods (seller_id, type);

-- Only one default payout method per seller
CREATE UNIQUE INDEX uq_seller_payout_methods_seller_default
    ON seller_payout_methods (seller_id)
    WHERE is_default = TRUE;

-- ============================================================================
-- PAYOUTS
-- ============================================================================

CREATE TABLE payouts (
                         id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    -- Owned by seller/store service.
    -- No FK because this is another microservice.
                         seller_id UUID NOT NULL,

                         amount NUMERIC(19, 4) NOT NULL,

                         status payout_status NOT NULL DEFAULT 'pending',

    -- Snapshot of the payout method actually used.
                         method payout_method_type NOT NULL,

    -- FK only because SellerPayoutMethod belongs to this same database.
                         payout_method_id UUID,

                         created_at TIMESTAMPTZ(6) NOT NULL DEFAULT NOW(),
                         paid_at TIMESTAMPTZ(6),

                         failure_reason TEXT,

                         CONSTRAINT fk_payouts_payout_method
                             FOREIGN KEY (payout_method_id)
                                 REFERENCES seller_payout_methods(id)
                                 ON DELETE SET NULL,

                         CONSTRAINT chk_payout_amount
                             CHECK (amount > 0),

                         CONSTRAINT chk_payout_paid_at
                             CHECK (
                                 status <> 'completed'
                                     OR paid_at IS NOT NULL
                                 )
);

CREATE INDEX idx_payouts_seller_status_date
    ON payouts (seller_id, status, created_at DESC);

CREATE INDEX idx_payouts_payout_method
    ON payouts (payout_method_id);

CREATE INDEX idx_payouts_status_date
    ON payouts (status, created_at DESC);

-- ============================================================================
-- UPDATED_AT
-- ============================================================================

CREATE OR REPLACE FUNCTION set_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_payment_methods_updated_at
    BEFORE UPDATE ON payment_methods
    FOR EACH ROW
    EXECUTE FUNCTION set_updated_at();

CREATE TRIGGER trg_transactions_updated_at
    BEFORE UPDATE ON transactions
    FOR EACH ROW
    EXECUTE FUNCTION set_updated_at();

CREATE TRIGGER trg_seller_payout_methods_updated_at
    BEFORE UPDATE ON seller_payout_methods
    FOR EACH ROW
    EXECUTE FUNCTION set_updated_at();

-- ============================================================================
-- REFUND TOTAL PROTECTION
-- ============================================================================
-- Prevents total refunds from exceeding the original transaction amount.

CREATE OR REPLACE FUNCTION validate_refund_amount()
RETURNS TRIGGER AS $$
DECLARE
transaction_amount NUMERIC(19, 4);
    already_refunded NUMERIC(19, 4);
BEGIN
SELECT amount
INTO transaction_amount
FROM transactions
WHERE id = NEW.transaction_id;

SELECT COALESCE(SUM(amount), 0)
INTO already_refunded
FROM refunds
WHERE transaction_id = NEW.transaction_id
  AND status = 'processed'
  AND id <> NEW.id;

IF already_refunded + NEW.amount > transaction_amount THEN
        RAISE EXCEPTION
            'Refund total (%) exceeds transaction amount (%)',
            already_refunded + NEW.amount,
            transaction_amount;
END IF;

RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_validate_refund_amount
    BEFORE INSERT OR UPDATE ON refunds
                         FOR EACH ROW
                         EXECUTE FUNCTION validate_refund_amount();