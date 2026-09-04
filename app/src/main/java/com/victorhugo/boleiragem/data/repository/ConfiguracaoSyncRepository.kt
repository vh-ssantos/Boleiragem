package com.victorhugo.boleiragem.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.victorhugo.boleiragem.data.model.ConfiguracaoPontuacao
import com.victorhugo.boleiragem.data.model.ConfiguracaoPontuacaoRemoto
import com.victorhugo.boleiragem.data.model.ConfiguracaoSorteio
import com.victorhugo.boleiragem.data.model.ConfiguracaoSorteioRemoto
import com.victorhugo.boleiragem.data.model.toRemoto
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Sincronização Fase 2 das configurações de um grupo compartilhado — sorteio (perfis, subcoleção
 * `configuracaoSorteio`) e pontuação (documento único, subcoleção `configuracaoPontuacao`).
 * Mesmo padrão de [GrupoRemotoRepository]: snapshot listener pra leitura, `.await()` pra escrita.
 */
@Singleton
class ConfiguracaoSyncRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    private fun configuracoesSorteio(grupoFirestoreId: String) =
        firestore.collection("grupos").document(grupoFirestoreId).collection("configuracaoSorteio")

    private fun configuracoesPontuacao(grupoFirestoreId: String) =
        firestore.collection("grupos").document(grupoFirestoreId).collection("configuracaoPontuacao")

    fun observarConfiguracoesSorteio(grupoFirestoreId: String): Flow<List<ConfiguracaoSorteioRemoto>> = callbackFlow {
        val registro = configuracoesSorteio(grupoFirestoreId).addSnapshotListener { snapshot, erro ->
            if (erro != null) {
                trySend(emptyList())
                return@addSnapshotListener
            }
            trySend(snapshot?.toObjects(ConfiguracaoSorteioRemoto::class.java) ?: emptyList())
        }
        awaitClose { registro.remove() }
    }

    suspend fun enviarConfiguracaoSorteio(grupoFirestoreId: String, configuracao: ConfiguracaoSorteio): String {
        val colecao = configuracoesSorteio(grupoFirestoreId)
        val documento = configuracao.firestoreId?.let { colecao.document(it) } ?: colecao.document()
        documento.set(configuracao.toRemoto().copy(id = documento.id)).await()
        return documento.id
    }

    fun observarConfiguracaoPontuacao(grupoFirestoreId: String): Flow<List<ConfiguracaoPontuacaoRemoto>> = callbackFlow {
        val registro = configuracoesPontuacao(grupoFirestoreId).addSnapshotListener { snapshot, erro ->
            if (erro != null) {
                trySend(emptyList())
                return@addSnapshotListener
            }
            trySend(snapshot?.toObjects(ConfiguracaoPontuacaoRemoto::class.java) ?: emptyList())
        }
        awaitClose { registro.remove() }
    }

    suspend fun enviarConfiguracaoPontuacao(grupoFirestoreId: String, configuracao: ConfiguracaoPontuacao): String {
        // Documento único por grupo — sempre o mesmo id fixo "unica", não precisamos gerar um novo.
        val documento = configuracoesPontuacao(grupoFirestoreId).document(configuracao.firestoreId ?: "unica")
        documento.set(configuracao.toRemoto().copy(id = documento.id)).await()
        return documento.id
    }
}
