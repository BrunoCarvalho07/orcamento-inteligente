package br.com.orcamentoia.infraestrutura.ia;

import br.com.orcamentoia.aplicacao.excecao.FalhaIntegracaoIaException;
import br.com.orcamentoia.aplicacao.porta.TranscritorDeAudio;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.audio.transcription.AudioTranscriptionPrompt;
import org.springframework.ai.audio.transcription.AudioTranscriptionResponse;
import org.springframework.ai.openai.OpenAiAudioTranscriptionModel;
import org.springframework.ai.openai.OpenAiAudioTranscriptionOptions;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Component;

/**
 * Adaptador de Speech-to-Text usando a API de transcrição da OpenAI via Spring AI.
 */
@Slf4j
@Component
@RequiredArgsConstructor
class TranscritorOpenAi implements TranscritorDeAudio {

    private final OpenAiAudioTranscriptionModel modeloDeTranscricao;

    @Override
    public String transcrever(byte[] conteudo, String nomeArquivo) {
        try {
            // O Spring AI lê o áudio de um Resource. ByteArrayResource não tem nome, e a OpenAI
            // identifica o formato (mp3, wav, webm...) pela extensão do nome do arquivo enviado;
            // por isso sobrescrevemos getFilename() com o nome original.
            ByteArrayResource audio = new ByteArrayResource(conteudo) {
                @Override
                public String getFilename() {
                    return nomeArquivo;
                }
            };

            // Idioma fixo em português: informar o idioma melhora a precisão e reduz a latência,
            // pois o modelo não precisa detectá-lo. O prompt ajuda a acertar vocabulário financeiro
            // que costuma ser transcrito errado ("reais", "cartão", nomes de categorias).
            OpenAiAudioTranscriptionOptions opcoes = OpenAiAudioTranscriptionOptions.builder()
                    .language("pt")
                    .prompt("Comando sobre finanças pessoais em português do Brasil: despesas, receitas, "
                            + "saldo, extrato, categorias e valores em reais.")
                    .temperature(0f)
                    .build();

            AudioTranscriptionResponse resposta =
                    modeloDeTranscricao.call(new AudioTranscriptionPrompt(audio, opcoes));

            String texto = resposta.getResult().getOutput();
            return texto == null ? "" : texto.trim();
        } catch (RuntimeException e) {
            log.error("Falha na transcrição de áudio", e);
            throw new FalhaIntegracaoIaException("Não foi possível transcrever o áudio.", e);
        }
    }
}
