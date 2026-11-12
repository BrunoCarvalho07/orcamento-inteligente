package br.com.orcamentoia.infraestrutura.persistencia;

import br.com.orcamentoia.dominio.modelo.Categoria;
import br.com.orcamentoia.dominio.modelo.TipoTransacao;
import br.com.orcamentoia.dominio.modelo.Transacao;
import br.com.orcamentoia.dominio.repositorio.TransacaoRepositorio;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Adaptador que implementa a porta {@link TransacaoRepositorio} usando Spring Data JPA.
 */
@Repository
@RequiredArgsConstructor
class TransacaoRepositorioJpa implements TransacaoRepositorio {

    private final TransacaoJpaRepository jpa;
    private final CategoriaJpaRepository categoriaJpa;
    private final PersistenciaMapper mapper;

    @Override
    public Transacao salvar(Transacao transacao) {
        Long categoriaId = transacao.getCategoria().getId();
        if (categoriaId == null) {
            // Erro de programação (não de usuário): o serviço de domínio sempre persiste a
            // categoria antes. Falhar alto aqui evita gravar lançamentos "soltos".
            throw new IllegalStateException("A categoria precisa estar persistida antes do lançamento.");
        }
        // Dentro da mesma transação esta chamada normalmente é resolvida pelo cache da sessão
        // (a categoria acabou de ser lida/gravada), então não gera consulta extra.
        CategoriaEntity categoria = categoriaJpa.findById(categoriaId).orElseThrow(
                () -> new IllegalStateException("Categoria %d não encontrada ao salvar lançamento.".formatted(categoriaId)));

        TransacaoEntity salva = jpa.save(mapper.paraEntidade(transacao, categoria));
        return mapper.paraDominio(salva);
    }

    @Override
    public List<Transacao> buscarPorPeriodo(LocalDate inicio, LocalDate fim) {
        return jpa.buscarPorPeriodo(inicio, fim).stream().map(mapper::paraDominio).toList();
    }

    @Override
    public List<Transacao> buscarPorPeriodoECategoria(LocalDate inicio, LocalDate fim, Categoria categoria) {
        return jpa.buscarPorPeriodoECategoria(categoria.getId(), inicio, fim)
                .stream().map(mapper::paraDominio).toList();
    }

    @Override
    public BigDecimal somarPorTipo(TipoTransacao tipo, LocalDate inicio, LocalDate fim) {
        return Optional.ofNullable(jpa.somarPorTipo(tipo, inicio, fim)).orElse(BigDecimal.ZERO);
    }

    @Override
    public BigDecimal somarPorTipoECategoria(TipoTransacao tipo, Categoria categoria,
                                             LocalDate inicio, LocalDate fim) {
        // SUM de zero linhas é NULL no SQL; para o domínio isso significa simplesmente "zero".
        return Optional.ofNullable(jpa.somarPorTipoECategoria(tipo, categoria.getId(), inicio, fim))
                .orElse(BigDecimal.ZERO);
    }
}
