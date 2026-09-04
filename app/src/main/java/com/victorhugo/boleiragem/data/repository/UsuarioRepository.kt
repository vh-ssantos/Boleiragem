package com.victorhugo.boleiragem.data.repository

import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.victorhugo.boleiragem.data.model.UsuarioPerfil
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UsuarioRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    private val usuarios get() = firestore.collection("usuarios")

    // Merge (não overwrite): só toca nome/email/uid. Antes usava .set(perfil) puro, que sobrescrevia
    // o documento inteiro a cada login e apagava posicaoFavorita/idade preenchidos na tela de perfil.
    suspend fun salvarPerfil(usuario: FirebaseUser) {
        val dados = mapOf(
            "uid" to usuario.uid,
            "nome" to (usuario.displayName?.takeIf { it.isNotBlank() } ?: usuario.email?.substringBefore("@") ?: "Jogador"),
            "email" to (usuario.email ?: "")
        )
        try {
            usuarios.document(usuario.uid).set(dados, SetOptions.merge()).await()
        } catch (_: Exception) {
            // Falha ao salvar perfil não deve travar o login; membros verão o UID cru como fallback
        }
    }

    suspend fun buscarPerfil(uid: String): UsuarioPerfil? {
        return try {
            usuarios.document(uid).get().await().toObject(UsuarioPerfil::class.java)
        } catch (_: Exception) {
            null
        }
    }

    suspend fun buscarPerfis(uids: List<String>): Map<String, UsuarioPerfil> {
        if (uids.isEmpty()) return emptyMap()
        return try {
            // Firestore limita whereIn a 30 itens; grupos não devem passar disso tão cedo
            usuarios.whereIn("uid", uids.take(30)).get().await()
                .toObjects(UsuarioPerfil::class.java)
                .associateBy { it.uid }
        } catch (_: Exception) {
            emptyMap()
        }
    }

    // Edição feita pelo próprio usuário na tela "Meu Perfil".
    suspend fun atualizarPerfil(uid: String, nome: String, posicaoFavorita: String?, idade: Int?): Boolean {
        val dados = mapOf(
            "nome" to nome,
            "posicaoFavorita" to posicaoFavorita,
            "idade" to idade
        )
        return try {
            usuarios.document(uid).set(dados, SetOptions.merge()).await()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
