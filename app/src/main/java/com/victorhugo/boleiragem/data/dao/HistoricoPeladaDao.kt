package com.victorhugo.boleiragem.data.dao

import androidx.room.*
import com.victorhugo.boleiragem.data.model.HistoricoPelada
import kotlinx.coroutines.flow.Flow

@Dao
interface HistoricoPeladaDao {
    @Query("SELECT * FROM historico_pelada ORDER BY dataFinalizacao DESC")
    fun getHistoricoPartidas(): Flow<List<HistoricoPelada>>

    @Insert
    suspend fun inserirHistoricoPelada(historicoPelada: HistoricoPelada): Long

    @Query("DELETE FROM historico_pelada WHERE id = :peladaId")
    suspend fun deletarHistoricoPelada(peladaId: Long)

    @Query("DELETE FROM historico_pelada")
    suspend fun limparHistorico()

    @Query("SELECT * FROM historico_pelada WHERE firestoreId = :firestoreId LIMIT 1")
    suspend fun getPorFirestoreId(firestoreId: String): HistoricoPelada?

    @Query("SELECT * FROM historico_pelada WHERE grupoId = :grupoId ORDER BY dataFinalizacao DESC")
    fun getHistoricoPorGrupo(grupoId: Long): Flow<List<HistoricoPelada>>
}
