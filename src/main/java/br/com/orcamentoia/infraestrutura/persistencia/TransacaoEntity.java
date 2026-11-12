package br.com.orcamentoia.infraestrutura.persistencia;

import br.com.orcamentoia.dominio.modelo.TipoTransacao;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Entidade de persistência do lançamento (tabela {@code transacoes}).
 *
 * <p>Os índices refletem as consultas reais da aplicação: extrato por período e totais por
 * categoria em um período. Sem eles, cada pergunta por voz faria varredura completa da tabela.</p>
 */
@Entity
@Table(name = "transacoes", indexes = {
        @Index(name = "idx_transacao_data", columnList = "data_transacao"),
        @Index(name = "idx_transacao_categoria_data", columnList = "categoria_id, data_transacao")
})
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TransacaoEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // STRING (e não ORDINAL): gravar a posição do enum quebraria os dados se alguém reordenasse
    // ou inserisse um valor no meio do enum no futuro.
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private TipoTransacao tipo;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal valor;

    @Column(nullable = false, length = 200)
    private String descricao;

    // Nome de coluna explícito para não colidir com palavras reservadas de alguns bancos.
    @Column(name = "data_transacao", nullable = false)
    private LocalDate data;

    // LAZY: só carrega a categoria quando for preciso. As consultas que precisam dela usam
    // "join fetch" (veja TransacaoJpaRepository), evitando o problema N+1.
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "categoria_id", nullable = false)
    private CategoriaEntity categoria;
}
