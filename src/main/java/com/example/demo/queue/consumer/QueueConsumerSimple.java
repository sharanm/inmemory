package com.example.demo.queue.consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.example.demo.api.Message;
import com.example.demo.api.QueueConsumer;
import com.example.demo.queue.producer.QueueProducerSimple;

public class QueueConsumerSimple implements QueueConsumer{
    Logger logger = LoggerFactory.getLogger(QueueProducerSimple.class);
    
    String id;
    
    public QueueConsumerSimple(String id) {
        this.id = id;
    }
    
    @Override    
    public void process(Message message) {
        logger.info("Consumer {}: consumed message: {}", id, message.getBody());
    }

    @Override
    public String getId() {
        return id;
    }
}
