package com.victorhugo.boleiragem.data.repository

import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.victorhugo.boleiragem.data.model.UsuarioPerfil
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UsuarioRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    private val usuarios get() = firestore.collection("usuarios")

    suspend fun salvarPerfil(usuario: FirebaseUser) {
        val perfil = UsuarioPerfil(
            uid = usuario.uid,
            nome = usuario.displayName?.takeIf { it.isNotBlank() } ?: usuario.email?.substringBefore("@") ?: "Jogador",
            email = usuario.email ?: ""
        )
        try {
            usuarios.document(usuario.uid).set(perfil).await()
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
}
