package com.example.producer.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.rule.EmbeddedKafkaRule;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
/**
 * Configuration for embedded Kafka broker that can run in any environment.
 * This configuration is only activated when the "local" profile is active.
 */
@Configuration
@Profile("local")
public class EmbeddedKafkaConfig {
    private static final Logger logger = LoggerFactory.getLogger(EmbeddedKafkaConfig.class);
    
    @Value("${kafka.topics:user.created}")
    private String kafkaTopics;
    
    @Value("${kafka.host:0.0.0.0}")
    private String kafkaHost;
    
    // If port is 0, a random available port will be used
    @Value("${kafka.port:0}")
    private int kafkaPort;

    private EmbeddedKafkaRule embeddedKafkaRule;
    private EmbeddedKafkaBroker embeddedKafkaBroker;
    private File logDir;
    private int actualPort;
    private String hostName;

    @PostConstruct
    public void setup() {
        try {
            // Determine hostname - use actual system hostname for accessibility
            hostName = determineHostName();
            
            // Use a random port if requested (port=0)
            actualPort = (kafkaPort == 0) ? findAvailableTcpPort() : kafkaPort;
            logger.info("Using Kafka port: {}", actualPort);
            
            // Create system-independent temp directory for Kafka logs
            Path tempPath = Files.createTempDirectory("kafka-logs");
            logDir = tempPath.toFile();
            
            // Force deletion on JVM exit
            logDir.deleteOnExit();
            
            // Create string of topics separated by commas
            String[] topics = kafkaTopics.split(",");
            
            // Create and configure the embedded Kafka rule
            embeddedKafkaRule = new EmbeddedKafkaRule(1, true, topics);
            embeddedKafkaRule.kafkaPorts(actualPort);
            
            // Set broker properties
            Map<String, String> brokerProperties = new HashMap<>();
            
            // Configure broker to be accessible from anywhere
            brokerProperties.put("listeners", "PLAINTEXT://" + kafkaHost + ":" + actualPort);
            
            // For advertised listeners, use the actual hostname or IP to ensure accessibility
            brokerProperties.put("advertised.listeners", "PLAINTEXT://" + hostName + ":" + actualPort);
            
            // Configure general broker properties
            brokerProperties.put("auto.create.topics.enable", "true");
            brokerProperties.put("offsets.topic.replication.factor", "1");
            brokerProperties.put("transaction.state.log.replication.factor", "1");
            brokerProperties.put("transaction.state.log.min.isr", "1");
            brokerProperties.put("log.dirs", logDir.getAbsolutePath());
            brokerProperties.put("num.partitions", "2");
            
            embeddedKafkaRule.brokerProperties(brokerProperties);
            
            // Start the broker
            embeddedKafkaRule.before();
            embeddedKafkaBroker = embeddedKafkaRule.getEmbeddedKafka();
            
            // Set bootstrap server property for other components to use
            String bootstrapServers = hostName + ":" + actualPort;
            System.setProperty("spring.kafka.bootstrap-servers", bootstrapServers);
            
            logger.info("Embedded Kafka broker started at: {}", bootstrapServers);
            logger.info("Kafka log directory: {}", logDir.getAbsolutePath());
            logger.info("Configured topics: {}", kafkaTopics);
            
            // Add shutdown hook for extra safety
            Runtime.getRuntime().addShutdownHook(new Thread(this::shutdownKafka));
            
        } catch (IOException e) {
            logger.error("Failed to create temp directory for Kafka logs", e);
            throw new RuntimeException("Failed to create temp directory for Kafka logs", e);
        } catch (Exception e) {
            logger.error("Failed to start embedded Kafka broker", e);
            throw new RuntimeException("Failed to start embedded Kafka broker", e);
        }
    }
    
    /**
     * Determine hostname for Kafka broker to be accessible from other machines
     */
    private String determineHostName() {
        try {
            // Get the host name or IP address from system
            String hostName = InetAddress.getLocalHost().getHostName();
            logger.info("Using hostname: {}", hostName);
            return hostName;
        } catch (UnknownHostException e) {
            logger.warn("Could not determine hostname, falling back to localhost", e);
            return "localhost";
        }
    }
    
    /**
     * Find an available TCP port in the default ephemeral port range.
     * This is a replacement for Spring's deprecated SocketUtils.
     * 
     * @return an available TCP port
     */
    private int findAvailableTcpPort() {
        int minPort = 49152; // Start of the ephemeral port range
        int maxPort = 65535; // End of the ephemeral port range
        
        return findAvailableTcpPort(minPort, maxPort);
    }
    
    /**
     * Find an available TCP port in the given range.
     * 
     * @param minPort the minimum port number
     * @param maxPort the maximum port number
     * @return an available TCP port
     * @throws IllegalStateException if no available port is found
     */
    private int findAvailableTcpPort(int minPort, int maxPort) {
        // Try up to 50 times to find an available port
        int maxAttempts = 50;
        int attemptCount = 0;
        
        while (attemptCount < maxAttempts) {
            int port = ThreadLocalRandom.current().nextInt(minPort, maxPort + 1);
            
            try (ServerSocket serverSocket = new ServerSocket(port)) {
                serverSocket.setReuseAddress(true);
                return port;
            } catch (IOException e) {
                // Port is not available, try again
                attemptCount++;
            }
        }
        
        throw new IllegalStateException("Could not find an available TCP port in the range [" + 
                                       minPort + ", " + maxPort + "] after " + maxAttempts + " attempts");
    }
    
    /**
     * Method for shutdown hook to ensure Kafka is properly stopped
     */
    private void shutdownKafka() {
        logger.info("Shutdown hook triggered, stopping Kafka broker");
        cleanup();
    }
    
    @PreDestroy
    public void cleanup() {
        if (embeddedKafkaRule != null) {
            try {
                embeddedKafkaRule.after();
                logger.info("Embedded Kafka broker stopped");
            } catch (Exception e) {
                logger.error("Error stopping embedded Kafka broker: {}", e.getMessage());
            }
        }
        
        // Additional cleanup for log directory
        if (logDir != null && logDir.exists()) {
            try {
                // Recursive delete on shutdown
                deleteDirectory(logDir);
                logger.info("Kafka log directory cleaned up: {}", logDir.getAbsolutePath());
            } catch (IOException e) {
                logger.warn("Failed to clean up Kafka log directory: {}", e.getMessage());
            }
        }
    }
    
    /**
     * Helper method to recursively delete a directory
     */
    private void deleteDirectory(File directory) throws IOException {
        if (directory.exists()) {
            File[] files = directory.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isDirectory()) {
                        deleteDirectory(file);
                    } else {
                        if (!file.delete()) {
                            logger.warn("Failed to delete file: {}", file.getAbsolutePath());
                        }
                    }
                }
            }
            if (!directory.delete()) {
                logger.warn("Failed to delete directory: {}", directory.getAbsolutePath());
            }
        }
    }
    
    @Bean
    public EmbeddedKafkaBroker embeddedKafkaBroker() {
        return embeddedKafkaBroker;
    }
    
    /**
     * Provide the broker port as a bean for use by other components
     */
    @Bean
    public Integer kafkaBrokerPort() {
        return actualPort;
    }
}
