package com.victorhugo.boleiragem.data.dao

import androidx.room.*
import com.victorhugo.boleiragem.data.model.ConfiguracaoSorteio
import kotlinx.coroutines.flow.Flow

@Dao
interface ConfiguracaoDao {
    // Obtém a configuração padrão atual de um grupo específico
    @Query("SELECT * FROM configuracao_sorteio WHERE isPadrao = 1 AND grupoId = :grupoId LIMIT 1")
    fun getConfiguracaoPadrao(grupoId: Long): Flow<ConfiguracaoSorteio?>

    // Obtém todos os perfis de configuração de um grupo específico
    @Query("SELECT * FROM configuracao_sorteio WHERE grupoId = :grupoId ORDER BY nome ASC")
    fun getTodasConfiguracoes(grupoId: Long): Flow<List<ConfiguracaoSorteio>>

    // Obtém todos os perfis de configuração de um grupo específico (versão síncrona)
    @Query("SELECT * FROM configuracao_sorteio WHERE grupoId = :grupoId ORDER BY nome ASC")
    suspend fun getTodasConfiguracoesSync(grupoId: Long): List<ConfiguracaoSorteio>

    // Obtém uma configuração específica pelo ID
    @Query("SELECT * FROM configuracao_sorteio WHERE id = :id")
    suspend fun getConfiguracaoById(id: Long): ConfiguracaoSorteio?

    // Insere ou atualiza uma configuração
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun salvarConfiguracao(configuracao: ConfiguracaoSorteio): Long

    // Remove uma configuração pelo ID
    @Query("DELETE FROM configuracao_sorteio WHERE id = :id")
    suspend fun deletarConfiguracao(id: Long)

    // Define uma configuração como padrão e remove o status padrão das outras no mesmo grupo
    @Transaction
    suspend fun definirConfiguracaoPadrao(id: Long, grupoId: Long) {
        // Remove o status padrão de todas as configurações do grupo
        resetTodasConfiguracoesNaoPadrao(grupoId)

        // Define a configuração selecionada como padrão
        marcarConfiguracoComoPadrao(id)
    }

    @Query("UPDATE configuracao_sorteio SET isPadrao = 0 WHERE grupoId = :grupoId")
    suspend fun resetTodasConfiguracoesNaoPadrao(grupoId: Long)

    @Query("UPDATE configuracao_sorteio SET isPadrao = 1 WHERE id = :id")
    suspend fun marcarConfiguracoComoPadrao(id: Long)

    @Query("SELECT * FROM configuracao_sorteio WHERE firestoreId = :firestoreId LIMIT 1")
    suspend fun getPorFirestoreId(firestoreId: String): ConfiguracaoSorteio?
}
