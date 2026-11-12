package br.com.orcamentoia.dominio.modelo;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.YearMonth;

/**
 * Situação de uma categoria em um mês: quanto entrou, quanto saiu e como isso se compara ao limite.
 *
 * <p>É um <i>record</i> porque é um valor calculado, sem identidade nem ciclo de vida. Todos os
 * números derivados (restante, percentual, alerta) são calculados aqui, no domínio, e não na
 * camada de IA ou no controller. Assim a regra "a partir de 80% do limite entra em atenção" existe
 * em um único lugar e é testável sem subir Spring.</p>
 *
 * @param categoria     nome de exibição da categoria
 * @param mes           mês de referência
 * @param limiteMensal  limite configurado ({@code null} se não houver)
 * @param totalDespesas soma das despesas da categoria no mês
 * @param totalReceitas soma das receitas da categoria no mês
 */
public record ResumoCategoria(
        String categoria,
        YearMonth mes,
        BigDecimal limiteMensal,
        BigDecimal totalDespesas,
        BigDecimal totalReceitas) {

    /** A partir de 80% do limite consumido o usuário é avisado (mas ainda não "estourou"). */
    private static final BigDecimal LIMIAR_ATENCAO = new BigDecimal("0.80");

    /** Nível de alerta de consumo do limite mensal. */
    public enum NivelAlerta {
        /** A categoria não tem limite configurado. */
        SEM_LIMITE,
        /** Consumo abaixo de 80% do limite. */
        NORMAL,
        /** Consumo entre 80% e 100% do limite (inclusive). */
        ATENCAO,
        /** Despesas acima do limite. */
        EXCEDIDO
    }

    public boolean possuiLimite() {
        return limiteMensal != null;
    }

    /** Receitas menos despesas da categoria no mês. */
    public BigDecimal saldoDaCategoria() {
        return totalReceitas.subtract(totalDespesas);
    }

    /** Quanto ainda pode ser gasto (negativo se estourou); {@code null} sem limite. */
    public BigDecimal restanteDoLimite() {
        return possuiLimite() ? limiteMensal.subtract(totalDespesas) : null;
    }

    /** Percentual do limite já consumido, com 1 casa decimal; {@code null} sem limite. */
    public BigDecimal percentualDoLimiteUtilizado() {
        if (!possuiLimite()) {
            return null;
        }
        return totalDespesas.multiply(BigDecimal.valueOf(100))
                .divide(limiteMensal, 1, RoundingMode.HALF_UP);
    }

    public NivelAlerta nivelAlerta() {
        if (!possuiLimite()) {
            return NivelAlerta.SEM_LIMITE;
        }
        if (totalDespesas.compareTo(limiteMensal) > 0) {
            return NivelAlerta.EXCEDIDO;
        }
        if (totalDespesas.compareTo(limiteMensal.multiply(LIMIAR_ATENCAO)) >= 0) {
            return NivelAlerta.ATENCAO;
        }
        return NivelAlerta.NORMAL;
    }
}
