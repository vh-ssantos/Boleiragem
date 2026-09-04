package com.victorhugo.boleiragem.ui.screens.infogrupo

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.victorhugo.boleiragem.data.model.DiaSemana
import com.victorhugo.boleiragem.data.model.GrupoPelada
import com.victorhugo.boleiragem.data.model.PapelGrupo
import com.victorhugo.boleiragem.data.model.TipoRecorrencia
import com.victorhugo.boleiragem.data.repository.AuthRepository
import com.victorhugo.boleiragem.data.repository.GrupoPeladaRepository
import com.victorhugo.boleiragem.data.repository.GrupoRemotoRepository
import com.victorhugo.boleiragem.data.repository.JogadorRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import javax.inject.Inject

data class InfoGrupoUiState(
    val carregando: Boolean = true,
    val grupo: GrupoPelada? = null,
    val papel: PapelGrupo = PapelGrupo.DONO,
    val jogadoresCadastrados: Int = 0,
    val membrosDoGrupo: Int? = null, // null = grupo nunca compartilhado, não faz sentido mostrar
    val diasAteProximaPelada: Int? = null // null = esporádica, sem dia fixo
)

@HiltViewModel
class InfoGrupoViewModel @Inject constructor(
    private val grupoPeladaRepository: GrupoPeladaRepository,
    private val jogadorRepository: JogadorRepository,
    private val grupoRemotoRepository: GrupoRemotoRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(InfoGrupoUiState())
    val uiState: StateFlow<InfoGrupoUiState> = _uiState.asStateFlow()

    private var grupoIdAtual: Long = -1L

    fun setGrupoId(grupoId: Long) {
        if (grupoId <= 0 || grupoId == grupoIdAtual) return
        grupoIdAtual = grupoId
        carregar(grupoId)
    }

    private fun carregar(grupoId: Long) {
        viewModelScope.launch {
            val grupo = grupoPeladaRepository.getGrupoPorId(grupoId) ?: return@launch
            val jogadores = jogadorRepository.countJogadoresAtivosPorGrupo(grupoId)
            _uiState.update {
                it.copy(
                    carregando = false,
                    grupo = grupo,
                    jogadoresCadastrados = jogadores,
                    diasAteProximaPelada = calcularDiasAteProximaPelada(grupo)
                )
            }

            val firestoreId = grupo.firestoreId ?: return@launch
            val uid = authRepository.usuarioAtual?.uid ?: return@launch
            try {
                grupoRemotoRepository.observarMeusGrupos(uid).collect { grupos ->
                    val remoto = grupos.firstOrNull { it.id == firestoreId } ?: return@collect
                    val membros = (listOf(remoto.donoId) + remoto.editoresIds + remoto.membrosIds).distinct().size
                    _uiState.update {
                        it.copy(papel = remoto.papelDe(uid), membrosDoGrupo = membros)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun calcularDiasAteProximaPelada(grupo: GrupoPelada): Int? {
        if (grupo.tipoRecorrencia != TipoRecorrencia.RECORRENTE) return null
        val dias = grupo.diasSemana.ifEmpty { listOfNotNull(grupo.diaSemana) }
        if (dias.isEmpty()) return null
        val hoje = LocalDate.now().dayOfWeek
        return dias.minOf { dia ->
            val alvo = dia.paraDayOfWeek()
            (alvo.value - hoje.value + 7) % 7
        }
    }
}

private fun DiaSemana.paraDayOfWeek(): DayOfWeek = when (this) {
    DiaSemana.SEGUNDA -> DayOfWeek.MONDAY
    DiaSemana.TERCA -> DayOfWeek.TUESDAY
    DiaSemana.QUARTA -> DayOfWeek.WEDNESDAY
    DiaSemana.QUINTA -> DayOfWeek.THURSDAY
    DiaSemana.SEXTA -> DayOfWeek.FRIDAY
    DiaSemana.SABADO -> DayOfWeek.SATURDAY
    DiaSemana.DOMINGO -> DayOfWeek.SUNDAY
}
