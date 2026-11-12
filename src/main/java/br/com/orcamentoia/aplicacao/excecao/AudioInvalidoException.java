package br.com.orcamentoia.aplicacao.excecao;

/**
 * O áudio recebido não pode ser processado por culpa de quem enviou: arquivo vazio, formato não
 * suportado, silêncio, comando longo demais.
 *
 * <p>Diferente de {@link FalhaIntegracaoIaException}: aqui o problema é do <em>cliente</em> (HTTP 400);
 * lá o problema é de um serviço externo (HTTP 502). Separar os dois permite ao cliente decidir se
 * vale tentar de novo (só no segundo caso).</p>
 */
public class AudioInvalidoException extends RuntimeException {

    public AudioInvalidoException(String mensagem) {
        super(mensagem);
    }
}
