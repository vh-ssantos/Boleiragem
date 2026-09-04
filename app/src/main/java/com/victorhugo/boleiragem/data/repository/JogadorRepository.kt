package com.victorhugo.boleiragem.data.repository

import com.victorhugo.boleiragem.data.dao.JogadorDao
import com.victorhugo.boleiragem.data.model.Jogador
import com.victorhugo.boleiragem.data.model.toLocal
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class JogadorRepository @Inject constructor(
    private val jogadorDao: JogadorDao,
    private val grupoPeladaRepository: GrupoPeladaRepository,
    private val authRepository: AuthRepository,
    private val jogadorSyncRepository: JogadorSyncRepository
) {
    // Métodos gerais (mantidos para compatibilidade)
    fun getJogadores(): Flow<List<Jogador>> = jogadorDao.getJogadores()

    fun getJogadoresAtivos(): Flow<List<Jogador>> = jogadorDao.getJogadoresAtivos()

    // Novos métodos específicos por pelada
    fun getJogadoresPorGrupo(grupoId: Long): Flow<List<Jogador>> =
        jogadorDao.getJogadoresPorGrupo(grupoId)

    fun getJogadoresAtivosPorGrupo(grupoId: Long): Flow<List<Jogador>> =
        jogadorDao.getJogadoresAtivosPorGrupo(grupoId)

    // Novo método adicionado
    suspend fun getJogadoresListAtivosPorGrupo(grupoId: Long): List<Jogador> =
        jogadorDao.getJogadoresListAtivosPorGrupo(grupoId)

    suspend fun getJogadoresListPorGrupo(grupoId: Long): List<Jogador> =
        jogadorDao.getJogadoresListPorGrupo(grupoId)

    suspend fun countJogadoresAtivosPorGrupo(grupoId: Long): Int =
        jogadorDao.countJogadoresAtivosPorGrupo(grupoId)

    suspend fun inserirJogador(jogador: Jogador): Long {
        val id = jogadorDao.inserirJogador(jogador)
        sincronizar(jogador.copy(id = id))
        return id
    }

    suspend fun atualizarJogador(jogador: Jogador) {
        jogadorDao.atualizarJogador(jogador)
        sincronizar(jogador)
    }

    suspend fun deletarJogador(jogador: Jogador) = jogadorDao.deletarJogador(jogador)

    suspend fun atualizarStatusJogador(id: Long, ativo: Boolean) {
        jogadorDao.atualizarStatusJogador(id, ativo)
        jogadorDao.getJogadorPorId(id)?.let { sincronizar(it) }
    }

    suspend fun getJogadorPorId(id: Long): Jogador? = jogadorDao.getJogadorPorId(id)

    // Métodos para registrar estatísticas de jogadores após as peladas
    suspend fun registrarVitoria(jogadoresIds: List<Long>, pontuacao: Int) {
        jogadorDao.registrarVitoria(jogadoresIds, pontuacao)
    }

    suspend fun registrarDerrota(jogadoresIds: List<Long>) {
        jogadorDao.registrarDerrota(jogadoresIds)
    }

    suspend fun registrarEmpate(jogadoresIds: List<Long>, pontuacao: Int) {
        jogadorDao.registrarEmpate(jogadoresIds, pontuacao)
    }

    // Push "melhor esforço" para o Firestore — só roda se o grupo já foi compartilhado (tem firestoreId)
    // e há um usuário autenticado. Falha de rede não deve travar o write local, por isso o try/catch.
    private suspend fun sincronizar(jogador: Jogador) {
        try {
            val grupoFirestoreId = grupoPeladaRepository.getGrupoPorId(jogador.grupoId)?.firestoreId ?: return
            if (authRepository.usuarioAtual == null) return
            val agora = System.currentTimeMillis()
            val jogadorComTimestamp = jogador.copy(atualizadoEm = agora)
            val firestoreId = jogadorSyncRepository.enviarJogador(grupoFirestoreId, jogadorComTimestamp)
            if (jogador.firestoreId != firestoreId || jogador.atualizadoEm != agora) {
                jogadorDao.atualizarJogador(jogadorComTimestamp.copy(firestoreId = firestoreId))
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // Pull: observa a subcoleção remota de jogadores do grupo e replica (upsert por firestoreId) no Room.
    // Resolução de conflito: last-write-wins por atualizadoEm — só sobrescreve local se o remoto for mais novo.
    fun observarSincronizacaoRemota(grupoId: Long, grupoFirestoreId: String): Flow<Unit> =
        jogadorSyncRepository.observarJogadores(grupoFirestoreId).map { remotos ->
            remotos.forEach { remoto ->
                if (remoto.id.isBlank()) return@forEach
                val local = jogadorDao.getJogadorPorFirestoreId(remoto.id)
                when {
                    local == null -> jogadorDao.inserirJogador(remoto.toLocal(grupoId))
                    remoto.atualizadoEm > local.atualizadoEm ->
                        jogadorDao.atualizarJogador(remoto.toLocal(grupoId).copy(id = local.id))
                }
            }
        }
}
