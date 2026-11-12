package br.com.orcamentoia.dominio;

import br.com.orcamentoia.dominio.excecao.CategoriaNaoEncontradaException;
import br.com.orcamentoia.dominio.excecao.RegraDeNegocioException;
import br.com.orcamentoia.dominio.modelo.Extrato;
import br.com.orcamentoia.dominio.modelo.ResultadoRegistro;
import br.com.orcamentoia.dominio.modelo.ResumoCategoria;
import br.com.orcamentoia.dominio.modelo.ResumoCategoria.NivelAlerta;
import br.com.orcamentoia.dominio.modelo.ResumoFinanceiro;
import br.com.orcamentoia.dominio.servico.OrcamentoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Testes das regras de negócio do orçamento, sem Spring, sem banco e sem IA.
 *
 * <p>O relógio é fixo (11/11/2026) para que "hoje", "ontem" e "mês atual" sejam determinísticos.</p>
 */
class OrcamentoServiceTest {

    private static final ZoneId FUSO = ZoneId.of("America/Sao_Paulo");
    private static final LocalDate HOJE = LocalDate.of(2026, 11, 11);

    private OrcamentoService servico;

    @BeforeEach
    void preparar() {
        Clock relogioFixo = Clock.fixed(HOJE.atTime(12, 0).atZone(FUSO).toInstant(), FUSO);
        servico = new OrcamentoService(
                new RepositoriosEmMemoria.CategoriasEmMemoria(),
                new RepositoriosEmMemoria.TransacoesEmMemoria(),
                relogioFixo);
    }

    @Test
    @DisplayName("Registrar despesa sem data usa hoje e cria a categoria automaticamente")
    void registraDespesaComDataPadrao() {
        ResultadoRegistro resultado = servico.registrarDespesa(new BigDecimal("45.90"), "Almoço", "Alimentação", null);

        assertThat(resultado.transacao().getData()).isEqualTo(HOJE);
        assertThat(resultado.transacao().getId()).isNotNull();
        assertThat(servico.listarCategorias()).extracting("nome").containsExactly("Alimentação");
    }

    @Test
    @DisplayName("Categorias são reconhecidas sem diferenciar acentos e maiúsculas")
    void reaproveitaCategoriaIgnorandoAcentos() {
        servico.registrarDespesa(new BigDecimal("10"), "Pão", "Alimentação", null);
        servico.registrarDespesa(new BigDecimal("20"), "Suco", "ALIMENTACAO", null);

        assertThat(servico.listarCategorias()).hasSize(1);
        assertThat(servico.consultarResumoDaCategoria("alimentacao", null).totalDespesas())
                .isEqualByComparingTo("30.00");
    }

    @Test
    @DisplayName("Rejeita valores zero, negativos e arredondados para zero")
    void rejeitaValoresInvalidos() {
        for (String valor : new String[]{"0", "-5", "0.004"}) {
            assertThatThrownBy(() -> servico.registrarDespesa(new BigDecimal(valor), "x", "Mercado", null))
                    .isInstanceOf(RegraDeNegocioException.class)
                    .hasMessageContaining("maior que zero");
        }
    }

    @Test
    @DisplayName("Rejeita data futura")
    void rejeitaDataFutura() {
        assertThatThrownBy(() -> servico.registrarDespesa(new BigDecimal("10"), "x", "Mercado", HOJE.plusDays(1)))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("data futura");
    }

    @Test
    @DisplayName("Lançamento com valor inválido não é gravado")
    void valorInvalidoNaoRegistraNada() {
        assertThatThrownBy(() -> servico.registrarDespesa(BigDecimal.ZERO, "x", "Nova", null))
                .isInstanceOf(RegraDeNegocioException.class);

        assertThat(servico.consultarExtrato(null, null, null).lancamentos()).isEmpty();
    }

    @Test
    @DisplayName("Alerta de limite: normal, atenção (>= 80%) e excedido (> 100%)")
    void alertaDeLimiteEvoluiConformeOGasto() {
        servico.definirLimiteMensal("Lazer", new BigDecimal("100"));

        ResultadoRegistro primeiro = servico.registrarDespesa(new BigDecimal("50"), "Cinema", "Lazer", null);
        assertThat(primeiro.situacaoDaCategoria().nivelAlerta()).isEqualTo(NivelAlerta.NORMAL);

        ResultadoRegistro segundo = servico.registrarDespesa(new BigDecimal("30"), "Pizza", "Lazer", null);
        assertThat(segundo.situacaoDaCategoria().nivelAlerta()).isEqualTo(NivelAlerta.ATENCAO);

        ResultadoRegistro terceiro = servico.registrarDespesa(new BigDecimal("30"), "Show", "Lazer", null);
        ResumoCategoria situacao = terceiro.situacaoDaCategoria();
        assertThat(situacao.nivelAlerta()).isEqualTo(NivelAlerta.EXCEDIDO);
        assertThat(situacao.restanteDoLimite()).isEqualByComparingTo("-10.00");
        assertThat(situacao.percentualDoLimiteUtilizado()).isEqualByComparingTo("110.0");
    }

    @Test
    @DisplayName("Categoria sem limite devolve SEM_LIMITE")
    void categoriaSemLimite() {
        ResultadoRegistro resultado = servico.registrarDespesa(new BigDecimal("10"), "x", "Diversos", null);

        assertThat(resultado.situacaoDaCategoria().nivelAlerta()).isEqualTo(NivelAlerta.SEM_LIMITE);
        assertThat(resultado.situacaoDaCategoria().restanteDoLimite()).isNull();
    }

