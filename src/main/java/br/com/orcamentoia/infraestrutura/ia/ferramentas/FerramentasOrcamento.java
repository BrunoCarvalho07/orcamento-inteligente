package br.com.orcamentoia.infraestrutura.ia.ferramentas;

import br.com.orcamentoia.dominio.excecao.RegraDeNegocioException;
import br.com.orcamentoia.dominio.servico.OrcamentoService;
import br.com.orcamentoia.infraestrutura.ia.ferramentas.DadosDasFerramentas.CategoriaDto;
import br.com.orcamentoia.infraestrutura.ia.ferramentas.DadosDasFerramentas.ExtratoDto;
import br.com.orcamentoia.infraestrutura.ia.ferramentas.DadosDasFerramentas.RegistroFeitoDto;
import br.com.orcamentoia.infraestrutura.ia.ferramentas.DadosDasFerramentas.ResumoDoMesDto;
import br.com.orcamentoia.infraestrutura.ia.ferramentas.DadosDasFerramentas.SituacaoCategoriaDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.function.Supplier;

/**
 * Ferramentas (Tool Calling) que o modelo de linguagem pode acionar.
 *
 * <p><b>Papel desta classe: tradutor, nunca dona de regra.</b> Cada método apenas converte os
 * parâmetros que o modelo enviou (texto) em tipos do domínio, chama o {@link OrcamentoService} e
 * devolve o resultado num formato amigável à IA. Nenhuma regra financeira mora aqui. Se a IA for
 * trocada por um formulário web, as regras continuam intactas.</p>
 *
 * <p><b>Como o modelo escolhe a ferramenta?</b> Lendo o {@code description} do método e dos
 * parâmetros. Por isso eles são escritos como instruções (o que faz, quando usar, em que formato
 * enviar). Descrições vagas são a causa número um de ferramentas chamadas errado.</p>
 *
 * <p><b>Segurança.</b> O modelo é uma entrada NÃO confiável (um comando falado pode tentar
 * manipulá-lo). Por isso todos os valores passam pelas validações do domínio, e as ferramentas
 * disponíveis só leem ou criam lançamentos; não existe ferramenta para apagar dados.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FerramentasOrcamento {

    private final OrcamentoService orcamento;

    @Tool(description = """
            Registra uma DESPESA (dinheiro que saiu). Use quando o usuário disser que gastou, pagou ou comprou algo.
            Só chame depois de saber o valor e a categoria; se faltar algum, pergunte ao usuário.
            Devolve o lançamento gravado e a situação atualizada da categoria no mês, incluindo o alerta de limite.""")
    public ResultadoFerramenta<RegistroFeitoDto> registrarDespesa(
            @ToolParam(description = "Valor em reais como número decimal com ponto, maior que zero. Ex.: 45.90") BigDecimal valor,
            @ToolParam(description = "Descrição curta do gasto, deduzida do comando. Ex.: 'Almoço no restaurante'") String descricao,
            @ToolParam(description = "Nome da categoria. Reutilize uma existente (veja listarCategorias). Ex.: 'Mercado'") String categoria,
            @ToolParam(description = "Data do gasto no formato AAAA-MM-DD. Omita se for hoje.", required = false) String data) {
        return executar("registrarDespesa", () -> RegistroFeitoDto.de(
                orcamento.registrarDespesa(valor, descricao, categoria, lerData(data))));
    }

    @Tool(description = """
            Registra uma RECEITA (dinheiro que entrou). Use quando o usuário disser que recebeu salário, pagamento ou qualquer entrada.
            Só chame depois de saber o valor e a categoria; se faltar algum, pergunte ao usuário.""")
    public ResultadoFerramenta<RegistroFeitoDto> registrarReceita(
            @ToolParam(description = "Valor em reais como número decimal com ponto, maior que zero. Ex.: 3500") BigDecimal valor,
            @ToolParam(description = "Descrição curta da receita. Ex.: 'Salário de novembro'") String descricao,
            @ToolParam(description = "Nome da categoria. Ex.: 'Salário'") String categoria,
            @ToolParam(description = "Data do recebimento no formato AAAA-MM-DD. Omita se for hoje.", required = false) String data) {
        return executar("registrarReceita", () -> RegistroFeitoDto.de(
                orcamento.registrarReceita(valor, descricao, categoria, lerData(data))));
    }

    @Tool(description = """
            Consulta o extrato: lista os lançamentos de um período, do mais recente ao mais antigo, com total de receitas,
            total de despesas e saldo do período. Use para perguntas como 'o que gastei esta semana' ou 'meus lançamentos de outubro'.
            Os totais devolvidos são exatos e consideram todos os lançamentos; use-os em vez de somar manualmente.""")
    public ResultadoFerramenta<ExtratoDto> consultarExtrato(
            @ToolParam(description = "Início do período, AAAA-MM-DD. Omita para usar o primeiro dia do mês atual.", required = false) String dataInicial,
            @ToolParam(description = "Fim do período, AAAA-MM-DD. Omita para usar hoje.", required = false) String dataFinal,
            @ToolParam(description = "Filtra por uma categoria. Omita para trazer todas.", required = false) String categoria) {
        return executar("consultarExtrato", () -> ExtratoDto.de(
                orcamento.consultarExtrato(lerData(dataInicial), lerData(dataFinal), categoria)));
    }

    @Tool(description = """
            Verifica a situação de UMA categoria em um mês: total gasto, total recebido, limite mensal, quanto ainda resta
            do limite e o alerta (SEM_LIMITE, NORMAL, ATENCAO ou EXCEDIDO). Use para 'quanto ainda posso gastar com lazer?'.""")
    public ResultadoFerramenta<SituacaoCategoriaDto> consultarSaldoDaCategoria(
            @ToolParam(description = "Nome da categoria. Ex.: 'Lazer'") String categoria,
            @ToolParam(description = "Mês de referência no formato AAAA-MM. Omita para o mês atual.", required = false) String mes) {
        return executar("consultarSaldoDaCategoria", () -> SituacaoCategoriaDto.de(
                orcamento.consultarResumoDaCategoria(categoria, lerMes(mes))));
    }

    @Tool(description = """
            Visão geral de um mês: total de receitas, total de despesas e saldo geral (receitas menos despesas), somando todas
            as categorias. Use para 'qual meu saldo?' ou 'como está meu mês?'.""")
    public ResultadoFerramenta<ResumoDoMesDto> consultarResumoDoMes(
            @ToolParam(description = "Mês de referência no formato AAAA-MM. Omita para o mês atual.", required = false) String mes) {
        return executar("consultarResumoDoMes", () -> ResumoDoMesDto.de(
                orcamento.consultarResumoFinanceiro(lerMes(mes))));
    }

    @Tool(description = """
            Lista todas as categorias cadastradas com seus limites mensais. Chame antes de registrar um lançamento
            quando não tiver certeza do nome exato da categoria, para reaproveitar uma existente.""")
    public ResultadoFerramenta<List<CategoriaDto>> listarCategorias() {
        return executar("listarCategorias", () -> orcamento.listarCategorias().stream()
                .map(CategoriaDto::de).toList());
    }

    @Tool(description = """
            Define ou altera o limite mensal de gastos de uma categoria (cria a categoria se ela não existir).
            Use quando o usuário pedir algo como 'quero gastar no máximo 500 por mês com transporte'.""")
    public ResultadoFerramenta<CategoriaDto> definirLimiteMensal(
            @ToolParam(description = "Nome da categoria. Ex.: 'Transporte'") String categoria,
            @ToolParam(description = "Novo limite mensal em reais, decimal com ponto, maior que zero. Ex.: 500") BigDecimal limiteMensal) {
        return executar("definirLimiteMensal", () -> CategoriaDto.de(
                orcamento.definirLimiteMensal(categoria, limiteMensal)));
    }

    // ------------------------------------------------------------------------------------
    // Apoio
    // ------------------------------------------------------------------------------------

    /**
     * Executa a operação transformando falhas em {@link ResultadoFerramenta#falha}.
     *
     * <ul>
     *   <li>{@link RegraDeNegocioException}: erro esperado, a mensagem é segura e útil para o modelo.</li>
     *   <li>Qualquer outra exceção é um defeito nosso: registramos o detalhe no log e entregamos ao
     *       modelo apenas uma mensagem genérica, para não vazar detalhes internos na resposta falada.</li>
     * </ul>
     */
    private <T> ResultadoFerramenta<T> executar(String nomeDaFerramenta, Supplier<T> operacao) {
        try {
            log.debug("Ferramenta '{}' acionada pelo modelo", nomeDaFerramenta);
            return ResultadoFerramenta.ok(operacao.get());
        } catch (RegraDeNegocioException e) {
            log.info("Ferramenta '{}' recusada por regra de negócio: {}", nomeDaFerramenta, e.getMessage());
            return ResultadoFerramenta.falha(e.getMessage());
        } catch (RuntimeException e) {
            log.error("Erro inesperado na ferramenta '{}'", nomeDaFerramenta, e);
            return ResultadoFerramenta.falha("Ocorreu um erro interno ao executar esta operação. Tente novamente em instantes.");
        }
    }

    /** Converte texto AAAA-MM-DD em data; vazio ou nulo vira {@code null} (o domínio aplica o padrão). */
    private static LocalDate lerData(String texto) {
        if (texto == null || texto.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(texto.trim());
        } catch (DateTimeParseException e) {
            // O modelo erra formatos de vez em quando; a mensagem o orienta a corrigir e repetir.
            throw new RegraDeNegocioException("Data inválida: '%s'. Use o formato AAAA-MM-DD.".formatted(texto));
        }
    }

    /** Converte texto AAAA-MM em mês; vazio ou nulo vira {@code null}. */
    private static YearMonth lerMes(String texto) {
        if (texto == null || texto.isBlank()) {
            return null;
        }
        try {
            return YearMonth.parse(texto.trim());
        } catch (DateTimeParseException e) {
            throw new RegraDeNegocioException("Mês inválido: '%s'. Use o formato AAAA-MM.".formatted(texto));
        }
    }
}
