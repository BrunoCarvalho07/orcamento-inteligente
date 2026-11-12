package br.com.orcamentoia.infraestrutura.ia;

import br.com.orcamentoia.aplicacao.excecao.FalhaIntegracaoIaException;
import br.com.orcamentoia.aplicacao.porta.AssistenteDeLinguagem;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.Locale;

/**
 * Adaptador que implementa {@link AssistenteDeLinguagem} com o {@link ChatClient} do Spring AI.
 *
 * <p>Ao chamar {@code .call()}, o Spring AI executa o laço de Tool Calling por nós: envia a mensagem,
 * o modelo pede uma ferramenta, o framework a executa, devolve o resultado ao modelo, e assim por
 * diante até existir uma resposta final em texto.</p>
 */
@Slf4j
@Component
class AssistenteSpringAi implements AssistenteDeLinguagem {

    private static final Locale PT_BR = Locale.forLanguageTag("pt-BR");

    private final ChatClient chatClient;
    private final Clock relogio;

    AssistenteSpringAi(@Qualifier("assistenteFinanceiroChatClient") ChatClient chatClient, Clock relogio) {
        this.chatClient = chatClient;
        this.relogio = relogio;
    }

    @Override
    public String responder(String mensagemDoUsuario) {
        try {
            String resposta = chatClient.prompt()
                    .user(comContextoDeData(mensagemDoUsuario))
                    .call()
                    .content();

            if (resposta == null || resposta.isBlank()) {
                throw new FalhaIntegracaoIaException("O modelo de linguagem devolveu uma resposta vazia.");
            }
            return resposta.trim();
        } catch (FalhaIntegracaoIaException e) {
            throw e;
        } catch (RuntimeException e) {
            // Chave inválida, limite de uso, timeout, JSON malformado... Para o chamador tudo isso é
            // "o provedor de IA falhou". O detalhe fica no log (com stack trace) e não na resposta.
            log.error("Falha ao consultar o modelo de linguagem", e);
            throw new FalhaIntegracaoIaException("Não foi possível obter resposta do assistente de IA.", e);
        }
    }

    /**
     * Modelos de linguagem não sabem que dia é hoje. Sem essa informação, "gastei 30 ontem" viraria
     * uma data inventada. Injetamos a data (e o dia da semana, para "sexta passada") na própria
     * mensagem: assim o prompt do sistema permanece 100% estático e a data sempre reflete o fuso
     * configurado em {@code app.fuso-horario}.
     */
    private String comContextoDeData(String mensagem) {
        LocalDate hoje = LocalDate.now(relogio);
        String diaDaSemana = hoje.getDayOfWeek().getDisplayName(TextStyle.FULL, PT_BR);
        return "[Contexto: hoje é %s, %s]%n%s".formatted(diaDaSemana, hoje, mensagem);
    }
}
