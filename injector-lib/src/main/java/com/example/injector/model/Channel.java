package com.example.injector.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.HashMap;
import java.util.Map;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class Channel {
    private Publish publish;
    
    @JsonProperty("x-headers")
    private Map<String, String> xHeaders;
    
    public Map<String, String> getXHeaders() {
        return xHeaders;
    }
}
