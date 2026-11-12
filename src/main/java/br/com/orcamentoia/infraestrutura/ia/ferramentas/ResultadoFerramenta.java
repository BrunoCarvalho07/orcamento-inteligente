package br.com.orcamentoia.infraestrutura.ia.ferramentas;

/**
 * Envelope padrão devolvido por TODAS as ferramentas ao modelo de linguagem.
 *
 * <p><b>Por que não lançar exceção?</b> Quando uma ferramenta lança exceção, o modelo recebe um erro
 * genérico e tende a improvisar uma resposta. Devolvendo {@code sucesso=false} com uma mensagem em
 * português, o modelo entende <em>por que</em> falhou ("limite inválido", "categoria inexistente") e
 * consegue explicar ao usuário ou pedir o dado que faltou. Também fica explícito para o modelo que
 * a operação NÃO aconteceu (regra 6 do prompt do sistema).</p>
 *
 * @param sucesso {@code true} se a operação foi concluída
 * @param erro    motivo da falha em linguagem simples; {@code null} em caso de sucesso
 * @param dados   resultado da operação; {@code null} em caso de falha
 */
public record ResultadoFerramenta<T>(boolean sucesso, String erro, T dados) {

    public static <T> ResultadoFerramenta<T> ok(T dados) {
        return new ResultadoFerramenta<>(true, null, dados);
    }

    public static <T> ResultadoFerramenta<T> falha(String erro) {
        return new ResultadoFerramenta<>(false, erro, null);
    }
}
