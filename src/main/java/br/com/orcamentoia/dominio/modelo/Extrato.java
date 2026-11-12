package br.com.orcamentoia.dominio.modelo;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Extrato de um período: os lançamentos e seus totais.
 *
 * <p><b>Por que os totais são calculados aqui?</b> Modelos de linguagem são ruins em aritmética e,
 * quando o extrato é longo, a camada de IA envia apenas parte das linhas ao modelo. Se pedíssemos
 * ao modelo para somar, o usuário ouviria um total errado com a maior naturalidade. Calculando no
 * domínio, sobre <em>todos</em> os lançamentos, o número falado é sempre exato.</p>
 *
 * @param inicio      início efetivo do período (já com o padrão aplicado)
 * @param fim         fim efetivo do período (já com o padrão aplicado)
 * @param lancamentos lançamentos do período, do mais recente para o mais antigo
 */
public record Extrato(LocalDate inicio, LocalDate fim, List<Transacao> lancamentos) {

    public Extrato {
        // Cópia defensiva: o record continua imutável mesmo que a lista original seja alterada.
        lancamentos = List.copyOf(lancamentos);
    }

    public BigDecimal totalReceitas() {
        return somar(TipoTransacao.RECEITA);
    }

    public BigDecimal totalDespesas() {
        return somar(TipoTransacao.DESPESA);
    }

    public BigDecimal saldo() {
        return totalReceitas().subtract(totalDespesas());
    }

    private BigDecimal somar(TipoTransacao tipo) {
        return lancamentos.stream()
                .filter(transacao -> transacao.getTipo() == tipo)
                .map(Transacao::getValor)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
