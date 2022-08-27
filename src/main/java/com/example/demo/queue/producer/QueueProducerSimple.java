package com.example.demo.queue.producer;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.example.demo.api.Message;
import com.example.demo.api.Queue;
import com.example.demo.api.QueueProducer;
import com.example.demo.api.exception.QueueFullException;
import com.example.demo.queue.message.JSONMessage;

public class QueueProducerSimple implements QueueProducer{
    Logger logger = LoggerFactory.getLogger(QueueProducerSimple.class);
    
    String id;
    
    public QueueProducerSimple(String id) {
        this.id = id;
    }

    @Override
    public void produce(Queue queue, String message) throws QueueFullException {
        UUID uuid = UUID.randomUUID();
        Message m = new JSONMessage(uuid.toString(), message);
        m.setBody(message);
        queue.offer(m); 
        logger.info("Producer {}: added message: {}", id, message);
    }
}
