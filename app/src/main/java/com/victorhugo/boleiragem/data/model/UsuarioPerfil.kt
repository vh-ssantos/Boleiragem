package com.victorhugo.boleiragem.data.model

/**
 * Documento do usuário no Firestore (coleção "usuarios").
 * Existe só pra outros membros de um grupo conseguirem ver um nome em vez do UID cru.
 * Gravado/atualizado automaticamente a cada login (ver AuthRepository/UsuarioRepository).
 */
data class UsuarioPerfil(
    val uid: String = "",
    val nome: String = "",
    val email: String = "",
    // Adicionados na tela "Meu Perfil" (Fase 3) — nulos pra quem nunca preencheu.
    val posicaoFavorita: String? = null,
    val idade: Int? = null
)
