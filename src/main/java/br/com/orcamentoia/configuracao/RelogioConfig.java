package br.com.orcamentoia.configuracao;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.ZoneId;

/**
 * Expõe um {@link Clock} como bean.
 *
 * <p><b>Por que não chamar {@code LocalDate.now()} direto nas classes?</b> Porque "hoje" é uma
 * dependência escondida: ela torna os testes frágeis (dependem do dia em que rodam) e, aqui, é
 * decisivo para a IA: quando o usuário diz "gastei 30 reais ontem", o que é "ontem" depende do
 * fuso horário do usuário, não do fuso do servidor na nuvem. Injetando o relógio, o fuso fica
 * configurável e os testes usam {@code Clock.fixed(...)}.</p>
 */
@Configuration
public class RelogioConfig {

    @Bean
    public Clock relogio(@Value("${app.fuso-horario:America/Sao_Paulo}") String fusoHorario) {
        return Clock.system(ZoneId.of(fusoHorario));
    }
}
