package com.eventbook.booking.kafka;

import com.eventbook.booking.event.PaymentResultEvent;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.apache.kafka.clients.admin.NewTopic;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class BookingKafkaConfiguration {

    @Bean
    ConsumerFactory<String, PaymentResultEvent> paymentResultConsumerFactory(
            @Value("${spring.kafka.bootstrap-servers}") String bootstrapServers,
            @Value("${spring.kafka.consumer.group-id}") String groupId) {
        Map<String, Object> properties = new HashMap<>();
        properties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        properties.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        properties.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        properties.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        return new DefaultKafkaConsumerFactory<>(properties, new StringDeserializer(),
                new JsonDeserializer<>(PaymentResultEvent.class, false));
    }

    @Bean
    ConcurrentKafkaListenerContainerFactory<String, PaymentResultEvent> paymentResultKafkaListenerContainerFactory(
            ConsumerFactory<String, PaymentResultEvent> paymentResultConsumerFactory) {
        var factory = new ConcurrentKafkaListenerContainerFactory<String, PaymentResultEvent>();
        factory.setConsumerFactory(paymentResultConsumerFactory);
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
    NewTopic refundRequestedTopic() {
        return TopicBuilder.name("payment.refund.requested").partitions(2).replicas(1).build();
    }

    @Bean
    NewTopic paymentRequestedDltTopic() {
        return TopicBuilder.name("payment.requested.DLT").partitions(8).replicas(1).build();
    }
}
