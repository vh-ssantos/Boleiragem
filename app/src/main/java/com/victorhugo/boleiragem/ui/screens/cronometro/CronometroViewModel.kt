package com.victorhugo.boleiragem.ui.screens.cronometro

import android.app.Application
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class ModoCronometro { PROGRESSIVO, REGRESSIVO }

data class CronometroUiState(
    val modo: ModoCronometro = ModoCronometro.PROGRESSIVO,
    val rodando: Boolean = false,
    val tempoMs: Long = 0L, // progressivo: decorrido; regressivo: restante
    val duracaoRegressivaMs: Long = 10 * 60_000L, // padrão 10 min, ajustável antes de iniciar
    val esgotado: Boolean = false
)

/**
 * Cronômetro standalone (sem depender de grupo) — pedido explícito do usuário porque os
 * testadores atuais podem nem ter criado um grupo ainda. Estado 100% em memória (ViewModel),
 * sem persistência: reseta se o processo for morto em segundo plano. Virar um serviço em primeiro
 * plano (sobrevive com o app fechado) fica pra depois, só se o uso real pedir.
 */
@HiltViewModel
class CronometroViewModel @Inject constructor(
    application: Application
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(CronometroUiState())
    val uiState: StateFlow<CronometroUiState> = _uiState.asStateFlow()

    private var jobTick: Job? = null
    private var horaDeInicio: Long = 0L
    private var baseMs: Long = 0L // tempo já acumulado antes do último "iniciar" (pra suportar pausa/retomada)

    fun selecionarModo(modo: ModoCronometro) {
        if (_uiState.value.rodando) return
        _uiState.update {
            it.copy(
                modo = modo,
                tempoMs = if (modo == ModoCronometro.REGRESSIVO) it.duracaoRegressivaMs else 0L,
                esgotado = false
            )
        }
        baseMs = 0L
    }

    fun definirDuracaoRegressiva(minutos: Int) {
        if (_uiState.value.rodando || minutos <= 0) return
        val duracaoMs = minutos * 60_000L
        _uiState.update { it.copy(duracaoRegressivaMs = duracaoMs, tempoMs = duracaoMs, esgotado = false) }
        baseMs = 0L
    }

    fun definirDuracaoRegressivaSegundos(segundos: Int) {
        if (_uiState.value.rodando || segundos <= 0) return
        val duracaoMs = segundos * 1000L
        _uiState.update { it.copy(duracaoRegressivaMs = duracaoMs, tempoMs = duracaoMs, esgotado = false) }
        baseMs = 0L
    }

    fun iniciar() {
        if (_uiState.value.rodando || _uiState.value.esgotado) return
        val estado = _uiState.value
        baseMs = if (estado.modo == ModoCronometro.REGRESSIVO) {
            estado.duracaoRegressivaMs - estado.tempoMs
        } else {
            estado.tempoMs
        }
        horaDeInicio = System.currentTimeMillis()
        _uiState.update { it.copy(rodando = true) }

        jobTick = viewModelScope.launch {
            while (true) {
                val decorrido = baseMs + (System.currentTimeMillis() - horaDeInicio)
                val estadoAtual = _uiState.value

                if (estadoAtual.modo == ModoCronometro.REGRESSIVO) {
                    val restante = (estadoAtual.duracaoRegressivaMs - decorrido).coerceAtLeast(0)
                    _uiState.update { it.copy(tempoMs = restante) }
                    if (restante <= 0) {
                        pausarInterno()
                        _uiState.update { it.copy(rodando = false, esgotado = true) }
                        dispararAlerta()
                        break
                    }
                } else {
                    _uiState.update { it.copy(tempoMs = decorrido) }
                }

                delay(200)
            }
        }
    }

    fun pausar() {
        pausarInterno()
        _uiState.update { it.copy(rodando = false) }
    }

    private fun pausarInterno() {
        jobTick?.cancel()
        jobTick = null
    }

    fun reiniciar() {
        pausarInterno()
        val estado = _uiState.value
        _uiState.update {
            it.copy(
                rodando = false,
                esgotado = false,
                tempoMs = if (estado.modo == ModoCronometro.REGRESSIVO) estado.duracaoRegressivaMs else 0L
            )
        }
        baseMs = 0L
    }

    private fun dispararAlerta() {
        try {
            val toneGenerator = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 100)
            toneGenerator.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 1500)
            viewModelScope.launch {
                delay(1600)
                toneGenerator.release()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        try {
            val contexto = getApplication<Application>()
            val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val manager = contexto.getSystemService(VibratorManager::class.java)
                manager?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                contexto.getSystemService(Vibrator::class.java)
            }
            val padrao = longArrayOf(0, 400, 200, 400, 200, 400)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createWaveform(padrao, -1))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(padrao, -1)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onCleared() {
        pausarInterno()
        super.onCleared()
    }
}
