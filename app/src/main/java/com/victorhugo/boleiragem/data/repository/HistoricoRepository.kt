package com.victorhugo.boleiragem.data.repository

import com.victorhugo.boleiragem.data.dao.HistoricoPeladaDao
import com.victorhugo.boleiragem.data.model.HistoricoPelada
import com.victorhugo.boleiragem.data.model.HistoricoTime
import com.victorhugo.boleiragem.data.model.toLocal
import com.victorhugo.boleiragem.data.model.toSnapshot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HistoricoRepository @Inject constructor(
    private val historicoPeladaDao: HistoricoPeladaDao,
    private val grupoPeladaRepository: GrupoPeladaRepository,
    private val authRepository: AuthRepository,
    private val historicoSyncRepository: HistoricoSyncRepository,
    private val jogadorRepository: JogadorRepository
) {
    fun getHistoricoPartidas(): Flow<List<HistoricoPelada>> {
        return historicoPeladaDao.getHistoricoPartidas()
    }

    fun getHistoricoPorGrupo(grupoId: Long): Flow<List<HistoricoPelada>> =
        historicoPeladaDao.getHistoricoPorGrupo(grupoId)

    // Suspend (não fire-and-forget) para permitir encadear um push de sincronização depois do write local.
    suspend fun salvarPeladaFinalizada(times: List<HistoricoTime>) {
        // Criamos um snapshot dos times para armazenar no histórico, marcando quais jogadores de
        // cada time têm conta vinculada (usuarioUid) — é isso que alimenta o "Meu Histórico" pessoal.
        val timeSnapshots = times.map { time ->
            val usuariosUids = time.jogadoresIds.mapNotNull { jogadorRepository.getJogadorPorId(it)?.usuarioUid }
            time.toSnapshot().copy(usuariosUids = usuariosUids)
        }

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

    // "Meu Histórico" (tela de Perfil): estatísticas pessoais do usuário, agregadas cross-grupo a
    // partir de todas as peladas finalizadas que este dispositivo conhece (locais + sincronizadas
    // via Fase 2). Só existe pra quem tem usuarioUid — convidado não tem como aparecer aqui.
    suspend fun getEstatisticasPessoais(usuarioUid: String): EstatisticasPessoais {
        val peladas = historicoPeladaDao.getHistoricoPartidas().first()
        var peladasJogadas = 0
        var vitorias = 0
        var derrotas = 0
        var empates = 0

        peladas.forEach { pelada ->
            val timesDoUsuario = pelada.times.filter { it.usuariosUids?.contains(usuarioUid) == true }
            if (timesDoUsuario.isNotEmpty()) {
                peladasJogadas++
                timesDoUsuario.forEach { time ->
                    vitorias += time.vitorias
                    derrotas += time.derrotas
                    empates += time.empates
                }
            }
        }

        return EstatisticasPessoais(peladasJogadas, vitorias, derrotas, empates)
    }

    // Tela "Meu Histórico" (Perfil): lista as peladas em que o usuário participou, mais recente primeiro.
    fun observarPeladasDoUsuario(usuarioUid: String): Flow<List<HistoricoPelada>> =
        getHistoricoPartidas().map { peladas ->
            peladas
                .filter { pelada -> pelada.times.any { it.usuariosUids?.contains(usuarioUid) == true } }
                .sortedByDescending { it.dataFinalizacao }
        }
}

data class EstatisticasPessoais(
    val peladasJogadas: Int = 0,
    val vitorias: Int = 0,
    val derrotas: Int = 0,
    val empates: Int = 0
)
