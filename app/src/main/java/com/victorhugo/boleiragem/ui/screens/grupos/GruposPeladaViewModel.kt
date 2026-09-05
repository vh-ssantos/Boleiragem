package com.victorhugo.boleiragem.ui.screens.grupos

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.victorhugo.boleiragem.data.model.ConfiguracaoSorteio
import com.victorhugo.boleiragem.data.model.DiaSemana
import com.victorhugo.boleiragem.data.model.GrupoPelada
import com.victorhugo.boleiragem.data.model.GrupoRemoto
import com.victorhugo.boleiragem.data.model.Jogador
import com.victorhugo.boleiragem.data.model.PapelGrupo
import com.victorhugo.boleiragem.data.model.PosicaoJogador // Import adicionado
import com.victorhugo.boleiragem.data.model.ResultadoSorteio
import com.victorhugo.boleiragem.data.model.TipoRecorrencia
import com.victorhugo.boleiragem.data.model.Time
import com.victorhugo.boleiragem.data.model.UsuarioPerfil
import com.victorhugo.boleiragem.data.repository.AuthRepository
import com.victorhugo.boleiragem.data.repository.ConfiguracaoRepository
import com.victorhugo.boleiragem.data.repository.GrupoPeladaRepository
import com.victorhugo.boleiragem.data.repository.GrupoRemotoRepository
import com.victorhugo.boleiragem.data.repository.JogadorRepository
import com.victorhugo.boleiragem.data.repository.SorteioRepository
import com.victorhugo.boleiragem.data.repository.UsuarioRepository
import com.victorhugo.boleiragem.domain.SorteioUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class TipoVisualizacao { LISTA, CARDS, MINIMALISTA }

