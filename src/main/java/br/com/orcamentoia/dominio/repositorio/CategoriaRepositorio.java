package br.com.orcamentoia.dominio.repositorio;

import br.com.orcamentoia.dominio.modelo.Categoria;

import java.util.List;
import java.util.Optional;

/**
 * Porta de saída do domínio para persistência de categorias.
 *
 * <p>É uma interface do <em>domínio</em>, implementada pela infraestrutura (veja
 * {@code CategoriaRepositorioJpa}). Essa inversão de dependência é o coração da arquitetura
 * hexagonal: o serviço de domínio declara <i>o que precisa</i> sem saber <i>como</i> é feito.
 * Trocar JPA por outro mecanismo (ou usar um repositório em memória nos testes) não exige mexer
 * nas regras de negócio.</p>
 */
public interface CategoriaRepositorio {

    /** Busca por nome ignorando maiúsculas, acentos e espaços extras. */
    Optional<Categoria> buscarPorNome(String nome);

    /** Lista todas as categorias em ordem alfabética. */
    List<Categoria> listarTodas();

    /** Insere (id nulo) ou atualiza (id preenchido) e devolve o objeto persistido. */
    Categoria salvar(Categoria categoria);
}
