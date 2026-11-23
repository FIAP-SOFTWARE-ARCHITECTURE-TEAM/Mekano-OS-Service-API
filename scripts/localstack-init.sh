#!/bin/sh
# Cria o tópico SNS FIFO de eventos da OS e uma fila de exemplo (DLQ incluída).
# Os demais serviços (Orçamento, Pagamento, Auditoria, WhatsApp, Estoque) criam suas filas e assinam o tópico.
set -e

awslocal sns create-topic --name os-events.fifo \
  --attributes FifoTopic=true,ContentBasedDeduplication=true

# Fila de consumo dos eventos que a OS recebe (orçamento/pagamento) + DLQ
awslocal sqs create-queue --queue-name os-inbound-dlq.fifo \
  --attributes FifoQueue=true
awslocal sqs create-queue --queue-name os-inbound.fifo \
  --attributes '{"FifoQueue":"true","ContentBasedDeduplication":"true","RedrivePolicy":"{\"deadLetterTargetArn\":\"arn:aws:sqs:us-east-1:000000000000:os-inbound-dlq.fifo\",\"maxReceiveCount\":\"5\"}"}'

echo "LocalStack: SNS/SQS prontos"
