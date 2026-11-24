package com.fiap.mekano.os.infrastructure.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fiap.mekano.os.domain.exception.AppException;
import com.fiap.mekano.os.domain.model.ItemOs;
import com.fiap.mekano.os.domain.model.OrdemDeServico;
import com.fiap.mekano.os.domain.model.StatusOS;
import com.fiap.mekano.os.domain.model.TipoItemOs;
import com.fiap.mekano.os.infrastructure.audit.AuditoriaOrigem;

import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

@QuarkusTest
class OrdemDeServicoRepositoryImplTest {

    @Inject
    OrdemDeServicoRepositoryImpl repository;

    @Inject
    OrdemDeServicoPanacheRepository panacheRepository;

    @BeforeEach
    @Transactional
    void limpar() {
        panacheRepository.deleteAll();
    }

    @Test
    @TestTransaction
    void saveCreateUpdateEAuditoria() {
        UUID cliente = UUID.randomUUID();
        UUID veiculo = UUID.randomUUID();
        OrdemDeServico os = OrdemDeServico.create(cliente, veiculo, "Troca de pastilha");
        os.adicionarItem(ItemOs.create(UUID.randomUUID(), TipoItemOs.PECA, "Pastilha", new BigDecimal("150.00"), 2L));

        OrdemDeServico salva = repository.save(os);

        assertThat(salva.getId()).isEqualTo(os.getId());
        assertThat(salva.getItens()).hasSize(1);
        assertThat(salva.getValorTotalItens()).isEqualByComparingTo("300.00");

        var entity = panacheRepository.find("uuid = ?1", salva.getId()).firstResult();
        assertThat(entity.getCreatedBy())
                .isIn(AuditoriaOrigem.PUBLICO.getCodigo(), AuditoriaOrigem.SISTEMA.getCodigo());

        salva.iniciarDiagnostico();
        OrdemDeServico atualizada = repository.save(salva);
        assertThat(atualizada.getStatus()).isEqualTo(StatusOS.EM_DIAGNOSTICO);
        assertThat(repository.findById(salva.getId())).isPresent();
    }

    @Test
    @TestTransaction
    void conflitoDeVersaoLanca409() {
        OrdemDeServico os = OrdemDeServico.create(UUID.randomUUID(), UUID.randomUUID(), "Freio");
        OrdemDeServico salva = repository.save(os);

        // Simula atualização concorrente incrementando a versão no banco
        OrdemDeServico v1 = OrdemDeServico.reconstitute(
                salva.getId(), salva.getClienteUuid(), salva.getVeiculoUuid(), "Outra",
                StatusOS.EM_DIAGNOSTICO, salva.getStatusEntrega(), null, null, null, null,
                null, null, null, null, null, salva.getCreatedAt(), 0L, List.of());

        salva.iniciarDiagnostico();
        repository.save(salva); // version passa a 1

        assertThatThrownBy(() -> repository.save(v1))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getStatus()).isEqualTo(409);
    }

    @Test
    @TestTransaction
    void filtrosOrdenacaoSoftDeleteEEstatisticas() {
        UUID cliente = UUID.randomUUID();
        UUID veiculo = UUID.randomUUID();
        UUID mecanico = UUID.randomUUID();

        OrdemDeServico recebida = OrdemDeServico.create(cliente, veiculo, "Recebida");
        repository.save(recebida);

        OrdemDeServico emExec = OrdemDeServico.create(cliente, veiculo, "Em execução");
        emExec.iniciarDiagnostico();
        emExec.adicionarItem(ItemOs.create(UUID.randomUUID(), TipoItemOs.SERVICO, "Revisão", BigDecimal.TEN, 1L));
        emExec.finalizarDiagnostico();
        emExec.registrarAprovacaoOrcamento(UUID.randomUUID());
        emExec.iniciarExecucao(mecanico, "ok");
        repository.save(emExec);

        assertThat(repository.countAll()).isEqualTo(2);
        assertThat(repository.findAll(0, 10, "createdAt,asc")).hasSize(2);
        assertThat(repository.existsByClienteUuidAndStatusIn(cliente, List.of(StatusOS.EM_EXECUCAO))).isTrue();
        assertThat(repository.existsByClienteUuidAndStatusIn(cliente, List.of())).isFalse();

        // EM_EXECUCAO tem maior prioridade que RECEBIDA
        List<OrdemDeServico> filtradas = repository.findAllWithFilters(
                null, cliente, veiculo, LocalDateTime.now().minusHours(1), LocalDateTime.now().plusHours(1), 0, 10);
        assertThat(filtradas).hasSize(2);
        assertThat(filtradas.get(0).getStatus()).isEqualTo(StatusOS.EM_EXECUCAO);

        // Finaliza execução para testar cálculo de tempo médio
        emExec = repository.findById(emExec.getId()).orElseThrow();
        emExec.finalizarExecucao("feito");
        repository.save(emExec);

        assertThat(repository.calcularTempoMedioExecucao(null, null)).isPresent();
        assertThat(repository.calcularTempoMedioPorMecanico(null, null)).containsKey(mecanico);

        // Soft delete
        repository.markAsDeleted(recebida.getId());
        assertThat(repository.findById(recebida.getId())).isEmpty();
        assertThat(repository.countAll()).isEqualTo(1);
    }
}
