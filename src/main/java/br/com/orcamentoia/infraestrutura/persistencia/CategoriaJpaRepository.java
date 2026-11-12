package br.com.orcamentoia.infraestrutura.persistencia;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Repositório Spring Data da categoria. Package-private de propósito: é um detalhe interno da
 * infraestrutura. O resto da aplicação só enxerga a porta {@code CategoriaRepositorio}.
 */
interface CategoriaJpaRepository extends JpaRepository<CategoriaEntity, Long> {

    /** Consulta derivada do nome do método: o Spring Data gera o SQL sozinho. */
    Optional<CategoriaEntity> findByNomeNormalizado(String nomeNormalizado);
}
