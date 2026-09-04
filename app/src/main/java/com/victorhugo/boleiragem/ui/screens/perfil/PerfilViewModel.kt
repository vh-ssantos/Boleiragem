package com.victorhugo.boleiragem.ui.screens.perfil

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.victorhugo.boleiragem.data.model.PosicaoJogador
import com.victorhugo.boleiragem.data.repository.AuthRepository
import com.victorhugo.boleiragem.data.repository.EstatisticasPessoais
import com.victorhugo.boleiragem.data.repository.HistoricoRepository
import com.victorhugo.boleiragem.data.repository.UsuarioRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class TipoConta { GOOGLE, EMAIL_SENHA, CONVIDADO }

data class PerfilUiState(
    val autenticado: Boolean = false,
    val tipoConta: TipoConta = TipoConta.CONVIDADO,
    val nome: String = "",
    val email: String = "",
    val posicaoFavorita: PosicaoJogador? = null,
    val idade: String = "",
    val estatisticas: EstatisticasPessoais? = null,
    val carregando: Boolean = true,
    val salvando: Boolean = false,
    val erro: String? = null,
    val salvoComSucesso: Boolean = false
)

@HiltViewModel
class PerfilViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val usuarioRepository: UsuarioRepository,
    private val historicoRepository: HistoricoRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(PerfilUiState())
    val uiState: StateFlow<PerfilUiState> = _uiState.asStateFlow()

    init {
        carregarPerfil()
    }

    private fun carregarPerfil() {
        val usuario = authRepository.usuarioAtual
        if (usuario == null) {
            // Convidado: sem conta, sem perfil pra carregar.
            _uiState.update { it.copy(autenticado = false, carregando = false) }
            return
        }

        val tipoConta = if (usuario.providerData.any { it.providerId == "google.com" }) {
            TipoConta.GOOGLE
        } else {
            TipoConta.EMAIL_SENHA
        }

        viewModelScope.launch {
            val perfil = usuarioRepository.buscarPerfil(usuario.uid)
            val estatisticas = historicoRepository.getEstatisticasPessoais(usuario.uid)
            _uiState.update {
                it.copy(
                    autenticado = true,
                    tipoConta = tipoConta,
                    nome = perfil?.nome ?: usuario.displayName.orEmpty(),
                    email = usuario.email.orEmpty(),
                    posicaoFavorita = perfil?.posicaoFavorita?.let { nome ->
                        runCatching { PosicaoJogador.valueOf(nome) }.getOrNull()
                    },
                    idade = perfil?.idade?.toString().orEmpty(),
                    estatisticas = estatisticas,
                    carregando = false
                )
            }
        }
    }

    fun onNomeChange(nome: String) {
        _uiState.update { it.copy(nome = nome, salvoComSucesso = false) }
    }

    fun onPosicaoChange(posicao: PosicaoJogador) {
        _uiState.update { it.copy(posicaoFavorita = posicao, salvoComSucesso = false) }
    }

    fun onIdadeChange(idade: String) {
        if (idade.length <= 3 && idade.all { it.isDigit() }) {
            _uiState.update { it.copy(idade = idade, salvoComSucesso = false) }
        }
    }

    fun salvar() {
        val usuario = authRepository.usuarioAtual ?: return
        val estado = _uiState.value
        if (estado.nome.isBlank()) {
            _uiState.update { it.copy(erro = "Informe seu nome") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(salvando = true, erro = null) }
            val sucesso = usuarioRepository.atualizarPerfil(
                uid = usuario.uid,
                nome = estado.nome.trim(),
                posicaoFavorita = estado.posicaoFavorita?.name,
                idade = estado.idade.toIntOrNull()
            )
            _uiState.update {
                it.copy(
                    salvando = false,
                    salvoComSucesso = sucesso,
                    erro = if (sucesso) null else "Não foi possível salvar. Tente novamente."
                )
            }
        }
    }
}
