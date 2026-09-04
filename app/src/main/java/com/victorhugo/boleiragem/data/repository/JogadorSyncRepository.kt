package com.victorhugo.boleiragem.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.victorhugo.boleiragem.data.model.Jogador
import com.victorhugo.boleiragem.data.model.JogadorRemoto
import com.victorhugo.boleiragem.data.model.toRemoto
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Sincronização Fase 2 dos jogadores de um grupo compartilhado, subcoleção `grupos/{grupoFirestoreId}/jogadores`.
 * Espelha o padrão de [GrupoRemotoRepository]: leitura reativa via snapshot listener, escrita pontual via `.await()`.
 * Só é chamado quando o grupo já tem `firestoreId` (compartilhado) e há usuário autenticado — ver `JogadorRepository`.
 */
@Singleton
class JogadorSyncRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    private fun jogadores(grupoFirestoreId: String) =
        firestore.collection("grupos").document(grupoFirestoreId).collection("jogadores")

    fun observarJogadores(grupoFirestoreId: String): Flow<List<JogadorRemoto>> = callbackFlow {
        val registro = jogadores(grupoFirestoreId).addSnapshotListener { snapshot, erro ->
            if (erro != null) {
                trySend(emptyList())
                return@addSnapshotListener
            }
            trySend(snapshot?.toObjects(JogadorRemoto::class.java) ?: emptyList())
        }
        awaitClose { registro.remove() }
    }

    // Envia o jogador (cria o documento se ainda não tiver firestoreId) e retorna o id do documento remoto.
    suspend fun enviarJogador(grupoFirestoreId: String, jogador: Jogador): String {
        val colecao = jogadores(grupoFirestoreId)
        val documento = jogador.firestoreId?.let { colecao.document(it) } ?: colecao.document()
        documento.set(jogador.toRemoto().copy(id = documento.id)).await()
        return documento.id
    }

    suspend fun excluirJogador(grupoFirestoreId: String, firestoreId: String) {
        jogadores(grupoFirestoreId).document(firestoreId).delete().await()
    }
}