@HiltViewModel
class GruposPeladaViewModel @Inject constructor(
    private val grupoPeladaRepository: GrupoPeladaRepository,
    private val jogadorRepository: JogadorRepository,
    private val configuracaoRepository: ConfiguracaoRepository,
    private val sorteioUseCase: SorteioUseCase,
    private val sorteioRepository: SorteioRepository,
    private val grupoRemotoRepository: GrupoRemotoRepository,
    private val authRepository: AuthRepository,
    private val usuarioRepository: UsuarioRepository
) : ViewModel() {

    private val _grupos = MutableStateFlow<List<GrupoPelada>>(emptyList())
    val grupos: StateFlow<List<GrupoPelada>> = _grupos.asStateFlow()

    private val _jogadoresPorGrupo = MutableStateFlow<Map<Long, Int>>(emptyMap())
    val jogadoresPorGrupo: StateFlow<Map<Long, Int>> = _jogadoresPorGrupo.asStateFlow()

    private val _tipoVisualizacao = MutableStateFlow(TipoVisualizacao.LISTA)
    val tipoVisualizacao: StateFlow<TipoVisualizacao> = _tipoVisualizacao.asStateFlow()

    private val _carregando = MutableStateFlow(false)
    val carregando: StateFlow<Boolean> = _carregando.asStateFlow()

    private val _mostrarDialogoGrupo = MutableStateFlow(false)
    val mostrarDialogoGrupo: StateFlow<Boolean> = _mostrarDialogoGrupo.asStateFlow()

    private val _grupoEmEdicao = MutableStateFlow<GrupoPelada?>(null)
    val grupoEmEdicao: StateFlow<GrupoPelada?> = _grupoEmEdicao.asStateFlow()

    private val _mostrarSeletorMapas = MutableStateFlow(false)
    val mostrarSeletorMapas: StateFlow<Boolean> = _mostrarSeletorMapas.asStateFlow()

    private val _retornandoDoMapa = MutableStateFlow(false)
    val retornandoDoMapa: StateFlow<Boolean> = _retornandoDoMapa.asStateFlow()

    private var localSelecionadoLatitude: Double? = null
    private var localSelecionadoLongitude: Double? = null
    private var localSelecionadoEndereco: String? = null
    private var localSelecionadoNome: String? = null

    private val _grupoParaCompartilhar = MutableStateFlow<GrupoPelada?>(null)
    val grupoParaCompartilhar: StateFlow<GrupoPelada?> = _grupoParaCompartilhar.asStateFlow()

    private val _mostrarTelaCompartilhamento = MutableStateFlow(false)
    val mostrarTelaCompartilhamento: StateFlow<Boolean> = _mostrarTelaCompartilhamento.asStateFlow()

    // Estados para Sorteio Rápido e Sorteio de Lista Colada
    private val _mostrarDialogoConfigSorteioRapido = MutableStateFlow(false)
    val mostrarDialogoConfigSorteioRapido: StateFlow<Boolean> = _mostrarDialogoConfigSorteioRapido.asStateFlow()

    private val _grupoSelecionadoParaSorteioRapido = MutableStateFlow<GrupoPelada?>(null)
    val grupoSelecionadoParaSorteioRapido: StateFlow<GrupoPelada?> = _grupoSelecionadoParaSorteioRapido.asStateFlow()

    private val _jogadoresAtivosParaDialogoSorteioRapido = MutableStateFlow(0)
    val jogadoresAtivosParaDialogoSorteioRapido: StateFlow<Int> = _jogadoresAtivosParaDialogoSorteioRapido.asStateFlow()

    private val _perfisConfiguracaoSorteio = MutableStateFlow<List<ConfiguracaoSorteio>>(emptyList())
    val perfisConfiguracaoSorteio: StateFlow<List<ConfiguracaoSorteio>> = _perfisConfiguracaoSorteio.asStateFlow()

    private val _usarPerfilExistenteSorteioRapido = MutableStateFlow(false)
    val usarPerfilExistenteSorteioRapido: StateFlow<Boolean> = _usarPerfilExistenteSorteioRapido.asStateFlow()

    private val _perfilConfigSelecionadoSorteioRapido = MutableStateFlow<ConfiguracaoSorteio?>(null)
    val perfilConfigSelecionadoSorteioRapido: StateFlow<ConfiguracaoSorteio?> = _perfilConfigSelecionadoSorteioRapido.asStateFlow()

    private val _jogadoresPorTimeSorteioRapido = MutableStateFlow(5)
    val jogadoresPorTimeSorteioRapido: StateFlow<Int> = _jogadoresPorTimeSorteioRapido.asStateFlow()

    private val _numeroDeTimesSorteioRapido = MutableStateFlow(2)
    val numeroDeTimesSorteioRapido: StateFlow<Int> = _numeroDeTimesSorteioRapido.asStateFlow()

    private val _navegarParaResultadoSorteio = MutableStateFlow<Boolean?>(null)
    val navegarParaResultadoSorteio: StateFlow<Boolean?> = _navegarParaResultadoSorteio.asStateFlow()

    private val _erroSorteioRapido = MutableStateFlow<String?>(null)
    val erroSorteioRapido: StateFlow<String?> = _erroSorteioRapido.asStateFlow()

    private val _podeRealizarSorteioRapido = MutableStateFlow(false)
    val podeRealizarSorteioRapido: StateFlow<Boolean> = _podeRealizarSorteioRapido.asStateFlow()

    // Novos estados para a funcionalidade de colar lista
    private val _listaJogadoresColados = MutableStateFlow<List<Jogador>>(emptyList())
    val listaJogadoresColados: StateFlow<List<Jogador>> = _listaJogadoresColados.asStateFlow()

    private val _isSorteioDeListaColada = MutableStateFlow(false)
    val isSorteioDeListaColada: StateFlow<Boolean> = _isSorteioDeListaColada.asStateFlow()

    // --- Grupos compartilhados (Firestore) ---

    // Papel do usuário logado (Dono/Responsável/Membro) em cada grupo compartilhado que participa,
    // por firestoreId — inclui os que ele mesmo criou. Grupos nunca compartilhados (firestoreId
    // nulo) não entram aqui; a UI trata a ausência de entrada como "Dono" (comportamento local de
    // sempre, sem restrição).
    private val _papeisGrupos = MutableStateFlow<Map<String, PapelGrupo>>(emptyMap())
    val papeisGrupos: StateFlow<Map<String, PapelGrupo>> = _papeisGrupos.asStateFlow()

    private val _mostrarDialogoEntrarComCodigo = MutableStateFlow(false)
    val mostrarDialogoEntrarComCodigo: StateFlow<Boolean> = _mostrarDialogoEntrarComCodigo.asStateFlow()

    private val _erroEntrarComCodigo = MutableStateFlow<String?>(null)
    val erroEntrarComCodigo: StateFlow<String?> = _erroEntrarComCodigo.asStateFlow()

    private val _carregandoEntrarComCodigo = MutableStateFlow(false)
    val carregandoEntrarComCodigo: StateFlow<Boolean> = _carregandoEntrarComCodigo.asStateFlow()

    // Grupo encontrado pelo código, aguardando confirmação do usuário ("Fulano te convidou para X")
    private val _convitePendente = MutableStateFlow<GrupoRemoto?>(null)
    val convitePendente: StateFlow<GrupoRemoto?> = _convitePendente.asStateFlow()

    private val _grupoParaConvidar = MutableStateFlow<GrupoRemoto?>(null)
    val grupoParaConvidar: StateFlow<GrupoRemoto?> = _grupoParaConvidar.asStateFlow()

    private val _carregandoConvite = MutableStateFlow(false)
    val carregandoConvite: StateFlow<Boolean> = _carregandoConvite.asStateFlow()

    private val _grupoParaGerenciarMembros = MutableStateFlow<GrupoRemoto?>(null)
    val grupoParaGerenciarMembros: StateFlow<GrupoRemoto?> = _grupoParaGerenciarMembros.asStateFlow()

    private val _perfisMembros = MutableStateFlow<Map<String, UsuarioPerfil>>(emptyMap())
    val perfisMembros: StateFlow<Map<String, UsuarioPerfil>> = _perfisMembros.asStateFlow()

    private var convidadoSessaoId: String? = null
    private var usuarioUidSessao: String? = null
    private var sessaoInicializada = false
    private var gruposJob: Job? = null
    private var gruposCompartilhadosJob: Job? = null

    val uidAtual: String? get() = authRepository.usuarioAtual?.uid

    init {
        viewModelScope.launch {
            authRepository.estadoAutenticacao.collect { usuario ->
                if (convidadoSessaoId == null) {
                    atualizarSessao(convidadoId = null, usuarioUid = usuario?.uid)
                }
            }
        }
    }

    fun definirSessaoConvidado(sessaoId: String?) {
        atualizarSessao(
            convidadoId = sessaoId,
            usuarioUid = if (sessaoId == null) uidAtual else null
        )
    }

    private fun atualizarSessao(convidadoId: String?, usuarioUid: String?) {
        if (
            sessaoInicializada &&
            convidadoSessaoId == convidadoId &&
            usuarioUidSessao == usuarioUid
        ) {
            return
        }

        sessaoInicializada = true
        convidadoSessaoId = convidadoId
        usuarioUidSessao = usuarioUid

        gruposJob?.cancel()
        gruposCompartilhadosJob?.cancel()
        _grupos.value = emptyList()
        _jogadoresPorGrupo.value = emptyMap()
        _papeisGrupos.value = emptyMap()
        _perfisMembros.value = emptyMap()

        carregarGrupos()
        observarGruposCompartilhados(usuarioUid)
        if (usuarioUid != null) {
            salvarPerfilUsuarioLogado()
        }
    }

    private fun salvarPerfilUsuarioLogado() {
        val usuario = authRepository.usuarioAtual ?: return
        viewModelScope.launch {
            try { usuarioRepository.salvarPerfil(usuario) }
            catch (e: Exception) { Log.e("GruposVM", "Erro ao salvar perfil do usuário", e) }
        }
    }

    private fun observarGruposCompartilhados(uid: String?) {
        uid ?: return // Modo convidado ("entrar sem conta") não tem grupos remotos
        gruposCompartilhadosJob = viewModelScope.launch {
            try {
                grupoRemotoRepository.observarMeusGrupos(uid).collect { todos ->
                    _papeisGrupos.value = todos.associate { it.id to it.papelDe(uid) }
                    val todosUids = todos.flatMap { listOf(it.donoId) + it.editoresIds + it.membrosIds }.distinct()
                    if (todosUids.isNotEmpty()) {
                        _perfisMembros.value = usuarioRepository.buscarPerfis(todosUids)
                    }
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                Log.e("GruposVM", "Erro ao observar grupos compartilhados", e)
            }
        }
    }

    fun abrirDialogoEntrarComCodigo() {
        _erroEntrarComCodigo.value = null
        _convitePendente.value = null
        _mostrarDialogoEntrarComCodigo.value = true
    }

    fun fecharDialogoEntrarComCodigo() {
        _mostrarDialogoEntrarComCodigo.value = false
        _convitePendente.value = null
        _erroEntrarComCodigo.value = null
    }

    fun buscarGrupoPorCodigo(codigo: String) {
        if (codigo.isBlank()) {
            _erroEntrarComCodigo.value = "Digite um código"
            return
        }
        val uid = uidAtual
        if (uid == null) {
            _erroEntrarComCodigo.value = "Entre com uma conta para participar de um grupo de outra pessoa"
            return
        }
        viewModelScope.launch {
            _carregandoEntrarComCodigo.value = true
            _erroEntrarComCodigo.value = null
            try {
                val grupo = grupoRemotoRepository.buscarPorCodigo(codigo)
                if (grupo == null) {
                    _erroEntrarComCodigo.value = "Código não encontrado. Confira e tente de novo."
                } else if (grupo.membrosIds.contains(uid)) {
                    _erroEntrarComCodigo.value = "Você já participa desse grupo"
                } else {
                    _convitePendente.value = grupo
                }
            } catch (e: Exception) {
                Log.e("GruposVM", "Erro ao buscar grupo por código", e)
                _erroEntrarComCodigo.value = "Não foi possível buscar o código. Tente novamente."
            } finally {
                _carregandoEntrarComCodigo.value = false
            }
        }
    }

    fun confirmarEntrarNoGrupo() {
        val grupo = _convitePendente.value ?: return
        val uid = uidAtual ?: return
        viewModelScope.launch {
            _carregandoEntrarComCodigo.value = true
            try {
                grupoRemotoRepository.entrarNoGrupo(grupo.id, uid)
                criarEspelhoLocalSeNecessario(grupo, uid)
                fecharDialogoEntrarComCodigo()
            } catch (e: Exception) {
                Log.e("GruposVM", "Erro ao entrar no grupo", e)
                _erroEntrarComCodigo.value = "Não foi possível entrar no grupo. Tente novamente."
            } finally {
                _carregandoEntrarComCodigo.value = false
            }
        }
    }

    // Dá a quem entrou por código um grupoId local de verdade, senão a pessoa nunca consegue abrir
    // a tela do grupo (era exatamente o gap: entrar só registrava o membro no Firestore, sem jeito
    // de ver o grupo por dentro). Idempotente: não duplica se a pessoa sair e entrar de novo.
    private suspend fun criarEspelhoLocalSeNecessario(grupoRemoto: GrupoRemoto, uid: String) {
        if (grupoPeladaRepository.getGrupoPorFirestoreId(grupoRemoto.id) != null) return
        grupoPeladaRepository.inserirGrupo(
            GrupoPelada(
                nome = grupoRemoto.nome,
                local = grupoRemoto.local,
                horario = grupoRemoto.horario,
                descricao = grupoRemoto.descricao,
                usuarioId = uid,
                compartilhado = true,
                firestoreId = grupoRemoto.id,
                tipoRecorrencia = runCatching { TipoRecorrencia.valueOf(grupoRemoto.tipoRecorrencia) }
                    .getOrDefault(TipoRecorrencia.ESPORADICA),
                diaSemana = grupoRemoto.diaSemana?.let { runCatching { DiaSemana.valueOf(it) }.getOrNull() },
                diasSemana = grupoRemoto.diasSemana.mapNotNull { runCatching { DiaSemana.valueOf(it) }.getOrNull() }
            )
        )
    }

    fun sairDeGrupoCompartilhado(grupo: GrupoRemoto) {
        val uid = uidAtual ?: return
        viewModelScope.launch {
            try { grupoRemotoRepository.sairDoGrupo(grupo.id, uid) }
            catch (e: Exception) { Log.e("GruposVM", "Erro ao sair do grupo", e) }
        }
    }

    // Usado a partir do card de um grupo já espelhado localmente (comunidade) — sai do grupo no
    // Firestore e remove o espelho local (se a pessoa entrar de novo depois,
    // criarEspelhoLocalSeNecessario cria uma linha nova, sem duplicar nada agora).
    fun sairDeGrupoCompartilhadoPorId(firestoreId: String) {
        val uid = uidAtual ?: return
        viewModelScope.launch {
            try {
                grupoRemotoRepository.sairDoGrupo(firestoreId, uid)
                grupoPeladaRepository.getGrupoPorFirestoreId(firestoreId)?.let {
                    grupoPeladaRepository.deletarGrupo(it)
                }
                carregarGrupos()
            } catch (e: Exception) {
                Log.e("GruposVM", "Erro ao sair do grupo", e)
            }
        }
    }

    /**
     * Abre o convite de um grupo local (do dono). Se o grupo ainda não foi compartilhado
     * (firestoreId nulo), cria o documento no Firestore agora ("compartilhar sob demanda").
     */
    fun abrirConvite(grupoLocal: GrupoPelada) {
        val uid = uidAtual
        if (uid == null) {
            Log.w("GruposVM", "Tentativa de convidar sem estar logado")
            return
        }
        viewModelScope.launch {
            _carregandoConvite.value = true
            try {
                val remoto = if (grupoLocal.firestoreId != null) {
                    grupoRemotoRepository.buscarPorId(grupoLocal.firestoreId)
                } else {
                    val donoNome = authRepository.usuarioAtual?.displayName?.takeIf { it.isNotBlank() }
                        ?: authRepository.usuarioAtual?.email ?: "Você"
                    val criado = grupoRemotoRepository.criarGrupoCompartilhado(
                        donoId = uid,
                        donoNome = donoNome,
                        nome = grupoLocal.nome,
                        local = grupoLocal.local,
                        horario = grupoLocal.horario,
                        descricao = grupoLocal.descricao,
                        imagemUrl = grupoLocal.imagemUrl,
                        tipoRecorrencia = grupoLocal.tipoRecorrencia.name,
                        diaSemana = grupoLocal.diaSemana?.name,
                        diasSemana = grupoLocal.diasSemana.map { it.name }
                    )
                    grupoPeladaRepository.atualizarGrupo(grupoLocal.copy(firestoreId = criado.id, usuarioId = uid, compartilhado = true))
                    carregarGrupos()
                    criado
                }
                _grupoParaConvidar.value = remoto
            } catch (e: Exception) {
                Log.e("GruposVM", "Erro ao gerar convite", e)
            } finally {
                _carregandoConvite.value = false
            }
        }
    }

    fun fecharConvite() { _grupoParaConvidar.value = null }

    fun abrirGerenciarMembros(grupo: GrupoRemoto) { _grupoParaGerenciarMembros.value = grupo }
    fun fecharGerenciarMembros() { _grupoParaGerenciarMembros.value = null }

    fun promoverParaEditor(grupo: GrupoRemoto, uidMembro: String) {
        viewModelScope.launch {
            try { grupoRemotoRepository.promoverParaEditor(grupo.id, uidMembro) }
            catch (e: Exception) { Log.e("GruposVM", "Erro ao promover editor", e) }
        }
    }

    fun removerEditor(grupo: GrupoRemoto, uidMembro: String) {
        viewModelScope.launch {
            try { grupoRemotoRepository.removerEditor(grupo.id, uidMembro) }
            catch (e: Exception) { Log.e("GruposVM", "Erro ao remover editor", e) }
        }
    }

    fun removerMembro(grupo: GrupoRemoto, uidMembro: String) {
        viewModelScope.launch {
            try { grupoRemotoRepository.removerMembro(grupo.id, uidMembro) }
            catch (e: Exception) { Log.e("GruposVM", "Erro ao remover membro", e) }
        }
    }

    fun atualizarPermiteConviteDeMembros(grupo: GrupoRemoto, permite: Boolean) {
        viewModelScope.launch {
            try { grupoRemotoRepository.atualizarPermiteConviteDeMembros(grupo.id, permite) }
            catch (e: Exception) { Log.e("GruposVM", "Erro ao atualizar permissão de convite", e) }
        }
    }

    fun carregarGrupos() {
        gruposJob?.cancel()
        gruposJob = viewModelScope.launch {
            _carregando.value = true
            try {
                val sessaoConvidado = convidadoSessaoId
                val sessaoUsuario = usuarioUidSessao
                if (sessaoConvidado == null && sessaoUsuario == null) {
                    _grupos.value = emptyList()
                    _jogadoresPorGrupo.value = emptyMap()
                    _carregando.value = false
                    return@launch
                }

                val fluxoGrupos = sessaoConvidado?.let { grupoPeladaRepository.getGruposPorUsuario(it) }
                    ?: grupoPeladaRepository.getTodosGrupos()
                fluxoGrupos.collect { gruposLista ->
                    _grupos.value = gruposLista
                    atualizarJogadoresPorGrupo()
                    _carregando.value = false
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                Log.e("GruposVM", "Erro ao carregar grupos", e)
                _carregando.value = false
            }
        }
    }

    private fun atualizarJogadoresPorGrupo() {
        viewModelScope.launch {
            val mapaContagem = mutableMapOf<Long, Int>()
            _grupos.value.forEach { grupo ->
                mapaContagem[grupo.id] = jogadorRepository.countJogadoresAtivosPorGrupo(grupo.id)
            }
            _jogadoresPorGrupo.value = mapaContagem
            _grupoSelecionadoParaSorteioRapido.value?.let { grupoAtualDialogo ->
                if (_mostrarDialogoConfigSorteioRapido.value && !_isSorteioDeListaColada.value) { // Só atualiza se não for lista colada
                    _jogadoresAtivosParaDialogoSorteioRapido.value = _jogadoresPorGrupo.value[grupoAtualDialogo.id] ?: 0
                    validarConfiguracaoSorteioRapido()
                }
            }
        }
    }

    fun alterarTipoVisualizacao(tipo: TipoVisualizacao) { _tipoVisualizacao.value = tipo }
    fun mostrarDialogoCriarGrupo() { _grupoEmEdicao.value = null; _mostrarDialogoGrupo.value = true }
    fun mostrarDialogoEditarGrupo(grupo: GrupoPelada) { _grupoEmEdicao.value = grupo; _mostrarDialogoGrupo.value = true }
    fun fecharDialogoGrupo() {
        _mostrarDialogoGrupo.value = false
        _grupoEmEdicao.value = null
        localSelecionadoLatitude = null
        localSelecionadoLongitude = null
        localSelecionadoEndereco = null
        localSelecionadoNome = null
    }

    fun salvarGrupo(
        nome: String, local: String, horario: String, imagemUrl: String?, descricao: String?,
        tipoRecorrencia: TipoRecorrencia, diaSemana: DiaSemana?, latitude: Double?, longitude: Double?,
        endereco: String?, localNome: String?, diasSemana: List<DiaSemana> = emptyList()
    ) {
        viewModelScope.launch {
            try {
                val grupoAtual = _grupoEmEdicao.value
                val diasSemanaFinal = if (tipoRecorrencia == TipoRecorrencia.RECORRENTE) {
                    if (diasSemana.isEmpty() && diaSemana != null) listOf(diaSemana) else diasSemana
                } else {
                    if (diaSemana != null) listOf(diaSemana) else emptyList()
                }
                val grupoSalvar = grupoAtual?.copy(
                    nome = nome, local = local, horario = horario, imagemUrl = imagemUrl, descricao = descricao,
                    tipoRecorrencia = tipoRecorrencia, diaSemana = diaSemana ?: DiaSemana.DOMINGO,
                    diasSemana = diasSemanaFinal, latitude = latitude, longitude = longitude, endereco = endereco,
                    localNome = localNome, ultimaModificacao = System.currentTimeMillis()
                ) ?: GrupoPelada(
                    id = 0, nome = nome, local = local, horario = horario, imagemUrl = imagemUrl, descricao = descricao,
                    tipoRecorrencia = tipoRecorrencia, diaSemana = diaSemana ?: DiaSemana.DOMINGO,
                    diasSemana = diasSemanaFinal, latitude = latitude, longitude = longitude, endereco = endereco,
                    localNome = localNome, dataCriacao = System.currentTimeMillis(), ultimaModificacao = System.currentTimeMillis(),
                    ativo = true,
                    jogadoresIds = emptyList(),
                    usuarioId = convidadoSessaoId ?: uidAtual ?: "local",
                    compartilhado = false
                )
                if (grupoAtual != null) grupoPeladaRepository.atualizarGrupo(grupoSalvar) else grupoPeladaRepository.inserirGrupo(grupoSalvar)
                fecharDialogoGrupo()
                kotlinx.coroutines.delay(200) 
                carregarGrupos()
            } catch (e: Exception) { Log.e("GruposVM", "Erro ao salvar grupo", e) }
        }
    }

    fun excluirGrupo(grupo: GrupoPelada) {
        viewModelScope.launch {
            try { grupoPeladaRepository.excluirGrupo(grupo); carregarGrupos() }
            catch (e: Exception) { Log.e("GruposVM", "Erro ao excluir grupo", e) }
        }
    }

    fun mostrarSeletorMapas() { _mostrarSeletorMapas.value = true }
    fun ocultarSeletorMapas() { _mostrarSeletorMapas.value = false }
    fun processarLocalSelecionado(latitude: Double, longitude: Double, endereco: String, nome: String?) {
        localSelecionadoLatitude = latitude; localSelecionadoLongitude = longitude; localSelecionadoEndereco = endereco; localSelecionadoNome = nome
        _retornandoDoMapa.value = true; _mostrarSeletorMapas.value = false; _mostrarDialogoGrupo.value = true
    }
    fun cancelarSelecaoLocalizacao() { _mostrarSeletorMapas.value = false; _mostrarDialogoGrupo.value = true }
    fun resetRetornoMapa() { _retornandoDoMapa.value = false }
    fun obterDadosLocalizacao(): Map<String, Any?> = mapOf("latitude" to localSelecionadoLatitude, "longitude" to localSelecionadoLongitude, "endereco" to localSelecionadoEndereco, "nomeLocal" to localSelecionadoNome)
    fun salvarLocalizacaoSelecionada(latitude: Double, longitude: Double, endereco: String, nome: String?) {
        localSelecionadoLatitude = latitude; localSelecionadoLongitude = longitude; localSelecionadoEndereco = endereco; localSelecionadoNome = nome
        _retornandoDoMapa.value = true; _mostrarSeletorMapas.value = false
    }
    fun exibirTelaCompartilhamento(grupo: GrupoPelada) { _grupoParaCompartilhar.value = grupo; _mostrarTelaCompartilhamento.value = true }
    fun ocultarTelaCompartilhamento() { _mostrarTelaCompartilhamento.value = false; _grupoParaCompartilhar.value = null }

    // --- Funções para Sorteio Rápido e Sorteio de Lista Colada ---

    fun onAbrirDialogoSorteioRapido(grupo: GrupoPelada) {
        _isSorteioDeListaColada.value = false // Garante que é um sorteio de grupo
        _grupoSelecionadoParaSorteioRapido.value = grupo
        _erroSorteioRapido.value = null
        configuracaoRepository.setGrupoId(grupo.id) // Configura o contexto do grupo para buscar perfis
        viewModelScope.launch {
            try {
                _jogadoresAtivosParaDialogoSorteioRapido.value = jogadorRepository.countJogadoresAtivosPorGrupo(grupo.id)
                val perfis = configuracaoRepository.getTodasConfiguracoes().firstOrNull() ?: emptyList()
                _perfisConfiguracaoSorteio.value = perfis
                val perfilPadrao = perfis.firstOrNull { p -> p.isPadrao } ?: perfis.firstOrNull()
                _perfilConfigSelecionadoSorteioRapido.value = perfilPadrao
                _usarPerfilExistenteSorteioRapido.value = perfilPadrao != null
                if (perfilPadrao != null) {
                    _jogadoresPorTimeSorteioRapido.value = perfilPadrao.qtdJogadoresPorTime
                    _numeroDeTimesSorteioRapido.value = perfilPadrao.qtdTimes
                } else {
                    _jogadoresPorTimeSorteioRapido.value = 5 // Valor padrão
                    _numeroDeTimesSorteioRapido.value = 2 // Valor padrão
                }
                validarConfiguracaoSorteioRapido()
            } catch (e: Exception) {
                Log.e("GruposVM", "Erro ao carregar dados para sorteio rápido de grupo", e)
                // Resetar estados em caso de erro
            }
        }
        _mostrarDialogoConfigSorteioRapido.value = true
    }

    fun parsearListaDeJogadores(textoLista: String): List<String> {
        val nomesExtraidos = mutableListOf<String>()
        val linhas = textoLista.lines()
        var processarLinhas = true

        for (linha in linhas) {
            val linhaTrimada = linha.trim()
            if (linhaTrimada.equals("LISTA DE ESPERA:", ignoreCase = true)) {
                processarLinhas = false
            }
            if (!processarLinhas) continue

            val regex = Regex("^\\s*\\d+\\s*-\\s*(.+)")
            val match = regex.find(linhaTrimada)
            if (match != null) {
                val nome = match.groupValues[1].trim()
                if (nome.isNotBlank()) {
                    nomesExtraidos.add(nome)
                }
            }
        }
        Log.d("GruposVM", "Nomes parseados: $nomesExtraidos")
        return nomesExtraidos
    }

    fun prepararSorteioDeListaColada(nomes: List<String>) {
        if (nomes.isEmpty()) {
            _erroSorteioRapido.value = "Nenhum nome válido encontrado na lista."
            return
        }
        _isSorteioDeListaColada.value = true
        _listaJogadoresColados.value = nomes.mapIndexed { index, nome ->
            Jogador(
                id = (index + 1).toLong() * -1, // IDs negativos para temporários
                nome = nome,
                ativo = true,
                grupoId = -2L, // ID de grupo dummy para jogadores de lista colada
                posicaoPrincipal = PosicaoJogador.MEIO_CAMPO, // Valor padrão
                posicaoSecundaria = null, // Valor padrão
                notaPosicaoPrincipal = 3, // Valor padrão (escala 1-5)
                notaPosicaoSecundaria = null, // Valor padrão
                disponivel = true // Default para jogadores da lista
            )
        }
        _grupoSelecionadoParaSorteioRapido.value = GrupoPelada(id = -2L, nome = "Jogadores da Lista Colada", local = "N/A", horario = "N/A") // Grupo Dummy
        _jogadoresAtivosParaDialogoSorteioRapido.value = nomes.size

        // Resetar configurações de perfil para sorteio de lista colada, forçando configuração manual
        _perfisConfiguracaoSorteio.value = emptyList() // Sem perfis para lista colada
        _perfilConfigSelecionadoSorteioRapido.value = null
        _usarPerfilExistenteSorteioRapido.value = false // Força configuração manual
        _jogadoresPorTimeSorteioRapido.value = 5 // Reset para padrão
        _numeroDeTimesSorteioRapido.value =
            1.coerceAtLeast(nomes.size / 5) // Sugestão inicial de times

        limparErroSorteioRapido()
        validarConfiguracaoSorteioRapido() // Valida com a nova contagem e config
        _mostrarDialogoConfigSorteioRapido.value = true
    }

    private fun validarConfiguracaoSorteioRapido() {
        val jogadoresDisponiveis = _jogadoresAtivosParaDialogoSorteioRapido.value
        val jogadoresPorTimeConfig: Int
        val numeroDeTimesConfig: Int

        if (_usarPerfilExistenteSorteioRapido.value && !_isSorteioDeListaColada.value) { // Perfis só para grupos normais
            val perfil = _perfilConfigSelecionadoSorteioRapido.value
            if (perfil == null && _perfisConfiguracaoSorteio.value.isNotEmpty()) {
                _erroSorteioRapido.value = "Selecione um perfil de configuração."
                _podeRealizarSorteioRapido.value = false
                return
            } else if (perfil == null && _perfisConfiguracaoSorteio.value.isEmpty()) {
                 _erroSorteioRapido.value = "Nenhum perfil disponível. Configure manualmente."
                 _podeRealizarSorteioRapido.value = false
                 return
            }
            jogadoresPorTimeConfig = perfil?.qtdJogadoresPorTime ?: _jogadoresPorTimeSorteioRapido.value
            numeroDeTimesConfig = perfil?.qtdTimes ?: _numeroDeTimesSorteioRapido.value
        } else {
            jogadoresPorTimeConfig = _jogadoresPorTimeSorteioRapido.value
            numeroDeTimesConfig = _numeroDeTimesSorteioRapido.value
        }

        if (jogadoresPorTimeConfig <= 0) {
            _erroSorteioRapido.value = "Jogadores por time deve ser > 0."
            _podeRealizarSorteioRapido.value = false
            return
        }
        if (numeroDeTimesConfig <= 0) {
            _erroSorteioRapido.value = "Número de times deve ser > 0."
            _podeRealizarSorteioRapido.value = false
            return
        }

        val jogadoresNecessarios = jogadoresPorTimeConfig * numeroDeTimesConfig
        if (jogadoresNecessarios <= 0) {
             _erroSorteioRapido.value = "Configuração de sorteio inválida."
             _podeRealizarSorteioRapido.value = false
             return
        }

        if (jogadoresDisponiveis < jogadoresNecessarios) {
            _erroSorteioRapido.value = "Necessários: $jogadoresNecessarios (${numeroDeTimesConfig}x${jogadoresPorTimeConfig}). Lista tem: $jogadoresDisponiveis."
            _podeRealizarSorteioRapido.value = false
        } else if (jogadoresDisponiveis > jogadoresNecessarios) {
            val sobram = jogadoresDisponiveis - jogadoresNecessarios
            _erroSorteioRapido.value = "Lista: $jogadoresDisponiveis. Sorteio: $jogadoresNecessarios (${numeroDeTimesConfig}x${jogadoresPorTimeConfig}). ${sobram} ficarão de fora."
            _podeRealizarSorteioRapido.value = true
        } else {
            _erroSorteioRapido.value = null
            _podeRealizarSorteioRapido.value = true
        }
    }

    fun onFecharDialogoSorteioRapido() {
        _mostrarDialogoConfigSorteioRapido.value = false
        if (_isSorteioDeListaColada.value) { // Reset específico para lista colada
            _isSorteioDeListaColada.value = false
            _listaJogadoresColados.value = emptyList()
            _grupoSelecionadoParaSorteioRapido.value = null // Limpa grupo dummy
        }
    }

    fun onUsarPerfilExistenteSorteioRapidoChanged(usar: Boolean) {
        if (_isSorteioDeListaColada.value) { // Não permitir uso de perfil para lista colada
            _usarPerfilExistenteSorteioRapido.value = false
            _perfilConfigSelecionadoSorteioRapido.value = null
        } else {
            _usarPerfilExistenteSorteioRapido.value = usar
            if (usar) {
                val perfilPadrao = _perfisConfiguracaoSorteio.value.firstOrNull { p -> p.isPadrao } ?: _perfisConfiguracaoSorteio.value.firstOrNull()
                _perfilConfigSelecionadoSorteioRapido.value = perfilPadrao
            } else {
                _perfilConfigSelecionadoSorteioRapido.value = null
            }
        }
        limparErroSorteioRapido()
        validarConfiguracaoSorteioRapido()
    }

    fun onPerfilConfigSorteioRapidoSelecionado(perfil: ConfiguracaoSorteio) {
        if (_isSorteioDeListaColada.value) return // Não aplicável para lista colada
        _perfilConfigSelecionadoSorteioRapido.value = perfil
        limparErroSorteioRapido()
        validarConfiguracaoSorteioRapido()
    }

    fun onJogadoresPorTimeSorteioRapidoChanged(novoValor: Int) {
        _jogadoresPorTimeSorteioRapido.value = novoValor
        limparErroSorteioRapido()
        validarConfiguracaoSorteioRapido()
    }

    fun onNumeroDeTimesSorteioRapidoChanged(novoValor: Int) {
        _numeroDeTimesSorteioRapido.value = novoValor
        limparErroSorteioRapido()
        validarConfiguracaoSorteioRapido()
    }

    fun limparErroSorteioRapido() {
        _erroSorteioRapido.value = null
    }

    fun onConfirmarSorteioRapido() {
        if (!_podeRealizarSorteioRapido.value) {
            if (_erroSorteioRapido.value.isNullOrBlank()) {
                 _erroSorteioRapido.value = "Configuração inválida ou jogadores insuficientes."
            }
            return
        }

        val grupoDummyOuReal = _grupoSelecionadoParaSorteioRapido.value
        if (grupoDummyOuReal == null) {
            _erroSorteioRapido.value = "Nenhum grupo ou lista base selecionado."
            return
        }

        viewModelScope.launch {
            try {
                val jogadoresParaSorteio: List<Jogador>
                if (_isSorteioDeListaColada.value) {
                    jogadoresParaSorteio = _listaJogadoresColados.value
                    if (jogadoresParaSorteio.isEmpty()) {
                        _erroSorteioRapido.value = "Não há jogadores na lista colada para o sorteio."
                        return@launch
                    }
                } else {
                    jogadoresParaSorteio = jogadorRepository.getJogadoresListAtivosPorGrupo(grupoDummyOuReal.id)
                    if (jogadoresParaSorteio.isEmpty()) {
                        _erroSorteioRapido.value = "Não há jogadores ativos no grupo para o sorteio."
                        return@launch
                    }
                }

                val configSorteioUsada: ConfiguracaoSorteio
                if (_usarPerfilExistenteSorteioRapido.value && _perfilConfigSelecionadoSorteioRapido.value != null && !_isSorteioDeListaColada.value) {
                    configSorteioUsada = _perfilConfigSelecionadoSorteioRapido.value!!
                } else {
                    configSorteioUsada = ConfiguracaoSorteio(
                        id = 0L, 
                        nome = if (_isSorteioDeListaColada.value) "Sorteio Lista Colada" else "Sorteio Rápido Manual",
                        qtdJogadoresPorTime = _jogadoresPorTimeSorteioRapido.value,
                        qtdTimes = _numeroDeTimesSorteioRapido.value,
                        aleatorio = true, 
                        isPadrao = false,
                        grupoId = if (_isSorteioDeListaColada.value) -2L else grupoDummyOuReal.id // ID dummy para lista colada
                    )
                }

                val jogadoresNecessarios = configSorteioUsada.qtdJogadoresPorTime * configSorteioUsada.qtdTimes
                if (jogadoresParaSorteio.size < jogadoresNecessarios) {
                    _erroSorteioRapido.value = "Jogadores insuficientes. Necessários: $jogadoresNecessarios, Disponíveis: ${jogadoresParaSorteio.size}."
                    return@launch
                }

                Log.d("GruposVM", "SORTEANDO (Lista Colada: ${_isSorteioDeListaColada.value}) com ${jogadoresParaSorteio.size} jogadores, ${configSorteioUsada.qtdTimes} times de ${configSorteioUsada.qtdJogadoresPorTime}")
                val jogadoresSorteaveis = jogadoresParaSorteio.shuffled()

                val resultadoSorteio: ResultadoSorteio? = sorteioUseCase.sortearTimesRapido(
                    jogadores = jogadoresSorteaveis, 
                    configuracao = configSorteioUsada, 
                    numeroTimesPrincipais = configSorteioUsada.qtdTimes
                )

                if (resultadoSorteio != null && resultadoSorteio.times.isNotEmpty()) {
                    sorteioRepository.salvarResultadoSorteioRapido(resultadoSorteio) // Salva como sorteio rápido
                    _navegarParaResultadoSorteio.value = true
                    onFecharDialogoSorteioRapido() // Isso também vai resetar _isSorteioDeListaColada
                } else {
                    _erroSorteioRapido.value = "Falha ao formar times. Verifique a configuração e jogadores."
                }

            } catch (e: Exception) {
                Log.e("GruposVM", "Erro ao confirmar sorteio", e)
                _erroSorteioRapido.value = "Erro inesperado: ${e.localizedMessage}"
            }
        }
    }

    fun onNavegacaoParaResultadoSorteioRealizada() {
        _navegarParaResultadoSorteio.value = null
    }
}
