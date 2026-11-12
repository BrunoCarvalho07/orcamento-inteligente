package br.com.orcamentoia.infraestrutura.persistencia;

import br.com.orcamentoia.dominio.modelo.Categoria;
import br.com.orcamentoia.dominio.repositorio.CategoriaRepositorio;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Adaptador que implementa a porta {@link CategoriaRepositorio} usando Spring Data JPA.
 *
 * <p>Este é o "outro lado" da inversão de dependência: o domínio define a interface e a
 * infraestrutura a implementa. Não há {@code @Transactional} aqui; a transação é aberta pelo
 * serviço de domínio e este adaptador participa dela.</p>
 */
@Repository
@RequiredArgsConstructor
class CategoriaRepositorioJpa implements CategoriaRepositorio {

    private final CategoriaJpaRepository jpa;
    private final PersistenciaMapper mapper;

    @Override
    public Optional<Categoria> buscarPorNome(String nome) {
        // A normalização é a MESMA usada ao gravar (Categoria.normalizar), por isso a busca é
        // insensível a acentos e maiúsculas.
        return jpa.findByNomeNormalizado(Categoria.normalizar(nome)).map(mapper::paraDominio);
    }

    @Override
    public List<Categoria> listarTodas() {
        return jpa.findAll(Sort.by("nome")).stream().map(mapper::paraDominio).toList();
    }

    @Override
    public Categoria salvar(Categoria categoria) {
        return mapper.paraDominio(jpa.save(mapper.paraEntidade(categoria)));
    }
}
