package br.com.orcamentoia.dominio;

import br.com.orcamentoia.dominio.modelo.Categoria;
import br.com.orcamentoia.dominio.modelo.TipoTransacao;
import br.com.orcamentoia.dominio.modelo.Transacao;
import br.com.orcamentoia.dominio.repositorio.CategoriaRepositorio;
import br.com.orcamentoia.dominio.repositorio.TransacaoRepositorio;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Implementações em memória das portas de repositório, para testar o domínio sem Spring nem banco.
 *
 * <p>Só é possível porque o domínio depende de <em>interfaces</em>. Em vez de Mockito, usamos
 * "fakes" que se comportam como o banco de verdade: os testes verificam o resultado observável
 * (o que foi gravado), e não quais métodos foram chamados, o que os torna menos frágeis.</p>
 */
final class RepositoriosEmMemoria {

    private RepositoriosEmMemoria() {
    }

    static class CategoriasEmMemoria implements CategoriaRepositorio {

        private final Map<Long, Categoria> porId = new LinkedHashMap<>();
        private final AtomicLong sequencia = new AtomicLong();

        @Override
        public Optional<Categoria> buscarPorNome(String nome) {
            String alvo = Categoria.normalizar(nome);
            return porId.values().stream().filter(c -> c.getNomeNormalizado().equals(alvo)).findFirst();
        }

        @Override
        public List<Categoria> listarTodas() {
            return porId.values().stream().sorted(Comparator.comparing(Categoria::getNome)).toList();
        }

        @Override
        public Categoria salvar(Categoria categoria) {
            Long id = categoria.getId() != null ? categoria.getId() : sequencia.incrementAndGet();
            Categoria salva = Categoria.reconstituir(id, categoria.getNome(), categoria.getLimiteMensal());
            porId.put(id, salva);
            return salva;
        }
    }

    static class TransacoesEmMemoria implements TransacaoRepositorio {

        private final List<Transacao> todas = new ArrayList<>();
        private final AtomicLong sequencia = new AtomicLong();

        @Override
        public Transacao salvar(Transacao t) {
            Transacao salva = Transacao.reconstituir(sequencia.incrementAndGet(), t.getTipo(), t.getValor(),
                    t.getDescricao(), t.getCategoria(), t.getData());
            todas.add(salva);
            return salva;
        }

        @Override
        public List<Transacao> buscarPorPeriodo(LocalDate inicio, LocalDate fim) {
            return todas.stream()
                    .filter(t -> !t.getData().isBefore(inicio) && !t.getData().isAfter(fim))
                    .sorted(Comparator.comparing(Transacao::getData).thenComparing(Transacao::getId).reversed())
                    .toList();
        }

        @Override
        public List<Transacao> buscarPorPeriodoECategoria(LocalDate inicio, LocalDate fim, Categoria categoria) {
            return buscarPorPeriodo(inicio, fim).stream()
                    .filter(t -> t.getCategoria().getId().equals(categoria.getId()))
                    .toList();
        }

        @Override
        public BigDecimal somarPorTipo(TipoTransacao tipo, LocalDate inicio, LocalDate fim) {
            return buscarPorPeriodo(inicio, fim).stream()
                    .filter(t -> t.getTipo() == tipo)
                    .map(Transacao::getValor)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        }

        @Override
        public BigDecimal somarPorTipoECategoria(TipoTransacao tipo, Categoria categoria,
                                                 LocalDate inicio, LocalDate fim) {
            return buscarPorPeriodoECategoria(inicio, fim, categoria).stream()
                    .filter(t -> t.getTipo() == tipo)
                    .map(Transacao::getValor)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        }
    }
}
