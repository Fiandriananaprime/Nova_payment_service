
CREATE TABLE phone_verifications (
                                     id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

                                     user_id UUID NOT NULL,

                                     phone VARCHAR(30) NOT NULL,

                                     code_hash VARCHAR(64) NOT NULL,

                                     expires_at TIMESTAMPTZ NOT NULL,

                                     attempts INTEGER NOT NULL DEFAULT 0,

                                     consumed BOOLEAN NOT NULL DEFAULT FALSE,

                                     created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_phone_verifications_user_phone_created
    ON phone_verifications (user_id, phone, created_at DESC);
