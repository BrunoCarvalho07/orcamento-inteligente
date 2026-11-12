package br.com.orcamentoia.infraestrutura.persistencia;

import br.com.orcamentoia.dominio.modelo.TipoTransacao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Repositório Spring Data do lançamento (package-private, detalhe interno da infraestrutura).
 *
 * <p>Usamos {@code @Query} explícita nas consultas com {@code join fetch} e agregações, pois nesses
 * casos o SQL gerado importa para o desempenho e a intenção fica documentada no próprio código.</p>
 */
interface TransacaoJpaRepository extends JpaRepository<TransacaoEntity, Long> {

    // "join fetch" traz a categoria na mesma consulta; sem ele, listar N lançamentos dispararia
    // N consultas extras (problema N+1) ao montar os objetos de domínio.
    @Query("""
            select t from TransacaoEntity t
            join fetch t.categoria
            where t.data between :inicio and :fim
            order by t.data desc, t.id desc
            """)
    List<TransacaoEntity> buscarPorPeriodo(@Param("inicio") LocalDate inicio,
                                           @Param("fim") LocalDate fim);

    @Query("""
            select t from TransacaoEntity t
            join fetch t.categoria c
            where c.id = :categoriaId
              and t.data between :inicio and :fim
            order by t.data desc, t.id desc
            """)
    List<TransacaoEntity> buscarPorPeriodoECategoria(@Param("categoriaId") Long categoriaId,
                                                     @Param("inicio") LocalDate inicio,
                                                     @Param("fim") LocalDate fim);

    // SUM sem lançamentos devolve NULL (e não zero); o adaptador converte para BigDecimal.ZERO.
    @Query("""
            select sum(t.valor) from TransacaoEntity t
            where t.tipo = :tipo
              and t.data between :inicio and :fim
            """)
    BigDecimal somarPorTipo(@Param("tipo") TipoTransacao tipo,
                            @Param("inicio") LocalDate inicio,
                            @Param("fim") LocalDate fim);

    @Query("""
            select sum(t.valor) from TransacaoEntity t
            where t.tipo = :tipo
              and t.categoria.id = :categoriaId
              and t.data between :inicio and :fim
            """)
    BigDecimal somarPorTipoECategoria(@Param("tipo") TipoTransacao tipo,
                                      @Param("categoriaId") Long categoriaId,
                                      @Param("inicio") LocalDate inicio,
                                      @Param("fim") LocalDate fim);
}
