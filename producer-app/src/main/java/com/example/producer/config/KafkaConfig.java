package com.example.producer.config;

import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.ProducerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Collections;

/**
 * Kafka configuration for both local and non-local profiles.
 * In local profile, this uses the embedded Kafka broker configured by EmbeddedKafkaConfig.
 * In other profiles, it uses the bootstrap servers from configuration.
 */
@Configuration
public class KafkaConfig {

    @Value("${spring.kafka.bootstrap-servers:localhost:9092}")
    private String bootstrapServers;

    /**
     * Configure Kafka properties for the local profile
     */
    @Bean
    @Primary
    @Profile("local")
    public KafkaProperties localKafkaProperties() {
        KafkaProperties properties = new KafkaProperties();
        properties.setBootstrapServers(Collections.singletonList(bootstrapServers));
        
        // Configure producer properties
        properties.getProducer().setKeySerializer(StringSerializer.class);
        properties.getProducer().setValueSerializer(StringSerializer.class);
        
        return properties;
    }
    
    /**
     * Configure producer factory for local profile
     */
    @Bean
    @Primary
    @Profile("local")
    public ProducerFactory<String, String> localProducerFactory(KafkaProperties kafkaProperties) {
        Map<String, Object> configProps = new HashMap<>(kafkaProperties.getProducer().getProperties());
        
        // Ensure serializers are explicitly set
        configProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        configProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        
        return new DefaultKafkaProducerFactory<>(configProps);
    }
    
    /**
     * Configure producer factory for non-local profiles
     */
    @Bean
    @Profile("!local")
    public ProducerFactory<String, String> producerFactory() {
        Map<String, Object> configProps = new HashMap<>();
        configProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        configProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        return new DefaultKafkaProducerFactory<>(configProps);
    }

    /**
     * Configure Kafka template for local profile
     */
    @Bean
    @Primary
    @Profile("local")
    public KafkaTemplate<String, String> localKafkaTemplate(ProducerFactory<String, String> localProducerFactory) {
        return new KafkaTemplate<>(localProducerFactory);
    }
    
    /**
     * Configure Kafka template for non-local profiles
     */
    @Bean
    @Profile("!local")
    public KafkaTemplate<String, String> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory());
    }
}
