package br.com.orcamentoia.infraestrutura.demo;

import br.com.orcamentoia.dominio.servico.OrcamentoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;

/**
 * Popula o banco com categorias e lançamentos de exemplo para facilitar o teste manual
 * ("quanto já gastei com mercado?" passa a ter resposta logo de cara).
 *
 * <p>Só roda quando {@code app.dados-demo=true}. Passa pelo {@link OrcamentoService}, e não direto
 * pelos repositórios, para que os dados de exemplo respeitem as mesmas regras de negócio que os
 * dados reais.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.dados-demo", havingValue = "true")
class CarregadorDeDadosDemo implements ApplicationRunner {

    private final OrcamentoService orcamento;
    private final Clock relogio;

    @Override
    public void run(ApplicationArguments args) {
        // Idempotência: se já existem categorias, não duplicamos os dados de exemplo.
        if (!orcamento.listarCategorias().isEmpty()) {
            return;
        }
        LocalDate hoje = LocalDate.now(relogio);

        orcamento.definirLimiteMensal("Mercado", new BigDecimal("1200.00"));
        orcamento.definirLimiteMensal("Lazer", new BigDecimal("400.00"));
        orcamento.definirLimiteMensal("Transporte", new BigDecimal("300.00"));

        orcamento.registrarReceita(new BigDecimal("5000.00"), "Salário", "Salário", hoje);
        orcamento.registrarDespesa(new BigDecimal("320.50"), "Compras da semana", "Mercado", hoje.minusDays(2));
        orcamento.registrarDespesa(new BigDecimal("89.90"), "Feira", "Mercado", hoje.minusDays(1));
        orcamento.registrarDespesa(new BigDecimal("120.00"), "Cinema e jantar", "Lazer", hoje.minusDays(3));
        orcamento.registrarDespesa(new BigDecimal("60.00"), "Aplicativo de transporte", "Transporte", hoje);

        log.info("Dados de demonstração carregados (3 categorias com limite, 1 receita, 4 despesas).");
    }
}
