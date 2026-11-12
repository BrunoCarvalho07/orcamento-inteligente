package br.com.orcamentoia.dominio.modelo;

import br.com.orcamentoia.dominio.excecao.RegraDeNegocioException;
import lombok.Getter;
import lombok.ToString;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Objects;

/**
 * Um lançamento financeiro (receita ou despesa) em uma data e categoria.
 *
 * <p>É um objeto de domínio imutável: todas as invariantes são verificadas no construtor, então
 * <b>é impossível existir uma {@code Transacao} inválida</b> em memória. Isso é mais seguro do que
 * validar só nos controllers, porque a mesma regra vale para REST, para a IA e para qualquer
 * entrada futura (importação de planilha, por exemplo).</p>
 *
 * <p>O valor é sempre positivo; quem diz se entra ou sai dinheiro é o {@link TipoTransacao}.
 * Representar despesa como número negativo é uma fonte clássica de bugs (descontar duas vezes,
 * somar com sinal errado).</p>
 */
@Getter
@ToString
public final class Transacao {

    public static final int TAMANHO_MAXIMO_DESCRICAO = 200;
    private static final String DESCRICAO_PADRAO = "Sem descrição";

    /**
     * Teto de segurança por lançamento. Um comando de voz mal transcrito ("quinze reais" virando
     * "quinze milhões") não deve corromper o orçamento silenciosamente.
     */
    private static final BigDecimal VALOR_MAXIMO = new BigDecimal("100000000.00");

    private final Long id;
    private final TipoTransacao tipo;
    private final BigDecimal valor;
    private final String descricao;
    private final Categoria categoria;
    private final LocalDate data;

    private Transacao(Long id, TipoTransacao tipo, BigDecimal valor, String descricao,
                      Categoria categoria, LocalDate data) {
        this.id = id;
        this.tipo = Objects.requireNonNull(tipo, "tipo é obrigatório");
        this.valor = validarValor(valor);
        this.descricao = normalizarDescricao(descricao);
        this.categoria = Objects.requireNonNull(categoria, "categoria é obrigatória");
        this.data = Objects.requireNonNull(data, "data é obrigatória");
    }

    /** Cria um lançamento novo, ainda sem identificador. */
    public static Transacao nova(TipoTransacao tipo, BigDecimal valor, String descricao,
                                 Categoria categoria, LocalDate data) {
        return new Transacao(null, tipo, valor, descricao, categoria, data);
    }

    /** Recria um lançamento já persistido (uso exclusivo dos adaptadores de persistência). */
    public static Transacao reconstituir(Long id, TipoTransacao tipo, BigDecimal valor, String descricao,
                                         Categoria categoria, LocalDate data) {
        return new Transacao(id, tipo, valor, descricao, categoria, data);
    }

    /** Efeito do lançamento no saldo: positivo para receitas, negativo para despesas. */
    public BigDecimal valorComSinal() {
        return valor.multiply(BigDecimal.valueOf(tipo.getSinal()));
    }

    private static BigDecimal validarValor(BigDecimal valor) {
        if (valor == null) {
            throw new RegraDeNegocioException("O valor do lançamento é obrigatório.");
        }
        // Arredondamos antes de validar: 0,004 vira 0,00 e deve ser rejeitado como "não positivo".
        BigDecimal arredondado = valor.setScale(2, RoundingMode.HALF_UP);
        if (arredondado.signum() <= 0) {
            throw new RegraDeNegocioException("O valor do lançamento deve ser maior que zero.");
        }
        if (arredondado.compareTo(VALOR_MAXIMO) > 0) {
            throw new RegraDeNegocioException(
                    "O valor informado é alto demais para um único lançamento; confira se está correto.");
        }
        return arredondado;
    }

    /**
     * Descrição ausente vira um texto padrão e descrições longas são truncadas (não rejeitadas).
     * Por voz, o usuário pode falar bastante; perder o lançamento por causa de uma descrição
     * prolixa seria pior do que guardar apenas o começo dela.
     */
    private static String normalizarDescricao(String descricao) {
        if (descricao == null || descricao.isBlank()) {
            return DESCRICAO_PADRAO;
        }
        String limpa = descricao.trim().replaceAll("\\s+", " ");
        return limpa.length() > TAMANHO_MAXIMO_DESCRICAO
                ? limpa.substring(0, TAMANHO_MAXIMO_DESCRICAO)
                : limpa;
    }
}
