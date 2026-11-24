package com.fiap.mekano.os.infrastructure.event;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fiap.mekano.os.domain.event.OSFinalizadaEvent;
import com.fiap.mekano.os.domain.port.in.OrdemDeServicoServicePort;
import com.fiap.mekano.os.domain.port.out.ProcessedEventsPort;
import com.fiap.mekano.os.infrastructure.entity.OutboxEventEntity;

import jakarta.enterprise.inject.Instance;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.PublishRequest;
import software.amazon.awssdk.services.sns.model.PublishResponse;
import software.amazon.awssdk.services.sqs.SqsClient;

@ExtendWith(MockitoExtension.class)
class OutboxAndConsumerTest {

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @Mock
    OutboxEventPanacheRepository outboxRepository;
    @Mock
    SnsClient snsClient;
    @Mock
    SqsClient sqsClient;
    @Mock
    ProcessedEventsPort processedEvents;
    @Mock
    Instance<OrdemDeServicoServicePort> osServiceInstance;
    @Mock
    OrdemDeServicoServicePort osService;

    @Test
    void outboxPublisherPersisteEventoPendente() {
        var publisher = new OutboxEventPublisher(outboxRepository, objectMapper);
        var event = OSFinalizadaEvent.of(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), java.math.BigDecimal.TEN);

        publisher.publish(event);

        ArgumentCaptor<OutboxEventEntity> captor = ArgumentCaptor.forClass(OutboxEventEntity.class);
        verify(outboxRepository).persist(captor.capture());
        OutboxEventEntity saved = captor.getValue();
        assertThat(saved.getEventId()).isEqualTo(event.eventId());
        assertThat(saved.getAggregateId()).isEqualTo(event.osUuid());
        assertThat(saved.getEventType()).isEqualTo("OSFinalizada");
        assertThat(saved.getStatus()).isEqualTo("PENDING");
        assertThat(saved.getPayload()).contains(event.osUuid().toString());
    }

    @Test
    void relayPublicaNoSnsFifoEMarcaPublishedOuFailed() {
        var relay = new OutboxRelayJob(
                outboxRepository, snsClient, Optional.of("arn:aws:sns:us-east-1:000000000000:os-events.fifo"), true);

        OutboxEventEntity entity = new OutboxEventEntity();
        entity.setEventId(UUID.randomUUID());
        entity.setAggregateId(UUID.randomUUID());
        entity.setEventType("OSFinalizada");
        entity.setPayload("{}");

        when(snsClient.publish(any(PublishRequest.class))).thenReturn(PublishResponse.builder().messageId("m1").build());
        relay.publishSingle(entity);

        assertThat(entity.getStatus()).isEqualTo("PUBLISHED");
        assertThat(entity.getPublishedAt()).isNotNull();

        OutboxEventEntity falha = new OutboxEventEntity();
        falha.setEventId(UUID.randomUUID());
        falha.setAggregateId(UUID.randomUUID());
        falha.setEventType("OSFinalizada");
        falha.setPayload("{}");
        falha.setAttempts(9);
        doThrow(new RuntimeException("SNS down")).when(snsClient).publish(any(PublishRequest.class));

        relay.publishSingle(falha);
        assertThat(falha.getStatus()).isEqualTo("FAILED");
        assertThat(falha.getAttempts()).isEqualTo(10);
        assertThat(falha.getLastError()).isEqualTo("SNS down");
    }

    @Test
    void consumerProcessaEventosComIdempotenciaEEnvelopeSns() {
        var consumer = new OsInboundSqsConsumer(
                sqsClient, objectMapper, processedEvents, osServiceInstance, Optional.empty(), false);
        when(osServiceInstance.get()).thenReturn(osService);

        UUID eventId = UUID.randomUUID();
        UUID osUuid = UUID.randomUUID();
        UUID orcUuid = UUID.randomUUID();

        // Envelope SNS (campo Message)
        String snsEnvelope = """
                {"Message":"{\\"eventId\\":\\"%s\\",\\"eventType\\":\\"OrcamentoAprovado\\",\\"osUuid\\":\\"%s\\",\\"orcamentoUuid\\":\\"%s\\"}"}
                """.formatted(eventId, osUuid, orcUuid);

        when(processedEvents.alreadyProcessed(eventId)).thenReturn(false);
        consumer.processarMensagem(snsEnvelope);
        verify(osService).registrarAprovacaoOrcamento(osUuid, orcUuid);
        verify(processedEvents).markProcessed(eventId, "OrcamentoAprovado");

        // Reentrega duplicada -> ignorada
        when(processedEvents.alreadyProcessed(eventId)).thenReturn(true);
        consumer.processarMensagem(snsEnvelope);
        verify(osService, org.mockito.Mockito.times(1)).registrarAprovacaoOrcamento(osUuid, orcUuid);

        // PagamentoConfirmado direto (sem envelope SNS)
        UUID pagEventId = UUID.randomUUID();
        when(processedEvents.alreadyProcessed(pagEventId)).thenReturn(false);
        consumer.processarMensagem("""
                {"eventId":"%s","eventType":"PagamentoConfirmado","osUuid":"%s"}
                """.formatted(pagEventId, osUuid));
        verify(osService).liberarEntregaPorPagamento(osUuid);

        // OrcamentoReprovado
        UUID repEventId = UUID.randomUUID();
        when(processedEvents.alreadyProcessed(repEventId)).thenReturn(false);
        consumer.processarMensagem("""
                {"eventId":"%s","eventType":"OrcamentoReprovado","osUuid":"%s","motivo":"Caro"}
                """.formatted(repEventId, osUuid));
        verify(osService).cancelarPorOrcamento(osUuid, "Caro");
    }

    @Test
    void consumerRejeitaPayloadInvalido() {
        var consumer = new OsInboundSqsConsumer(
                sqsClient, objectMapper, processedEvents, osServiceInstance, Optional.empty(), false);

        assertThatThrownBy(() -> consumer.processarMensagem("{invalido"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> consumer.processarMensagem("{}"))
                .isInstanceOf(IllegalArgumentException.class);
        verify(processedEvents, never()).markProcessed(any(), any());
    }
}
