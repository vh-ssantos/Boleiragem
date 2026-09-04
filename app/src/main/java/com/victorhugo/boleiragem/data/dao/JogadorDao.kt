package com.victorhugo.boleiragem.data.dao

import androidx.room.*
import com.victorhugo.boleiragem.data.model.Jogador
import kotlinx.coroutines.flow.Flow

@Dao
interface JogadorDao {
    @Query("SELECT * FROM jogadores ORDER BY nome ASC")
    fun getJogadores(): Flow<List<Jogador>>

    @Query("SELECT * FROM jogadores ORDER BY nome ASC")
    suspend fun getJogadoresList(): List<Jogador>

    @Query("SELECT * FROM jogadores WHERE ativo = 1 ORDER BY nome ASC")
    fun getJogadoresAtivos(): Flow<List<Jogador>>

    @Query("SELECT * FROM jogadores WHERE grupoId = :grupoId ORDER BY nome ASC")
    fun getJogadoresPorGrupo(grupoId: Long): Flow<List<Jogador>>

    @Query("SELECT * FROM jogadores WHERE grupoId = :grupoId AND ativo = 1 ORDER BY nome ASC")
    fun getJogadoresAtivosPorGrupo(grupoId: Long): Flow<List<Jogador>>

    @Query("SELECT * FROM jogadores WHERE grupoId = :grupoId AND ativo = 1 ORDER BY nome ASC")
    suspend fun getJogadoresListAtivosPorGrupo(grupoId: Long): List<Jogador>

    @Query("SELECT * FROM jogadores WHERE grupoId = :grupoId ORDER BY nome ASC")
    suspend fun getJogadoresListPorGrupo(grupoId: Long): List<Jogador>

    @Query("SELECT COUNT(*) FROM jogadores WHERE grupoId = :grupoId AND ativo = 1")
    suspend fun countJogadoresAtivosPorGrupo(grupoId: Long): Int

    @Insert
    suspend fun inserirJogador(jogador: Jogador): Long

    @Update
    suspend fun atualizarJogador(jogador: Jogador)

    @Delete
    suspend fun deletarJogador(jogador: Jogador)

    @Query("UPDATE jogadores SET ativo = :ativo WHERE id = :id")
    suspend fun atualizarStatusJogador(id: Long, ativo: Boolean)

    @Query("SELECT * FROM jogadores WHERE id = :id")
    suspend fun getJogadorPorId(id: Long): Jogador?

    @Query("UPDATE jogadores SET totalJogos = totalJogos + 1, vitorias = vitorias + 1, pontuacaoTotal = pontuacaoTotal + :pontuacao WHERE id IN (:jogadoresIds)")
    suspend fun registrarVitoria(jogadoresIds: List<Long>, pontuacao: Int)

    @Query("UPDATE jogadores SET totalJogos = totalJogos + 1, derrotas = derrotas + 1 WHERE id IN (:jogadoresIds)")
    suspend fun registrarDerrota(jogadoresIds: List<Long>)

    @Query("UPDATE jogadores SET totalJogos = totalJogos + 1, empates = empates + 1, pontuacaoTotal = pontuacaoTotal + :pontuacao WHERE id IN (:jogadoresIds)")
    suspend fun registrarEmpate(jogadoresIds: List<Long>, pontuacao: Int)

    @Query("SELECT * FROM jogadores WHERE id IN (:jogadoresIds)")
    suspend fun getJogadoresPorIds(jogadoresIds: List<Long>): List<Jogador>

    @Query("SELECT * FROM jogadores WHERE firestoreId = :firestoreId LIMIT 1")
    suspend fun getJogadorPorFirestoreId(firestoreId: String): Jogador?
}
