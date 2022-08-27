package com.example.demo.api;

import java.util.List;

import com.example.demo.api.exception.QueueFullException;

public interface Queue {

    public void subscribe(QueueConsumer consumer, MessageFilteringCriteria expression);
    
    public void subscribe(QueueConsumer consumer, MessageFilteringCriteria expression, 
            List<QueueConsumer> predecessors);
    
    public void offer(Message message) throws QueueFullException;
    
    public void shutdown();

}
