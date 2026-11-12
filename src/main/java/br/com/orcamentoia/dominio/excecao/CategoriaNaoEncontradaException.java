package br.com.orcamentoia.dominio.excecao;

import java.util.List;

/**
 * Lançada ao <em>consultar</em> uma categoria que não existe.
 *
 * <p>Ao <em>registrar</em> lançamentos nós criamos a categoria automaticamente (é o comportamento
 * natural por voz), mas numa consulta criar categoria seria um efeito colateral surpreendente.</p>
 *
 * <p>A mensagem lista as categorias existentes de propósito: quando essa exceção chega à IA como
 * resultado de uma ferramenta, o modelo usa a lista para se corrigir sozinho (por exemplo, trocar
 * "comida" por "Alimentação") ou para perguntar ao usuário qual ele quis dizer.</p>
 */
public class CategoriaNaoEncontradaException extends RegraDeNegocioException {

    public CategoriaNaoEncontradaException(String nomeBuscado, List<String> categoriasExistentes) {
        super(montarMensagem(nomeBuscado, categoriasExistentes));
    }

    private static String montarMensagem(String nomeBuscado, List<String> categoriasExistentes) {
        if (categoriasExistentes.isEmpty()) {
            return "A categoria '%s' não existe e ainda não há nenhuma categoria cadastrada."
                    .formatted(nomeBuscado);
        }
        return "A categoria '%s' não existe. Categorias disponíveis: %s."
                .formatted(nomeBuscado, String.join(", ", categoriasExistentes));
    }
}
