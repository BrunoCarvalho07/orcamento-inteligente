package br.com.orcamentoia;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Ponto de entrada da aplicação.
 *
 * <p>Organização dos pacotes (de dentro para fora):</p>
 * <ul>
 *   <li><b>dominio</b>: regras de negócio puras (modelo, portas de repositório e serviço de domínio).
 *       Não conhece JPA, HTTP nem Spring AI.</li>
 *   <li><b>aplicacao</b>: casos de uso que orquestram o domínio e as portas externas (voz, IA).</li>
 *   <li><b>infraestrutura</b>: adaptadores concretos (JPA, Spring AI, OpenAI Audio).</li>
 *   <li><b>api</b>: controllers REST, DTOs e tratamento global de erros.</li>
 * </ul>
 *
 * <p>A regra de dependência é sempre apontar para dentro: {@code api} e {@code infraestrutura}
 * dependem de {@code aplicacao}/{@code dominio}, nunca o contrário. Isso permite trocar o provedor
 * de IA ou o banco de dados sem tocar nas regras financeiras.</p>
 */
@SpringBootApplication
public class OrcamentoInteligenteApplication {

    public static void main(String[] args) {
        SpringApplication.run(OrcamentoInteligenteApplication.class, args);
    }
}
