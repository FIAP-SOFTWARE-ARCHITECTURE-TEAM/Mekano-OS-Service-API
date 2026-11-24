package com.fiap.mekano.os.infrastructure.entity;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "ordens_de_servico")
@Getter
@Setter
@NoArgsConstructor
public class OrdemDeServicoEntity extends BaseEntity {

    @Column(nullable = false, unique = true)
    private UUID uuid;

    @Column(name = "cliente_uuid", nullable = false)
    private UUID clienteUuid;

    @Column(name = "veiculo_uuid", nullable = false)
    private UUID veiculoUuid;

    @Column(name = "descricao_problema", nullable = false, length = 500)
    private String descricaoProblema;

    @Column(nullable = false, length = 30)
    private String status;

    @Column(name = "status_entrega", nullable = false, length = 30)
    private String statusEntrega;

    @Column(name = "motivo_cancelamento", length = 500)
    private String motivoCancelamento;

    @Column(name = "orcamento_uuid")
    private UUID orcamentoUuid;

    @Column(name = "data_aprovacao")
    private LocalDateTime dataAprovacao;

    @Column(name = "mecanico_uuid")
    private UUID mecanicoUuid;

    @Column(name = "execucao_iniciada_em")
    private LocalDateTime execucaoIniciadaEm;

    @Column(name = "execucao_finalizada_em")
    private LocalDateTime execucaoFinalizadaEm;

    @Column(name = "observacao_execucao", length = 1000)
    private String observacaoExecucao;

    @Column(name = "entregue_em")
    private LocalDateTime entregueEm;

    @Column(name = "recebido_por", length = 200)
    private String recebidoPor;

    @Version
    @Column(nullable = false)
    private Long version;

    @OneToMany(mappedBy = "ordemDeServico", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<ItemOsEntity> itens = new ArrayList<>();

    public void substituirItens(List<ItemOsEntity> novos) {
        this.itens.clear();
        if (novos != null) {
            for (ItemOsEntity item : novos) {
                item.setOrdemDeServico(this);
                this.itens.add(item);
            }
        }
    }
}
