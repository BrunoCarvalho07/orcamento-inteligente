package br.com.orcamentoia.aplicacao.servico;

import br.com.orcamentoia.aplicacao.excecao.AudioInvalidoException;
import br.com.orcamentoia.aplicacao.excecao.FalhaIntegracaoIaException;
import br.com.orcamentoia.aplicacao.modelo.AudioSintetizado;
import br.com.orcamentoia.aplicacao.modelo.RespostaDoAssistente;
import br.com.orcamentoia.aplicacao.porta.AssistenteDeLinguagem;
import br.com.orcamentoia.aplicacao.porta.SintetizadorDeVoz;
import br.com.orcamentoia.aplicacao.porta.TranscritorDeAudio;
import br.com.orcamentoia.dominio.excecao.RegraDeNegocioException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Locale;
import java.util.Set;

/**
 * Caso de uso principal: orquestra o fluxo completo de um comando do usuário.
 *
 * <pre>
 *   áudio ──► [Speech-to-Text] ──► texto ──► [IA + Tool Calling] ──► resposta ──► [Text-to-Speech] ──► áudio
 *                                              │
 *                                              └─► OrcamentoService (regras reais + banco)
 * </pre>
 *
 * <p>Esta classe só <b>coordena</b>: não sabe qual provedor transcreve, qual modelo conversa ou como
 * o banco funciona; conhece apenas as portas. É aqui que ficam as decisões do fluxo, como "se a voz
 * falhar, ainda devolvemos o texto".</p>
 *
 * <p>Não usa {@code @Transactional}: as chamadas externas à IA são lentas (segundos) e não devem
 * segurar uma conexão de banco aberta. Cada operação de orçamento já é transacional dentro do
 * {@code OrcamentoService}.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AssistenteFinanceiroService {

    public static final int TAMANHO_MAXIMO_COMANDO = 500;
    public static final int TAMANHO_MAXIMO_TEXTO_PARA_VOZ = 1000;

    /** Formatos aceitos pela API de transcrição da OpenAI. */
    private static final Set<String> EXTENSOES_PERMITIDAS =
            Set.of("flac", "mp3", "mp4", "mpeg", "mpga", "m4a", "ogg", "wav", "webm");

    private final TranscritorDeAudio transcritor;
    private final AssistenteDeLinguagem assistente;
    private final SintetizadorDeVoz sintetizador;

    /**
     * Processa um comando falado.
     *
     * @param conteudo          bytes do arquivo de áudio
     * @param nomeArquivo       nome original do arquivo (a extensão define o formato)
     * @param responderEmAudio  se {@code true}, gera também a resposta falada
     */
    public RespostaDoAssistente processarAudio(byte[] conteudo, String nomeArquivo, boolean responderEmAudio) {
        if (conteudo == null || conteudo.length == 0) {
            throw new AudioInvalidoException("O arquivo de áudio está vazio.");
        }
        String extensao = extrairExtensao(nomeArquivo);
        if (!EXTENSOES_PERMITIDAS.contains(extensao)) {
            throw new AudioInvalidoException("Formato de áudio não suportado. Use um destes: %s."
                    .formatted(String.join(", ", EXTENSOES_PERMITIDAS.stream().sorted().toList())));
        }

        // Montamos um nome de arquivo nosso (só a extensão validada) em vez de repassar o nome
        // enviado pelo cliente: o nome original é entrada externa e não precisa chegar ao provedor.
        String transcricao = transcritor.transcrever(conteudo, "comando." + extensao);
        if (transcricao.isBlank()) {
            throw new AudioInvalidoException("Não consegui entender nenhuma fala no áudio. Tente gravar novamente.");
        }
        log.info("Comando transcrito ({} caracteres)", transcricao.length());
        return responder(transcricao, responderEmAudio);
    }

    /** Processa um comando digitado: mesmo fluxo do áudio, sem a etapa de transcrição. */
    public RespostaDoAssistente processarTexto(String texto, boolean responderEmAudio) {
        if (texto == null || texto.isBlank()) {
            throw new RegraDeNegocioException("O comando não pode ser vazio.");
        }
        String comando = texto.trim();
        if (comando.length() > TAMANHO_MAXIMO_COMANDO) {
            // Limitar o tamanho protege o custo da chamada à IA e dificulta injeção de instruções longas.
            throw new RegraDeNegocioException(
                    "O comando deve ter no máximo %d caracteres.".formatted(TAMANHO_MAXIMO_COMANDO));
        }
        return responder(comando, responderEmAudio);
    }

    /** Converte um texto qualquer em voz (usado pelo endpoint de síntese isolado). */
    public AudioSintetizado converterTextoEmVoz(String texto) {
        if (texto == null || texto.isBlank()) {
            throw new RegraDeNegocioException("O texto para síntese de voz não pode ser vazio.");
        }
        if (texto.length() > TAMANHO_MAXIMO_TEXTO_PARA_VOZ) {
            throw new RegraDeNegocioException(
                    "O texto deve ter no máximo %d caracteres.".formatted(TAMANHO_MAXIMO_TEXTO_PARA_VOZ));
        }
        return sintetizador.sintetizar(texto.trim());
    }

    private RespostaDoAssistente responder(String comando, boolean responderEmAudio) {
        String resposta = assistente.responder(comando);

        AudioSintetizado audio = null;
        if (responderEmAudio) {
            try {
                audio = sintetizador.sintetizar(resposta);
            } catch (FalhaIntegracaoIaException e) {
                // Degradação elegante: a operação financeira JÁ aconteceu e a resposta em texto é
                // válida. Falhar a requisição inteira só porque a voz caiu faria o usuário repetir o
                // comando e registrar a despesa em duplicidade. Devolvemos o texto sem áudio.
                log.warn("Síntese de voz indisponível; devolvendo apenas o texto.");
            }
        }
        return new RespostaDoAssistente(comando, resposta, audio);
    }

    private static String extrairExtensao(String nomeArquivo) {
        if (nomeArquivo == null || !nomeArquivo.contains(".")) {
            throw new AudioInvalidoException("O arquivo de áudio precisa ter extensão (ex.: comando.mp3).");
        }
        return nomeArquivo.substring(nomeArquivo.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
    }
}
