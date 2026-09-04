package com.victorhugo.boleiragem.data.repository

import com.victorhugo.boleiragem.data.dao.HistoricoPeladaDao
import com.victorhugo.boleiragem.data.model.HistoricoPelada
import com.victorhugo.boleiragem.data.model.HistoricoTime
import com.victorhugo.boleiragem.data.model.toLocal
import com.victorhugo.boleiragem.data.model.toSnapshot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HistoricoRepository @Inject constructor(
    private val historicoPeladaDao: HistoricoPeladaDao,
    private val grupoPeladaRepository: GrupoPeladaRepository,
    private val authRepository: AuthRepository,
    private val historicoSyncRepository: HistoricoSyncRepository
) {
    fun getHistoricoPartidas(): Flow<List<HistoricoPelada>> {
        return historicoPeladaDao.getHistoricoPartidas()
    }

    fun getHistoricoPorGrupo(grupoId: Long): Flow<List<HistoricoPelada>> =
        historicoPeladaDao.getHistoricoPorGrupo(grupoId)

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

        val id = withContext(Dispatchers.IO) {
            historicoPeladaDao.inserirHistoricoPelada(novaPelada)
        }
        sincronizar(novaPelada.copy(id = id))
    }

    suspend fun deletarPelada(peladaId: Long) {
        historicoPeladaDao.deletarHistoricoPelada(peladaId)
    }

    // Push "melhor esforço" — pelada finalizada é um snapshot imutável, então isto só cria, nunca atualiza.
    private suspend fun sincronizar(pelada: HistoricoPelada) {
        try {
            if (pelada.grupoId <= 0) return
            val grupoFirestoreId = grupoPeladaRepository.getGrupoPorId(pelada.grupoId)?.firestoreId ?: return
            if (authRepository.usuarioAtual == null) return
            val firestoreId = historicoSyncRepository.enviarPelada(grupoFirestoreId, pelada)
            if (pelada.firestoreId != firestoreId) {
                historicoPeladaDao.inserirHistoricoPelada(pelada.copy(id = 0, firestoreId = firestoreId))
                historicoPeladaDao.deletarHistoricoPelada(pelada.id)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // Pull: replica peladas finalizadas de outros dispositivos para o Room local (só cria, nunca atualiza).
    fun observarSincronizacaoRemota(grupoId: Long, grupoFirestoreId: String): Flow<Unit> =
        historicoSyncRepository.observarHistorico(grupoFirestoreId).map { remotas ->
            remotas.forEach { remota ->
                if (remota.id.isBlank()) return@forEach
                if (historicoPeladaDao.getPorFirestoreId(remota.id) == null) {
                    historicoPeladaDao.inserirHistoricoPelada(remota.toLocal(grupoId))
                }
            }
        }
}
