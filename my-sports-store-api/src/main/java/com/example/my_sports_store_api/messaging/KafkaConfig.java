package com.example.my_sports_store_api.messaging;

import com.example.my_sports_store_api.exception.BadRequestException;
import com.example.my_sports_store_api.exception.ResourceNotFoundException;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.util.backoff.FixedBackOff;

import java.util.stream.Stream;

@Configuration
@EnableScheduling
public class KafkaConfig {

    private static final int PARTITIONS = 3;

    @Bean
    public KafkaAdmin.NewTopics topics() {
        // Each topic gets a .DLT twin with the same partition count, since the recoverer keeps the partition number
        NewTopic[] topics = Stream.of(Topics.ORDER_CREATED, Topics.PAYMENT_RESULT, Topics.ORDER_STATUS_CHANGED)
                .flatMap(name -> Stream.of(name, name + ".DLT"))
                .map(name -> TopicBuilder.name(name).partitions(PARTITIONS).replicas(1).build())
                .toArray(NewTopic[]::new);
        return new KafkaAdmin.NewTopics(topics);
    }

    @Bean
    public DefaultErrorHandler kafkaErrorHandler(KafkaTemplate<String, String> kafkaTemplate) {
        DefaultErrorHandler errorHandler = new DefaultErrorHandler(
                new DeadLetterPublishingRecoverer(kafkaTemplate), new FixedBackOff(1000L, 3));
        // Retrying cannot fix a malformed message or a reference to a missing order
        errorHandler.addNotRetryableExceptions(
                JsonProcessingException.class, ResourceNotFoundException.class, BadRequestException.class);
        return errorHandler;
    }
}
