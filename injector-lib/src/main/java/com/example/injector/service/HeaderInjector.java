package com.example.injector.service;

import com.example.injector.model.AsyncApiSpec;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.header.Headers;
import org.apache.kafka.common.header.internals.RecordHeader;

import java.io.InputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
public class HeaderInjector {
    private final AsyncApiSpec spec;
    private final ObjectMapper objectMapper;

    public HeaderInjector(InputStream specInputStream) throws IOException {
        this.objectMapper = new ObjectMapper(new YAMLFactory());
        this.spec = objectMapper.readValue(specInputStream, AsyncApiSpec.class);
    }

    public void injectHeaders(String topic, Headers headers, Map<String, Object> messagePayload) {
        var channel = spec.getChannels().get(topic);
        if (channel == null || channel.getXHeaders() == null) {
            log.warn("No header configuration found for topic: {}", topic);
            return;
        }

        Map<String, String> headerValues = new HashMap<>();
        channel.getXHeaders().forEach((key, value) -> {
            String resolvedValue = resolveHeaderValue(value, messagePayload);
            headerValues.put(key, resolvedValue);
        });

        headerValues.forEach((key, value) -> {
            Header header = new RecordHeader(key, value.getBytes());
            headers.add(header);
        });
    }

    private String resolveHeaderValue(String template, Map<String, Object> payload) {
        if (template.startsWith("${")) {
            String expression = template.substring(2, template.length() - 1);
            if (expression.equals("uuid()")) {
                return UUID.randomUUID().toString();
            } else if (expression.equals("timestamp()")) {
                return String.valueOf(System.currentTimeMillis());
            } else if (expression.startsWith("message.payload.")) {
                String field = expression.substring("message.payload.".length());
                Object value = payload.get(field);
                return value != null ? value.toString() : "";
            }
        }
        return template;
    }
} 