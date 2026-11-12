package br.com.orcamentoia.dominio.servico;

import br.com.orcamentoia.dominio.excecao.CategoriaNaoEncontradaException;
import br.com.orcamentoia.dominio.excecao.RegraDeNegocioException;
import br.com.orcamentoia.dominio.modelo.Categoria;
import br.com.orcamentoia.dominio.modelo.Extrato;
import br.com.orcamentoia.dominio.modelo.ResultadoRegistro;
import br.com.orcamentoia.dominio.modelo.ResumoCategoria;
import br.com.orcamentoia.dominio.modelo.ResumoFinanceiro;
import br.com.orcamentoia.dominio.modelo.TipoTransacao;
import br.com.orcamentoia.dominio.modelo.Transacao;
import br.com.orcamentoia.dominio.repositorio.CategoriaRepositorio;
import br.com.orcamentoia.dominio.repositorio.TransacaoRepositorio;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

/**
 * Serviço de domínio: concentra TODAS as regras do orçamento financeiro.
 *
 * <p><b>Desacoplamento.</b> Esta classe depende apenas de portas ({@link CategoriaRepositorio},
 * {@link TransacaoRepositorio}) e de um {@link Clock}. Ela não conhece JPA, HTTP, áudio nem Spring AI.
 * A IA (ferramentas) e o REST chamam exatamente os mesmos métodos; as regras de negócio nunca
 * são duplicadas nem dependem do "canal" de entrada.</p>
 *
 * <p><b>Sobre as anotações do Spring.</b> Usamos {@code @Service} e {@code @Transactional} aqui por
 * pragmatismo: é o único ponto onde a fronteira transacional faz sentido (cada operação de negócio
 * é atômica) e evita criar uma camada extra só para abrir transações. A lógica em si continua
 * sendo Java puro, testável com repositórios em memória (veja {@code OrcamentoServiceTest}).</p>
 */
