package com.mandiflow.analytics.config;

import com.mandiflow.events.Topics;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaAdmin;

import java.util.stream.Stream;

/** Creates every topic and its .DLT twin with 3 partitions. Safe to run from every service. */
@Configuration
public class KafkaTopicConfig {

    @Bean
    KafkaAdmin.NewTopics mandiTopics() {
        return new KafkaAdmin.NewTopics(
                Topics.ALL.stream()
                        .flatMap(t -> Stream.of(t, t + Topics.DLT_SUFFIX))
                        .map(t -> TopicBuilder.name(t).partitions(Topics.PARTITIONS).replicas(1).build())
                        .toArray(NewTopic[]::new));
    }
}
