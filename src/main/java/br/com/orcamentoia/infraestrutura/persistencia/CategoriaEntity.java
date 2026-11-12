package br.com.orcamentoia.infraestrutura.persistencia;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * Entidade de persistência da categoria (tabela {@code categorias}).
 *
 * <p><b>Não é o objeto de domínio.</b> Esta classe existe só para o Hibernate: tem construtor vazio,
 * setters e mapeamentos. As regras de negócio vivem em {@code Categoria}. A conversão entre as duas
 * é feita por {@link PersistenciaMapper}.</p>
 *
 * <p>Usamos {@code @Getter/@Setter} em vez de {@code @Data} de propósito: {@code equals/hashCode}
 * gerados sobre todos os campos funcionam mal com entidades gerenciadas e proxies lazy.</p>
 *
 * <p>O construtor sem argumentos é {@code protected}: o JPA exige que ele exista, mas não queremos
 * que código de aplicação crie entidades vazias por engano (ele é acessível ao mapper porque está
 * no mesmo pacote).</p>
 */
@Entity
@Table(name = "categorias",
        uniqueConstraints = @UniqueConstraint(name = "uk_categoria_nome_normalizado", columnNames = "nome_normalizado"))
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CategoriaEntity {

    // IDENTITY delega a geração do id ao banco (auto incremento / serial), simples e portável
    // entre H2 e PostgreSQL.
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 60)
    private String nome;

    // A unicidade é garantida pelo BANCO (constraint acima), não só pelo código: é a única defesa
    // realmente confiável contra duas requisições simultâneas criando a mesma categoria.
    @Column(name = "nome_normalizado", nullable = false, length = 60)
    private String nomeNormalizado;

    // NUMERIC(15,2): dinheiro nunca deve ser armazenado em float/double (erros de arredondamento).
    @Column(name = "limite_mensal", precision = 15, scale = 2)
    private BigDecimal limiteMensal;
}
