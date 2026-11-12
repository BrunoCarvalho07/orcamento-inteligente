package br.com.orcamentoia.aplicacao.porta;

/**
 * Porta de Speech-to-Text: transforma áudio falado em texto.
 *
 * <p>Recebe {@code byte[]} e um nome de arquivo (e não {@code MultipartFile}) para que a camada de
 * aplicação não dependa de nada de HTTP/Servlet. O nome do arquivo importa: provedores como a OpenAI
 * usam a <b>extensão</b> para descobrir o formato do áudio.</p>
 */
public interface TranscritorDeAudio {

    /**
     * @param conteudo    bytes do arquivo de áudio
     * @param nomeArquivo nome original do arquivo, com extensão (ex.: {@code comando.webm})
     * @return texto transcrito (pode ser vazio se o áudio não tiver fala)
     * @throws br.com.orcamentoia.aplicacao.excecao.FalhaIntegracaoIaException se o provedor falhar
     */
    String transcrever(byte[] conteudo, String nomeArquivo);
}
