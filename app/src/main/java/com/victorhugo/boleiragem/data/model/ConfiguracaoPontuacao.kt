package com.victorhugo.boleiragem.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "configuracao_pontuacao")
data class ConfiguracaoPontuacao(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0, // Antes era sempre 1 (config global única) — agora uma linha por grupo, ver `grupoId`
    val pontosPorVitoria: Int = 10,
    val pontosPorDerrota: Int = -10,
    val pontosPorEmpate: Int = -5,
    // Grupo ao qual esta configuração de pontuação pertence.
    val grupoId: Long = 0L,
    // ID do documento correspondente na subcoleção "configuracaoPontuacao" do Firestore, quando o grupo sincroniza. Null = só local.
    val firestoreId: String? = null,
    val atualizadoEm: Long = 0L
)
