package com.victorhugo.boleiragem.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.victorhugo.boleiragem.data.db.Converters

@Entity(tableName = "historico_pelada")
data class HistoricoPelada(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val dataFinalizacao: Long = System.currentTimeMillis(),
    @TypeConverters(Converters::class)
    val times: List<HistoricoTimeSnapshot> = emptyList(),
    // Grupo ao qual esta pelada pertence. -1 = pelada antiga, salva antes deste campo existir.
    val grupoId: Long = -1L,
    // ID do documento correspondente na subcoleção "historico" do Firestore, quando o grupo sincroniza. Null = só local.
    val firestoreId: String? = null,
    val atualizadoEm: Long = 0L
)

/**
 * Versão imutável do HistoricoTime para armazenar no histórico de peladas
 */
data class HistoricoTimeSnapshot(
    val id: Long,
    val nome: String,
    val vitorias: Int,
    val derrotas: Int,
    val empates: Int,
    val jogadoresIds: List<Long>,
    val mediaEstrelas: Float,
    val mediaPontuacao: Float
)

/**
 * Extensão para converter um HistoricoTime em um HistoricoTimeSnapshot
 */
fun HistoricoTime.toSnapshot(): HistoricoTimeSnapshot {
    return HistoricoTimeSnapshot(
        id = this.id,
        nome = this.nome,
        vitorias = this.vitorias,
        derrotas = this.derrotas,
        empates = this.empates,
        jogadoresIds = this.jogadoresIds,
        mediaEstrelas = this.mediaEstrelas,
        mediaPontuacao = this.mediaPontuacao
    )
}
