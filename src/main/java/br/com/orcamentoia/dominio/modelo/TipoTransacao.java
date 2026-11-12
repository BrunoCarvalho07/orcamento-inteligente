package br.com.orcamentoia.dominio.modelo;

/**
 * Natureza de um lançamento financeiro.
 *
 * <p>O {@link #sinal} existe para que o cálculo de saldo seja uma simples soma
 * ({@code valor * sinal}) em vez de espalhar {@code if (tipo == DESPESA)} pelo código.</p>
 */
public enum TipoTransacao {

    RECEITA(1),
    DESPESA(-1);

    private final int sinal;

    TipoTransacao(int sinal) {
        this.sinal = sinal;
    }

    public int getSinal() {
        return sinal;
    }
}
