package com.victorhugo.boleiragem.ui.screens.configuracao

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.victorhugo.boleiragem.data.model.ConfiguracaoPontuacao
import com.victorhugo.boleiragem.data.repository.PontuacaoRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ConfiguracaoPontuacaoViewModel @Inject constructor(
    private val pontuacaoRepository: PontuacaoRepository
) : ViewModel() {

    // Inicializa com um objeto padrão para evitar NullPointerException
    private val _configuracaoPontuacao = MutableStateFlow(ConfiguracaoPontuacao(pontosPorVitoria = 10, pontosPorDerrota = -10, pontosPorEmpate = -5))
    val configuracaoPontuacao: StateFlow<ConfiguracaoPontuacao> = _configuracaoPontuacao.asStateFlow()

    private val _salvandoConfiguracao = MutableStateFlow(false)
    val salvandoConfiguracao: StateFlow<Boolean> = _salvandoConfiguracao.asStateFlow()

    private val _configuracaoSalva = MutableStateFlow(false)
    val configuracaoSalva: StateFlow<Boolean> = _configuracaoSalva.asStateFlow()

    private val _grupoId = MutableStateFlow(-1L)

    // Chamado pela tela via LaunchedEffect(grupoId), mesmo padrão das demais telas do app
    fun setGrupoId(id: Long) {
        if (_grupoId.value == id) return
        _grupoId.value = id
        pontuacaoRepository.setGrupoId(id)
        carregarConfiguracao()
    }

    private fun carregarConfiguracao() {
        viewModelScope.launch {
            pontuacaoRepository.getConfiguracaoPontuacao().collect { configuracao ->
                _configuracaoPontuacao.value = configuracao
            }
        }
    }

    fun atualizarPontosPorVitoria(pontos: Int) {
        // Adiciona verificação de nulidade para evitar NullPointerException
        val configAtual = _configuracaoPontuacao.value ?: ConfiguracaoPontuacao()
        _configuracaoPontuacao.value = configAtual.copy(pontosPorVitoria = pontos)
    }

    fun atualizarPontosPorDerrota(pontos: Int) {
        // Adiciona verificação de nulidade para evitar NullPointerException
        val configAtual = _configuracaoPontuacao.value ?: ConfiguracaoPontuacao()
        _configuracaoPontuacao.value = configAtual.copy(pontosPorDerrota = pontos)
    }

    fun atualizarPontosPorEmpate(pontos: Int) {
        // Adiciona verificação de nulidade para evitar NullPointerException
        val configAtual = _configuracaoPontuacao.value ?: ConfiguracaoPontuacao()
        _configuracaoPontuacao.value = configAtual.copy(pontosPorEmpate = pontos)
    }

    fun salvarConfiguracao() {
        viewModelScope.launch {
            _salvandoConfiguracao.value = true

            try {
                // Verifica se a configuração não é nula antes de salvar
                val configParaSalvar = _configuracaoPontuacao.value ?: ConfiguracaoPontuacao()

                // Salva a configuração e recalcula a pontuação dos jogadores
                pontuacaoRepository.atualizarConfiguracaoPontuacao(configParaSalvar)

                // Aguarda um momento para garantir que o recálculo tenha sido concluído
                kotlinx.coroutines.delay(500)

                _configuracaoSalva.value = true

                // Reset do estado após 2 segundos
                kotlinx.coroutines.delay(2000)
                _configuracaoSalva.value = false

            } catch (e: Exception) {
                // Tratamento de erros
                e.printStackTrace()
            } finally {
                _salvandoConfiguracao.value = false
            }
        }
    }
}
