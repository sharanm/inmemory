package com.example.demo.api.exception;

public class QueueFullException extends Exception{

    private static final long serialVersionUID = 1L;
    
    public QueueFullException(String message) {
        super(message);
    }

}
