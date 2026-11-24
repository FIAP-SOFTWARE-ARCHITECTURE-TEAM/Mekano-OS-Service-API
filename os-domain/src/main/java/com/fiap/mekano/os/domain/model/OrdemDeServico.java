package com.fiap.mekano.os.domain.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import com.fiap.mekano.os.domain.exception.AppException;
import com.fiap.mekano.os.domain.exception.Messages;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

/**
 * Aggregate root do contexto de Ordem de Serviço.
 *
 * <p>Ciclo de vida: {@code RECEBIDA → EM_DIAGNOSTICO → AGUARDANDO_APROVACAO →
 * AGUARDANDO_EXECUCAO → EM_EXECUCAO → FINALIZADA → ENTREGUE}, com cancelamento possível
 * antes da finalização. Transições ocorrem apenas por métodos explícitos (nunca {@code setStatus}).
 *
 * <p>Orçamento e Pagamento pertencem a outros serviços: a OS guarda apenas a referência
 * {@code orcamentoUuid} e reage aos eventos deles ({@link #registrarAprovacaoOrcamento},
 * {@link #cancelar}, {@link #liberarEntrega}).
 *
 * <p>Criação via {@link #create}; reconstrução a partir do banco via {@link #reconstitute}.
 */
@Getter
@Builder(access = AccessLevel.PRIVATE)
@ToString
public class OrdemDeServico {

    private final UUID id;
    private UUID clienteUuid;
    private UUID veiculoUuid;
    private String descricaoProblema;
    private StatusOS status;
    private StatusEntrega statusEntrega;
    private String motivoCancelamento;
    private UUID orcamentoUuid;
    private LocalDateTime dataAprovacao;
    private UUID mecanicoUuid;
    private LocalDateTime execucaoIniciadaEm;
    private LocalDateTime execucaoFinalizadaEm;
    private String observacaoExecucao;
    private LocalDateTime entregueEm;
    private String recebidoPor;
    private final LocalDateTime createdAt;
    private final Long version;
    private List<ItemOs> itens;

    /** Cria uma nova OS com status {@code RECEBIDA}. */
    public static OrdemDeServico create(UUID clienteUuid, UUID veiculoUuid, String descricaoProblema) {
        validarDados(clienteUuid, veiculoUuid, descricaoProblema);
        return OrdemDeServico.builder()
                .id(UUID.randomUUID())
                .clienteUuid(clienteUuid)
                .veiculoUuid(veiculoUuid)
                .descricaoProblema(descricaoProblema.strip())
                .status(StatusOS.RECEBIDA)
                .statusEntrega(StatusEntrega.NAO_LIBERADA)
                .createdAt(LocalDateTime.now())
                .version(0L)
                .itens(new ArrayList<>())
                .build();
    }

    /** Reconstrói a OS a partir de dados persistidos (não gera UUID nem timestamp). */
    @SuppressWarnings("java:S107")
    public static OrdemDeServico reconstitute(UUID id, UUID clienteUuid, UUID veiculoUuid,
                                              String descricaoProblema, StatusOS status,
                                              StatusEntrega statusEntrega, String motivoCancelamento,
                                              UUID orcamentoUuid, LocalDateTime dataAprovacao,
                                              UUID mecanicoUuid, LocalDateTime execucaoIniciadaEm,
                                              LocalDateTime execucaoFinalizadaEm, String observacaoExecucao,
                                              LocalDateTime entregueEm, String recebidoPor,
                                              LocalDateTime createdAt, Long version, List<ItemOs> itens) {
        return OrdemDeServico.builder()
                .id(id)
                .clienteUuid(clienteUuid)
                .veiculoUuid(veiculoUuid)
                .descricaoProblema(descricaoProblema)
                .status(status)
                .statusEntrega(statusEntrega == null ? StatusEntrega.NAO_LIBERADA : statusEntrega)
                .motivoCancelamento(motivoCancelamento)
                .orcamentoUuid(orcamentoUuid)
                .dataAprovacao(dataAprovacao)
                .mecanicoUuid(mecanicoUuid)
                .execucaoIniciadaEm(execucaoIniciadaEm)
                .execucaoFinalizadaEm(execucaoFinalizadaEm)
                .observacaoExecucao(observacaoExecucao)
                .entregueEm(entregueEm)
                .recebidoPor(recebidoPor)
                .createdAt(createdAt)
                .version(version)
                .itens(itens == null ? new ArrayList<>() : new ArrayList<>(itens))
                .build();
    }

    // ───────────── Dados cadastrais e itens ─────────────

    /** Atualiza os dados cadastrais. Permitido apenas em {@code RECEBIDA}. */
    public void atualizar(UUID clienteUuid, UUID veiculoUuid, String descricaoProblema) {
        if (status != StatusOS.RECEBIDA) {
            throw AppException.unprocessable(Messages.get("os.transicao.invalida", status, "ATUALIZAR"));
        }
        validarDados(clienteUuid, veiculoUuid, descricaoProblema);
        this.clienteUuid = clienteUuid;
        this.veiculoUuid = veiculoUuid;
        this.descricaoProblema = descricaoProblema.strip();
    }

    /** Adiciona um item (snapshot). Permitido em {@code RECEBIDA} e {@code EM_DIAGNOSTICO}. */
    public void adicionarItem(ItemOs item) {
        garantirItensEditaveis();
        if (item == null) {
            throw AppException.badRequest(Messages.get("os.item.required"));
        }
        itens.add(item);
    }

    /** Substitui todos os itens. Permitido em {@code RECEBIDA} e {@code EM_DIAGNOSTICO}. */
    public void substituirItens(List<ItemOs> novosItens) {
        garantirItensEditaveis();
        List<ItemOs> copia = novosItens == null ? new ArrayList<>() : new ArrayList<>(novosItens);
        if (copia.stream().anyMatch(i -> i == null)) {
            throw AppException.badRequest(Messages.get("os.item.required"));
        }
        this.itens = copia;
    }

