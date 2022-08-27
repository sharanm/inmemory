package com.example.demo.api;

import com.example.demo.api.exception.QueueFullException;

public interface QueueProducer {
    
    public void produce(Queue queue, String message) throws QueueFullException;
}
