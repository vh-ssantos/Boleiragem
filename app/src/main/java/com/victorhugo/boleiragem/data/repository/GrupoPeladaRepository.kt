package com.victorhugo.boleiragem.data.repository

import com.victorhugo.boleiragem.data.dao.GrupoPeladaDao
import com.victorhugo.boleiragem.data.model.GrupoPelada
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repositório para gerenciar os grupos de pelada
 */
@Singleton
class GrupoPeladaRepository @Inject constructor(
    private val grupoPeladaDao: GrupoPeladaDao
) {
    /**
     * Obtém todos os grupos de pelada ativos
     */
    fun getGruposAtivos(): Flow<List<GrupoPelada>> {
        return grupoPeladaDao.getGruposPeladaAtivos()
    }

    /**
     * Obtém todos os grupos de pelada, incluindo inativos
     */
    fun getTodosGrupos(): Flow<List<GrupoPelada>> {
        return grupoPeladaDao.getTodosGruposPelada()
    }

    /**
     * Obtém os grupos de pelada associados a um usuário específico
     */
    fun getGruposPorUsuario(usuarioId: String): Flow<List<GrupoPelada>> {
        return grupoPeladaDao.getGruposPeladaPorUsuario(usuarioId)
    }

    /**
     * Obtém um grupo de pelada pelo ID
     */
    suspend fun getGrupoPorId(id: Long): GrupoPelada? {
        return grupoPeladaDao.getGrupoPeladaPorId(id)
    }

    /**
     * Espelho local de um grupo compartilhado (identificado pelo doc do Firestore). Usado tanto
     * pra checar idempotência ao entrar num grupo por código (Fase 3/comunidade) quanto por quem
     * já é dono e teve o grupo sincronizado.
     */
    suspend fun getGrupoPorFirestoreId(firestoreId: String): GrupoPelada? {
        return grupoPeladaDao.getGrupoPeladaPorFirestoreId(firestoreId)
    }

    /**
     * Insere um novo grupo de pelada no banco de dados
     * @return ID do grupo inserido
     */
    suspend fun inserirGrupo(grupoPelada: GrupoPelada): Long {
        return grupoPeladaDao.inserirGrupoPelada(grupoPelada)
    }

    /**
     * Atualiza um grupo de pelada existente
     */
    suspend fun atualizarGrupo(grupoPelada: GrupoPelada) {
        // Atualiza o timestamp de modificação
        val grupoAtualizado = grupoPelada.copy(ultimaModificacao = System.currentTimeMillis())
        grupoPeladaDao.atualizarGrupoPelada(grupoAtualizado)
    }

    /**
     * Desativa um grupo de pelada (não remove do banco)
     */
    suspend fun desativarGrupo(id: Long) {
        grupoPeladaDao.desativarGrupoPelada(id)
    }

    /**
     * Remove completamente um grupo de pelada do banco
     */
    suspend fun deletarGrupo(grupoPelada: GrupoPelada) {
        grupoPeladaDao.deletarGrupoPelada(grupoPelada)
    }
    
    /**
     * Exclui (deleta) um grupo de pelada do banco de dados
     */
    suspend fun excluirGrupo(grupoPelada: GrupoPelada) {
        grupoPeladaDao.deletarGrupoPelada(grupoPelada)
    }

    /**
     * Verifica se existem grupos cadastrados
     * @return true se existir pelo menos um grupo
     */
    suspend fun existeGrupoCadastrado(): Boolean {
        return grupoPeladaDao.getQuantidadeGrupos() > 0
    }
}
