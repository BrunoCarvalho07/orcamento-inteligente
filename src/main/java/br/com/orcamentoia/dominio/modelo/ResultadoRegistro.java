package br.com.orcamentoia.dominio.modelo;

/**
 * Resultado de registrar um lançamento: o que foi gravado e como a categoria ficou depois disso.
 *
 * <p>Devolver a situação da categoria junto com o lançamento evita uma segunda consulta e, mais
 * importante, permite avisar o usuário no mesmo instante ("você já usou 92% do limite de Lazer").</p>
 *
 * @param transacao          lançamento persistido (já com identificador)
 * @param situacaoDaCategoria resumo do mês do lançamento, incluindo o alerta de limite
 */
public record ResultadoRegistro(Transacao transacao, ResumoCategoria situacaoDaCategoria) {
}
