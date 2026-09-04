package com.victorhugo.boleiragem.data.repository

import com.victorhugo.boleiragem.data.dao.ConfiguracaoDao
import com.victorhugo.boleiragem.data.model.ConfiguracaoSorteio
import com.victorhugo.boleiragem.data.model.toLocal
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ConfiguracaoRepository @Inject constructor(
    private val configuracaoDao: ConfiguracaoDao,
    private val grupoPeladaRepository: GrupoPeladaRepository,
    private val authRepository: AuthRepository,
    private val configuracaoSyncRepository: ConfiguracaoSyncRepository
) {
    // ID do grupo atual como um StateFlow
    private val _grupoIdAtualFlow = MutableStateFlow(0L)

    // Mutex para sincronização
    private val mutex = Mutex()

    // Cache para controlar se já criamos o perfil padrão para um grupo
    private val perfisPadraoCriados = mutableSetOf<Long>()

    // Método para definir o ID do grupo atual
    fun setGrupoId(id: Long) {
        _grupoIdAtualFlow.value = id
        // Pode ser útil limpar o cache de perfis padrão se a lógica exigir
        // perfisPadraoCriados.remove(id) // Descomente se necessário
    }

    // Obtém a configuração padrão atual para o grupo atual de forma reativa
    fun getConfiguracao(): Flow<ConfiguracaoSorteio> =
        _grupoIdAtualFlow.flatMapLatest { currentGroupId ->
            configuracaoDao.getConfiguracaoPadrao(currentGroupId).map {
                it ?: criarConfiguracaoPadrao(currentGroupId)
            }
        }

    // Obtém todos os perfis de configuração do grupo atual de forma reativa
    fun getTodasConfiguracoes(): Flow<List<ConfiguracaoSorteio>> =
        _grupoIdAtualFlow.flatMapLatest { currentGroupId ->
            configuracaoDao.getTodasConfiguracoes(currentGroupId)
        }

    // Obtém uma configuração específica pelo ID
    suspend fun getConfiguracaoById(id: Long): ConfiguracaoSorteio? =
        configuracaoDao.getConfiguracaoById(id)

    // Salva uma configuração (nova ou existente)
    suspend fun salvarConfiguracao(configuracao: ConfiguracaoSorteio): Long {
        // Garante que a configuração tem o grupoId atual
        val configComGrupoId = configuracao.copy(grupoId = _grupoIdAtualFlow.value)
        val id = configuracaoDao.salvarConfiguracao(configComGrupoId)
        sincronizar(configComGrupoId.copy(id = id))
        return id
    }

    // Push "melhor esforço" — só roda se o grupo já foi compartilhado (tem firestoreId) e há usuário autenticado.
    private suspend fun sincronizar(configuracao: ConfiguracaoSorteio) {
        try {
            val grupoFirestoreId = grupoPeladaRepository.getGrupoPorId(configuracao.grupoId)?.firestoreId ?: return
            if (authRepository.usuarioAtual == null) return
            val agora = System.currentTimeMillis()
            val configComTimestamp = configuracao.copy(atualizadoEm = agora)
            val firestoreId = configuracaoSyncRepository.enviarConfiguracaoSorteio(grupoFirestoreId, configComTimestamp)
            if (configuracao.firestoreId != firestoreId || configuracao.atualizadoEm != agora) {
                configuracaoDao.salvarConfiguracao(configComTimestamp.copy(firestoreId = firestoreId))
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // Pull: replica perfis de configuração de sorteio de outros dispositivos para o Room local.
    fun observarSincronizacaoRemota(grupoId: Long, grupoFirestoreId: String): Flow<Unit> =
        configuracaoSyncRepository.observarConfiguracoesSorteio(grupoFirestoreId).map { remotas ->
            remotas.forEach { remota ->
                if (remota.id.isBlank()) return@forEach
                val local = configuracaoDao.getPorFirestoreId(remota.id)
                when {
                    local == null -> configuracaoDao.salvarConfiguracao(remota.toLocal(grupoId))
                    remota.atualizadoEm > local.atualizadoEm ->
                        configuracaoDao.salvarConfiguracao(remota.toLocal(grupoId).copy(id = local.id))
                }
            }
        }

    // Remove uma configuração pelo ID
    suspend fun deletarConfiguracao(id: Long) {
        configuracaoDao.deletarConfiguracao(id)
    }

    // Define uma configuração como padrão
    suspend fun definirConfiguracaoPadrao(id: Long) {
        configuracaoDao.definirConfiguracaoPadrao(id, _grupoIdAtualFlow.value)
    }

    // Cria e salva uma configuração padrão se não existir nenhuma
    private suspend fun criarConfiguracaoPadrao(grupoId: Long): ConfiguracaoSorteio {
        // Usa mutex para garantir que apenas uma thread execute esse código por vez
        return mutex.withLock {
            // Primeiro verifica se já temos um perfil padrão no cache
            if (grupoId in perfisPadraoCriados) {
                // Busca novamente para obter o perfil que foi criado
                val perfilExistente = configuracaoDao.getConfiguracaoPadrao(grupoId).firstOrNull()
                if (perfilExistente != null) {
                    return@withLock perfilExistente
                }
            }

            // Busca para verificar se já existe algum perfil padrão
            val perfilPadrao = configuracaoDao.getConfiguracaoPadrao(grupoId).firstOrNull()
            if (perfilPadrao != null) {
                // Já existe um perfil padrão, adiciona ao cache e retorna
                perfisPadraoCriados.add(grupoId)
                return@withLock perfilPadrao
            }

            // Verifica se existem configurações não-padrão para este grupo
            val configuracoesExistentes = configuracaoDao.getTodasConfiguracoesSync(grupoId)
            if (configuracoesExistentes.isNotEmpty()) {
                // Usa a primeira configuração existente e marca como padrão
                val primeiraConfig = configuracoesExistentes.first()
                val configAtualizada = primeiraConfig.copy(isPadrao = true)
                configuracaoDao.salvarConfiguracao(configAtualizada)
                perfisPadraoCriados.add(grupoId)
                return@withLock configAtualizada
            }

            // Se não existir nenhuma configuração, cria uma nova
            val novaConfigPadrao = ConfiguracaoSorteio(
                nome = "Padrão",
                qtdJogadoresPorTime = 5,
                qtdTimes = 2,
                aleatorio = true,
                isPadrao = true,
                grupoId = grupoId
            )

            // Salva no banco de dados e adiciona ao cache
            configuracaoDao.salvarConfiguracao(novaConfigPadrao)
            perfisPadraoCriados.add(grupoId)
            novaConfigPadrao
        }
    }
}
