package com.victorhugo.boleiragem.data.model

import com.google.firebase.firestore.DocumentId

/**
 * Documento de um jogador na subcoleção "jogadores" de um grupo no Firestore (Fase 2).
 * Espelha [Jogador], exceto pelo `id` local (autogerado pelo Room, não faz sentido fora do dispositivo) —
 * o vínculo entre a linha local e este documento é feito por [Jogador.firestoreId].
 */
data class JogadorRemoto(
    @DocumentId
    val id: String = "",
    val nome: String = "",
    val posicaoPrincipal: String = PosicaoJogador.MEIO_CAMPO.name,
    val posicaoSecundaria: String? = null,
    val notaPosicaoPrincipal: Int = 1,
    val notaPosicaoSecundaria: Int? = null,
    val ativo: Boolean = true,
    val disponivel: Boolean = true,
    val totalJogos: Int = 0,
    val vitorias: Int = 0,
    val derrotas: Int = 0,
    val empates: Int = 0,
    val pontuacaoTotal: Int = 0,
    val usuarioUid: String? = null,
    val atualizadoEm: Long = 0L
)

fun Jogador.toRemoto(): JogadorRemoto = JogadorRemoto(
    id = firestoreId ?: "",
    nome = nome,
    posicaoPrincipal = posicaoPrincipal.name,
    posicaoSecundaria = posicaoSecundaria?.name,
    notaPosicaoPrincipal = notaPosicaoPrincipal,
    notaPosicaoSecundaria = notaPosicaoSecundaria,
    ativo = ativo,
    disponivel = disponivel,
    totalJogos = totalJogos,
    vitorias = vitorias,
    derrotas = derrotas,
    empates = empates,
    pontuacaoTotal = pontuacaoTotal,
    usuarioUid = usuarioUid,
    atualizadoEm = atualizadoEm
)

fun JogadorRemoto.toLocal(grupoId: Long): Jogador = Jogador(
    grupoId = grupoId,
    nome = nome,
    posicaoPrincipal = runCatching { PosicaoJogador.valueOf(posicaoPrincipal) }.getOrDefault(PosicaoJogador.MEIO_CAMPO),
    posicaoSecundaria = posicaoSecundaria?.let { runCatching { PosicaoJogador.valueOf(it) }.getOrNull() },
    notaPosicaoPrincipal = notaPosicaoPrincipal,
    notaPosicaoSecundaria = notaPosicaoSecundaria,
    ativo = ativo,
    disponivel = disponivel,
    totalJogos = totalJogos,
    vitorias = vitorias,
    derrotas = derrotas,
    empates = empates,
    pontuacaoTotal = pontuacaoTotal,
    firestoreId = id,
    atualizadoEm = atualizadoEm,
    usuarioUid = usuarioUid
)
