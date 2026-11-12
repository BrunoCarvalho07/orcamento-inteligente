package br.com.orcamentoia.aplicacao.modelo;

/**
 * Áudio gerado pela síntese de voz.
 *
 * @param conteudo  bytes do áudio
 * @param tipoMidia tipo MIME (ex.: {@code audio/mpeg} para MP3)
 */
public record AudioSintetizado(byte[] conteudo, String tipoMidia) {
}
