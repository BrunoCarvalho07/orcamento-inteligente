package br.com.orcamentoia.aplicacao;

import br.com.orcamentoia.aplicacao.excecao.AudioInvalidoException;
import br.com.orcamentoia.aplicacao.excecao.FalhaIntegracaoIaException;
import br.com.orcamentoia.aplicacao.modelo.AudioSintetizado;
import br.com.orcamentoia.aplicacao.modelo.RespostaDoAssistente;
import br.com.orcamentoia.aplicacao.servico.AssistenteFinanceiroService;
import br.com.orcamentoia.dominio.excecao.RegraDeNegocioException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Testa o fluxo áudio → texto → IA → voz com dublês simples (lambdas) no lugar dos provedores reais.
 * Nenhum teste aqui faz chamada de rede nem gasta crédito de IA.
 */
class AssistenteFinanceiroServiceTest {

    private static final byte[] AUDIO = {1, 2, 3};
    private static final AudioSintetizado VOZ = new AudioSintetizado(new byte[]{9, 9}, "audio/mpeg");

    private final List<String> nomesRecebidosPeloTranscritor = new ArrayList<>();

    private AssistenteFinanceiroService criar(String transcricao, boolean vozFunciona) {
        return new AssistenteFinanceiroService(
                (conteudo, nome) -> {
                    nomesRecebidosPeloTranscritor.add(nome);
                    return transcricao;
                },
                mensagem -> "Resposta para: " + mensagem,
                texto -> {
                    if (!vozFunciona) {
                        throw new FalhaIntegracaoIaException("voz fora do ar");
                    }
                    return VOZ;
                });
    }

    @Test
    @DisplayName("Fluxo completo de áudio: transcreve, responde e gera a voz")
    void fluxoCompletoDeAudio() {
        RespostaDoAssistente resposta = criar("gastei 30 no mercado", true)
                .processarAudio(AUDIO, "meu comando.MP3", true);

        assertThat(resposta.transcricao()).isEqualTo("gastei 30 no mercado");
        assertThat(resposta.resposta()).isEqualTo("Resposta para: gastei 30 no mercado");
        assertThat(resposta.possuiAudio()).isTrue();
        // O nome enviado ao provedor é o nosso, com a extensão normalizada.
        assertThat(nomesRecebidosPeloTranscritor).containsExactly("comando.mp3");
    }

    @Test
    @DisplayName("Sem pedir áudio, não chama a síntese de voz")
    void naoGeraVozQuandoNaoPedida() {
        RespostaDoAssistente resposta = criar("qual meu saldo", false)
                .processarAudio(AUDIO, "a.wav", false);

        assertThat(resposta.possuiAudio()).isFalse();
    }

    @Test
    @DisplayName("Se a voz falhar, a resposta em texto ainda é devolvida")
    void degradaParaTextoQuandoAVozFalha() {
        RespostaDoAssistente resposta = criar("qual meu saldo", false)
                .processarTexto("qual meu saldo", true);

        assertThat(resposta.resposta()).isNotBlank();
        assertThat(resposta.possuiAudio()).isFalse();
    }

    @Test
    @DisplayName("Rejeita áudio vazio, sem extensão ou em formato não suportado")
    void rejeitaAudiosInvalidos() {
        AssistenteFinanceiroService servico = criar("x", true);

        assertThatThrownBy(() -> servico.processarAudio(new byte[0], "a.mp3", false))
                .isInstanceOf(AudioInvalidoException.class).hasMessageContaining("vazio");
        assertThatThrownBy(() -> servico.processarAudio(AUDIO, "semextensao", false))
                .isInstanceOf(AudioInvalidoException.class).hasMessageContaining("extensão");
        assertThatThrownBy(() -> servico.processarAudio(AUDIO, "virus.exe", false))
                .isInstanceOf(AudioInvalidoException.class).hasMessageContaining("não suportado");
    }

    @Test
    @DisplayName("Transcrição vazia (silêncio) vira erro claro para o usuário")
    void silencioNaoVaiParaAIA() {
        assertThatThrownBy(() -> criar("   ", true).processarAudio(AUDIO, "a.mp3", false))
                .isInstanceOf(AudioInvalidoException.class)
                .hasMessageContaining("Não consegui entender");
    }

    @Test
    @DisplayName("Comando de texto vazio ou longo demais é rejeitado")
    void validaComandoDeTexto() {
        AssistenteFinanceiroService servico = criar("x", true);

        assertThatThrownBy(() -> servico.processarTexto("  ", false)).isInstanceOf(RegraDeNegocioException.class);
        assertThatThrownBy(() -> servico.processarTexto("a".repeat(501), false))
                .isInstanceOf(RegraDeNegocioException.class).hasMessageContaining("500");
    }

    @Test
    @DisplayName("Falha do provedor na transcrição se propaga (vira HTTP 502)")
    void falhaDaTranscricaoSePropaga() {
        AssistenteFinanceiroService servico = new AssistenteFinanceiroService(
                (c, n) -> {
                    throw new FalhaIntegracaoIaException("fora do ar");
                },
                m -> "x", t -> VOZ);

        assertThatThrownBy(() -> servico.processarAudio(AUDIO, "a.mp3", false))
                .isInstanceOf(FalhaIntegracaoIaException.class);
    }
}