@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class OrcamentoService {

    private final CategoriaRepositorio categorias;
    private final TransacaoRepositorio transacoes;
    private final Clock relogio;

    // ------------------------------------------------------------------------------------
    // Comandos (alteram estado)
    // ------------------------------------------------------------------------------------

    /**
     * Registra uma despesa.
     *
     * @param data data do gasto; {@code null} significa "hoje"
     */
    public ResultadoRegistro registrarDespesa(BigDecimal valor, String descricao,
                                              String nomeCategoria, LocalDate data) {
        return registrar(TipoTransacao.DESPESA, valor, descricao, nomeCategoria, data);
    }

    /**
     * Registra uma receita.
     *
     * @param data data do recebimento; {@code null} significa "hoje"
     */
    public ResultadoRegistro registrarReceita(BigDecimal valor, String descricao,
                                              String nomeCategoria, LocalDate data) {
        return registrar(TipoTransacao.RECEITA, valor, descricao, nomeCategoria, data);
    }

    /**
     * Define (ou altera) o limite mensal de gastos de uma categoria, criando-a se necessário.
     */
    public Categoria definirLimiteMensal(String nomeCategoria, BigDecimal limiteMensal) {
        Categoria categoria = obterOuCriarCategoria(nomeCategoria);
        Categoria atualizada = categorias.salvar(categoria.comLimiteMensal(limiteMensal));
        log.info("Limite mensal da categoria '{}' definido em {}", atualizada.getNome(), atualizada.getLimiteMensal());
        return atualizada;
    }

    private ResultadoRegistro registrar(TipoTransacao tipo, BigDecimal valor, String descricao,
                                        String nomeCategoria, LocalDate data) {
        LocalDate hoje = hoje();
        LocalDate dataEfetiva = data != null ? data : hoje;

        // Regra: a API registra fatos que já aconteceram. Rejeitar datas futuras também protege
        // contra um erro comum da IA ao interpretar datas relativas ("ontem" => ano errado).
        if (dataEfetiva.isAfter(hoje)) {
            throw new RegraDeNegocioException(
                    "Não é possível registrar lançamentos com data futura (%s). Informe uma data até hoje (%s)."
                            .formatted(dataEfetiva, hoje));
        }

        // Se algo abaixo lançar exceção (ex.: valor inválido), a transação do banco é desfeita por
        // inteiro, inclusive a criação automática da categoria. Não ficam "categorias órfãs".
        Categoria categoria = obterOuCriarCategoria(nomeCategoria);
        Transacao salva = transacoes.salvar(Transacao.nova(tipo, valor, descricao, categoria, dataEfetiva));

        // O resumo é do mês do LANÇAMENTO, não do mês atual: registrar um gasto de outubro em
        // novembro deve avisar sobre o limite de outubro.
        ResumoCategoria situacao = calcularResumo(categoria, YearMonth.from(dataEfetiva));

        log.info("{} de {} registrada na categoria '{}' em {}", tipo, salva.getValor(), categoria.getNome(), dataEfetiva);
        return new ResultadoRegistro(salva, situacao);
    }

    /**
     * Encontra a categoria pelo nome ou cria uma nova sem limite.
     *
     * <p>Criar automaticamente é o comportamento certo para comandos de voz ("gastei 40 na farmácia"):
     * exigir cadastro prévio quebraria a fluidez. Em caso extremo de duas requisições simultâneas
     * criando a mesma categoria, a restrição de unicidade do banco impede duplicidade e uma das
     * chamadas falha; para este projeto isso é aceitável (uma nova tentativa resolve).</p>
     */
    private Categoria obterOuCriarCategoria(String nome) {
        return categorias.buscarPorNome(nome)
                .orElseGet(() -> categorias.salvar(Categoria.nova(nome, null)));
    }

    // ------------------------------------------------------------------------------------
    // Consultas (somente leitura)
    // ------------------------------------------------------------------------------------

    /**
     * Extrato: lançamentos de um período (do mais recente para o mais antigo) com seus totais.
     *
     * <p>O período efetivo, depois de aplicados os padrões, volta dentro do {@link Extrato}; assim o
     * chamador sabe exatamente o que foi consultado quando passou {@code null}.</p>
     *
     * @param inicio          início do período; {@code null} = primeiro dia do mês atual
     * @param fim             fim do período; {@code null} = hoje
     * @param nomeCategoria   filtro opcional por categoria; {@code null} ou vazio = todas
     */
    @Transactional(readOnly = true)
    public Extrato consultarExtrato(LocalDate inicio, LocalDate fim, String nomeCategoria) {
        LocalDate hoje = hoje();
        LocalDate inicioEfetivo = inicio != null ? inicio : YearMonth.from(hoje).atDay(1);
        LocalDate fimEfetivo = fim != null ? fim : hoje;

        if (inicioEfetivo.isAfter(fimEfetivo)) {
            throw new RegraDeNegocioException(
                    "A data inicial (%s) não pode ser posterior à data final (%s)."
                            .formatted(inicioEfetivo, fimEfetivo));
        }

        List<Transacao> lancamentos;
        if (nomeCategoria == null || nomeCategoria.isBlank()) {
            lancamentos = transacoes.buscarPorPeriodo(inicioEfetivo, fimEfetivo);
        } else {
            Categoria categoria = exigirCategoria(nomeCategoria);
            lancamentos = transacoes.buscarPorPeriodoECategoria(inicioEfetivo, fimEfetivo, categoria);
        }
        return new Extrato(inicioEfetivo, fimEfetivo, lancamentos);
    }

    /**
     * Situação de uma categoria em um mês (saldo, limite, restante e alerta).
     *
     * @param mes mês de referência; {@code null} = mês atual
     */
    @Transactional(readOnly = true)
    public ResumoCategoria consultarResumoDaCategoria(String nomeCategoria, YearMonth mes) {
        Categoria categoria = exigirCategoria(nomeCategoria);
        return calcularResumo(categoria, mes != null ? mes : YearMonth.from(hoje()));
    }

    /**
     * Visão geral do mês: total de receitas, despesas e saldo.
     *
     * @param mes mês de referência; {@code null} = mês atual
     */
    @Transactional(readOnly = true)
    public ResumoFinanceiro consultarResumoFinanceiro(YearMonth mes) {
        YearMonth mesEfetivo = mes != null ? mes : YearMonth.from(hoje());
        LocalDate inicio = mesEfetivo.atDay(1);
        LocalDate fim = mesEfetivo.atEndOfMonth();
        return new ResumoFinanceiro(
                mesEfetivo,
                transacoes.somarPorTipo(TipoTransacao.RECEITA, inicio, fim),
                transacoes.somarPorTipo(TipoTransacao.DESPESA, inicio, fim));
    }

    @Transactional(readOnly = true)
    public List<Categoria> listarCategorias() {
        return categorias.listarTodas();
    }

    // ------------------------------------------------------------------------------------
    // Apoio
    // ------------------------------------------------------------------------------------

    private ResumoCategoria calcularResumo(Categoria categoria, YearMonth mes) {
        LocalDate inicio = mes.atDay(1);
        LocalDate fim = mes.atEndOfMonth();
        return new ResumoCategoria(
                categoria.getNome(),
                mes,
                categoria.getLimiteMensal(),
                transacoes.somarPorTipoECategoria(TipoTransacao.DESPESA, categoria, inicio, fim),
                transacoes.somarPorTipoECategoria(TipoTransacao.RECEITA, categoria, inicio, fim));
    }

    private Categoria exigirCategoria(String nome) {
        return categorias.buscarPorNome(nome).orElseThrow(() -> new CategoriaNaoEncontradaException(
                nome, categorias.listarTodas().stream().map(Categoria::getNome).toList()));
    }

    private LocalDate hoje() {
        return LocalDate.now(relogio);
    }
}
