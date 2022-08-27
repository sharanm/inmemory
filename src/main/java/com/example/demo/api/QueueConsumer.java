package com.example.demo.api;

public interface QueueConsumer {
    
    public String getId();
    
    public void process(Message message);
}
