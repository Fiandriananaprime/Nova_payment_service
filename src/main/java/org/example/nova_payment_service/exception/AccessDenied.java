package org.example.nova_payment_service.exception;

public class AccessDenied extends RuntimeException{
    public AccessDenied (String message){
        super(message);
    }
}
