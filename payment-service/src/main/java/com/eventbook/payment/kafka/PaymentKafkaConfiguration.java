package com.eventbook.payment.kafka;

import com.eventbook.payment.event.PaymentRequestedEvent;
import com.eventbook.payment.event.RefundRequestedEvent;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.ExponentialBackOffWithMaxRetries;
import org.springframework.kafka.support.serializer.JsonDeserializer;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Configuration
public class PaymentKafkaConfiguration {

    @Bean
    ConsumerFactory<String, PaymentRequestedEvent> chargeConsumerFactory(
            @Value("${spring.kafka.bootstrap-servers}") String bootstrapServers,
            @Value("${spring.kafka.consumer.group-id}") String groupId) {
        return consumerFactory(bootstrapServers, groupId, PaymentRequestedEvent.class);
    }

    @Bean
    ConsumerFactory<String, RefundRequestedEvent> refundConsumerFactory(
            @Value("${spring.kafka.bootstrap-servers}") String bootstrapServers,
            @Value("${spring.kafka.consumer.group-id}") String groupId) {
        return consumerFactory(bootstrapServers, groupId + "-refunds", RefundRequestedEvent.class);
    }

    @Bean
    DefaultErrorHandler paymentErrorHandler(
            KafkaTemplate<String, Object> kafkaTemplate,
            @Value("${app.kafka.retry.max-retries:3}") int maxRetries,
            @Value("${app.kafka.retry.initial-interval:1s}") Duration initialInterval,
            @Value("${app.kafka.retry.multiplier:2}") double multiplier,
            @Value("${app.kafka.retry.max-interval:4s}") Duration maxInterval) {
        var recoverer = new DeadLetterPublishingRecoverer(kafkaTemplate,
                (record, exception) -> new TopicPartition(record.topic() + ".DLT", record.partition()));
        var backOff = new ExponentialBackOffWithMaxRetries(maxRetries);
        backOff.setInitialInterval(initialInterval.toMillis());
        backOff.setMultiplier(multiplier);
        backOff.setMaxInterval(maxInterval.toMillis());
        return new DefaultErrorHandler(recoverer, backOff);
    }

    @Bean
    ConcurrentKafkaListenerContainerFactory<String, PaymentRequestedEvent> chargeKafkaListenerContainerFactory(
            ConsumerFactory<String, PaymentRequestedEvent> chargeConsumerFactory,
            DefaultErrorHandler paymentErrorHandler,
            @Value("${app.kafka.listeners.charge-concurrency:8}") int concurrency) {
        var factory = new ConcurrentKafkaListenerContainerFactory<String, PaymentRequestedEvent>();
        factory.setConsumerFactory(chargeConsumerFactory);
        factory.setConcurrency(concurrency);
        factory.setCommonErrorHandler(paymentErrorHandler);
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL);
        return factory;
    }

    @Bean
    ConcurrentKafkaListenerContainerFactory<String, RefundRequestedEvent> refundKafkaListenerContainerFactory(
            ConsumerFactory<String, RefundRequestedEvent> refundConsumerFactory,
            @Value("${app.kafka.listeners.refund-concurrency:2}") int concurrency) {
        var factory = new ConcurrentKafkaListenerContainerFactory<String, RefundRequestedEvent>();
        factory.setConsumerFactory(refundConsumerFactory);
        factory.setConcurrency(concurrency);
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL);
        return factory;
    }

    @Bean
    NewTopic paymentRequestedTopic() {
        return TopicBuilder.name("payment.requested").partitions(8).replicas(1).build();
    }

    @Bean
    NewTopic paymentCompletedTopic() {
        return TopicBuilder.name("payment.completed").partitions(8).replicas(1).build();
    }

    @Bean
    NewTopic paymentFailedTopic() {
        return TopicBuilder.name("payment.failed").partitions(8).replicas(1).build();
    }

    @Bean
    NewTopic paymentRefundRequestedTopic() {
        return TopicBuilder.name("payment.refund.requested").partitions(2).replicas(1).build();
    }

    @Bean
    NewTopic paymentRequestedDltTopic() {
        return TopicBuilder.name("payment.requested.DLT").partitions(8).replicas(1).build();
    }

    private <T> ConsumerFactory<String, T> consumerFactory(
            String bootstrapServers, String groupId, Class<T> eventType) {
        Map<String, Object> properties = new HashMap<>();
        properties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        properties.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        properties.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        properties.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        return new DefaultKafkaConsumerFactory<>(properties, new StringDeserializer(),
                new JsonDeserializer<>(eventType, false));
    }
}
