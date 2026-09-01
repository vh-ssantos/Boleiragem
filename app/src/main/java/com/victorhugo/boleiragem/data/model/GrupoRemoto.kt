package com.victorhugo.boleiragem.data.model

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

/**
 * Papel de um usuário dentro de um grupo compartilhado.
 * DONO: criou o grupo, controle total, único que pode promover/remover editores.
 * EDITOR: designado pelo dono, mesmos direitos de edição de conteúdo (jogadores, sorteio, regras).
 * MEMBRO: entrou via convite, acesso somente leitura (vê jogadores, histórico, estatísticas).
 */
enum class PapelGrupo {
    DONO, EDITOR, MEMBRO
}

/**
 * Documento do grupo de pelada no Firestore (coleção "grupos").
 * Espelha o essencial de [GrupoPelada] + o controle de membros/convite.
 * A sincronização do conteúdo interno do grupo (jogadores, sorteios, histórico) é a Fase 2 — ainda não existe aqui.
 */
data class GrupoRemoto(
    @DocumentId
    val id: String = "",
    val nome: String = "",
    val local: String = "",
    val horario: String = "",
    val descricao: String? = null,
    val imagemUrl: String? = null,
    val donoId: String = "",
    val donoNome: String = "",
    val editoresIds: List<String> = emptyList(),
    val membrosIds: List<String> = emptyList(),
    val codigoConvite: String = "",
    val permiteConviteDeMembros: Boolean = false,
    @ServerTimestamp
    val criadoEm: Date? = null
) {
    fun papelDe(uid: String): PapelGrupo = when {
        uid == donoId -> PapelGrupo.DONO
        editoresIds.contains(uid) -> PapelGrupo.EDITOR
        else -> PapelGrupo.MEMBRO
    }

    fun podeEditar(uid: String): Boolean = uid == donoId || editoresIds.contains(uid)

    fun podeConvidar(uid: String): Boolean = podeEditar(uid) || permiteConviteDeMembros
}