    @Test
    @DisplayName("O alerta considera o mês do lançamento, não o mês atual")
    void alertaUsaOMesDoLancamento() {
        servico.definirLimiteMensal("Mercado", new BigDecimal("100"));
        LocalDate mesPassado = HOJE.minusMonths(1);

        servico.registrarDespesa(new BigDecimal("150"), "Compras", "Mercado", mesPassado);
        ResultadoRegistro deHoje = servico.registrarDespesa(new BigDecimal("10"), "Pão", "Mercado", null);

        assertThat(deHoje.situacaoDaCategoria().totalDespesas()).isEqualByComparingTo("10.00");
        assertThat(servico.consultarResumoDaCategoria("Mercado", YearMonth.from(mesPassado)).nivelAlerta())
                .isEqualTo(NivelAlerta.EXCEDIDO);
    }

    @Test
    @DisplayName("Limite inválido é rejeitado")
    void rejeitaLimiteInvalido() {
        assertThatThrownBy(() -> servico.definirLimiteMensal("Lazer", BigDecimal.ZERO))
                .isInstanceOf(RegraDeNegocioException.class);
    }

    @Test
    @DisplayName("Resumo do mês soma receitas e despesas e calcula o saldo")
    void resumoDoMes() {
        servico.registrarReceita(new BigDecimal("5000"), "Salário", "Salário", null);
        servico.registrarDespesa(new BigDecimal("1200.50"), "Aluguel", "Moradia", null);
        servico.registrarDespesa(new BigDecimal("299.50"), "Mercado", "Mercado", null);
        // Lançamento de outro mês não pode entrar na conta.
        servico.registrarDespesa(new BigDecimal("999"), "Antigo", "Moradia", HOJE.minusMonths(2));

        ResumoFinanceiro resumo = servico.consultarResumoFinanceiro(null);

        assertThat(resumo.totalReceitas()).isEqualByComparingTo("5000.00");
        assertThat(resumo.totalDespesas()).isEqualByComparingTo("1500.00");
        assertThat(resumo.saldo()).isEqualByComparingTo("3500.00");
    }

    @Test
    @DisplayName("Extrato padrão cobre do dia 1º do mês até hoje, do mais recente ao mais antigo")
    void extratoPadrao() {
        servico.registrarDespesa(new BigDecimal("10"), "a", "Mercado", HOJE.minusDays(5));
        servico.registrarDespesa(new BigDecimal("20"), "b", "Mercado", HOJE);
        servico.registrarDespesa(new BigDecimal("30"), "fora", "Mercado", HOJE.minusMonths(1));
        servico.registrarReceita(new BigDecimal("100"), "c", "Extra", HOJE.minusDays(1));

        Extrato extrato = servico.consultarExtrato(null, null, null);

        assertThat(extrato.inicio()).isEqualTo(LocalDate.of(2026, 11, 1));
        assertThat(extrato.fim()).isEqualTo(HOJE);
        assertThat(extrato.lancamentos()).extracting("descricao").containsExactly("b", "c", "a");
        assertThat(extrato.totalDespesas()).isEqualByComparingTo("30.00");
        assertThat(extrato.saldo()).isEqualByComparingTo("70.00");
    }

    @Test
    @DisplayName("Extrato pode ser filtrado por categoria")
    void extratoFiltradoPorCategoria() {
        servico.registrarDespesa(new BigDecimal("10"), "a", "Mercado", null);
        servico.registrarDespesa(new BigDecimal("20"), "b", "Lazer", null);

        Extrato extrato = servico.consultarExtrato(null, null, "mercado");

        assertThat(extrato.lancamentos()).extracting("descricao").containsExactly("a");
    }

    @Test
    @DisplayName("Extrato rejeita período invertido")
    void extratoComPeriodoInvertido() {
        assertThatThrownBy(() -> servico.consultarExtrato(HOJE, HOJE.minusDays(1), null))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("não pode ser posterior");
    }

    @Test
    @DisplayName("Consultar categoria inexistente informa as categorias disponíveis")
    void categoriaInexistenteListaAsExistentes() {
        servico.registrarDespesa(new BigDecimal("10"), "x", "Mercado", null);

        assertThatThrownBy(() -> servico.consultarResumoDaCategoria("Viagens", null))
                .isInstanceOf(CategoriaNaoEncontradaException.class)
                .hasMessageContaining("Viagens")
                .hasMessageContaining("Mercado");
    }

    @Test
    @DisplayName("O relógio respeita o fuso configurado (23h30 em São Paulo ainda é o mesmo dia)")
    void hojeDependeDoFuso() {
        Instant tardeDaNoiteEmSaoPaulo = LocalDate.of(2026, 11, 11).atTime(23, 30).atZone(FUSO).toInstant();
        OrcamentoService servicoNoite = new OrcamentoService(
                new RepositoriosEmMemoria.CategoriasEmMemoria(),
                new RepositoriosEmMemoria.TransacoesEmMemoria(),
                Clock.fixed(tardeDaNoiteEmSaoPaulo, FUSO));

        // Em UTC já seria dia 12; no fuso de São Paulo continua sendo dia 11.
        assertThat(servicoNoite.registrarDespesa(new BigDecimal("5"), "x", "Mercado", null).transacao().getData())
                .isEqualTo(LocalDate.of(2026, 11, 11));
    }
}
