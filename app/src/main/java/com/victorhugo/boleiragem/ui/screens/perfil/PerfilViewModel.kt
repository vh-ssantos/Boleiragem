package com.victorhugo.boleiragem.ui.screens.perfil

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.victorhugo.boleiragem.data.model.GrupoPelada
import com.victorhugo.boleiragem.data.model.HistoricoPelada
import com.victorhugo.boleiragem.data.model.PapelGrupo
import com.victorhugo.boleiragem.data.model.PosicaoJogador
import com.victorhugo.boleiragem.data.repository.AuthRepository
import com.victorhugo.boleiragem.data.repository.EstatisticasPessoais
import com.victorhugo.boleiragem.data.repository.GrupoPeladaRepository
import com.victorhugo.boleiragem.data.repository.GrupoRemotoRepository
import com.victorhugo.boleiragem.data.repository.HistoricoRepository
import com.victorhugo.boleiragem.data.repository.UsuarioRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class TipoConta { GOOGLE, EMAIL_SENHA, CONVIDADO }

// Item da tela "Meus Grupos": tanto os grupos próprios (Room) quanto os que o usuário participa
// como convidado (Firestore-only) — ver GruposPeladaViewModel.observarGruposCompartilhados, mesmo padrão.
data class GrupoAssociado(
    val nome: String,
    val papel: String
)

data class PerfilUiState(
    val autenticado: Boolean = false,
    val uid: String = "",
    val tipoConta: TipoConta = TipoConta.CONVIDADO,
    val nome: String = "",
    val email: String = "",
    val posicaoFavorita: PosicaoJogador? = null,
    val idade: String = "",
    val estatisticas: EstatisticasPessoais? = null,
    val peladasParticipadas: List<HistoricoPelada> = emptyList(),
    val meusGrupos: List<GrupoAssociado> = emptyList(),
    val carregando: Boolean = true,
    val salvando: Boolean = false,
    val erro: String? = null,
    val salvoComSucesso: Boolean = false,
    val mensagemRedefinicaoSenha: String? = null
)

@HiltViewModel
class PerfilViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val usuarioRepository: UsuarioRepository,
    private val historicoRepository: HistoricoRepository,
    private val grupoPeladaRepository: GrupoPeladaRepository,
    private val grupoRemotoRepository: GrupoRemotoRepository
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
                    uid = usuario.uid,
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

        viewModelScope.launch {
            historicoRepository.observarPeladasDoUsuario(usuario.uid).collect { peladas ->
                _uiState.update { it.copy(peladasParticipadas = peladas) }
            }
        }

        viewModelScope.launch {
            carregarMeusGrupos(usuario.uid)
        }
    }

    private suspend fun carregarMeusGrupos(uid: String) {
        val gruposProprios = grupoPeladaRepository.getGruposAtivos().first()
            .map { grupo: GrupoPelada -> GrupoAssociado(nome = grupo.nome, papel = "Dono") }

        _uiState.update { it.copy(meusGrupos = gruposProprios) }

        try {
            grupoRemotoRepository.observarMeusGrupos(uid).collect { remotos ->
                val comoConvidado = remotos
                    .filter { it.donoId != uid }
                    .map { grupo ->
                        val papel = when (grupo.papelDe(uid)) {
                            PapelGrupo.EDITOR -> "Editor"
                            PapelGrupo.MEMBRO -> "Membro"
                            PapelGrupo.DONO -> "Dono"
                        }
                        GrupoAssociado(nome = grupo.nome, papel = papel)
                    }
                _uiState.update { it.copy(meusGrupos = gruposProprios + comoConvidado) }
            }
        } catch (e: Exception) {
            e.printStackTrace()
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

    // Item de menu "Redefinir senha" — só exibido pra conta e-mail/senha (ver PerfilMenuSheet).
    fun redefinirSenha() {
        val email = _uiState.value.email
        if (email.isBlank()) return

        viewModelScope.launch {
            val resultado = authRepository.enviarEmailRedefinicaoSenha(email)
            _uiState.update {
                it.copy(
                    mensagemRedefinicaoSenha = if (resultado.isSuccess) {
                        "E-mail de redefinição enviado para $email"
                    } else {
                        "Não foi possível enviar o e-mail. Tente novamente."
                    }
                )
            }
        }
    }

    fun limparMensagemRedefinicaoSenha() {
        _uiState.update { it.copy(mensagemRedefinicaoSenha = null) }
    }
}
