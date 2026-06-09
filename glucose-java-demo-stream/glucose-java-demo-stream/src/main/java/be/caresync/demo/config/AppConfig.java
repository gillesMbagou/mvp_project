package be.caresync.demo.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.*;
import org.springframework.kafka.support.serializer.JsonSerializer;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.Map;

@Configuration
@EnableIntegration
@EnableScheduling
public class AppConfig {

    @Value("${spring.kafka.bootstrap-servers:localhost:29092}")
    private String bootstrapServers;

    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    @Bean
    public ProducerFactory<String, Object> producerFactory() {
        return new DefaultKafkaProducerFactory<>(Map.of(
            ProducerConfig.BOOTSTRAP_SERVERS_CONFIG,      bootstrapServers,
            ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG,   StringSerializer.class,
            ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class,
            ProducerConfig.ACKS_CONFIG,                   "1",
            ProducerConfig.CLIENT_ID_CONFIG,              "caresync-sim"
        ));
    }

    @Bean
    public KafkaTemplate<String, Object> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory());
    }

    // ── Consumer factory dédié au notification-service (valeurs en String brut) ──

    @Bean
    public ConsumerFactory<String, String> notificationConsumerFactory() {
        return new DefaultKafkaConsumerFactory<>(Map.of(
            ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG,      bootstrapServers,
            ConsumerConfig.GROUP_ID_CONFIG,               "notification-service",
            ConsumerConfig.AUTO_OFFSET_RESET_CONFIG,      "latest",
            ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,   StringDeserializer.class,
            ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class
        ));
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, String> notificationListenerContainerFactory() {
        var factory = new ConcurrentKafkaListenerContainerFactory<String, String>();
        factory.setConsumerFactory(notificationConsumerFactory());
        return factory;
    }
}
