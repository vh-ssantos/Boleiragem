package com.victorhugo.boleiragem.data.dao

import androidx.room.*
import com.victorhugo.boleiragem.data.model.HistoricoTime
import kotlinx.coroutines.flow.Flow

@Dao
interface HistoricoTimeDao {
    @Query("SELECT * FROM historico_time ORDER BY nome ASC")
    fun getHistoricoTimes(): Flow<List<HistoricoTime>>

    @Insert
    suspend fun inserirHistoricoTime(historicoTime: HistoricoTime): Long

    @Update
    suspend fun atualizarHistoricoTime(historicoTime: HistoricoTime)

    @Delete
    suspend fun deletarHistoricoTime(historicoTime: HistoricoTime)

    @Query("SELECT * FROM historico_time WHERE id = :id")
    suspend fun getHistoricoTimePorId(id: Long): HistoricoTime?

    // Métodos novos para gerenciar o histórico da última pelada
    @Query("SELECT * FROM historico_time WHERE isUltimoPelada = 1")
    fun getTimesUltimaPelada(): Flow<List<HistoricoTime>>

    @Query("SELECT * FROM historico_time WHERE isUltimoPelada = 1 AND grupoId = :grupoId")
    fun getTimesUltimaPeladaPorGrupo(grupoId: Long): Flow<List<HistoricoTime>>

    @Query("UPDATE historico_time SET isUltimoPelada = 0")
    suspend fun limparUltimaPelada()

    @Query("UPDATE historico_time SET vitorias = vitorias + 1 WHERE id = :timeId")
    suspend fun registrarVitoria(timeId: Long)

    @Query("UPDATE historico_time SET derrotas = derrotas + 1 WHERE id = :timeId")
    suspend fun registrarDerrota(timeId: Long)

    @Query("UPDATE historico_time SET empates = empates + 1 WHERE id = :timeId")
    suspend fun registrarEmpate(timeId: Long)

    @Transaction
    suspend fun salvarNovoSorteio(times: List<HistoricoTime>) {
        limparUltimaPelada()
        times.forEach { time ->
            inserirHistoricoTime(time.copy(isUltimoPelada = true))
        }
    }
}
