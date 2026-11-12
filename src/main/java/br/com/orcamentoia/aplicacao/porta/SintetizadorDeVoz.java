package br.com.orcamentoia.aplicacao.porta;

import br.com.orcamentoia.aplicacao.modelo.AudioSintetizado;

/**
 * Porta de Text-to-Speech: transforma texto em áudio falado.
 */
public interface SintetizadorDeVoz {

    /**
     * @param texto texto a ser falado
     * @return áudio gerado, com seu tipo de mídia
     * @throws br.com.orcamentoia.aplicacao.excecao.FalhaIntegracaoIaException se o provedor falhar
     */
    AudioSintetizado sintetizar(String texto);
}
