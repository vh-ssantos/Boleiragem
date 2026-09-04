package com.victorhugo.boleiragem.data.model

import com.google.firebase.firestore.DocumentId

/**
 * Documento da configuração de pontuação na subcoleção "configuracaoPontuacao" de um grupo
 * no Firestore (Fase 2). Espelha [ConfiguracaoPontuacao].
 */
data class ConfiguracaoPontuacaoRemoto(
    @DocumentId
    val id: String = "",
    val pontosPorVitoria: Int = 10,
    val pontosPorDerrota: Int = -10,
    val pontosPorEmpate: Int = -5,
    val atualizadoEm: Long = 0L
)

fun ConfiguracaoPontuacao.toRemoto(): ConfiguracaoPontuacaoRemoto = ConfiguracaoPontuacaoRemoto(
    id = firestoreId ?: "",
    pontosPorVitoria = pontosPorVitoria,
    pontosPorDerrota = pontosPorDerrota,
    pontosPorEmpate = pontosPorEmpate,
    atualizadoEm = atualizadoEm
)

fun ConfiguracaoPontuacaoRemoto.toLocal(grupoId: Long): ConfiguracaoPontuacao = ConfiguracaoPontuacao(
    pontosPorVitoria = pontosPorVitoria,
    pontosPorDerrota = pontosPorDerrota,
    pontosPorEmpate = pontosPorEmpate,
    grupoId = grupoId,
    firestoreId = id,
    atualizadoEm = atualizadoEm
)
