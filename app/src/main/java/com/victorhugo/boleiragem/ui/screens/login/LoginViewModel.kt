package com.victorhugo.boleiragem.ui.screens.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.victorhugo.boleiragem.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class ModoAutenticacao {
    LOGIN, CADASTRO
}

data class LoginUiState(
    val email: String = "",
    val senha: String = "",
    val modo: ModoAutenticacao = ModoAutenticacao.LOGIN,
    val carregando: Boolean = false,
    val erro: String? = null,
    val mensagemInfo: String? = null,
    val autenticado: Boolean = false
)

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        LoginUiState(autenticado = authRepository.usuarioAtual != null)
    )
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    fun onEmailChange(email: String) {
        _uiState.update { it.copy(email = email, erro = null) }
    }

    fun onSenhaChange(senha: String) {
        _uiState.update { it.copy(senha = senha, erro = null) }
    }

    fun alternarModo() {
        _uiState.update {
            it.copy(
                modo = if (it.modo == ModoAutenticacao.LOGIN) ModoAutenticacao.CADASTRO else ModoAutenticacao.LOGIN,
                erro = null,
                mensagemInfo = null
            )
        }
    }

    fun confirmar() {
        val estado = _uiState.value
        val email = estado.email.trim()
        val senha = estado.senha

        if (email.isBlank() || senha.isBlank()) {
            _uiState.update { it.copy(erro = "Preencha e-mail e senha") }
            return
        }
        if (estado.modo == ModoAutenticacao.CADASTRO && senha.length < 6) {
            _uiState.update { it.copy(erro = "A senha deve ter pelo menos 6 caracteres") }
            return
        }

        _uiState.update { it.copy(carregando = true, erro = null, mensagemInfo = null) }
        viewModelScope.launch {
            val resultado = if (estado.modo == ModoAutenticacao.LOGIN) {
                authRepository.entrarComEmailSenha(email, senha)
            } else {
                authRepository.cadastrarComEmailSenha(email, senha)
            }

            resultado.fold(
                onSuccess = {
                    _uiState.update { it.copy(carregando = false, autenticado = true) }
                },
                onFailure = { excecao ->
                    _uiState.update {
                        it.copy(carregando = false, erro = mensagemDeErro(excecao, estado.modo))
                    }
                }
            )
        }
    }

    fun entrarComGoogle(idToken: String) {
        _uiState.update { it.copy(carregando = true, erro = null, mensagemInfo = null) }
        viewModelScope.launch {
            val resultado = authRepository.entrarComGoogle(idToken)
            resultado.fold(
                onSuccess = {
                    _uiState.update { it.copy(carregando = false, autenticado = true) }
                },
                onFailure = { excecao ->
                    _uiState.update {
                        it.copy(carregando = false, erro = excecao.message ?: "Não foi possível entrar com Google")
                    }
                }
            )
        }
    }

    fun erroGoogle(mensagem: String) {
        _uiState.update { it.copy(carregando = false, erro = mensagem) }
    }

    fun esqueciSenha() {
        val email = _uiState.value.email.trim()
        if (email.isBlank()) {
            _uiState.update { it.copy(erro = "Informe seu e-mail para redefinir a senha") }
            return
        }
        _uiState.update { it.copy(carregando = true, erro = null, mensagemInfo = null) }
        viewModelScope.launch {
            val resultado = authRepository.enviarEmailRedefinicaoSenha(email)
            resultado.fold(
                onSuccess = {
                    _uiState.update {
                        it.copy(carregando = false, mensagemInfo = "E-mail de redefinição enviado para $email")
                    }
                },
                onFailure = { excecao ->
                    _uiState.update {
                        it.copy(carregando = false, erro = mensagemDeErro(excecao, ModoAutenticacao.LOGIN))
                    }
                }
            )
        }
    }

    private fun mensagemDeErro(excecao: Throwable, modo: ModoAutenticacao): String {
        return when (excecao) {
            is FirebaseAuthInvalidUserException,
            is FirebaseAuthInvalidCredentialsException -> "E-mail ou senha inválidos"
            is FirebaseAuthUserCollisionException -> "Já existe uma conta com esse e-mail"
            is FirebaseAuthWeakPasswordException -> "Senha muito fraca. Use pelo menos 6 caracteres"
            is FirebaseNetworkException -> "Sem conexão com a internet. Tente novamente"
            else -> excecao.message ?: "Não foi possível ${if (modo == ModoAutenticacao.LOGIN) "entrar" else "criar a conta"}"
        }
    }
}
