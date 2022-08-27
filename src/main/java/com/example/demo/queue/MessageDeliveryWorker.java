package com.example.demo.queue;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.example.demo.api.Message;
import com.example.demo.api.Queue;
import com.example.demo.api.QueueConsumer;
import com.example.demo.api.exception.MessageDeliveryException;
import com.example.demo.api.exception.QueueFullException;

public class MessageDeliveryWorker implements Runnable{
    
    Logger logger = LoggerFactory.getLogger(MessageDeliveryWorker.class);

    Message message;
    Map<QueueConsumer, ConsumerContext> consumerContextMap = new HashMap<>();
    long ttl;
    TimeUnit ttlUnit;
    private Queue dlq;
    Map<QueueConsumer, Boolean> delivery = new HashMap<>();
    
    public MessageDeliveryWorker(Message message, Map<QueueConsumer, ConsumerContext> consumerContextMap,
            Queue dlq, long ttl, TimeUnit ttlUnit) {
        super();
        this.message = message;
        this.consumerContextMap = consumerContextMap;
        this.dlq = dlq;
        this.ttl = ttl;
        this.ttlUnit = ttlUnit;
    }

    private void deliver(QueueConsumer consumer, Message message, Map<QueueConsumer, Boolean> delivery) throws QueueFullException {
        if(delivery.containsKey(consumer) && delivery.get(consumer) == true) {
            return;
        }
        
        try{
            for(QueueConsumer predecessor: consumerContextMap.get(consumer).getPredecessors()) {
                deliver(predecessor, message, delivery);
            }
            logger.debug("Delivered to all predecessor of consumer: {}", consumer.getId());
            deliver0(consumer, message);
            logger.debug("Delivered to consumer: {}", consumer.getId());
            delivery.put(consumer, true);
        }
        catch (MessageDeliveryException e) {
             if(dlq != null) {
                 dlq.offer(message);
             }
        }
        
    }
    
    private void deliver0(QueueConsumer consumer, Message message) throws MessageDeliveryException{
        for(int attempts = 3; attempts > 0; attempts--) {
            try{
                logger.debug("Performing TTL calculation for consumer: {}", consumer.getId());
                if(ttlUnit != null && message.getIngestionTime()
                        .plusMillis(ttlUnit.toMillis(ttl)).isBefore(Instant.now())) {
                  return;
                }
                logger.debug("Invoking process for consumer: {}", consumer.getId());
                consumer.process(message);
                return;
            }
            catch (Exception e){
                logger.error(String.format("Unable to process message for consumer:%s, message:%s: {}",
                        consumer, message), e);
            }
        }
        
        throw new MessageDeliveryException("Error occured");

    }

    @Override
    public void run() {
        consumerContextMap.keySet().forEach((consumer) -> {
            try {
                if(consumerContextMap.get(consumer).getExpression().test(message)) {
                    logger.debug("Trying to deliver to consumer: {}", consumer.getId());
                    deliver(consumer, message, delivery);
                }
            } catch (QueueFullException e) {
                logger.error("Exception while delivering: {}", e);
            }
        });        
    }
}
