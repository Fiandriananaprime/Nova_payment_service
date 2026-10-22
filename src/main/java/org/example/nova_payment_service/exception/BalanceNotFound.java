package org.example.nova_payment_service.exception;

public class BalanceNotFound extends RuntimeException {
    public BalanceNotFound(String message) {
        super(message);
    }
}
