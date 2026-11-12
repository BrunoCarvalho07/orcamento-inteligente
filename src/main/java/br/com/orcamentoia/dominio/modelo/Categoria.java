package br.com.orcamentoia.dominio.modelo;

import br.com.orcamentoia.dominio.excecao.RegraDeNegocioException;
import lombok.Getter;
import lombok.ToString;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.Normalizer;
import java.util.Locale;

/**
 * Categoria de lançamentos (Mercado, Transporte, Lazer...) com um limite mensal opcional.
 *
 * <p><b>Objeto de domínio imutável.</b> Esta classe não é a entidade JPA (veja
 * {@code CategoriaEntity}). Separar as duas permite que o domínio evolua sem ser contaminado por
 * detalhes de persistência (construtor vazio, setters, proxies do Hibernate) e que as regras
 * de validação rodem sempre, em qualquer caminho de criação do objeto.</p>
 *
 * <p>Em vez de setters, mudanças geram um novo objeto ({@link #comLimiteMensal(BigDecimal)}).
 * Objetos imutáveis são seguros para compartilhar entre threads e fáceis de raciocinar.</p>
 */
@Getter
@ToString
public final class Categoria {

    public static final int TAMANHO_MAXIMO_NOME = 60;

    /**
     * Teto para limites. Protege contra erros de transcrição de voz (por exemplo, "cem mil" ouvido
     * como "cem milhões") e contra estouro da coluna numérica do banco.
     */
    private static final BigDecimal LIMITE_MAXIMO = new BigDecimal("100000000.00");

    /** Nulo enquanto a categoria ainda não foi persistida. */
    private final Long id;

    /** Nome para exibição, exatamente como foi cadastrado (com acentos e maiúsculas). */
    private final String nome;

    /**
     * Forma canônica do nome (minúsculas, sem acentos, espaços colapsados), usada para comparar.
     *
     * <p>Por voz, "Alimentação", "alimentacao" e "ALIMENTAÇÃO" precisam ser a mesma categoria;
     * sem essa normalização o orçamento seria fragmentado em categorias duplicadas.</p>
     */
    private final String nomeNormalizado;

    /** Limite de gastos por mês. {@code null} significa "sem limite". */
    private final BigDecimal limiteMensal;

    private Categoria(Long id, String nome, BigDecimal limiteMensal) {
        this.id = id;
        this.nome = validarNome(nome);
        this.nomeNormalizado = normalizar(this.nome);
        this.limiteMensal = validarLimite(limiteMensal);
    }

    /** Cria uma categoria que ainda não foi persistida. */
    public static Categoria nova(String nome, BigDecimal limiteMensal) {
        return new Categoria(null, nome, limiteMensal);
    }

    /** Recria uma categoria já persistida (uso exclusivo dos adaptadores de persistência). */
    public static Categoria reconstituir(Long id, String nome, BigDecimal limiteMensal) {
        return new Categoria(id, nome, limiteMensal);
    }

    /** Devolve uma cópia desta categoria com outro limite mensal ({@code null} remove o limite). */
    public Categoria comLimiteMensal(BigDecimal novoLimite) {
        return new Categoria(this.id, this.nome, novoLimite);
    }

    public boolean possuiLimite() {
        return limiteMensal != null;
    }

    /**
     * Normaliza um nome para comparação: remove acentos (decompondo em NFD e descartando as marcas
     * diacríticas), converte para minúsculas e colapsa espaços.
     *
     * <p>É estático e público porque o adaptador de persistência precisa aplicar exatamente a mesma
     * regra ao buscar por nome.</p>
     */
    public static String normalizar(String nome) {
        if (nome == null || nome.isBlank()) {
            throw new RegraDeNegocioException("O nome da categoria é obrigatório.");
        }
        String semAcentos = Normalizer.normalize(nome.trim(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        // Locale.ROOT evita surpresas em máquinas configuradas com locales especiais (ex.: turco).
        return semAcentos.toLowerCase(Locale.ROOT).replaceAll("\\s+", " ");
    }

    private static String validarNome(String nome) {
        if (nome == null || nome.isBlank()) {
            throw new RegraDeNegocioException("O nome da categoria é obrigatório.");
        }
        String limpo = nome.trim().replaceAll("\\s+", " ");
        if (limpo.length() > TAMANHO_MAXIMO_NOME) {
            throw new RegraDeNegocioException(
                    "O nome da categoria deve ter no máximo %d caracteres.".formatted(TAMANHO_MAXIMO_NOME));
        }
        return limpo;
    }

    private static BigDecimal validarLimite(BigDecimal limite) {
        if (limite == null) {
            return null;
        }
        if (limite.signum() <= 0) {
            throw new RegraDeNegocioException("O limite mensal deve ser maior que zero.");
        }
        if (limite.compareTo(LIMITE_MAXIMO) > 0) {
            throw new RegraDeNegocioException("O limite mensal informado é alto demais; confira o valor.");
        }
        // Dinheiro sempre com 2 casas decimais, arredondando como o mercado financeiro (HALF_UP).
        return limite.setScale(2, RoundingMode.HALF_UP);
    }
}
