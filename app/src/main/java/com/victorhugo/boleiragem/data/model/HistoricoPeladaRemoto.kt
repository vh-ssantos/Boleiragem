package com.victorhugo.boleiragem.data.model

import com.google.firebase.firestore.DocumentId

/**
 * Documento de uma pelada finalizada na subcoleção "historico" de um grupo no Firestore (Fase 2).
 * Snapshot imutável — uma vez criado, nunca é atualizado, só criado ou (raramente) apagado.
 */
data class HistoricoPeladaRemoto(
    @DocumentId
    val id: String = "",
    val dataFinalizacao: Long = 0L,
    val times: List<HistoricoTimeSnapshotRemoto> = emptyList(),
    val atualizadoEm: Long = 0L
)

data class HistoricoTimeSnapshotRemoto(
    val nome: String = "",
    val vitorias: Int = 0,
    val derrotas: Int = 0,
    val empates: Int = 0,
    val jogadoresIds: List<Long> = emptyList(),
    val mediaEstrelas: Float = 0f,
    val mediaPontuacao: Float = 0f
)

fun HistoricoPelada.toRemoto(): HistoricoPeladaRemoto = HistoricoPeladaRemoto(
    id = firestoreId ?: "",
    dataFinalizacao = dataFinalizacao,
    times = times.map {
        HistoricoTimeSnapshotRemoto(
            nome = it.nome,
            vitorias = it.vitorias,
            derrotas = it.derrotas,
            empates = it.empates,
            jogadoresIds = it.jogadoresIds,
            mediaEstrelas = it.mediaEstrelas,
            mediaPontuacao = it.mediaPontuacao
        )
    },
    atualizadoEm = atualizadoEm
)

fun HistoricoPeladaRemoto.toLocal(grupoId: Long): HistoricoPelada = HistoricoPelada(
    dataFinalizacao = dataFinalizacao,
    times = times.map {
        HistoricoTimeSnapshot(
            id = 0L,
            nome = it.nome,
            vitorias = it.vitorias,
            derrotas = it.derrotas,
            empates = it.empates,
            jogadoresIds = it.jogadoresIds,
            mediaEstrelas = it.mediaEstrelas,
            mediaPontuacao = it.mediaPontuacao
        )
    },
    grupoId = grupoId,
    firestoreId = id,
    atualizadoEm = atualizadoEm
)
