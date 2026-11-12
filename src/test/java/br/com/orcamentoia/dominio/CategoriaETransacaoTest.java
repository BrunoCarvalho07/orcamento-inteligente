package br.com.orcamentoia.dominio;

import br.com.orcamentoia.dominio.excecao.RegraDeNegocioException;
import br.com.orcamentoia.dominio.modelo.Categoria;
import br.com.orcamentoia.dominio.modelo.ResumoCategoria;
import br.com.orcamentoia.dominio.modelo.ResumoCategoria.NivelAlerta;
import br.com.orcamentoia.dominio.modelo.TipoTransacao;
import br.com.orcamentoia.dominio.modelo.Transacao;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Invariantes dos objetos de domínio: não deve ser possível criar um objeto inválido. */
class CategoriaETransacaoTest {

    @Test
    @DisplayName("Normalização remove acentos, caixa e espaços repetidos")
    void normalizaNomes() {
        assertThat(Categoria.normalizar("  Alimentação   FORA  de Casa ")).isEqualTo("alimentacao fora de casa");
    }

    @Test
    @DisplayName("Categoria exige nome e limite positivo")
    void validaCategoria() {
        assertThatThrownBy(() -> Categoria.nova("  ", null)).isInstanceOf(RegraDeNegocioException.class);
        assertThatThrownBy(() -> Categoria.nova("x".repeat(61), null)).isInstanceOf(RegraDeNegocioException.class);
        assertThatThrownBy(() -> Categoria.nova("Lazer", new BigDecimal("-1"))).isInstanceOf(RegraDeNegocioException.class);
    }

    @Test
    @DisplayName("Limite é arredondado para 2 casas")
    void arredondaLimite() {
        assertThat(Categoria.nova("Lazer", new BigDecimal("10.005")).getLimiteMensal()).isEqualByComparingTo("10.01");
    }

    @Test
    @DisplayName("Valor é arredondado e descrição vazia recebe texto padrão")
    void normalizaTransacao() {
        Transacao t = Transacao.nova(TipoTransacao.DESPESA, new BigDecimal("10.555"), "  ",
                Categoria.nova("Mercado", null), LocalDate.of(2026, 1, 1));

        assertThat(t.getValor()).isEqualByComparingTo("10.56");
        assertThat(t.getDescricao()).isEqualTo("Sem descrição");
    }

    @Test
    @DisplayName("Descrição longa é truncada, não rejeitada")
    void truncaDescricao() {
        Transacao t = Transacao.nova(TipoTransacao.DESPESA, BigDecimal.TEN, "a".repeat(500),
                Categoria.nova("Mercado", null), LocalDate.of(2026, 1, 1));

        assertThat(t.getDescricao()).hasSize(Transacao.TAMANHO_MAXIMO_DESCRICAO);
    }

    @Test
    @DisplayName("Despesa tem sinal negativo e receita positivo no saldo")
    void sinalDoValor() {
        Categoria c = Categoria.nova("X", null);
        LocalDate d = LocalDate.of(2026, 1, 1);

        assertThat(Transacao.nova(TipoTransacao.DESPESA, BigDecimal.TEN, "a", c, d).valorComSinal())
                .isEqualByComparingTo("-10.00");
        assertThat(Transacao.nova(TipoTransacao.RECEITA, BigDecimal.TEN, "a", c, d).valorComSinal())
                .isEqualByComparingTo("10.00");
    }

    @Test
    @DisplayName("Valor absurdo (erro típico de transcrição) é rejeitado")
    void rejeitaValorAbsurdo() {
        assertThatThrownBy(() -> Transacao.nova(TipoTransacao.DESPESA, new BigDecimal("999999999"), "a",
                Categoria.nova("X", null), LocalDate.of(2026, 1, 1)))
                .isInstanceOf(RegraDeNegocioException.class).hasMessageContaining("alto demais");
    }

    @Test
    @DisplayName("Limiares do alerta: 79,9% normal; 80% atenção; 100% atenção; acima, excedido")
    void limiaresDoAlerta() {
        YearMonth mes = YearMonth.of(2026, 11);
        BigDecimal limite = new BigDecimal("100.00");

        assertThat(resumo(mes, limite, "79.99").nivelAlerta()).isEqualTo(NivelAlerta.NORMAL);
        assertThat(resumo(mes, limite, "80.00").nivelAlerta()).isEqualTo(NivelAlerta.ATENCAO);
        assertThat(resumo(mes, limite, "100.00").nivelAlerta()).isEqualTo(NivelAlerta.ATENCAO);
        assertThat(resumo(mes, limite, "100.01").nivelAlerta()).isEqualTo(NivelAlerta.EXCEDIDO);
    }

    private static ResumoCategoria resumo(YearMonth mes, BigDecimal limite, String despesas) {
        return new ResumoCategoria("Lazer", mes, limite, new BigDecimal(despesas), BigDecimal.ZERO);
    }
}
