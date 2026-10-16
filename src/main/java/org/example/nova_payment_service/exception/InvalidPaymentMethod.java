package org.example.nova_payment_service.exception;

public class InvalidPaymentMethod extends RuntimeException{
    public InvalidPaymentMethod(String message){
        super(message);
    }
}
