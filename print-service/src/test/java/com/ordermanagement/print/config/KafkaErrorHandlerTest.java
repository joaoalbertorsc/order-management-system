package com.ordermanagement.print.config;

import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerGroupMetadata;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.kafka.core.KafkaOperations;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.listener.ListenerExecutionFailedException;
import org.springframework.kafka.listener.MessageListenerContainer;

import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class KafkaErrorHandlerTest {

    private static final String DEAD_LETTER_TOPIC = "pedidos.print-service.DLT";

    @SuppressWarnings("unchecked")
    private final KafkaOperations<String, Object> template = mock(KafkaOperations.class);
    @SuppressWarnings("unchecked")
    private final Consumer<String, Object> consumer = mock(Consumer.class);
    private final MessageListenerContainer container = mock(MessageListenerContainer.class);
    private final ConsumerRecord<String, Object> record = new ConsumerRecord<>("pedidos", 2, 41L, "order-1", "event");
    private DefaultErrorHandler errorHandler;

    @BeforeEach
    void setUp() {
        when(template.send(any(ProducerRecord.class))).thenReturn(CompletableFuture.completedFuture(null));
        when(consumer.groupMetadata()).thenReturn(new ConsumerGroupMetadata("print-service"));
        errorHandler = new KafkaConsumerConfig().kafkaErrorHandler(template, DEAD_LETTER_TOPIC);
    }

    @Test
    void failedPrintIsRetriedTwiceThenMovedToTheDeadLetterTopic() {
        RuntimeException error = new IllegalStateException("console unavailable");

        assertThat(errorHandler.handleOne(failure(error), record, consumer, container)).isFalse();
        assertThat(errorHandler.handleOne(failure(error), record, consumer, container)).isFalse();
        verify(template, never()).send(any(ProducerRecord.class));

        assertThat(errorHandler.handleOne(failure(error), record, consumer, container)).isTrue();
        @SuppressWarnings("unchecked")
        ArgumentCaptor<ProducerRecord<String, Object>> sent = ArgumentCaptor.forClass(ProducerRecord.class);
        verify(template).send(sent.capture());
        assertThat(sent.getValue().topic()).isEqualTo(DEAD_LETTER_TOPIC);
        assertThat(sent.getValue().partition()).isNull();
        assertThat(sent.getValue().key()).isEqualTo("order-1");
    }

    private static ListenerExecutionFailedException failure(Exception cause) {
        return new ListenerExecutionFailedException("listener failed", cause);
    }
}
