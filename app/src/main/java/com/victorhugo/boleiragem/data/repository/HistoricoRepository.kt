package com.victorhugo.boleiragem.data.repository

import com.victorhugo.boleiragem.data.dao.HistoricoPeladaDao
import com.victorhugo.boleiragem.data.model.HistoricoPelada
import com.victorhugo.boleiragem.data.model.HistoricoTime
import com.victorhugo.boleiragem.data.model.toSnapshot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HistoricoRepository @Inject constructor(
    private val historicoPeladaDao: HistoricoPeladaDao
) {
    fun getHistoricoPartidas(): Flow<List<HistoricoPelada>> {
        return historicoPeladaDao.getHistoricoPartidas()
    }

    // Suspend (não fire-and-forget) para permitir encadear um push de sincronização depois do write local.
    suspend fun salvarPeladaFinalizada(times: List<HistoricoTime>) {
        // Criamos um snapshot dos times para armazenar no histórico
        val timeSnapshots = times.map { it.toSnapshot() }

        // Criamos um novo registro de pelada finalizada
        val novaPelada = HistoricoPelada(
            dataFinalizacao = System.currentTimeMillis(),
            times = timeSnapshots,
            grupoId = times.firstOrNull()?.grupoId ?: -1L,
            atualizadoEm = System.currentTimeMillis()
        )

        withContext(Dispatchers.IO) {
            historicoPeladaDao.inserirHistoricoPelada(novaPelada)
        }
    }

    suspend fun deletarPelada(peladaId: Long) {
        historicoPeladaDao.deletarHistoricoPelada(peladaId)
    }
}
