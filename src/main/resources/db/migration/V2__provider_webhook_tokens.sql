ALTER TABLE transactions
    ADD COLUMN provider_notification_token_hash CHAR(64),
    ADD COLUMN provider_transaction_reference VARCHAR(255);

CREATE UNIQUE INDEX uq_transactions_provider_notification_token
    ON transactions (provider_notification_token_hash)
    WHERE provider_notification_token_hash IS NOT NULL;

CREATE UNIQUE INDEX uq_transactions_mobile_provider_reference
    ON transactions (method, provider_reference)
    WHERE method IN ('mvola', 'orange_money')
      AND provider_reference IS NOT NULL;
