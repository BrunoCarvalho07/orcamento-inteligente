package br.com.orcamentoia.api.dto;

import br.com.orcamentoia.aplicacao.servico.AssistenteFinanceiroService;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Contratos de ENTRADA da API REST.
 *
 * <p>A validação com Bean Validation é a <b>primeira barreira</b>: rejeita lixo óbvio na borda, com
 * mensagens claras por campo. As regras de negócio (domínio) continuam validando tudo de novo: a
 * validação da borda dá boa experiência ao cliente; a do domínio garante a integridade, venha a
 * chamada de onde vier (REST, IA ou futuras integrações).</p>
 */
public final class RequisicoesApi {

    private RequisicoesApi() {
    }

    /** Corpo para registrar despesa ou receita. */
    public record LancamentoRequest(
            @NotNull(message = "O valor é obrigatório.")
            @DecimalMin(value = "0.01", message = "O valor deve ser de pelo menos 0,01.")
            @Digits(integer = 12, fraction = 2, message = "O valor deve ter no máximo 2 casas decimais.")
            BigDecimal valor,

            @Size(max = 200, message = "A descrição deve ter no máximo 200 caracteres.")
            String descricao,

            @NotBlank(message = "A categoria é obrigatória.")
            @Size(max = 60, message = "A categoria deve ter no máximo 60 caracteres.")
            String categoria,

            // Opcional: sem data, vale hoje. @PastOrPresent usa o relógio da JVM; a regra definitiva
            // de "não futura" (no fuso configurado) é do domínio.
            @PastOrPresent(message = "A data não pode estar no futuro.")
            LocalDate data) {
    }

    /** Corpo para definir o limite mensal de uma categoria. */
    public record LimiteRequest(
            @NotNull(message = "O limite mensal é obrigatório.")
            @DecimalMin(value = "0.01", message = "O limite deve ser de pelo menos 0,01.")
            @Digits(integer = 12, fraction = 2, message = "O limite deve ter no máximo 2 casas decimais.")
            BigDecimal limiteMensal) {
    }

    /** Comando em texto para o assistente (simula a entrada de voz já transcrita). */
    public record ComandoTextoRequest(
            @NotBlank(message = "O comando é obrigatório.")
            @Size(max = AssistenteFinanceiroService.TAMANHO_MAXIMO_COMANDO,
                    message = "O comando deve ter no máximo 500 caracteres.")
            String texto,

            /** Se {@code true}, a resposta vem também em áudio (base64). */
            boolean responderEmAudio) {
    }

    /** Texto a ser convertido em voz. */
    public record SinteseRequest(
            @NotBlank(message = "O texto é obrigatório.")
            @Size(max = AssistenteFinanceiroService.TAMANHO_MAXIMO_TEXTO_PARA_VOZ,
                    message = "O texto deve ter no máximo 1000 caracteres.")
            String texto) {
    }
}
