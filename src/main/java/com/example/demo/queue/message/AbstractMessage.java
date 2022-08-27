package com.example.demo.queue.message;

import java.time.Instant;
import java.time.LocalDateTime;

import org.json.JSONObject;

import com.example.demo.api.Message;

public abstract class AbstractMessage implements Message{
    private String id;
    private Instant ingestionTime;
    private String body;
    
    public String getId() {
        return id;
    }
    public Instant getIngestionTime() {
        return ingestionTime;
    }
    public String getBody() {
        return body;
    }
    public void setId(String id) {
        this.id = id;
    }

    public void setBody(String body) {
        this.body = body;
    }

    @Override
    public String getHeaders() {
        // TODO Auto-generated method stub
        return null;
    }
    @Override
    public String setHeaders() {
        // TODO Auto-generated method stub
        return null;
    }
    
    @Override
    public String toString() {
        return "MessageSimple [id=" + id + ", ingestionTime=" + ingestionTime + ", body=" + body + "]";
    }
    @Override
    public void setIngestionTime(Instant ingestionTime) {
        this.ingestionTime = ingestionTime;
        
    }
    
}
