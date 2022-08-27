package com.example.demo.api;

import java.time.Instant;
import java.time.LocalDateTime;

public interface Message {
    
    public String getId();
    public String getHeaders();
    public Instant getIngestionTime();
    public String getBody();
    public String setHeaders();
    public void setBody(String message);
    public void setIngestionTime(Instant ingestionTime);
    
}