    /** Itens da OS (visão somente leitura). */
    public List<ItemOs> getItens() {
        return Collections.unmodifiableList(itens);
    }

    /** Soma dos valores dos itens (snapshot). */
    public BigDecimal getValorTotalItens() {
        return itens.stream().map(ItemOs::getValorTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    // ───────────── Transições do ciclo de vida ─────────────

    /** {@code RECEBIDA → EM_DIAGNOSTICO}. */
    public void iniciarDiagnostico() {
        transicionar(StatusOS.EM_DIAGNOSTICO);
    }

    /** {@code EM_DIAGNOSTICO → AGUARDANDO_APROVACAO}. Exige ao menos um item. */
    public void finalizarDiagnostico() {
        if (status == StatusOS.EM_DIAGNOSTICO && itens.isEmpty()) {
            throw AppException.unprocessable(Messages.get("os.itens.sem.itens"));
        }
        transicionar(StatusOS.AGUARDANDO_APROVACAO);
    }

    /**
     * {@code AGUARDANDO_APROVACAO → AGUARDANDO_EXECUCAO}. Chamado ao consumir o evento
     * de orçamento aprovado.
     */
    public void registrarAprovacaoOrcamento(UUID orcamentoUuid) {
        if (orcamentoUuid == null) {
            throw AppException.badRequest(Messages.get("os.orcamento.required"));
        }
        transicionar(StatusOS.AGUARDANDO_EXECUCAO);
        this.orcamentoUuid = orcamentoUuid;
        this.dataAprovacao = LocalDateTime.now();
    }

    /** {@code AGUARDANDO_EXECUCAO → EM_EXECUCAO}. */
    public void iniciarExecucao(UUID mecanicoUuid, String observacao) {
        if (mecanicoUuid == null) {
            throw AppException.badRequest(Messages.get("os.mecanico.required"));
        }
        transicionar(StatusOS.EM_EXECUCAO);
        this.mecanicoUuid = mecanicoUuid;
        this.execucaoIniciadaEm = LocalDateTime.now();
        this.observacaoExecucao = normalizar(observacao);
    }

    /** {@code EM_EXECUCAO → FINALIZADA}. */
    public void finalizarExecucao(String observacao) {
        transicionar(StatusOS.FINALIZADA);
        this.execucaoFinalizadaEm = LocalDateTime.now();
        String obs = normalizar(observacao);
        if (obs != null) {
            this.observacaoExecucao = obs;
        }
    }

    /**
     * Cancela a OS (qualquer status não terminal, até {@code EM_EXECUCAO}). Usado em
     * cancelamento manual, orçamento reprovado ou expirado.
     */
    public void cancelar(String motivo) {
        if (motivo == null || motivo.isBlank()) {
            throw AppException.badRequest(Messages.get("os.cancelamento.motivo.required"));
        }
        transicionar(StatusOS.CANCELADA);
        this.motivoCancelamento = motivo.strip();
    }

    /**
     * Libera a entrega após a confirmação de pagamento (evento do serviço de Pagamento).
     * Idempotente: chamar novamente com a entrega já liberada ou concluída não altera nada.
     */
    public void liberarEntrega() {
        if (statusEntrega != StatusEntrega.NAO_LIBERADA) {
            return;
        }
        if (status != StatusOS.FINALIZADA) {
            throw AppException.unprocessable(Messages.get("os.entrega.liberacao.invalida", status));
        }
        this.statusEntrega = StatusEntrega.LIBERADA_PARA_ENTREGA;
    }

    /** {@code FINALIZADA → ENTREGUE}. Exige entrega liberada (pagamento confirmado). */
    public void entregar(String recebidoPor) {
        if (recebidoPor == null || recebidoPor.isBlank()) {
            throw AppException.badRequest(Messages.get("os.entrega.recebedor.required"));
        }
        if (status != StatusOS.FINALIZADA) {
            throw AppException.unprocessable(Messages.get("os.transicao.invalida", status, StatusOS.ENTREGUE));
        }
        if (!statusEntrega.podeTransicionarPara(StatusEntrega.ENTREGUE)) {
            throw AppException.unprocessable(Messages.get("os.entrega.nao.liberada", statusEntrega));
        }
        transicionar(StatusOS.ENTREGUE);
        this.statusEntrega = StatusEntrega.ENTREGUE;
        this.entregueEm = LocalDateTime.now();
        this.recebidoPor = recebidoPor.strip();
    }

    // ───────────── Internos ─────────────

    private void transicionar(StatusOS destino) {
        if (!status.podeTransicionarPara(destino)) {
            throw AppException.unprocessable(Messages.get("os.transicao.invalida", status, destino));
        }
        this.status = destino;
    }

    private void garantirItensEditaveis() {
        if (status != StatusOS.RECEBIDA && status != StatusOS.EM_DIAGNOSTICO) {
            throw AppException.unprocessable(Messages.get("os.itens.nao.editaveis", status));
        }
    }

    private static void validarDados(UUID clienteUuid, UUID veiculoUuid, String descricaoProblema) {
        if (clienteUuid == null) {
            throw AppException.badRequest(Messages.get("os.cliente.required"));
        }
        if (veiculoUuid == null) {
            throw AppException.badRequest(Messages.get("os.veiculo.required"));
        }
        if (descricaoProblema == null || descricaoProblema.isBlank()) {
            throw AppException.badRequest(Messages.get("os.descricao.required"));
        }
    }

    private static String normalizar(String texto) {
        return texto == null || texto.isBlank() ? null : texto.strip();
    }
}
