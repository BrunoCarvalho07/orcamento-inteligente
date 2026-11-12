package br.com.orcamentoia.infraestrutura.ia;

import br.com.orcamentoia.aplicacao.excecao.FalhaIntegracaoIaException;
import br.com.orcamentoia.aplicacao.modelo.AudioSintetizado;
import br.com.orcamentoia.aplicacao.porta.SintetizadorDeVoz;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.audio.tts.TextToSpeechModel;
import org.springframework.ai.audio.tts.TextToSpeechPrompt;
import org.springframework.ai.audio.tts.TextToSpeechResponse;
import org.springframework.ai.openai.OpenAiAudioSpeechOptions;
import org.springframework.ai.openai.api.OpenAiAudioApi;
import org.springframework.stereotype.Component;

/**
 * Adaptador de Text-to-Speech usando a API de voz da OpenAI via Spring AI.
 *
 * <p>Injetamos a interface portável {@link TextToSpeechModel} (e não a classe da OpenAI) para que
 * trocar de provedor de voz exija apenas outra configuração.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
class SintetizadorOpenAi implements SintetizadorDeVoz {

    /** MP3 é tocado por qualquer navegador e celular e é compacto para respostas curtas. */
    private static final String TIPO_MIDIA_MP3 = "audio/mpeg";

    private final TextToSpeechModel modeloDeVoz;

    @Override
    public AudioSintetizado sintetizar(String texto) {
        try {
            OpenAiAudioSpeechOptions opcoes = OpenAiAudioSpeechOptions.builder()
                    .model(OpenAiAudioApi.TtsModel.GPT_4_O_MINI_TTS.value)
                    .voice(OpenAiAudioApi.SpeechRequest.Voice.NOVA)
                    .responseFormat(OpenAiAudioApi.SpeechRequest.AudioResponseFormat.MP3)
                    .speed(1.0)
                    // "instructions" orienta o sotaque e o tom. Sem isso, o modelo pode ler texto em
                    // português com sotaque estrangeiro.
                    .instructions("Fale em português do Brasil, com tom cordial, claro e natural.")
                    .build();

            TextToSpeechResponse resposta = modeloDeVoz.call(new TextToSpeechPrompt(texto, opcoes));
            return new AudioSintetizado(resposta.getResult().getOutput(), TIPO_MIDIA_MP3);
        } catch (RuntimeException e) {
            log.error("Falha na síntese de voz", e);
            throw new FalhaIntegracaoIaException("Não foi possível gerar o áudio da resposta.", e);
        }
    }
}
