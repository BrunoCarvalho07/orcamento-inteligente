package br.com.orcamentoia.api.controller;

import br.com.orcamentoia.api.dto.RequisicoesApi.LancamentoRequest;
import br.com.orcamentoia.api.dto.RequisicoesApi.LimiteRequest;
import br.com.orcamentoia.api.dto.RespostasApi.CategoriaResponse;
import br.com.orcamentoia.api.dto.RespostasApi.ExtratoResponse;
import br.com.orcamentoia.api.dto.RespostasApi.RegistroResponse;
import br.com.orcamentoia.api.dto.RespostasApi.ResumoMesResponse;
import br.com.orcamentoia.api.dto.RespostasApi.SituacaoCategoriaResponse;
import br.com.orcamentoia.dominio.servico.OrcamentoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

/**
 * API REST tradicional do orçamento, sem IA.
 *
 * <p>Existe por dois motivos: (1) permite testar e popular o sistema sem chave de API; (2) prova que as
 * regras de negócio são independentes da IA: os mesmos métodos do {@code OrcamentoService} servem
 * tanto a este controller quanto às ferramentas do modelo.</p>
 */
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class OrcamentoController {

    private final OrcamentoService orcamento;

    @PostMapping("/despesas")
    @ResponseStatus(HttpStatus.CREATED)
    public RegistroResponse registrarDespesa(@Valid @RequestBody LancamentoRequest requisicao) {
        return RegistroResponse.de(orcamento.registrarDespesa(
                requisicao.valor(), requisicao.descricao(), requisicao.categoria(), requisicao.data()));
    }

    @PostMapping("/receitas")
    @ResponseStatus(HttpStatus.CREATED)
    public RegistroResponse registrarReceita(@Valid @RequestBody LancamentoRequest requisicao) {
        return RegistroResponse.de(orcamento.registrarReceita(
                requisicao.valor(), requisicao.descricao(), requisicao.categoria(), requisicao.data()));
    }

    /** Todos os parâmetros são opcionais; sem eles, o extrato é do mês atual até hoje. */
    @GetMapping("/extrato")
    public ExtratoResponse extrato(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fim,
            @RequestParam(required = false) String categoria) {
        return ExtratoResponse.de(orcamento.consultarExtrato(inicio, fim, categoria));
    }

    @GetMapping("/resumo")
    public ResumoMesResponse resumoDoMes(
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM") YearMonth mes) {
        return ResumoMesResponse.de(orcamento.consultarResumoFinanceiro(mes));
    }

    @GetMapping("/categorias")
    public List<CategoriaResponse> categorias() {
        return orcamento.listarCategorias().stream().map(CategoriaResponse::de).toList();
    }

    @GetMapping("/categorias/{nome}/resumo")
    public SituacaoCategoriaResponse resumoDaCategoria(
            @PathVariable String nome,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM") YearMonth mes) {
        return SituacaoCategoriaResponse.de(orcamento.consultarResumoDaCategoria(nome, mes));
    }

    /** PUT porque é idempotente: repetir a chamada deixa o limite no mesmo valor. */
    @PutMapping("/categorias/{nome}/limite")
    public CategoriaResponse definirLimite(@PathVariable String nome, @Valid @RequestBody LimiteRequest requisicao) {
        return CategoriaResponse.de(orcamento.definirLimiteMensal(nome, requisicao.limiteMensal()));
    }
}
