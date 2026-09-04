package com.victorhugo.boleiragem.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.victorhugo.boleiragem.data.model.GrupoPelada
import kotlinx.coroutines.flow.Flow

@Dao
interface GrupoPeladaDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun inserirGrupoPelada(grupoPelada: GrupoPelada): Long

    @Update
    suspend fun atualizarGrupoPelada(grupoPelada: GrupoPelada)

    @Delete
    suspend fun deletarGrupoPelada(grupoPelada: GrupoPelada)

    @Query("SELECT * FROM grupo_pelada WHERE id = :id")
    suspend fun getGrupoPeladaPorId(id: Long): GrupoPelada?

    @Query("SELECT * FROM grupo_pelada WHERE firestoreId = :firestoreId LIMIT 1")
    suspend fun getGrupoPeladaPorFirestoreId(firestoreId: String): GrupoPelada?

    @Query("SELECT * FROM grupo_pelada WHERE ativo = 1 ORDER BY ultimaModificacao DESC")
    fun getGruposPeladaAtivos(): Flow<List<GrupoPelada>>

    @Query("SELECT * FROM grupo_pelada ORDER BY ultimaModificacao DESC")
    fun getTodosGruposPelada(): Flow<List<GrupoPelada>>

    @Query("SELECT * FROM grupo_pelada WHERE usuarioId = :usuarioId AND ativo = 1 ORDER BY ultimaModificacao DESC")
    fun getGruposPeladaPorUsuario(usuarioId: String): Flow<List<GrupoPelada>>

    @Query("UPDATE grupo_pelada SET ativo = 0 WHERE id = :id")
    suspend fun desativarGrupoPelada(id: Long)

    @Query("UPDATE grupo_pelada SET ultimaModificacao = :timestamp WHERE id = :id")
    suspend fun atualizarTimestamp(id: Long, timestamp: Long = System.currentTimeMillis())
    
    @Query("SELECT COUNT(*) FROM grupo_pelada")
    suspend fun getQuantidadeGrupos(): Int
}
