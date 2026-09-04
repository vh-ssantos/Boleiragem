package com.victorhugo.boleiragem.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.victorhugo.boleiragem.data.db.Converters

@Entity(tableName = "historico_time")
data class HistoricoTime(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val nome: String,
    val vitorias: Int = 0,
    val derrotas: Int = 0,
    val empates: Int = 0,
    val dataUltimoSorteio: Long = System.currentTimeMillis(),
    @TypeConverters(Converters::class)
    val jogadoresIds: List<Long> = emptyList(),
    val mediaEstrelas: Float = 0f,
    val mediaPontuacao: Float = 0f,
    val isUltimoPelada: Boolean = false,
    val ehTimeReserva: Boolean = false,
    // Grupo ao qual este time pertence. -1 = registro antigo, salvo antes deste campo existir.
    val grupoId: Long = -1L
)

data class HistoricoSorteio(
    val id: Long = 0,
    val dataSorteio: Long = System.currentTimeMillis(),
    val times: List<HistoricoTime> = emptyList(),
    val isAtivo: Boolean = true
)
