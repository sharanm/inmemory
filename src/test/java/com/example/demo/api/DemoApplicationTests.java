package com.example.demo.api;

import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.example.demo.api.exception.QueueFullException;
import com.example.demo.queue.InMemoryQueue;
import com.example.demo.queue.consumer.QueueConsumerSimple;
import com.example.demo.queue.message.JSONMessage;
import com.example.demo.queue.producer.QueueProducerSimple;

public class DemoApplicationTests {
    
    Logger logger = LoggerFactory.getLogger(DemoApplicationTests.class);
    
    List<ConsumedMessage> processedMsg;
    Map<QueueConsumer, List<Message>> processedMsgMap;
    QueueProducerSimple producer;
    Queue queue;
    
    class DummyConsumer extends QueueConsumerSimple{

        public DummyConsumer(String id) {
            super(id);
        }

        @Override
        public void process(Message message) {
            super.process(message);
            processedMsg.add(new ConsumedMessage(this, message));
            
            if(!processedMsgMap.containsKey(this)) {
                processedMsgMap.put(this, new ArrayList<Message>());
            }
            processedMsgMap.get(this).add(message);
        }
    }
    
    class SlowConsumer extends DummyConsumer{
        public SlowConsumer(String id) {
            super(id);
        }

        @Override
        public void process(Message message) {
            try {
                Thread.sleep(2000);
                super.process(message);
            } catch (InterruptedException e) {
            }
        }
    }
    
    class ErroroneousConsumer extends DummyConsumer{
        public int tries = 0;
        public ErroroneousConsumer(String id) {
            super(id);
        }

        @Override
        public void process(Message message) {
            tries += 1;
            throw new ArithmeticException();
        }
    }
    
    class ConsumedMessage extends JSONMessage{
        DummyConsumer consumer;
        private Message message;
        
        ConsumedMessage(DummyConsumer consumer, Message message){
            super(message.getId(), message.getBody());
            this.consumer = consumer;
            this.message = message;
        }
        
        @Override
        public String getBody() {
            return message.getBody();
        }
        
        public DummyConsumer getConsumer() {
            return consumer;
        }
    }

    @Before
    public void setUp() throws Exception {
        logger.info("Performing Setup...............");
        producer = new QueueProducerSimple("foo");
        queue = new InMemoryQueue(5);
        processedMsg = new ArrayList<ConsumedMessage>();
        processedMsgMap = new HashMap<QueueConsumer, List<Message>>();
    }

    @Test
    public void singleProducerSingleConsumerSingleMessage() throws Exception {
        
        DummyConsumer dConsumer = new DummyConsumer("12");
        queue.subscribe(dConsumer, (x) -> true);
        producer.produce(queue, "{'a': 1}");
        
        queue.shutdown();
        
        assertTrue(!processedMsg.isEmpty());
        assertTrue(processedMsg.size() == 1);
        assertTrue(processedMsg.get(0).getBody().contains("1"));
    }
    
    @Test
    public void singleProducerSingleConsumerMultipleMessage() throws Exception {
        
        DummyConsumer dConsumer = new DummyConsumer("12");
        queue.subscribe(dConsumer, (x) -> true);
        producer.produce(queue, "{'a': 1}");
        producer.produce(queue, "{'a': 2}");
        producer.produce(queue, "{'a': 3}");
        
        queue.shutdown();
        
        assertTrue(!processedMsg.isEmpty());
        assertTrue(processedMsg.size() == 3);
        assertTrue(processedMsg.get(0).getBody().contains("1"));
        assertTrue(processedMsg.get(1).getBody().contains("2"));
        assertTrue(processedMsg.get(2).getBody().contains("3"));
    }
    
    @Test
    public void singleProducerMultipleConsumerMultipleMessage() throws Exception {
        
        DummyConsumer dConsumer = new DummyConsumer("12");
        queue.subscribe(dConsumer, (x) -> true);
        
        DummyConsumer dConsumer1 = new DummyConsumer("34");
        queue.subscribe(dConsumer1, (x) -> true);
        
        producer.produce(queue, "{'a': 1}");
        producer.produce(queue, "{'a': 2}");
        producer.produce(queue, "{'a': 3}");
        
        queue.shutdown();
        
        assertTrue(processedMsgMap.get(dConsumer).size() == 3);
        assertTrue(msgBodyPresent(processedMsgMap.get(dConsumer), "1"));
        assertTrue(msgBodyPresent(processedMsgMap.get(dConsumer), "2"));
        assertTrue(msgBodyPresent(processedMsgMap.get(dConsumer), "3"));
    }
    
