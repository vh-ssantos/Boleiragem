package com.victorhugo.boleiragem.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.victorhugo.boleiragem.data.model.ConfiguracaoPontuacao
import kotlinx.coroutines.flow.Flow

@Dao
interface ConfiguracaoPontuacaoDao {
    // Uma configuração por grupo agora (antes era um singleton global de id fixo = 1).
    @Query("SELECT * FROM configuracao_pontuacao WHERE grupoId = :grupoId LIMIT 1")
    fun getConfiguracaoPontuacao(grupoId: Long): Flow<ConfiguracaoPontuacao?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun inserirConfiguracaoPontuacao(configuracaoPontuacao: ConfiguracaoPontuacao): Long

    // Retorna o número de linhas afetadas — usado para decidir se é preciso inserir em vez de atualizar.
    @Update
    suspend fun atualizarConfiguracaoPontuacao(configuracaoPontuacao: ConfiguracaoPontuacao): Int
}
