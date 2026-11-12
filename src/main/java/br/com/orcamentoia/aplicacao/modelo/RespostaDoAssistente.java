package br.com.orcamentoia.aplicacao.modelo;

/**
 * Resultado completo de um comando enviado ao assistente.
 *
 * @param transcricao texto entendido (para comandos de texto, é a própria mensagem enviada)
 * @param resposta    resposta do assistente em texto
 * @param audio       resposta falada; {@code null} se a voz não foi pedida <b>ou</b> se a síntese
 *                    falhou (nesse caso o texto continua válido, veja o caso de uso)
 */
public record RespostaDoAssistente(String transcricao, String resposta, AudioSintetizado audio) {

    public boolean possuiAudio() {
        return audio != null && audio.conteudo() != null && audio.conteudo().length > 0;
    }
}
