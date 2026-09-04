package com.victorhugo.boleiragem.data.model

data class Time(
    val id: Int,
    val nome: String,
    val jogadores: List<Jogador>,
    val ehTimeReserva: Boolean = false
)

data class ResultadoSorteio(
    val times: List<Time>,
    val tipoDeSorteio: String = "",
    val dataHoraSorteio: Long = System.currentTimeMillis(),
    val grupoId: Long = -1L
)
