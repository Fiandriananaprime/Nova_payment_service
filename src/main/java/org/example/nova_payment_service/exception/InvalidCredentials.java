package org.example.nova_payment_service.exception;

public class InvalidCredentials extends RuntimeException{
    public InvalidCredentials( String message){
        super(message);
    }
}