    @Test
    public void singleProducerMultipleConsumerWithFilterCriteria() throws Exception {
        
        DummyConsumer dConsumer = new DummyConsumer("12");
        queue.subscribe(dConsumer, (x) -> x.getBody().contains("http"));
        
        DummyConsumer dConsumer1 = new DummyConsumer("34");
        queue.subscribe(dConsumer1, (x) -> !x.getBody().contains("http"));
        
        producer.produce(queue, "{'a': 1http}");
        producer.produce(queue, "{'a': 2}");
        producer.produce(queue, "{'a': 3http}");
        producer.produce(queue, "{'a': 4}");
        
        queue.shutdown();
        
        logger.info("dict: " + processedMsgMap);
        
        assertTrue(processedMsgMap.get(dConsumer).size() == 2);
        assertTrue(processedMsgMap.get(dConsumer).get(0).getBody().contains("1http"));
        assertTrue(processedMsgMap.get(dConsumer).get(1).getBody().contains("3http"));
        
        assertTrue(processedMsgMap.get(dConsumer1).get(0).getBody().contains("2"));
        assertTrue(processedMsgMap.get(dConsumer1).get(1).getBody().contains("4"));
    }
    
    @Test
    public void singleProducerMultipleConsumerWithDependency() throws Exception {
        
        DummyConsumer dConsumer = new DummyConsumer("12");
        queue.subscribe(dConsumer, (x) -> true);
        
        DummyConsumer dConsumer1 = new DummyConsumer("34");
        queue.subscribe(dConsumer1, (x) -> true);
        
        DummyConsumer dConsumer2 = new DummyConsumer("56");
        List<QueueConsumer> predecessor = new ArrayList<>();
        predecessor.add(dConsumer);
        predecessor.add(dConsumer1);
        queue.subscribe(dConsumer2, (x) -> true, predecessor);
        
        producer.produce(queue, "{'a': 1}");
        
        queue.shutdown();
        
        assertTrue(processedMsg.size() == 3);
        assertTrue(processedMsg.get(2).getConsumer().equals(dConsumer2));
    }
    
    @Test(expected = QueueFullException.class)
    public void queueFull() throws Exception {
        queue = new InMemoryQueue(1);
        
        DummyConsumer dConsumer = new SlowConsumer("12");
        queue.subscribe(dConsumer, (x) -> true);
        producer.produce(queue, "{'a': 1}");
        
        producer.produce(queue, "{'a': 2}");
        
        queue.shutdown();
    }
    
    @Test
    public void queueNotFull() throws Exception {
        
        queue = new InMemoryQueue(1);
        
        DummyConsumer dConsumer = new SlowConsumer("12");
        queue.subscribe(dConsumer, (x) -> true);
        producer.produce(queue, "{'a': 1}");
        
        Thread.sleep(3000);
        
        producer.produce(queue, "{'a': 2}");
        
        queue.shutdown();
        
        assertTrue(!processedMsg.isEmpty());
        assertTrue(processedMsg.size() == 1);
        assertTrue(processedMsg.get(0).getBody().contains("1"));
    }
    
    @Test
    public void processingException() throws Exception {
        
        Queue dlq = new InMemoryQueue(5);
        queue = new InMemoryQueue(1, dlq, 3, TimeUnit.SECONDS);
        
        DummyConsumer dConsumer = new ErroroneousConsumer("12");
        queue.subscribe(dConsumer, (x) -> true);
        producer.produce(queue, "{'a': 1}");
        
        DummyConsumer dConsumer1 = new DummyConsumer("12");
        dlq.subscribe(dConsumer1, (x) -> true);
        
        queue.shutdown();
        dlq.shutdown();
        
        assertTrue(!processedMsg.isEmpty());
        assertTrue(processedMsg.size() == 1);
        assertTrue(processedMsg.get(0).getBody().contains("1"));
        assertTrue(((ErroroneousConsumer)dConsumer).tries > 1);
    }

    @Test
    public void messageExpired() throws Exception {
        
        queue = new InMemoryQueue(5, 1, TimeUnit.SECONDS);
        
        DummyConsumer dConsumer = new SlowConsumer("12");
        queue.subscribe(dConsumer, (x) -> true);
        producer.produce(queue, "{'a': 1}");
        
        queue.shutdown();
        
        assertTrue(processedMsg.isEmpty());
    }
    
    
    private boolean msgBodyPresent(List<Message> messages, String msg) {
        for(Message message: messages) {
            if(message.getBody().contains(msg)) {
                return true;
            }
        }
        return false;
    }
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
}
