package org.example.nova_payment_service.exception;

public class PaymentMethodNotFound extends RuntimeException{
    public PaymentMethodNotFound( String message){
        super(message);
    }
}
