package org.example.nova_payment_service.exception;

public class BalanceException extends RuntimeException {
    public BalanceException(String message) {
        super(message);
    }
}
