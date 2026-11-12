package br.com.orcamentoia.infraestrutura.persistencia;

import br.com.orcamentoia.dominio.modelo.Categoria;
import br.com.orcamentoia.dominio.modelo.Transacao;
import org.springframework.stereotype.Component;

/**
 * Converte entre objetos de domínio e entidades JPA.
 *
 * <p>Escrito à mão (em vez de MapStruct/ModelMapper) porque o mapeamento é pequeno e passa pelas
 * fábricas {@code reconstituir(...)} do domínio, que revalidam os dados. Ferramentas de mapeamento
 * automático costumam contornar construtores e invariantes.</p>
 */
@Component
class PersistenciaMapper {

    Categoria paraDominio(CategoriaEntity entidade) {
        return Categoria.reconstituir(entidade.getId(), entidade.getNome(), entidade.getLimiteMensal());
    }

    CategoriaEntity paraEntidade(Categoria categoria) {
        CategoriaEntity entidade = new CategoriaEntity();
        entidade.setId(categoria.getId());
        entidade.setNome(categoria.getNome());
        entidade.setNomeNormalizado(categoria.getNomeNormalizado());
        entidade.setLimiteMensal(categoria.getLimiteMensal());
        return entidade;
    }

    /** Pressupõe que a categoria da entidade já esteja carregada (join fetch ou sessão aberta). */
    Transacao paraDominio(TransacaoEntity entidade) {
        return Transacao.reconstituir(
                entidade.getId(),
                entidade.getTipo(),
                entidade.getValor(),
                entidade.getDescricao(),
                paraDominio(entidade.getCategoria()),
                entidade.getData());
    }

    /**
     * @param categoriaEntidade entidade JÁ gerenciada pelo JPA. Recebemos a entidade pronta, em vez
     *                          de convertê-la aqui, para que o Hibernate a reconheça como a mesma
     *                          instância da sessão (evita um "merge" desnecessário).
     */
    TransacaoEntity paraEntidade(Transacao transacao, CategoriaEntity categoriaEntidade) {
        TransacaoEntity entidade = new TransacaoEntity();
        entidade.setId(transacao.getId());
        entidade.setTipo(transacao.getTipo());
        entidade.setValor(transacao.getValor());
        entidade.setDescricao(transacao.getDescricao());
        entidade.setData(transacao.getData());
        entidade.setCategoria(categoriaEntidade);
        return entidade;
    }
}
