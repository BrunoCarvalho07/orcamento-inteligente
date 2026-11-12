package br.com.orcamentoia.dominio.excecao;

/**
 * Violação de uma regra de negócio (valor negativo, data futura, limite inválido...).
 *
 * <p><b>Por que uma exceção própria?</b> Para que as camadas externas consigam distinguir "o usuário
 * pediu algo inválido" (erro esperado, vira HTTP 422 ou uma explicação em linguagem natural para a IA)
 * de "o sistema quebrou" (erro inesperado, vira HTTP 500). Ela é não verificada ({@link RuntimeException})
 * porque, na prática, nenhum chamador consegue "se recuperar" dela localmente; o tratamento é
 * centralizado no {@code GlobalExceptionHandler} e nas ferramentas de IA.</p>
 *
 * <p>A mensagem é escrita para humanos (em português) e pode ser exibida ao usuário final.</p>
 */
public class RegraDeNegocioException extends RuntimeException {

    public RegraDeNegocioException(String mensagem) {
        super(mensagem);
    }
}
