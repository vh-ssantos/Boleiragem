package com.victorhugo.boleiragem.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class PosicaoJogador(val sigla: String) {
    GOLEIRO("GOL"),
    DEFESA("DEF"),
    MEIO_CAMPO("MC"),
    ALA("ALA"),
    PIVO("PIV")
}

@Entity(tableName = "jogadores")
data class Jogador(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val grupoId: Long, // Campo obrigatório para vincular jogador à pelada específica
    val nome: String,
    val posicaoPrincipal: PosicaoJogador,
    val posicaoSecundaria: PosicaoJogador?,
    val notaPosicaoPrincipal: Int, // 1 a 5
    val notaPosicaoSecundaria: Int?, // 1 a 5
    val ativo: Boolean = true,
    val disponivel: Boolean = true, // Novo campo para controlar disponibilidade para jogos
    val totalJogos: Int = 0,
    val vitorias: Int = 0,
    val derrotas: Int = 0,
    val empates: Int = 0,
    val pontuacaoTotal: Int = 0,
    // ID do documento correspondente na subcoleção "jogadores" do Firestore, quando este jogador sincroniza (grupo compartilhado). Null = só local.
    val firestoreId: String? = null,
    val atualizadoEm: Long = 0L,
    // UID da conta Firebase que este jogador representa. Null para jogadores avulsos/criados por convidado (sem conta).
    val usuarioUid: String? = null
)
