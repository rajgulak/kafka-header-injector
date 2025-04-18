package com.example.producer.config;

import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.rule.EmbeddedKafkaRule;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.ProducerFactory;

import java.util.Map;
import java.util.HashMap;

@Configuration
@Profile("local") // Only activate this in local development profile
@EmbeddedKafka(partitions = 1, topics = {"user.created"})
public class LocalKafkaConfig {

    @Bean
    public EmbeddedKafkaRule embeddedKafkaRule() {
        EmbeddedKafkaRule rule = new EmbeddedKafkaRule(1, true, "user.created");
        rule.kafkaPorts(9092);
        return rule;
    }
    
    @Bean
    public EmbeddedKafkaBroker embeddedKafkaBroker(EmbeddedKafkaRule embeddedKafkaRule) {
        return embeddedKafkaRule.getEmbeddedKafka();
    }
    
    @Bean
    @Primary
    public KafkaProperties localKafkaProperties(EmbeddedKafkaBroker embeddedKafkaBroker) {
        KafkaProperties properties = new KafkaProperties();
        properties.setBootstrapServers(java.util.Collections.singletonList(embeddedKafkaBroker.getBrokersAsString()));
        
        // Configure producer properties
        properties.getProducer().setKeySerializer(StringSerializer.class);
        properties.getProducer().setValueSerializer(StringSerializer.class);
        
        return properties;
    }
    
    @Bean
    @Primary
    public ProducerFactory<String, String> localProducerFactory(KafkaProperties localKafkaProperties, 
                                                               EmbeddedKafkaBroker embeddedKafkaBroker) {
        Map<String, Object> configProps = new HashMap<>(localKafkaProperties.getProducer().getProperties());
        
        // Ensure serializers are explicitly set
        configProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        configProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, embeddedKafkaBroker.getBrokersAsString());
        
        return new DefaultKafkaProducerFactory<>(configProps);
    }
    
    @Bean
    @Primary
    public KafkaTemplate<String, String> localKafkaTemplate(ProducerFactory<String, String> localProducerFactory) {
        return new KafkaTemplate<>(localProducerFactory);
    }
}

