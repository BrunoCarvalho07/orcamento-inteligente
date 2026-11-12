package br.com.orcamentoia.dominio.repositorio;

import br.com.orcamentoia.dominio.modelo.Categoria;
import br.com.orcamentoia.dominio.modelo.TipoTransacao;
import br.com.orcamentoia.dominio.modelo.Transacao;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Porta de saída do domínio para persistência de lançamentos.
 *
 * <p>Os totais são calculados pelo banco ({@code SUM}), não carregando todas as linhas em memória.
 * Em um orçamento com anos de histórico isso faz diferença enorme, e as assinaturas deixam claro
 * para o domínio que "somar" é uma operação da persistência.</p>
 *
 * <p>Todos os intervalos de datas são <b>inclusivos</b> nas duas pontas.</p>
 */
public interface TransacaoRepositorio {

    Transacao salvar(Transacao transacao);

    /** Lançamentos do período, do mais recente para o mais antigo. */
    List<Transacao> buscarPorPeriodo(LocalDate inicio, LocalDate fim);

    /** Igual a {@link #buscarPorPeriodo}, restrito a uma categoria. */
    List<Transacao> buscarPorPeriodoECategoria(LocalDate inicio, LocalDate fim, Categoria categoria);

    /** Soma dos valores de um tipo no período; {@link BigDecimal#ZERO} se não houver lançamentos. */
    BigDecimal somarPorTipo(TipoTransacao tipo, LocalDate inicio, LocalDate fim);

    /** Soma dos valores de um tipo e categoria no período; zero se não houver lançamentos. */
    BigDecimal somarPorTipoECategoria(TipoTransacao tipo, Categoria categoria, LocalDate inicio, LocalDate fim);
}
