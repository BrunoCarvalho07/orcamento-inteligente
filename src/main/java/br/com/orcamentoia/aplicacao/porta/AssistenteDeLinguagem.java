package br.com.orcamentoia.aplicacao.porta;

/**
 * Porta do "cérebro" da aplicação: entende um comando em linguagem natural, aciona as operações
 * de orçamento necessárias e devolve a resposta em texto.
 *
 * <p>A implementação atual usa Spring AI (ChatClient + Tool Calling), mas o caso de uso não sabe
 * disso: poderia ser outro provedor, um modelo local ou um dublê nos testes.</p>
 */
public interface AssistenteDeLinguagem {

    /**
     * @param mensagemDoUsuario comando em texto (digitado ou vindo da transcrição de voz)
     * @return resposta pronta para ser exibida ou falada
     * @throws br.com.orcamentoia.aplicacao.excecao.FalhaIntegracaoIaException se o modelo falhar
     */
    String responder(String mensagemDoUsuario);
}
