package com.example.injector.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.util.Map;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class AsyncApiSpec {
    private String asyncapi;
    private Info info;
    private Map<String, Channel> channels;
    private Components components;
}

