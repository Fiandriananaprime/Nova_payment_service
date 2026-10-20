package org.example.nova_payment_service.exception;

public class PaymentMethodAlreadyVerified extends RuntimeException {

    public PaymentMethodAlreadyVerified(String message) {
        super(message);
    }
}