package org.example.nova_payment_service.exception;

public class PaymentMethodNotHandled extends RuntimeException {
    public PaymentMethodNotHandled(String message) {
        super(message);
    }
}
