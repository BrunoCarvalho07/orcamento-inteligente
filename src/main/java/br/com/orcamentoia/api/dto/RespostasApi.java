package br.com.orcamentoia.api.dto;

import br.com.orcamentoia.aplicacao.modelo.RespostaDoAssistente;
import br.com.orcamentoia.dominio.modelo.Categoria;
import br.com.orcamentoia.dominio.modelo.Extrato;
import br.com.orcamentoia.dominio.modelo.ResultadoRegistro;
import br.com.orcamentoia.dominio.modelo.ResumoCategoria;
import br.com.orcamentoia.dominio.modelo.ResumoFinanceiro;
import br.com.orcamentoia.dominio.modelo.Transacao;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Base64;
import java.util.List;

/**
 * Contratos de SAÍDA da API REST.
 *
 * <p>São records separados dos objetos de domínio e das entidades JPA: o formato público da API é um
 * contrato com os clientes e não deve mudar só porque o domínio foi refatorado. Cada record sabe se
 * montar a partir do objeto de domínio ({@code de(...)}), mantendo a conversão num lugar óbvio.</p>
 */
public final class RespostasApi {

    private RespostasApi() {
    }

    public record LancamentoResponse(Long id, String tipo, BigDecimal valor, String descricao,
                                     String categoria, LocalDate data) {

        public static LancamentoResponse de(Transacao t) {
            return new LancamentoResponse(t.getId(), t.getTipo().name(), t.getValor(), t.getDescricao(),
                    t.getCategoria().getNome(), t.getData());
        }
    }

    public record SituacaoCategoriaResponse(String categoria, String mes, BigDecimal limiteMensal,
                                            BigDecimal totalDespesas, BigDecimal totalReceitas,
                                            BigDecimal restanteDoLimite, BigDecimal percentualDoLimiteUtilizado,
                                            String alerta) {

        public static SituacaoCategoriaResponse de(ResumoCategoria r) {
            return new SituacaoCategoriaResponse(r.categoria(), r.mes().toString(), r.limiteMensal(),
                    r.totalDespesas(), r.totalReceitas(), r.restanteDoLimite(),
                    r.percentualDoLimiteUtilizado(), r.nivelAlerta().name());
        }
    }

    public record RegistroResponse(LancamentoResponse lancamento, SituacaoCategoriaResponse situacaoDaCategoria) {

        public static RegistroResponse de(ResultadoRegistro r) {
            return new RegistroResponse(LancamentoResponse.de(r.transacao()),
                    SituacaoCategoriaResponse.de(r.situacaoDaCategoria()));
        }
    }

    public record ExtratoResponse(LocalDate inicio, LocalDate fim, BigDecimal totalReceitas,
                                  BigDecimal totalDespesas, BigDecimal saldo,
                                  List<LancamentoResponse> lancamentos) {

        public static ExtratoResponse de(Extrato e) {
            return new ExtratoResponse(e.inicio(), e.fim(), e.totalReceitas(), e.totalDespesas(), e.saldo(),
                    e.lancamentos().stream().map(LancamentoResponse::de).toList());
        }
    }

    public record ResumoMesResponse(String mes, BigDecimal totalReceitas, BigDecimal totalDespesas, BigDecimal saldo) {

        public static ResumoMesResponse de(ResumoFinanceiro r) {
            return new ResumoMesResponse(r.mes().toString(), r.totalReceitas(), r.totalDespesas(), r.saldo());
        }
    }

    public record CategoriaResponse(String nome, BigDecimal limiteMensal) {

        public static CategoriaResponse de(Categoria c) {
            return new CategoriaResponse(c.getNome(), c.getLimiteMensal());
        }
    }

    /**
     * Resposta do assistente.
     *
     * @param audioBase64  resposta falada codificada em Base64 (JSON não carrega binário); {@code null}
     *                     se não foi pedida ou se a síntese falhou
     * @param tipoMidiaAudio tipo MIME do áudio (ex.: audio/mpeg); {@code null} se não há áudio
     */
    public record ComandoResponse(String transcricao, String resposta, String audioBase64, String tipoMidiaAudio) {

        public static ComandoResponse de(RespostaDoAssistente r) {
            if (!r.possuiAudio()) {
                return new ComandoResponse(r.transcricao(), r.resposta(), null, null);
            }
            return new ComandoResponse(r.transcricao(), r.resposta(),
                    Base64.getEncoder().encodeToString(r.audio().conteudo()), r.audio().tipoMidia());
        }
    }
}
