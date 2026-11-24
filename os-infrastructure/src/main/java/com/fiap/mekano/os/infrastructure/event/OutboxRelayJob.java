package com.fiap.mekano.os.infrastructure.event;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import com.fiap.mekano.os.infrastructure.entity.OutboxEventEntity;

import io.quarkus.logging.Log;
import io.quarkus.panache.common.Page;
import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.MessageAttributeValue;
import software.amazon.awssdk.services.sns.model.PublishRequest;

/**
 * Publica eventos pendentes da tabela {@code outbox_events} para o tópico SNS FIFO.
 * Usa {@code aggregateId} como {@code MessageGroupId} para preservar a ordem por OS.
 */
@ApplicationScoped
public class OutboxRelayJob {

    private static final int MAX_ATTEMPTS = 10;
    private static final int BATCH_SIZE = 20;

    private final OutboxEventPanacheRepository repository;
    private final SnsClient snsClient;
    private final String topicArn;
    private final boolean relayEnabled;

    public OutboxRelayJob(
            OutboxEventPanacheRepository repository,
            SnsClient snsClient,
            @ConfigProperty(name = "mekano.events.topic-arn") Optional<String> topicArn,
            @ConfigProperty(name = "mekano.events.relay-enabled", defaultValue = "false") boolean relayEnabled) {
        this.repository = repository;
        this.snsClient = snsClient;
        this.topicArn = topicArn.orElse("");
        this.relayEnabled = relayEnabled;
    }

    @Scheduled(every = "${mekano.events.outbox-interval:5s}")
    @Transactional
    public void processPending() {
        if (!relayEnabled || topicArn == null || topicArn.isBlank()) {
            return;
        }
        List<OutboxEventEntity> pending = repository
                .find("status = ?1 ORDER BY createdAt ASC", "PENDING")
                .page(Page.of(0, BATCH_SIZE))
                .list();

        for (OutboxEventEntity event : pending) {
            publishSingle(event);
        }
    }

    void publishSingle(OutboxEventEntity event) {
        try {
            var request = PublishRequest.builder()
                    .topicArn(topicArn)
                    .message(event.getPayload())
                    .messageGroupId(event.getAggregateId().toString())
                    .messageDeduplicationId(event.getEventId().toString())
                    .messageAttributes(Map.of(
                            "eventType", MessageAttributeValue.builder()
                                    .dataType("String")
                                    .stringValue(event.getEventType())
                                    .build()))
                    .build();
            snsClient.publish(request);
            event.setStatus("PUBLISHED");
            event.setPublishedAt(LocalDateTime.now());
            event.setLastError(null);
        } catch (Exception e) {
            int nextAttempts = event.getAttempts() + 1;
            event.setAttempts(nextAttempts);
            event.setLastError(e.getMessage());
            if (nextAttempts >= MAX_ATTEMPTS) {
                event.setStatus("FAILED");
            }
            Log.warnf("Falha ao publicar outbox event %s (%s): %s",
                    event.getEventId(), event.getEventType(), e.getMessage());
        }
    }
}
