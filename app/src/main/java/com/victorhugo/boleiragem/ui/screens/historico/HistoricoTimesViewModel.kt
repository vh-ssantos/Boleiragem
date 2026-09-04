package com.victorhugo.boleiragem.ui.screens.historico

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.victorhugo.boleiragem.data.model.HistoricoTime
import com.victorhugo.boleiragem.data.model.Jogador
import com.victorhugo.boleiragem.data.repository.HistoricoRepository
import com.victorhugo.boleiragem.data.repository.JogadorRepository
import com.victorhugo.boleiragem.data.repository.PontuacaoRepository
import com.victorhugo.boleiragem.data.repository.SorteioRepository
import com.victorhugo.boleiragem.ui.screens.times.ResultadoConfronto
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

// Classe auxiliar para armazenar as alterações temporárias nas estatísticas dos jogadores
data class EstatisticasTemporarias(
    val jogos: Int = 0,
    val vitorias: Int = 0,
    val derrotas: Int = 0,
    val empates: Int = 0
)

@HiltViewModel
class HistoricoTimesViewModel @Inject constructor(
    private val sorteioRepository: SorteioRepository,
    private val pontuacaoRepository: PontuacaoRepository,
    private val jogadorRepository: JogadorRepository,
    private val historicoRepository: HistoricoRepository
) : ViewModel() {

    private val _historicoTimes = MutableStateFlow<List<HistoricoTime>>(emptyList())
    val historicoTimes: StateFlow<List<HistoricoTime>> = _historicoTimes.asStateFlow()

    private val _peladaFinalizada = MutableStateFlow(false)
    val peladaFinalizada: StateFlow<Boolean> = _peladaFinalizada.asStateFlow()

    // Mapa para armazenar os jogadores de cada time
    private val _jogadoresPorTime = MutableStateFlow<Map<Long, List<Jogador>>>(emptyMap())
    val jogadoresPorTime: StateFlow<Map<Long, List<Jogador>>> = _jogadoresPorTime.asStateFlow()

    // Flag para controlar a exibição do diálogo de transferência
    private val _mostrarDialogoTransferencia = MutableStateFlow(false)
    val mostrarDialogoTransferencia: StateFlow<Boolean> = _mostrarDialogoTransferencia.asStateFlow()

    // Estado para controlar se é uma substituição específica do time reserva
    private val _modoSubstituicaoTimeReserva = MutableStateFlow(false)
    val modoSubstituicaoTimeReserva: StateFlow<Boolean> = _modoSubstituicaoTimeReserva.asStateFlow()

    // Estado para armazenar o time pré-selecionado (quando vem do botão do time reserva)
    private val _timePreSelecionado = MutableStateFlow<HistoricoTime?>(null)
    val timePreSelecionado: StateFlow<HistoricoTime?> = _timePreSelecionado.asStateFlow()

    // Estado para controlar a visibilidade do componente de confronto
    private val _mostrarComponenteConfronto = MutableStateFlow(false)
    val mostrarComponenteConfronto = _mostrarComponenteConfronto.asStateFlow()

    // Mapa para armazenar estatísticas temporárias dos jogadores até que a pelada seja finalizada
    private val estatisticasTemporariasJogadores = mutableMapOf<Long, EstatisticasTemporarias>()

    // Estado para armazenar o ID do grupo atual
    private val _grupoId = MutableStateFlow(-1L)
    val grupoId: StateFlow<Long> = _grupoId.asStateFlow()

    init {
        carregarHistoricoTimes()
    }

    private fun carregarHistoricoTimes() {
        viewModelScope.launch {
            // Obter apenas os times da última pelada
            sorteioRepository.getTimesUltimaPelada().collect { historicoTimes ->
                _historicoTimes.value = historicoTimes
                carregarJogadoresPorTime(historicoTimes)
            }
        }
    }

    private fun carregarJogadoresPorTime(times: List<HistoricoTime>) {
        viewModelScope.launch {
            val jogadoresPorTimeMap = mutableMapOf<Long, List<Jogador>>()

            for (time in times) {
                val jogadores = time.jogadoresIds.mapNotNull { id ->
                    jogadorRepository.getJogadorPorId(id)
                }
                jogadoresPorTimeMap[time.id] = jogadores
            }

            _jogadoresPorTime.value = jogadoresPorTimeMap
        }
    }

    fun adicionarVitoria(time: HistoricoTime) {
        viewModelScope.launch {
            val timeAtualizado = time.copy(vitorias = time.vitorias + 1)
            sorteioRepository.atualizarHistoricoTime(timeAtualizado)

            // Incrementa o contador de jogos e vitórias para cada jogador do time
            val jogadoresDoTime = _jogadoresPorTime.value[time.id] ?: emptyList()
            jogadoresDoTime.forEach { jogador ->
                atualizarEstatisticasJogador(jogador, tipoResultado = "vitoria")
            }
        }
    }

    fun adicionarDerrota(time: HistoricoTime) {
        viewModelScope.launch {
            val timeAtualizado = time.copy(derrotas = time.derrotas + 1)
            sorteioRepository.atualizarHistoricoTime(timeAtualizado)

            // Incrementa o contador de jogos e derrotas para cada jogador do time
            val jogadoresDoTime = _jogadoresPorTime.value[time.id] ?: emptyList()
            jogadoresDoTime.forEach { jogador ->
                atualizarEstatisticasJogador(jogador, tipoResultado = "derrota")
            }
        }
    }

    fun adicionarEmpate(time: HistoricoTime) {
        viewModelScope.launch {
            val timeAtualizado = time.copy(empates = time.empates + 1)
            sorteioRepository.atualizarHistoricoTime(timeAtualizado)

            // Incrementa o contador de jogos e empates para cada jogador do time
            val jogadoresDoTime = _jogadoresPorTime.value[time.id] ?: emptyList()
            jogadoresDoTime.forEach { jogador ->
                atualizarEstatisticasJogador(jogador, tipoResultado = "empate")
            }
        }
    }

    // Função auxiliar para atualizar as estatísticas temporárias do jogador
    private suspend fun atualizarEstatisticasJogador(jogador: Jogador, tipoResultado: String) {
        // Obtém as estatísticas temporárias atuais do jogador ou cria uma nova entrada se não existir
        val estatisticas = estatisticasTemporariasJogadores[jogador.id] ?: EstatisticasTemporarias()

        // Atualiza as estatísticas temporárias com base no tipo de resultado
        val estatisticasAtualizadas = when (tipoResultado) {
            "vitoria" -> estatisticas.copy(
                jogos = estatisticas.jogos + 1,
                vitorias = estatisticas.vitorias + 1
            )
            "derrota" -> estatisticas.copy(
                jogos = estatisticas.jogos + 1,
                derrotas = estatisticas.derrotas + 1
            )
            "empate" -> estatisticas.copy(
                jogos = estatisticas.jogos + 1,
                empates = estatisticas.empates + 1
            )
            else -> estatisticas
        }

        // Armazena as estatísticas atualizadas no mapa temporário
        estatisticasTemporariasJogadores[jogador.id] = estatisticasAtualizadas
    }

    fun diminuirVitoria(time: HistoricoTime) {
        if (time.vitorias > 0) {
            viewModelScope.launch {
                val timeAtualizado = time.copy(vitorias = time.vitorias - 1)
                sorteioRepository.atualizarHistoricoTime(timeAtualizado)

                // Diminui o contador de jogos e vitórias para cada jogador do time
                val jogadoresDoTime = _jogadoresPorTime.value[time.id] ?: emptyList()
                jogadoresDoTime.forEach { jogador ->
                    diminuirEstatisticasJogador(jogador, tipoResultado = "vitoria")
                }
            }
        }
    }

    fun diminuirDerrota(time: HistoricoTime) {
        if (time.derrotas > 0) {
            viewModelScope.launch {
                val timeAtualizado = time.copy(derrotas = time.derrotas - 1)
                sorteioRepository.atualizarHistoricoTime(timeAtualizado)

                // Diminui o contador de jogos e derrotas para cada jogador do time
                val jogadoresDoTime = _jogadoresPorTime.value[time.id] ?: emptyList()
                jogadoresDoTime.forEach { jogador ->
                    diminuirEstatisticasJogador(jogador, tipoResultado = "derrota")
                }
            }
        }
    }

    fun diminuirEmpate(time: HistoricoTime) {
        if (time.empates > 0) {
            viewModelScope.launch {
                val timeAtualizado = time.copy(empates = time.empates - 1)
                sorteioRepository.atualizarHistoricoTime(timeAtualizado)

                // Diminui o contador de jogos e empates para cada jogador do time
                val jogadoresDoTime = _jogadoresPorTime.value[time.id] ?: emptyList()
                jogadoresDoTime.forEach { jogador ->
                    diminuirEstatisticasJogador(jogador, tipoResultado = "empate")
                }
            }
        }
    }

    // Função auxiliar para diminuir as estatísticas temporárias do jogador
    private suspend fun diminuirEstatisticasJogador(jogador: Jogador, tipoResultado: String) {
        // Obtém as estatísticas temporárias atuais do jogador
        val estatisticas = estatisticasTemporariasJogadores[jogador.id] ?: return

        // Verifica se há estatísticas para diminuir
        if ((tipoResultado == "vitoria" && estatisticas.vitorias <= 0) ||
            (tipoResultado == "derrota" && estatisticas.derrotas <= 0) ||
            (tipoResultado == "empate" && estatisticas.empates <= 0) ||
            estatisticas.jogos <= 0) {
            return
        }

        // Atualiza as estatísticas temporárias com base no tipo de resultado
        val estatisticasAtualizadas = when (tipoResultado) {
            "vitoria" -> estatisticas.copy(
                jogos = estatisticas.jogos - 1,
                vitorias = estatisticas.vitorias - 1
            )
            "derrota" -> estatisticas.copy(
                jogos = estatisticas.jogos - 1,
                derrotas = estatisticas.derrotas - 1
            )
            "empate" -> estatisticas.copy(
                jogos = estatisticas.jogos - 1,
                empates = estatisticas.empates - 1
            )
            else -> estatisticas
        }

        // Armazena as estatísticas atualizadas no mapa temporário ou remove se todas as estatísticas são zero
        if (estatisticasAtualizadas.jogos <= 0 &&
            estatisticasAtualizadas.vitorias <= 0 &&
            estatisticasAtualizadas.derrotas <= 0 &&
            estatisticasAtualizadas.empates <= 0) {
            estatisticasTemporariasJogadores.remove(jogador.id)
        } else {
            estatisticasTemporariasJogadores[jogador.id] = estatisticasAtualizadas
        }
    }

    fun finalizarPelada() {
        viewModelScope.launch {
            try {
                // Aplicar as estatísticas temporárias aos jogadores
                aplicarEstatisticasTemporarias()

                // Obter a data e hora atual
                val dataHoraAtual = System.currentTimeMillis()

                // Atualizar a data e hora de todos os times antes de salvar
                val timesAtualizados = _historicoTimes.value.map { time ->
                    time.copy(dataUltimoSorteio = dataHoraAtual)
                }

                // Atualizar os times no repositório
                timesAtualizados.forEach { time ->
                    sorteioRepository.atualizarHistoricoTime(time)
                }

                // Atualizar o valor local do estado
                _historicoTimes.value = timesAtualizados

                // Salvar histórico da pelada finalizada
                historicoRepository.salvarPeladaFinalizada(timesAtualizados)

                // Resetar o estado de sorteio não contabilizado
                sorteioRepository.resetSorteioContabilizacao()

                // Limpar as estatísticas temporárias após salvar
                estatisticasTemporariasJogadores.clear()

                // Indica que a pelada foi finalizada com sucesso
                _peladaFinalizada.value = true

                // Após 3 segundos, resetar a mensagem de sucesso e limpar a tela
                kotlinx.coroutines.delay(3000)
                _peladaFinalizada.value = false

                // Limpar os times atuais e jogadores para permitir um novo sorteio
                _historicoTimes.value = emptyList()
                _jogadoresPorTime.value = emptyMap()

                // Ocultar o componente de confronto, se estiver visível
                _mostrarComponenteConfronto.value = false
            } catch (e: Exception) {
                // Log do erro
                e.printStackTrace()
            }
        }
    }

    // Método para aplicar as estatísticas temporárias aos jogadores no banco de dados
    private suspend fun aplicarEstatisticasTemporarias() {
        // Para cada jogador com estatísticas temporárias
        for ((jogadorId, estatisticas) in estatisticasTemporariasJogadores) {
            // Obter o jogador do banco de dados
            val jogador = jogadorRepository.getJogadorPorId(jogadorId) ?: continue

            // Atualizar as estatísticas do jogador com as temporárias acumuladas
            val jogadorAtualizado = jogador.copy(
                totalJogos = jogador.totalJogos + estatisticas.jogos,
                vitorias = jogador.vitorias + estatisticas.vitorias,
                derrotas = jogador.derrotas + estatisticas.derrotas,
                empates = jogador.empates + estatisticas.empates
            )

            // Atualizar a pontuação e salvar o jogador no banco de dados
            pontuacaoRepository.atualizarPontuacaoJogador(jogadorAtualizado, _grupoId.value)
        }
    }

    fun cancelarPeladaAtual() {
        viewModelScope.launch {
            sorteioRepository.limparHistoricoTimes()
        }
    }

    // Mostrar o diálogo de transferência de jogadores (modo normal)
    fun mostrarDialogoTransferencia() {
        _modoSubstituicaoTimeReserva.value = false
        _mostrarDialogoTransferencia.value = true
    }

    // Método específico para abrir transferência com time reserva
    fun mostrarDialogoTransferenciaTimeReserva() {
        _modoSubstituicaoTimeReserva.value = true
        _mostrarDialogoTransferencia.value = true
    }

    // Fechar o diálogo de transferência de jogadores
    fun fecharDialogoTransferencia() {
        _mostrarDialogoTransferencia.value = false
        _modoSubstituicaoTimeReserva.value = false
        _timePreSelecionado.value = null // Limpar o time pré-selecionado
    }

    // Transferir jogador entre times
    fun transferirJogador(jogador: Jogador, timeOrigemId: Long, timeDestinoId: Long) {
        viewModelScope.launch {
            try {
                // Buscar os times de origem e destino
                val timeOrigem = _historicoTimes.value.find { it.id == timeOrigemId }
                val timeDestino = _historicoTimes.value.find { it.id == timeDestinoId }

                if (timeOrigem != null && timeDestino != null) {
                    // Verificar se o time de origem tem estatísticas registradas
                    val temEstatisticas = timeOrigem.vitorias > 0 || timeOrigem.empates > 0 || timeOrigem.derrotas > 0

                    // Remover o jogador do time de origem
                    val novoJogadoresIdsOrigem = timeOrigem.jogadoresIds.filter { it != jogador.id }

                    // Adicionar o jogador ao time de destino
                    val novoJogadoresIdsDestino = timeDestino.jogadoresIds + jogador.id

                    // Calcular novas médias de estrelas para os times
                    val novoTimeOrigem = if (novoJogadoresIdsOrigem.isNotEmpty()) {
                        val jogadoresOrigem = novoJogadoresIdsOrigem.mapNotNull { jogadorRepository.getJogadorPorId(it) }
                        val mediaEstrelasOrigem = jogadoresOrigem.map { it.notaPosicaoPrincipal.toFloat() }.average().toFloat()
                        val mediaPontuacaoOrigem = jogadoresOrigem.map { it.pontuacaoTotal.toFloat() }.average().toFloat()

                        timeOrigem.copy(
                            jogadoresIds = novoJogadoresIdsOrigem,
                            mediaEstrelas = mediaEstrelasOrigem,
                            mediaPontuacao = mediaPontuacaoOrigem,
                            // Se tem estatísticas, zerar contadores
                            vitorias = if (temEstatisticas) 0 else timeOrigem.vitorias,
                            derrotas = if (temEstatisticas) 0 else timeOrigem.derrotas,
                            empates = if (temEstatisticas) 0 else timeOrigem.empates
                        )
                    } else {
                        // Time ficou sem jogadores, manter valores zerados
                        timeOrigem.copy(
                            jogadoresIds = emptyList(),
                            mediaEstrelas = 0f,
                            mediaPontuacao = 0f,
                            vitorias = 0,
                            derrotas = 0,
                            empates = 0
                        )
                    }

                    val jogadoresDestino = novoJogadoresIdsDestino.mapNotNull { jogadorRepository.getJogadorPorId(it) }
                    val mediaEstrelasDestino = jogadoresDestino.map { it.notaPosicaoPrincipal.toFloat() }.average().toFloat()
                    val mediaPontuacaoDestino = jogadoresDestino.map { it.pontuacaoTotal.toFloat() }.average().toFloat()

                    val novoTimeDestino = timeDestino.copy(
                        jogadoresIds = novoJogadoresIdsDestino,
                        mediaEstrelas = mediaEstrelasDestino,
                        mediaPontuacao = mediaPontuacaoDestino,
                        // Se time de origem tem estatísticas, zerar contadores do destino também
                        vitorias = if (temEstatisticas) 0 else timeDestino.vitorias,
                        derrotas = if (temEstatisticas) 0 else timeDestino.derrotas,
                        empates = if (temEstatisticas) 0 else timeDestino.empates
                    )

                    // Atualizar no banco de dados
                    sorteioRepository.atualizarHistoricoTime(novoTimeOrigem)
                    sorteioRepository.atualizarHistoricoTime(novoTimeDestino)

                    // Se havia estatísticas registradas, salvar no histórico antes de zerar
                    if (temEstatisticas) {
                        // Salvar os times antigos no histórico antes de atualizá-los
                        val timesAntigos = _historicoTimes.value
                        historicoRepository.salvarPeladaFinalizada(timesAntigos)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // Métodos para gerenciar confrontos
    fun mostrarComponenteConfronto() {
        _mostrarComponenteConfronto.value = true
    }

    fun ocultarComponenteConfronto() {
        _mostrarComponenteConfronto.value = false
    }

    // Método atualizado para lidar com diferentes resultados de confronto
    fun finalizarConfronto(timeA: HistoricoTime, timeB: HistoricoTime, resultado: ResultadoConfronto) {
        viewModelScope.launch {
            when (resultado) {
                ResultadoConfronto.VITORIA_TIME1 -> {
                    // Time A venceu, Time B perdeu
                    adicionarVitoria(timeA)
                    adicionarDerrota(timeB)
                }
                ResultadoConfronto.VITORIA_TIME2 -> {
                    // Time B venceu, Time A perdeu
                    adicionarVitoria(timeB)
                    adicionarDerrota(timeA)
                }
                ResultadoConfronto.EMPATE -> {
                    // Empate para ambos os times
                    adicionarEmpate(timeA)
                    adicionarEmpate(timeB)
                }
            }

            // Oculta o componente após a finalização
            ocultarComponenteConfronto()
        }
    }

    // Método para abrir a tela de transferência específica para time reserva
    fun abrirTransferenciaTimeReserva(timeReserva: HistoricoTime) {
        // Definir o time reserva como pré-selecionado
        _timePreSelecionado.value = timeReserva
        // Ativar o modo substituição time reserva
        _modoSubstituicaoTimeReserva.value = true
        // Mostrar o diálogo de transferência
        _mostrarDialogoTransferencia.value = true
    }

    // Método para definir o ID do grupo atual
    fun setGrupoId(id: Long) {
        _grupoId.value = id
        // Recarregar os dados para o grupo específico
        carregarHistoricoTimes(id)
    }

    // Método para carregar os times do histórico com base no ID do grupo
    private fun carregarHistoricoTimes(grupoId: Long) {
        viewModelScope.launch {
            // Obter apenas os times da última pelada do grupo atual
            sorteioRepository.getTimesUltimaPeladaPorGrupo(grupoId).collect { historicoTimes ->
                _historicoTimes.value = historicoTimes
                carregarJogadoresPorTime(historicoTimes)
            }
        }
    }
}
