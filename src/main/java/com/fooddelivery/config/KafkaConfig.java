package com.fooddelivery.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.*;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer;
import org.springframework.util.backoff.FixedBackOff;

import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableKafka
public class KafkaConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Value("${food-delivery.topics.order-placed}")
    private String orderPlacedTopic;

    @Value("${food-delivery.topics.restaurant-notification}")
    private String restaurantNotificationTopic;

    @Value("${food-delivery.topics.delivery-assigned}")
    private String deliveryAssignedTopic;

    @Value("${food-delivery.topics.delivery-pickup}")
    private String deliveryPickupTopic;

    @Value("${food-delivery.topics.delivery-completed}")
    private String deliveryCompletedTopic;

    // --- Topic Declarations ---
    @Bean
    public NewTopic topicOrderPlaced() {
        return TopicBuilder.name(orderPlacedTopic).partitions(3).replicas(1).build();
    }

    @Bean
    public NewTopic topicRestaurantNotification() {
        return TopicBuilder.name(restaurantNotificationTopic).partitions(3).replicas(1).build();
    }

    @Bean
    public NewTopic topicDeliveryAssigned() {
        return TopicBuilder.name(deliveryAssignedTopic).partitions(3).replicas(1).build();
    }

    @Bean
    public NewTopic topicDeliveryPickup() {
        return TopicBuilder.name(deliveryPickupTopic).partitions(3).replicas(1).build();
    }

    @Bean
    public NewTopic topicDeliveryCompleted() {
        return TopicBuilder.name(deliveryCompletedTopic).partitions(3).replicas(1).build();
    }

    // --- Producer Configuration ---
    @Bean
    public ProducerFactory<String, Object> producerFactory() {
        Map<String, Object> config = new HashMap<>();
        config.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        config.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        config.put(ProducerConfig.ACKS_CONFIG, "all");
        config.put(ProducerConfig.RETRIES_CONFIG, 3);
        config.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
        return new DefaultKafkaProducerFactory<>(config);
    }

    @Bean
    public KafkaTemplate<String, Object> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory());
    }

    // --- Consumer Configuration ---
    @Bean
    public ConsumerFactory<String, Object> consumerFactory() {
        Map<String, Object> config = new HashMap<>();
        config.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        config.put(ConsumerConfig.GROUP_ID_CONFIG, "food-delivery-group");
        config.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        config.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        config.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        config.put(JsonDeserializer.TRUSTED_PACKAGES, "*");
        config.put(JsonDeserializer.USE_TYPE_INFO_HEADERS, false);
        config.put(JsonDeserializer.VALUE_DEFAULT_TYPE, "java.util.Map");
        return new DefaultKafkaConsumerFactory<>(config);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, Object> kafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, Object> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory());
        factory.setCommonErrorHandler(errorHandler());
        return factory;
    }

    @Bean
    public CommonErrorHandler errorHandler() {
        // Retry 3 times with 1-second interval before logging error
        return new DefaultErrorHandler(new FixedBackOff(1000L, 3));
    }
}
