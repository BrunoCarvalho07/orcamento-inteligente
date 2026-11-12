package br.com.orcamentoia.api.erro;

import br.com.orcamentoia.aplicacao.excecao.AudioInvalidoException;
import br.com.orcamentoia.aplicacao.excecao.FalhaIntegracaoIaException;
import br.com.orcamentoia.dominio.excecao.CategoriaNaoEncontradaException;
import br.com.orcamentoia.dominio.excecao.RegraDeNegocioException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

import java.util.List;

/**
 * Tratamento centralizado de erros: toda falha vira uma resposta HTTP padronizada.
 *
 * <p>{@code @RestControllerAdvice} = {@code @ControllerAdvice} + {@code @ResponseBody}: um único lugar
 * intercepta as exceções de todos os controllers, então eles ficam livres de try/catch e o formato de
 * erro é igual em toda a API.</p>
 *
 * <p>O formato é o {@link ProblemDetail}, o padrão <b>RFC 9457</b> (Problem Details for HTTP APIs), já
 * suportado pelo Spring: campos {@code title}, {@code status}, {@code detail} e extensões. Clientes
 * que conhecem o padrão sabem interpretá-lo sem documentação extra.</p>
 *
 * <p><b>Regra de ouro de segurança:</b> detalhes internos (stack traces, mensagens de SQL ou do
 * provedor de IA) vão para o LOG, nunca para o cliente.</p>
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // ---- Erros de negócio ----------------------------------------------------------------

    /** 404: o recurso consultado não existe. Mais específico que o handler de RegraDeNegocioException. */
    @ExceptionHandler(CategoriaNaoEncontradaException.class)
    public ProblemDetail aoNaoEncontrarCategoria(CategoriaNaoEncontradaException e) {
        return problema(HttpStatus.NOT_FOUND, "Categoria não encontrada", e.getMessage());
    }

    /** 422: a requisição está bem formada, mas viola uma regra de negócio. */
    @ExceptionHandler(RegraDeNegocioException.class)
    public ProblemDetail aoViolarRegraDeNegocio(RegraDeNegocioException e) {
        return problema(HttpStatus.UNPROCESSABLE_ENTITY, "Regra de negócio violada", e.getMessage());
    }

    // ---- Erros de áudio e de IA ----------------------------------------------------------

    @ExceptionHandler(AudioInvalidoException.class)
    public ProblemDetail aoReceberAudioInvalido(AudioInvalidoException e) {
        return problema(HttpStatus.BAD_REQUEST, "Áudio inválido", e.getMessage());
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ProblemDetail aoExcederTamanhoDoUpload(MaxUploadSizeExceededException e) {
        return problema(HttpStatus.CONTENT_TOO_LARGE, "Arquivo grande demais",
                "O arquivo excede o tamanho máximo permitido (10 MB).");
    }

    /** 502: o problema está num serviço externo, não no cliente nem (diretamente) em nós. */
    @ExceptionHandler(FalhaIntegracaoIaException.class)
    public ProblemDetail aoFalharIntegracaoComIa(FalhaIntegracaoIaException e) {
        log.error("Falha em integração de IA: {}", e.getMessage(), e);
        return problema(HttpStatus.BAD_GATEWAY, "Serviço de IA indisponível",
                "Não foi possível concluir a operação com o serviço de IA. Tente novamente em instantes.");
    }

    // ---- Erros de requisição malformada --------------------------------------------------

    /** Falha de @Valid em corpo JSON: devolve a lista de campos com problema. */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail aoFalharValidacaoDoCorpo(MethodArgumentNotValidException e) {
        List<String> erros = e.getBindingResult().getFieldErrors().stream()
                .map(erro -> "%s: %s".formatted(erro.getField(), erro.getDefaultMessage()))
                .toList();
        return problemaDeValidacao(erros);
    }

    /** Falha de validação em parâmetros de método (Spring 6.1+). */
    @ExceptionHandler(HandlerMethodValidationException.class)
    public ProblemDetail aoFalharValidacaoDeParametros(HandlerMethodValidationException e) {
        List<String> erros = e.getAllErrors().stream().map(MessageSourceResolvable::getDefaultMessage).toList();
        return problemaDeValidacao(erros);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ProblemDetail aoReceberCorpoIlegivel(HttpMessageNotReadableException e) {
        // Não repassamos e.getMessage(): ela expõe detalhes do parser JSON e nomes de classes.
        return problema(HttpStatus.BAD_REQUEST, "Corpo da requisição inválido",
                "O corpo da requisição está ausente ou não é um JSON válido (confira tipos e formato de datas AAAA-MM-DD).");
    }

    @ExceptionHandler(MissingServletRequestPartException.class)
    public ProblemDetail aoFaltarParteDoMultipart(MissingServletRequestPartException e) {
        return problema(HttpStatus.BAD_REQUEST, "Parte obrigatória ausente",
                "Envie o campo '%s' no formulário multipart.".formatted(e.getRequestPartName()));
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ProblemDetail aoFaltarParametro(MissingServletRequestParameterException e) {
        return problema(HttpStatus.BAD_REQUEST, "Parâmetro obrigatório ausente",
                "O parâmetro '%s' é obrigatório.".formatted(e.getParameterName()));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ProblemDetail aoReceberParametroComTipoErrado(MethodArgumentTypeMismatchException e) {
        return problema(HttpStatus.BAD_REQUEST, "Parâmetro inválido",
                "O valor informado para '%s' é inválido (datas: AAAA-MM-DD; meses: AAAA-MM)."
                        .formatted(e.getName()));
    }

    // ---- Rede de segurança ---------------------------------------------------------------

    /** Qualquer erro não previsto: logamos tudo e respondemos de forma genérica. */
    @ExceptionHandler(Exception.class)
    public ProblemDetail aoOcorrerErroInesperado(Exception e) {
        log.error("Erro inesperado", e);
        return problema(HttpStatus.INTERNAL_SERVER_ERROR, "Erro interno",
                "Ocorreu um erro inesperado. Tente novamente mais tarde.");
    }

    // ---- Apoio ---------------------------------------------------------------------------

    private static ProblemDetail problema(HttpStatus status, String titulo, String detalhe) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(status, detalhe);
        problema.setTitle(titulo);
        return problema;
    }

    private static ProblemDetail problemaDeValidacao(List<String> erros) {
        ProblemDetail problema = problema(HttpStatus.BAD_REQUEST, "Dados inválidos",
                "Um ou mais campos são inválidos.");
        problema.setProperty("erros", erros);
        return problema;
    }
}
