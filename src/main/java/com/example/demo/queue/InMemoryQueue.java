package com.example.demo.queue;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.TemporalAccessor;
import java.time.temporal.TemporalUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.example.demo.api.Message;
import com.example.demo.api.MessageFilteringCriteria;
import com.example.demo.api.Queue;
import com.example.demo.api.QueueConsumer;
import com.example.demo.api.exception.QueueFullException;
import com.example.demo.reaper.QueueResourceReaper;

public class InMemoryQueue implements Queue {
    
    final int MAX_DELIVERY_THREADS = 5; 
    Logger logger = LoggerFactory.getLogger(InMemoryQueue.class);

    LinkedList<Message> messages;
    Map<QueueConsumer, ConsumerContext> consumerContextMap = new HashMap<>();
    int size;
    long ttl;
    TimeUnit ttlUnit;
    ExecutorService executorService;
    QueueResourceReaper queueResourceReaper;
    private Queue dlq;
    
    public InMemoryQueue(int size) {
        this(size, null, 100, TimeUnit.SECONDS);
    }
    
    public InMemoryQueue(int size, long ttl, TimeUnit unit) {
        this(size, null, ttl, unit);
    }
    
    public InMemoryQueue(int size, Queue dlq, long ttl, TimeUnit seconds) {
        messages = new LinkedList<>();
        this.size = size;
        this.ttlUnit = seconds;
        this.ttl = ttl;
        this.dlq = dlq;
        executorService = Executors.newFixedThreadPool(MAX_DELIVERY_THREADS);
        queueResourceReaper = new QueueResourceReaper(messages);
        new Thread(queueResourceReaper).start();
    }
    
    public void subscribe(QueueConsumer consumer, MessageFilteringCriteria expression) {
        consumerContextMap.put(consumer, new ConsumerContext(consumer, expression, new ArrayList<>()));
    }
    
    public void subscribe(QueueConsumer consumer, MessageFilteringCriteria expression, 
            List<QueueConsumer> predecessors) {
        consumerContextMap.put(consumer, new ConsumerContext(consumer, expression, predecessors));
    }
    
    public synchronized void offer(Message message) throws QueueFullException{
        if (messages.size() < size) {
            message.setIngestionTime(Instant.now());
            messages.add(message);
            process(message);
            return;
        }
        throw new QueueFullException("Queue full, size: " + size);
    }

    private void process(Message message){
        Future<?> messageDelivery = executorService.submit(new MessageDeliveryWorker(message, consumerContextMap, dlq,
                ttl, ttlUnit));
        queueResourceReaper.monitorDelivery(messageDelivery, message);
    }
    
    public void shutdown() {
        logger.debug("Initiating queue shutdown");
        try {
            executorService.awaitTermination(1, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            logger.error("Error while terminating thread pool: {}", e);
            executorService.shutdown();
        }
        queueResourceReaper.shutdown();
    }
}
