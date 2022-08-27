package com.example.demo.queue;

import java.util.List;

import com.example.demo.api.MessageFilteringCriteria;
import com.example.demo.api.QueueConsumer;

public class ConsumerContext {
    
    private QueueConsumer consumer;
    private MessageFilteringCriteria expression;
    private List<QueueConsumer> predecessors;

    public ConsumerContext(QueueConsumer consumer, MessageFilteringCriteria expression,
            List<QueueConsumer> predecessors) {
        this.consumer = consumer;
        this.expression = expression;
        this.predecessors = predecessors;
    }

    public QueueConsumer getConsumer() {
        return consumer;
    }

    public MessageFilteringCriteria getExpression() {
        return expression;
    }

    public List<QueueConsumer> getPredecessors() {
        return predecessors;
    }

    public void setConsumer(QueueConsumer consumer) {
        this.consumer = consumer;
    }

    public void setExpression(MessageFilteringCriteria expression) {
        this.expression = expression;
    }

    public void setPredecessors(List<QueueConsumer> predecessors) {
        this.predecessors = predecessors;
    }
}
