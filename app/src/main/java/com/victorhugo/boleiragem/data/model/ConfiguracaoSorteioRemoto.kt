package com.victorhugo.boleiragem.data.model

import com.google.firebase.firestore.DocumentId

/**
 * Documento de um perfil de configuração de sorteio na subcoleção "configuracaoSorteio" de um grupo
 * no Firestore (Fase 2). Espelha [ConfiguracaoSorteio].
 */
data class ConfiguracaoSorteioRemoto(
    @DocumentId
    val id: String = "",
    val nome: String = "Padrão",
    val qtdJogadoresPorTime: Int = 5,
    val qtdTimes: Int = 2,
    val aleatorio: Boolean = true,
    val criteriosExtras: List<String> = emptyList(),
    val isPadrao: Boolean = false,
    val atualizadoEm: Long = 0L
)

fun ConfiguracaoSorteio.toRemoto(): ConfiguracaoSorteioRemoto = ConfiguracaoSorteioRemoto(
    id = firestoreId ?: "",
    nome = nome,
    qtdJogadoresPorTime = qtdJogadoresPorTime,
    qtdTimes = qtdTimes,
    aleatorio = aleatorio,
    criteriosExtras = criteriosExtras.map { it.name },
    isPadrao = isPadrao,
    atualizadoEm = atualizadoEm
)

fun ConfiguracaoSorteioRemoto.toLocal(grupoId: Long): ConfiguracaoSorteio = ConfiguracaoSorteio(
    nome = nome,
    qtdJogadoresPorTime = qtdJogadoresPorTime,
    qtdTimes = qtdTimes,
    aleatorio = aleatorio,
    criteriosExtras = criteriosExtras.mapNotNull { runCatching { CriterioSorteio.valueOf(it) }.getOrNull() }.toSet(),
    isPadrao = isPadrao,
    grupoId = grupoId,
    firestoreId = id,
    atualizadoEm = atualizadoEm
)
