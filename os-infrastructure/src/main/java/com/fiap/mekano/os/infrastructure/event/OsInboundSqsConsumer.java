package com.fiap.mekano.os.infrastructure.event;

import java.util.List;
import java.util.Optional;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fiap.mekano.os.domain.port.in.OrdemDeServicoServicePort;
import com.fiap.mekano.os.domain.port.out.ProcessedEventsPort;

import io.quarkus.logging.Log;
import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.transaction.Transactional;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;

/**
 * Consumidor idempotente da fila SQS de entrada ({@code os-inbound.fifo}).
 * Processa eventos externos de Orçamento e Pagamento garantindo idempotência via {@link ProcessedEventsPort}.
 */
@ApplicationScoped
public class OsInboundSqsConsumer {

    private final SqsClient sqsClient;
    private final ObjectMapper objectMapper;
    private final ProcessedEventsPort processedEvents;
    private final Instance<OrdemDeServicoServicePort> osServiceInstance;
    private final String queueUrl;
    private final boolean consumerEnabled;

    public OsInboundSqsConsumer(
            SqsClient sqsClient,
            ObjectMapper objectMapper,
            ProcessedEventsPort processedEvents,
            Instance<OrdemDeServicoServicePort> osServiceInstance,
            @ConfigProperty(name = "mekano.events.inbound-queue-url") Optional<String> queueUrl,
            @ConfigProperty(name = "mekano.events.consumer-enabled", defaultValue = "false") boolean consumerEnabled) {
        this.sqsClient = sqsClient;
        this.objectMapper = objectMapper;
        this.processedEvents = processedEvents;
        this.osServiceInstance = osServiceInstance;
        this.queueUrl = queueUrl.orElse("");
        this.consumerEnabled = consumerEnabled;
    }

    @Scheduled(every = "${mekano.events.sqs-poll-interval:5s}")
    public void poll() {
        if (!consumerEnabled || queueUrl == null || queueUrl.isBlank()) {
            return;
        }
        List<Message> messages = sqsClient.receiveMessage(ReceiveMessageRequest.builder()
                .queueUrl(queueUrl)
                .maxNumberOfMessages(10)
                .waitTimeSeconds(1)
                .build()).messages();

        for (Message message : messages) {
            try {
                processarMensagem(message.body());
                sqsClient.deleteMessage(DeleteMessageRequest.builder()
                        .queueUrl(queueUrl)
                        .receiptHandle(message.receiptHandle())
                        .build());
            } catch (Exception e) {
                Log.warnf("Erro ao processar mensagem SQS %s: %s", message.messageId(), e.getMessage());
            }
        }
    }

    @Transactional
    public void processarMensagem(String rawBody) {
        OsInboundEventPayload payload = extrairPayload(rawBody);
        if (payload.eventId() == null || payload.eventType() == null || payload.osUuid() == null) {
            throw new IllegalArgumentException("Evento inválido: campos obrigatórios ausentes");
        }
        if (processedEvents.alreadyProcessed(payload.eventId())) {
            return;
        }

        OrdemDeServicoServicePort osService = osServiceInstance.get();
        switch (payload.eventType()) {
            case "OrcamentoAprovado" -> osService.registrarAprovacaoOrcamento(payload.osUuid(), payload.orcamentoUuid());
            case "OrcamentoReprovado", "OrcamentoExpirado" -> osService.cancelarPorOrcamento(
                    payload.osUuid(),
                    payload.motivo() != null ? payload.motivo() : payload.eventType());
            case "PagamentoConfirmado" -> osService.liberarEntregaPorPagamento(payload.osUuid());
            default -> Log.debugf("Evento ignorado pelo serviço de OS: %s", payload.eventType());
        }

        processedEvents.markProcessed(payload.eventId(), payload.eventType());
    }

    private OsInboundEventPayload extrairPayload(String rawBody) {
        try {
            JsonNode root = objectMapper.readTree(rawBody);
            if (root.has("Message") && root.get("Message").isTextual()) {
                return objectMapper.readValue(root.get("Message").asText(), OsInboundEventPayload.class);
            }
            return objectMapper.treeToValue(root, OsInboundEventPayload.class);
        } catch (Exception e) {
            throw new IllegalArgumentException("Payload JSON inválido: " + e.getMessage(), e);
        }
    }
}
