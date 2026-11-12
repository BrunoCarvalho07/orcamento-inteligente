package br.com.orcamentoia.dominio.modelo;

import java.math.BigDecimal;
import java.time.YearMonth;

/**
 * Visão consolidada do mês, somando todas as categorias.
 *
 * @param mes           mês de referência
 * @param totalReceitas soma de todas as receitas do mês
 * @param totalDespesas soma de todas as despesas do mês
 */
public record ResumoFinanceiro(YearMonth mes, BigDecimal totalReceitas, BigDecimal totalDespesas) {

    /** Saldo do mês: o que entrou menos o que saiu. */
    public BigDecimal saldo() {
        return totalReceitas.subtract(totalDespesas);
    }
}
