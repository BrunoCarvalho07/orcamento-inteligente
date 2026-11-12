package br.com.orcamentoia.aplicacao.excecao;

/**
 * Falha ao se comunicar com um serviço de IA externo (transcrição, modelo de linguagem ou síntese
 * de voz): chave inválida, indisponibilidade, limite de taxa, timeout etc.
 *
 * <p>Encapsula a exceção original do provedor ({@code cause}) para que ela apareça nos logs, mas a
 * mensagem exposta ao cliente é genérica: detalhes do provedor (nomes de modelo, trechos de
 * resposta) não devem vazar pela API.</p>
 */
public class FalhaIntegracaoIaException extends RuntimeException {

    public FalhaIntegracaoIaException(String mensagem, Throwable causa) {
        super(mensagem, causa);
    }

    public FalhaIntegracaoIaException(String mensagem) {
        super(mensagem);
    }
}
