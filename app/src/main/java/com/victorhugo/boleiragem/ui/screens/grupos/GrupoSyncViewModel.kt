package com.victorhugo.boleiragem.ui.screens.grupos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.victorhugo.boleiragem.data.repository.AuthRepository
import com.victorhugo.boleiragem.data.repository.ConfiguracaoRepository
import com.victorhugo.boleiragem.data.repository.GrupoPeladaRepository
import com.victorhugo.boleiragem.data.repository.HistoricoRepository
import com.victorhugo.boleiragem.data.repository.JogadorRepository
import com.victorhugo.boleiragem.data.repository.PontuacaoRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Ponto único de entrada da sincronização Fase 2: enquanto um grupo compartilhado está aberto em
 * MainScreen, mantém os 4 listeners remotos (jogadores/histórico/configuração de sorteio/pontuação)
 * ativos, replicando mudanças de outros dispositivos para o Room local. Grupos locais (sem
 * firestoreId) ou modo convidado (sem usuário autenticado) não acionam nada aqui.
 */
@HiltViewModel
class GrupoSyncViewModel @Inject constructor(
    private val grupoPeladaRepository: GrupoPeladaRepository,
    private val authRepository: AuthRepository,
    private val jogadorRepository: JogadorRepository,
    private val historicoRepository: HistoricoRepository,
    private val configuracaoRepository: ConfiguracaoRepository,
    private val pontuacaoRepository: PontuacaoRepository
) : ViewModel() {

    private var jobSincronizacao: Job? = null

    fun sincronizarGrupo(grupoId: Long) {
        jobSincronizacao?.cancel()
        if (grupoId <= 0 || authRepository.usuarioAtual == null) return

        jobSincronizacao = viewModelScope.launch {
            val grupo = grupoPeladaRepository.getGrupoPorId(grupoId) ?: return@launch
            val grupoFirestoreId = grupo.firestoreId ?: return@launch

            launch { jogadorRepository.observarSincronizacaoRemota(grupoId, grupoFirestoreId).collect() }
            launch { historicoRepository.observarSincronizacaoRemota(grupoId, grupoFirestoreId).collect() }
            launch { configuracaoRepository.observarSincronizacaoRemota(grupoId, grupoFirestoreId).collect() }
            launch { pontuacaoRepository.observarSincronizacaoRemota(grupoId, grupoFirestoreId).collect() }
        }
    }

    override fun onCleared() {
        jobSincronizacao?.cancel()
        super.onCleared()
    }
}
