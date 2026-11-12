package br.com.orcamentoia.api.controller;

import br.com.orcamentoia.api.dto.RequisicoesApi.ComandoTextoRequest;
import br.com.orcamentoia.api.dto.RequisicoesApi.SinteseRequest;
import br.com.orcamentoia.api.dto.RespostasApi.ComandoResponse;
import br.com.orcamentoia.aplicacao.excecao.AudioInvalidoException;
import br.com.orcamentoia.aplicacao.modelo.AudioSintetizado;
import br.com.orcamentoia.aplicacao.servico.AssistenteFinanceiroService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/**
 * Endpoints do assistente por voz e texto.
 *
 * <p>O controller é fino de propósito: converte HTTP em chamadas ao caso de uso e o resultado em JSON.
 * Nenhuma regra mora aqui, e erros não são tratados com try/catch: as exceções sobem até o
 * {@code GlobalExceptionHandler}, que as traduz para respostas HTTP padronizadas.</p>
 */
@RestController
@RequestMapping("/api/v1/assistente")
@RequiredArgsConstructor
public class AssistenteController {

    private final AssistenteFinanceiroService assistente;

    /**
     * Fluxo principal do desafio: recebe um áudio, transcreve, a IA aciona as ferramentas de orçamento
     * e devolve a resposta (opcionalmente também em áudio).
     *
     * <p>Exemplo: {@code curl -F "audio=@comando.mp3" -F "responderEmAudio=true" .../api/v1/assistente/audio}</p>
     */
    @PostMapping(value = "/audio", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ComandoResponse comandoPorAudio(
            @RequestPart("audio") MultipartFile audio,
            @RequestParam(name = "responderEmAudio", defaultValue = "false") boolean responderEmAudio) {
        try {
            return ComandoResponse.de(assistente.processarAudio(
                    audio.getBytes(), audio.getOriginalFilename(), responderEmAudio));
        } catch (IOException e) {
            // Falha ao ler o upload (conexão interrompida, disco...). Para o cliente, é um áudio inválido.
            throw new AudioInvalidoException("Não foi possível ler o arquivo de áudio enviado.");
        }
    }

    /** Mesmo fluxo, mas com o comando já em texto: útil para testar sem gravar áudio. */
    @PostMapping(value = "/texto", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ComandoResponse comandoPorTexto(@Valid @RequestBody ComandoTextoRequest requisicao) {
        return ComandoResponse.de(assistente.processarTexto(requisicao.texto(), requisicao.responderEmAudio()));
    }

    /** Text-to-Speech isolado: devolve o áudio (MP3) diretamente, pronto para tocar. */
    @PostMapping(value = "/sintese", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<byte[]> sintetizar(@Valid @RequestBody SinteseRequest requisicao) {
        AudioSintetizado audio = assistente.converterTextoEmVoz(requisicao.texto());
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(audio.tipoMidia()))
                .body(audio.conteudo());
    }
}
