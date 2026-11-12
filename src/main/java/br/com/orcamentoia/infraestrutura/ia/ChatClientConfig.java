package br.com.orcamentoia.infraestrutura.ia;

import br.com.orcamentoia.infraestrutura.ia.ferramentas.FerramentasOrcamento;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;

/**
 * Monta o {@link ChatClient} customizado do assistente financeiro.
 *
 * <p>O Spring AI já fornece um {@link ChatClient.Builder} auto-configurado, ligado ao modelo escolhido
 * nas propriedades. Aqui só acrescentamos o que é específico do nosso produto: o prompt do sistema e as
 * ferramentas padrão.</p>
 */
@Configuration
class ChatClientConfig {

    /**
     * O prompt do sistema fica em um arquivo de texto, não em uma String Java: é o "contrato de
     * comportamento" da IA, deve ser fácil de ler, revisar em code review e ajustar sem recompilar
     * trechos de código.
     */
    @Bean
    ChatClient assistenteFinanceiroChatClient(
            ChatClient.Builder construtor,
            FerramentasOrcamento ferramentas,
            @Value("classpath:prompts/assistente-financeiro.txt") Resource promptDoSistema) {

        return construtor
                .defaultSystem(promptDoSistema)
                // Ferramentas "padrão": ficam disponíveis em TODA chamada deste client. Aqui é
                // adequado porque não há ferramentas destrutivas (nada apaga ou altera lançamentos).
                // Se existissem, o certo seria habilitá-las só por chamada, com .tools(...).
                .defaultTools(ferramentas)
                .build();
    }
}
