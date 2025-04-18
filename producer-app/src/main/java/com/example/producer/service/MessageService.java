package com.example.producer.service;

import com.example.injector.service.HeaderInjector;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.apache.kafka.common.header.Headers;
import org.apache.kafka.common.header.internals.RecordHeaders;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Service
@Slf4j
public class MessageService {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final String specPath;
    private final ResourceLoader resourceLoader;
    private HeaderInjector headerInjector;
    private final ObjectMapper objectMapper;

    public MessageService(KafkaTemplate<String, String> kafkaTemplate,
                        @Value("${asyncapi.spec.path}") String specPath,
                        ResourceLoader resourceLoader) {
        this.kafkaTemplate = kafkaTemplate;
        this.specPath = specPath;
        this.resourceLoader = resourceLoader;
        this.objectMapper = new ObjectMapper();
    }

    @PostConstruct
    public void init() throws Exception {
        Resource resource = resourceLoader.getResource(specPath);
        try (InputStream inputStream = resource.getInputStream()) {
            this.headerInjector = new HeaderInjector(inputStream);
        }
    }

    public List<String> getAvailableTopics() {
        return List.of("user.created");
    }

    public Map<String, String> publishMessage(String topic, String payload) {
        try {
            Map<String, Object> payloadMap = objectMapper.readValue(payload, Map.class);
            Headers headers = new RecordHeaders();
            
            headerInjector.injectHeaders(topic, headers, payloadMap);
            
            CompletableFuture<SendResult<String, String>> future = kafkaTemplate.send(topic, payload);
            future.get(); // Wait for the message to be sent
            
            Map<String, String> headerMap = new HashMap<>();
            headers.forEach(header -> headerMap.put(header.key(), new String(header.value())));
            return headerMap;
            
        } catch (Exception e) {
            log.error("Error publishing message", e);
            throw new RuntimeException("Failed to publish message", e);
        }
    }
} 