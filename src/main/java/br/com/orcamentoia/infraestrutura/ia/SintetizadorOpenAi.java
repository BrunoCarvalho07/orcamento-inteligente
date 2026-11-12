package br.com.orcamentoia.infraestrutura.ia;

import br.com.orcamentoia.aplicacao.excecao.FalhaIntegracaoIaException;
import br.com.orcamentoia.aplicacao.modelo.AudioSintetizado;
import br.com.orcamentoia.aplicacao.porta.SintetizadorDeVoz;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.audio.tts.TextToSpeechModel;
import org.springframework.ai.audio.tts.TextToSpeechPrompt;
import org.springframework.ai.audio.tts.TextToSpeechResponse;
import org.springframework.stereotype.Component;

/**
 * Adaptador de Text-to-Speech usando a API de voz da OpenAI via Spring AI.
 *
 * <p>Injetamos a interface portável {@link TextToSpeechModel} (e não a classe da OpenAI) para que
 * trocar de provedor de voz exija apenas outra configuração.</p>
 *
 * <p><b>Por que a voz é configurada no {@code application.yml}?</b> Modelo, voz, formato e tom
 * (propriedades {@code spring.ai.openai.audio.speech.*}) são escolhas de configuração, não de lógica.
 * Mantê-las fora do código evita depender de classes internas do provedor, que mudam entre versões do
 * Spring AI, e permite trocar a voz sem recompilar.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
class SintetizadorOpenAi implements SintetizadorDeVoz {

    /**
     * MP3 é tocado por qualquer navegador e celular e é compacto para respostas curtas. Precisa
     * estar de acordo com {@code spring.ai.openai.audio.speech.response-format} (padrão: mp3).
     */
    private static final String TIPO_MIDIA_MP3 = "audio/mpeg";

    private final TextToSpeechModel modeloDeVoz;

    @Override
    public AudioSintetizado sintetizar(String texto) {
        try {
            TextToSpeechResponse resposta = modeloDeVoz.call(new TextToSpeechPrompt(texto));
            return new AudioSintetizado(resposta.getResult().getOutput(), TIPO_MIDIA_MP3);
        } catch (RuntimeException e) {
            log.error("Falha na síntese de voz", e);
            throw new FalhaIntegracaoIaException("Não foi possível gerar o áudio da resposta.", e);
        }
    }
}
