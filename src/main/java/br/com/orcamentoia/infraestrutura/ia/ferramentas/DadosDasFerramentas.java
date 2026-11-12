package br.com.orcamentoia.infraestrutura.ia.ferramentas;

import br.com.orcamentoia.dominio.modelo.Categoria;
import br.com.orcamentoia.dominio.modelo.Extrato;
import br.com.orcamentoia.dominio.modelo.ResultadoRegistro;
import br.com.orcamentoia.dominio.modelo.ResumoCategoria;
import br.com.orcamentoia.dominio.modelo.ResumoFinanceiro;
import br.com.orcamentoia.dominio.modelo.Transacao;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * DTOs que as ferramentas devolvem ao modelo (serializados em JSON pelo Spring AI).
 *
 * <p><b>Por que DTOs próprios e não os objetos de domínio?</b> O que sai daqui é lido por um modelo
 * de linguagem, então é desenhado para ele: campos com nomes autoexplicativos, datas já em texto
 * ISO e valores numéricos simples. Assim o formato enviado à IA pode mudar sem tocar no domínio
 * (e vice-versa). Os DTOs da API REST ficam em outro pacote pelo mesmo motivo: cada fronteira tem o
 * seu contrato.</p>
 */
public final class DadosDasFerramentas {

    /**
     * Máximo de linhas do extrato enviadas ao modelo. Extratos enormes estourariam o contexto e o
     * custo da chamada; os TOTAIS, porém, sempre consideram todos os lançamentos.
     */
    static final int MAXIMO_LANCAMENTOS_NO_EXTRATO = 30;

    private DadosDasFerramentas() {
    }

    public record LancamentoDto(Long id, String tipo, BigDecimal valor, String descricao,
                         String categoria, LocalDate data) {

        static LancamentoDto de(Transacao transacao) {
            return new LancamentoDto(transacao.getId(), transacao.getTipo().name(), transacao.getValor(),
                    transacao.getDescricao(), transacao.getCategoria().getNome(), transacao.getData());
        }
    }

    public record SituacaoCategoriaDto(String categoria, String mes, BigDecimal limiteMensal,
                                BigDecimal totalDespesas, BigDecimal totalReceitas,
                                BigDecimal saldoDaCategoria, BigDecimal restanteDoLimite,
                                BigDecimal percentualDoLimiteUtilizado, String alerta) {

        static SituacaoCategoriaDto de(ResumoCategoria resumo) {
            return new SituacaoCategoriaDto(resumo.categoria(), resumo.mes().toString(),
                    resumo.limiteMensal(), resumo.totalDespesas(), resumo.totalReceitas(),
                    resumo.saldoDaCategoria(), resumo.restanteDoLimite(),
                    resumo.percentualDoLimiteUtilizado(), resumo.nivelAlerta().name());
        }
    }

    public record RegistroFeitoDto(LancamentoDto lancamento, SituacaoCategoriaDto situacaoDaCategoria,
                            String alerta) {

        static RegistroFeitoDto de(ResultadoRegistro resultado) {
            SituacaoCategoriaDto situacao = SituacaoCategoriaDto.de(resultado.situacaoDaCategoria());
            return new RegistroFeitoDto(LancamentoDto.de(resultado.transacao()), situacao, situacao.alerta());
        }
    }

    public record ExtratoDto(LocalDate inicio, LocalDate fim, int totalDeLancamentos, boolean listaParcial,
                      BigDecimal totalReceitas, BigDecimal totalDespesas, BigDecimal saldo,
                      List<LancamentoDto> lancamentos) {

        static ExtratoDto de(Extrato extrato) {
            List<LancamentoDto> linhas = extrato.lancamentos().stream()
                    .limit(MAXIMO_LANCAMENTOS_NO_EXTRATO)
                    .map(LancamentoDto::de)
                    .toList();
            return new ExtratoDto(extrato.inicio(), extrato.fim(), extrato.lancamentos().size(),
                    extrato.lancamentos().size() > linhas.size(),
                    extrato.totalReceitas(), extrato.totalDespesas(), extrato.saldo(), linhas);
        }
    }

    public record ResumoDoMesDto(String mes, BigDecimal totalReceitas, BigDecimal totalDespesas, BigDecimal saldo) {

        static ResumoDoMesDto de(ResumoFinanceiro resumo) {
            return new ResumoDoMesDto(resumo.mes().toString(), resumo.totalReceitas(),
                    resumo.totalDespesas(), resumo.saldo());
        }
    }

    public record CategoriaDto(String nome, BigDecimal limiteMensal) {

        static CategoriaDto de(Categoria categoria) {
            return new CategoriaDto(categoria.getNome(), categoria.getLimiteMensal());
        }
    }
}
