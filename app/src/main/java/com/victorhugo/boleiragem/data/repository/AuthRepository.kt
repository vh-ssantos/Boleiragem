package com.victorhugo.boleiragem.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val firebaseAuth: FirebaseAuth
) {
    val usuarioAtual: FirebaseUser?
        get() = firebaseAuth.currentUser

    val estadoAutenticacao: Flow<FirebaseUser?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            trySend(auth.currentUser)
        }
        firebaseAuth.addAuthStateListener(listener)
        awaitClose { firebaseAuth.removeAuthStateListener(listener) }
    }

    suspend fun entrarComEmailSenha(email: String, senha: String): Result<FirebaseUser> {
        return try {
            val resultado = firebaseAuth.signInWithEmailAndPassword(email, senha).await()
            val usuario = resultado.user ?: return Result.failure(IllegalStateException("Falha ao autenticar usuário"))
            Result.success(usuario)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun cadastrarComEmailSenha(email: String, senha: String): Result<FirebaseUser> {
        return try {
            val resultado = firebaseAuth.createUserWithEmailAndPassword(email, senha).await()
            val usuario = resultado.user ?: return Result.failure(IllegalStateException("Falha ao criar usuário"))
            Result.success(usuario)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun entrarComGoogle(idToken: String): Result<FirebaseUser> {
        return try {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            val resultado = firebaseAuth.signInWithCredential(credential).await()
            val usuario = resultado.user ?: return Result.failure(IllegalStateException("Falha ao autenticar usuário"))
            Result.success(usuario)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun enviarEmailRedefinicaoSenha(email: String): Result<Unit> {
        return try {
            firebaseAuth.sendPasswordResetEmail(email).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun sair() {
        firebaseAuth.signOut()
    }
}
