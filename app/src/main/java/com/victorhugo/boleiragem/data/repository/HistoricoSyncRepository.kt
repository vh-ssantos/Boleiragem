package com.victorhugo.boleiragem.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.victorhugo.boleiragem.data.model.HistoricoPelada
import com.victorhugo.boleiragem.data.model.HistoricoPeladaRemoto
import com.victorhugo.boleiragem.data.model.toRemoto
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Sincronização Fase 2 do histórico de peladas finalizadas de um grupo compartilhado,
 * subcoleção `grupos/{grupoFirestoreId}/historico`. Cada documento é um snapshot imutável
 * (criado uma vez, nunca atualizado) — não há conflito de última escrita a resolver aqui.
 */
@Singleton
class HistoricoSyncRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    private fun historico(grupoFirestoreId: String) =
        firestore.collection("grupos").document(grupoFirestoreId).collection("historico")

    fun observarHistorico(grupoFirestoreId: String): Flow<List<HistoricoPeladaRemoto>> = callbackFlow {
        val registro = historico(grupoFirestoreId).addSnapshotListener { snapshot, erro ->
            if (erro != null) {
                trySend(emptyList())
                return@addSnapshotListener
            }
            trySend(snapshot?.toObjects(HistoricoPeladaRemoto::class.java) ?: emptyList())
        }
        awaitClose { registro.remove() }
    }

    suspend fun enviarPelada(grupoFirestoreId: String, pelada: HistoricoPelada): String {
        val colecao = historico(grupoFirestoreId)
        val documento = pelada.firestoreId?.let { colecao.document(it) } ?: colecao.document()
        documento.set(pelada.toRemoto().copy(id = documento.id)).await()
        return documento.id
    }
}
