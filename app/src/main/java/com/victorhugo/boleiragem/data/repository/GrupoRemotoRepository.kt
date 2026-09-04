package com.victorhugo.boleiragem.data.repository

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.victorhugo.boleiragem.data.model.GrupoRemoto
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.random.Random

@Singleton
class GrupoRemotoRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    private val grupos get() = firestore.collection("grupos")

    /** Grupos onde o usuário é dono ou membro/editor, sincronizado em tempo real. */
    fun observarMeusGrupos(uid: String): Flow<List<GrupoRemoto>> = callbackFlow {
        val registro = grupos
            .whereArrayContains("membrosIds", uid)
            .addSnapshotListener { snapshot, erro ->
                if (erro != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val lista = snapshot?.toObjects(GrupoRemoto::class.java) ?: emptyList()
                trySend(lista)
            }
        awaitClose { registro.remove() }
    }

    /**
     * Cria (ou converte um grupo local existente em) um grupo compartilhado no Firestore.
     * @return o [GrupoRemoto] criado, já com o código de convite gerado.
     */
    suspend fun criarGrupoCompartilhado(
        donoId: String,
        donoNome: String,
        nome: String,
        local: String,
        horario: String,
        descricao: String?,
        imagemUrl: String?,
        tipoRecorrencia: String = "ESPORADICA",
        diaSemana: String? = null,
        diasSemana: List<String> = emptyList()
    ): GrupoRemoto {
        val codigo = gerarCodigoConvite()
        val grupo = GrupoRemoto(
            nome = nome,
            local = local,
            horario = horario,
            descricao = descricao,
            imagemUrl = imagemUrl,
            donoId = donoId,
            donoNome = donoNome,
            editoresIds = emptyList(),
            membrosIds = listOf(donoId),
            codigoConvite = codigo,
            permiteConviteDeMembros = false,
            tipoRecorrencia = tipoRecorrencia,
            diaSemana = diaSemana,
            diasSemana = diasSemana
        )
        val referencia = grupos.add(grupo).await()
        return grupo.copy(id = referencia.id)
    }

    suspend fun buscarPorId(grupoId: String): GrupoRemoto? {
        return grupos.document(grupoId).get().await().toObject(GrupoRemoto::class.java)
    }

    suspend fun buscarPorCodigo(codigo: String): GrupoRemoto? {
        val snapshot = grupos.whereEqualTo("codigoConvite", codigo.trim().uppercase()).limit(1).get().await()
        return snapshot.documents.firstOrNull()?.toObject(GrupoRemoto::class.java)
    }

    suspend fun entrarNoGrupo(grupoId: String, uid: String) {
        grupos.document(grupoId).update("membrosIds", FieldValue.arrayUnion(uid)).await()
    }

    suspend fun sairDoGrupo(grupoId: String, uid: String) {
        grupos.document(grupoId).update(
            mapOf(
                "membrosIds" to FieldValue.arrayRemove(uid),
                "editoresIds" to FieldValue.arrayRemove(uid)
            )
        ).await()
    }

    suspend fun promoverParaEditor(grupoId: String, uid: String) {
        grupos.document(grupoId).update("editoresIds", FieldValue.arrayUnion(uid)).await()
    }

    suspend fun removerEditor(grupoId: String, uid: String) {
        grupos.document(grupoId).update("editoresIds", FieldValue.arrayRemove(uid)).await()
    }

    suspend fun removerMembro(grupoId: String, uid: String) {
        sairDoGrupo(grupoId, uid)
    }

    suspend fun atualizarPermiteConviteDeMembros(grupoId: String, permite: Boolean) {
        grupos.document(grupoId).update("permiteConviteDeMembros", permite).await()
    }

    suspend fun atualizarDadosBasicos(
        grupoId: String,
        nome: String,
        local: String,
        horario: String,
        descricao: String?,
        imagemUrl: String?
    ) {
        grupos.document(grupoId).update(
            mapOf(
                "nome" to nome,
                "local" to local,
                "horario" to horario,
                "descricao" to descricao,
                "imagemUrl" to imagemUrl
            )
        ).await()
    }

    suspend fun excluirGrupo(grupoId: String) {
        grupos.document(grupoId).delete().await()
    }

    private fun gerarCodigoConvite(): String {
        val caracteres = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789" // sem O/0/I/1 pra evitar confusão visual
        return (1..6).map { caracteres[Random.nextInt(caracteres.length)] }.joinToString("")
    }
}
