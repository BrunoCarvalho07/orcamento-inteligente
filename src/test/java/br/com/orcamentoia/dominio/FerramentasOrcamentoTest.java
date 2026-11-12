package br.com.orcamentoia.dominio;

import br.com.orcamentoia.dominio.servico.OrcamentoService;
import br.com.orcamentoia.infraestrutura.ia.ferramentas.FerramentasOrcamento;
import br.com.orcamentoia.infraestrutura.ia.ferramentas.ResultadoFerramenta;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Testa as ferramentas exatamente como o modelo as aciona: com texto (datas AAAA-MM-DD) e esperando um
 * {@link ResultadoFerramenta} em vez de exceções. Valida o contrato entre a IA e o domínio sem usar IA.
 */
class FerramentasOrcamentoTest {

    private static final ZoneId FUSO = ZoneId.of("America/Sao_Paulo");
    private FerramentasOrcamento ferramentas;

    @BeforeEach
    void preparar() {
        Clock relogio = Clock.fixed(LocalDate.of(2026, 11, 11).atTime(12, 0).atZone(FUSO).toInstant(), FUSO);
        ferramentas = new FerramentasOrcamento(new OrcamentoService(
                new RepositoriosEmMemoria.CategoriasEmMemoria(),
                new RepositoriosEmMemoria.TransacoesEmMemoria(),
                relogio));
    }

    @Test
    @DisplayName("Registrar despesa devolve o lançamento e o alerta da categoria")
    void registraDespesa() {
        ferramentas.definirLimiteMensal("Mercado", new BigDecimal("100"));

        var resultado = ferramentas.registrarDespesa(new BigDecimal("90"), "Compras", "Mercado", "2026-11-10");

        assertThat(resultado.sucesso()).isTrue();
        assertThat(resultado.dados().lancamento().data()).isEqualTo(LocalDate.of(2026, 11, 10));
        assertThat(resultado.dados().alerta()).isEqualTo("ATENCAO");
    }

    @Test
    @DisplayName("Violação de regra vira falha com mensagem, sem lançar exceção")
    void regraViolada() {
        var resultado = ferramentas.registrarDespesa(new BigDecimal("-5"), "x", "Mercado", null);

        assertThat(resultado.sucesso()).isFalse();
        assertThat(resultado.erro()).contains("maior que zero");
        assertThat(resultado.dados()).isNull();
    }

    @Test
    @DisplayName("Data em formato errado orienta o modelo a corrigir")
    void dataInvalida() {
        var resultado = ferramentas.registrarDespesa(BigDecimal.TEN, "x", "Mercado", "ontem");

        assertThat(resultado.sucesso()).isFalse();
        assertThat(resultado.erro()).contains("AAAA-MM-DD");
    }

    @Test
    @DisplayName("Consulta de categoria inexistente lista as categorias para o modelo se corrigir")
    void categoriaInexistente() {
        ferramentas.registrarDespesa(BigDecimal.TEN, "x", "Mercado", null);

        ResultadoFerramenta<?> resultado = ferramentas.consultarSaldoDaCategoria("Viagem", null);

        assertThat(resultado.sucesso()).isFalse();
        assertThat(resultado.erro()).contains("Mercado");
    }

    @Test
    @DisplayName("Extrato só envia ao modelo as primeiras linhas, mas os totais consideram todas")
    void extratoLongoTemTotaisCompletos() {
        for (int i = 0; i < 40; i++) {
            ferramentas.registrarDespesa(BigDecimal.TEN, "item " + i, "Mercado", null);
        }

        var extrato = ferramentas.consultarExtrato(null, null, null).dados();

        assertThat(extrato.totalDeLancamentos()).isEqualTo(40);
        assertThat(extrato.lancamentos()).hasSize(30);
        assertThat(extrato.listaParcial()).isTrue();
        assertThat(extrato.totalDespesas()).isEqualByComparingTo("400.00");
    }

    @Test
    @DisplayName("Resumo do mês e listagem de categorias funcionam sem parâmetros")
    void consultasSimples() {
        ferramentas.registrarReceita(new BigDecimal("1000"), "Salário", "Salário", null);
        ferramentas.registrarDespesa(new BigDecimal("250"), "Mercado", "Mercado", null);

        assertThat(ferramentas.consultarResumoDoMes(null).dados().saldo()).isEqualByComparingTo("750.00");
        assertThat(ferramentas.listarCategorias().dados()).hasSize(2);
    }
}
