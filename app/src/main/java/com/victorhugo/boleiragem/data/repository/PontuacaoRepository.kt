package com.victorhugo.boleiragem.data.repository

import com.victorhugo.boleiragem.data.dao.ConfiguracaoPontuacaoDao
import com.victorhugo.boleiragem.data.dao.JogadorDao
import com.victorhugo.boleiragem.data.model.ConfiguracaoPontuacao
import com.victorhugo.boleiragem.data.model.HistoricoTime
import com.victorhugo.boleiragem.data.model.Jogador
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PontuacaoRepository @Inject constructor(
    private val configuracaoPontuacaoDao: ConfiguracaoPontuacaoDao,
    private val jogadorDao: JogadorDao
) {
    // Configuração de pontuação agora é por grupo (antes era um singleton global de id fixo = 1,
    // o que fazia todos os grupos compartilharem a mesma pontuação — bug de antes do multi-grupo).
    private val _grupoIdAtualFlow = MutableStateFlow(0L)
    private val mutex = Mutex()
    private val configuracoesPadraoCriadas = mutableSetOf<Long>()

    fun setGrupoId(id: Long) {
        _grupoIdAtualFlow.value = id
    }

    // Obtém a configuração de pontuação do grupo atual (definido via setGrupoId) de forma reativa
    fun getConfiguracaoPontuacao(): Flow<ConfiguracaoPontuacao> =
        _grupoIdAtualFlow.flatMapLatest { grupoId -> getConfiguracaoPontuacao(grupoId) }

    fun getConfiguracaoPontuacao(grupoId: Long): Flow<ConfiguracaoPontuacao> =
        configuracaoPontuacaoDao.getConfiguracaoPontuacao(grupoId).map {
            it ?: criarConfiguracaoPadrao(grupoId)
        }

    // Atualiza a configuração de pontuação de um grupo
    suspend fun atualizarConfiguracaoPontuacao(configuracao: ConfiguracaoPontuacao) {
        val grupoId = configuracao.grupoId
        val existente = configuracaoPontuacaoDao.getConfiguracaoPontuacao(grupoId).firstOrNull()
        val configParaSalvar = configuracao.copy(id = existente?.id ?: 0)

        val linhasAtualizadas = if (configParaSalvar.id != 0L) {
            configuracaoPontuacaoDao.atualizarConfiguracaoPontuacao(configParaSalvar)
        } else {
            0
        }

        // Se nenhuma linha foi atualizada (config ainda não existe para este grupo), insere
        if (linhasAtualizadas == 0) {
            configuracaoPontuacaoDao.inserirConfiguracaoPontuacao(configParaSalvar.copy(id = 0))
        }

        configuracoesPadraoCriadas.add(grupoId)
        recalcularPontuacaoJogadores(grupoId)
    }

    // Finaliza a partida e atualiza as estatísticas dos jogadores de um grupo
    suspend fun finalizarPartida(times: List<HistoricoTime>) {
        val grupoId = times.firstOrNull()?.grupoId ?: return
        val configuracao = getConfiguracaoPontuacao(grupoId).first()

        val todosJogadores = withContext(Dispatchers.IO) {
            jogadorDao.getJogadoresList()
        }
        val jogadoresMapa = todosJogadores.associateBy { it.id }

        times.forEach { time ->
            time.jogadoresIds.forEach { jogadorId ->
                jogadoresMapa[jogadorId]?.let { jogador ->
                    val jogadorAtualizado = jogador.copy(
                        totalJogos = jogador.totalJogos + 1,
                        vitorias = jogador.vitorias + if (time.vitorias > 0) 1 else 0,
                        derrotas = jogador.derrotas + if (time.derrotas > 0) 1 else 0,
                        empates = jogador.empates + if (time.empates > 0) 1 else 0
                    )
                    val jogadorComPontuacao = jogadorAtualizado.copy(
                        pontuacaoTotal = calcularPontuacao(jogadorAtualizado, configuracao)
                    )
                    withContext(Dispatchers.IO) {
                        jogadorDao.atualizarJogador(jogadorComPontuacao)
                    }
                }
            }
        }
    }

    // Recalcula a pontuação de todos os jogadores de um grupo com base na configuração atual dele
    private suspend fun recalcularPontuacaoJogadores(grupoId: Long) {
        val configuracao = configuracaoPontuacaoDao.getConfiguracaoPontuacao(grupoId).firstOrNull() ?: return

        val jogadores = withContext(Dispatchers.IO) {
            jogadorDao.getJogadoresListPorGrupo(grupoId)
        }

        withContext(Dispatchers.IO) {
            jogadores.forEach { jogador ->
                val novaPontuacaoTotal = calcularPontuacao(jogador, configuracao)
                if (novaPontuacaoTotal != jogador.pontuacaoTotal) {
                    jogadorDao.atualizarJogador(jogador.copy(pontuacaoTotal = novaPontuacaoTotal))
                }
            }
        }
    }

    // Atualiza as estatísticas e a pontuação de um jogador específico, usando a config do grupo informado
    suspend fun atualizarPontuacaoJogador(jogador: Jogador, grupoId: Long) {
        try {
            val configuracao = getConfiguracaoPontuacao(grupoId).first()
            val jogadorComPontuacao = jogador.copy(pontuacaoTotal = calcularPontuacao(jogador, configuracao))
            withContext(Dispatchers.IO) {
                jogadorDao.atualizarJogador(jogadorComPontuacao)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun calcularPontuacao(jogador: Jogador, configuracao: ConfiguracaoPontuacao): Int {
        return jogador.vitorias * configuracao.pontosPorVitoria +
            jogador.derrotas * configuracao.pontosPorDerrota +
            jogador.empates * configuracao.pontosPorEmpate
    }

    // Cria e salva uma configuração padrão para o grupo, se ainda não existir nenhuma
    private suspend fun criarConfiguracaoPadrao(grupoId: Long): ConfiguracaoPontuacao {
        return mutex.withLock {
            if (grupoId in configuracoesPadraoCriadas) {
                configuracaoPontuacaoDao.getConfiguracaoPontuacao(grupoId).firstOrNull()?.let {
                    return@withLock it
                }
            }

            val existente = configuracaoPontuacaoDao.getConfiguracaoPontuacao(grupoId).firstOrNull()
            if (existente != null) {
                configuracoesPadraoCriadas.add(grupoId)
                return@withLock existente
            }

            val novaConfig = ConfiguracaoPontuacao(grupoId = grupoId)
            configuracaoPontuacaoDao.inserirConfiguracaoPontuacao(novaConfig)
            configuracoesPadraoCriadas.add(grupoId)
            configuracaoPontuacaoDao.getConfiguracaoPontuacao(grupoId).first() ?: novaConfig
        }
    }
}
