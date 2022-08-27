package com.example.demo.reaper;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.example.demo.api.Message;
import com.example.demo.queue.InMemoryQueue;

public class QueueResourceReaper implements Runnable{
    
    Queue<DeliveryFuture> tasks = new ConcurrentLinkedQueue<DeliveryFuture>();
    AtomicBoolean shutdownFlag = new AtomicBoolean(false);
    LinkedList<Message> messages; 
    
    Logger logger = LoggerFactory.getLogger(InMemoryQueue.class);
    
    public QueueResourceReaper(LinkedList<Message> messages) {
        this.messages = messages;
    }

    public void monitorDelivery(Future<?> task, Message message) {
        tasks.add(new DeliveryFuture(message, task));
    }
    
    public void shutdown() {
        shutdownFlag.set(true);
        logger.debug("Shutting down resource reaper...");
    }

    @Override
    public void run() {
        logger.debug("Resource reaper started...");
        while(!shutdownFlag.get()) {
            for(DeliveryFuture deliveryFuture: tasks) {
                try {
                    deliveryFuture.getDelivery().get(1000, TimeUnit.MILLISECONDS);
                    messages.remove(deliveryFuture.getMessage());
                } catch (Exception e) {
                    continue;
                }
            }
        }        
    }
    
    class DeliveryFuture{
        Message message;
        Future<?> delivery;
        
        public DeliveryFuture(Message message, Future<?> delivery) {
            super();
            this.message = message;
            this.delivery = delivery;
        }

        public Message getMessage() {
            return message;
        }

        public Future<?> getDelivery() {
            return delivery;
        }

        public void setMessage(Message message) {
            this.message = message;
        }

        public void setDelivery(Future<?> delivery) {
            this.delivery = delivery;
        }
    }
}
