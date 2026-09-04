package com.victorhugo.boleiragem.ui.screens.grupos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.victorhugo.boleiragem.data.repository.AuthRepository
import com.victorhugo.boleiragem.data.repository.ConfiguracaoRepository
import com.victorhugo.boleiragem.data.repository.GrupoPeladaRepository
import com.victorhugo.boleiragem.data.repository.GrupoRemotoRepository
import com.victorhugo.boleiragem.data.repository.HistoricoRepository
import com.victorhugo.boleiragem.data.repository.JogadorRepository
import com.victorhugo.boleiragem.data.repository.PontuacaoRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Ponto único de entrada da Fase 2 (sync de conteúdo), Fase 3 (vínculo Jogador-usuário) e da
 * permissão dentro do grupo (`podeEditar`), acionado sempre que MainScreen abre um grupo:
 * - Fase 3, roda pra qualquer grupo (local ou compartilhado) enquanto houver usuário autenticado:
 *   garante que esse usuário tem um Jogador vinculado a si (`usuarioUid`) neste grupo. Convidado
 *   nunca tem uid, então nunca aciona isto — comportamento igual ao de hoje pra convidado.
 * - Fase 2, só roda se o grupo também tiver `firestoreId` (já foi compartilhado): mantém os 4
 *   listeners remotos (jogadores/histórico/configuração de sorteio/pontuação) ativos, replicando
 *   mudanças de outros dispositivos para o Room local.
 * - `podeEditar`: grupo nunca compartilhado (sem firestoreId) é sempre editável — comportamento
 *   local de sempre, sem restrição. Grupo compartilhado só é editável por quem é Dono ou
 *   Responsável (`PapelGrupo.EDITOR`) — membro comum vê as telas, mas com os controles desabilitados.
 */
@HiltViewModel
class GrupoSyncViewModel @Inject constructor(
    private val grupoPeladaRepository: GrupoPeladaRepository,
    private val grupoRemotoRepository: GrupoRemotoRepository,
    private val authRepository: AuthRepository,
    private val jogadorRepository: JogadorRepository,
    private val historicoRepository: HistoricoRepository,
    private val configuracaoRepository: ConfiguracaoRepository,
    private val pontuacaoRepository: PontuacaoRepository
) : ViewModel() {

    private var jobSincronizacao: Job? = null

    private val _podeEditar = MutableStateFlow(true)
    val podeEditar: StateFlow<Boolean> = _podeEditar.asStateFlow()

    fun sincronizarGrupo(grupoId: Long) {
        jobSincronizacao?.cancel()
        _podeEditar.value = true
        if (grupoId <= 0) return
        val usuario = authRepository.usuarioAtual ?: return // convidado: nada a fazer aqui

        jobSincronizacao = viewModelScope.launch {
            jogadorRepository.garantirJogadorDoUsuario(grupoId, usuario.uid, usuario.displayName)

            val grupo = grupoPeladaRepository.getGrupoPorId(grupoId) ?: return@launch
            val grupoFirestoreId = grupo.firestoreId ?: return@launch

            launch { jogadorRepository.observarSincronizacaoRemota(grupoId, grupoFirestoreId).collect() }
            launch { historicoRepository.observarSincronizacaoRemota(grupoId, grupoFirestoreId).collect() }
            launch { configuracaoRepository.observarSincronizacaoRemota(grupoId, grupoFirestoreId).collect() }
            launch { pontuacaoRepository.observarSincronizacaoRemota(grupoId, grupoFirestoreId).collect() }

            launch {
                grupoRemotoRepository.observarMeusGrupos(usuario.uid).collect { grupos ->
                    val remoto = grupos.firstOrNull { it.id == grupoFirestoreId }
                    _podeEditar.value = remoto?.podeEditar(usuario.uid) ?: false
                }
            }
        }
    }

    override fun onCleared() {
        jobSincronizacao?.cancel()
        super.onCleared()
    }
}
