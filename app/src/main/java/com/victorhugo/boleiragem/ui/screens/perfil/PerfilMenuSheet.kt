package com.victorhugo.boleiragem.ui.screens.perfil

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import com.victorhugo.boleiragem.ui.common.EmptyState

private enum class PerfilTelaId { PERFIL, HISTORICO, GRUPOS }

/**
 * Avatar clicável para o TopAppBar (padrão Nubank) — acessível tanto em "Minhas Peladas" quanto
 * dentro de um grupo. Ao tocar, abre um bottom sheet que é só um MENU (ícone + texto + chevron),
 * cada item levando a uma tela cheia própria — nada de formulário embutido direto no sheet.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PerfilAvatarButton(
    viewModel: PerfilViewModel = hiltViewModel(),
    onSairClick: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var mostrarMenu by remember { mutableStateOf(false) }
    var telaAberta by remember { mutableStateOf<PerfilTelaId?>(null) }
    val sheetState = rememberModalBottomSheetState()

    IconButton(onClick = { mostrarMenu = true }) {
        AvatarIniciais(nome = uiState.nome, tamanho = 32.dp)
    }

    if (mostrarMenu) {
        ModalBottomSheet(
            onDismissRequest = { mostrarMenu = false },
            sheetState = sheetState
        ) {
            PerfilMenuConteudo(
                uiState = uiState,
                onItemClick = { tela ->
                    mostrarMenu = false
                    telaAberta = tela
                },
                onRedefinirSenhaClick = viewModel::redefinirSenha,
                onSairClick = {
                    Log.d("PerfilMenu", "Usuário tocou em sair pelo menu de perfil")
                    mostrarMenu = false
                    onSairClick()
                },
                onFecharClick = { mostrarMenu = false }
            )
        }
    }

    when (telaAberta) {
        PerfilTelaId.PERFIL -> TelaCheiaDialog(onDismiss = { telaAberta = null }) {
            TelaMeuPerfil(
                uiState = uiState,
                onNomeChange = viewModel::onNomeChange,
                onPosicaoChange = viewModel::onPosicaoChange,
                onIdadeChange = viewModel::onIdadeChange,
                onSalvarClick = viewModel::salvar,
                onBackClick = { telaAberta = null }
            )
        }
        PerfilTelaId.HISTORICO -> TelaCheiaDialog(onDismiss = { telaAberta = null }) {
            TelaMeuHistorico(
                usuarioUid = uiState.uid,
                estatisticas = uiState.estatisticas,
                peladas = uiState.peladasParticipadas,
                onBackClick = { telaAberta = null }
            )
        }
        PerfilTelaId.GRUPOS -> TelaCheiaDialog(onDismiss = { telaAberta = null }) {
            TelaMeusGrupos(
                grupos = uiState.meusGrupos,
                onBackClick = { telaAberta = null }
            )
        }
        null -> Unit
    }
}

@Composable
private fun TelaCheiaDialog(onDismiss: () -> Unit, content: @Composable () -> Unit) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        content()
    }
}

@Composable
private fun PerfilMenuConteudo(
    uiState: PerfilUiState,
    onItemClick: (PerfilTelaId) -> Unit,
    onRedefinirSenhaClick: () -> Unit,
    onSairClick: () -> Unit,
    onFecharClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .padding(horizontal = 8.dp)
            .padding(bottom = 16.dp)
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
    ) {
        Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
            IconButton(onClick = onFecharClick, modifier = Modifier.align(Alignment.CenterEnd)) {
                Icon(imageVector = Icons.Filled.Close, contentDescription = "Fechar")
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AvatarIniciais(nome = uiState.nome, tamanho = 48.dp)
            Spacer(modifier = Modifier.padding(start = 12.dp))
            Column {
                Text(
                    text = uiState.nome.ifBlank { "Convidado" },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                if (uiState.autenticado) {
                    Text(text = uiState.email, style = MaterialTheme.typography.labelSmall)
                }
            }
        }

        Spacer(modifier = Modifier.padding(top = 16.dp))

        when {
            uiState.carregando -> {
                Box(modifier = Modifier.fillMaxWidth().padding(32.dp)) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
            }
            !uiState.autenticado -> {
                EmptyState(
                    icone = Icons.Filled.Info,
                    titulo = "Você está usando o Boleiragem sem conta",
                    descricao = "Sem conta, não é possível ter um perfil nem um histórico entre peladas — " +
                        "só o resultado da pelada mais recente jogada neste aparelho fica disponível. " +
                        "Entre com e-mail ou Google para ter perfil e histórico completos.",
                    modifier = Modifier.fillMaxWidth()
                )
                BotaoSairPerfil(texto = "Sair do modo convidado", onClick = onSairClick)
            }
            else -> {
                ItemMenu(
                    icone = Icons.Filled.Person,
                    texto = "Meu perfil",
                    onClick = { onItemClick(PerfilTelaId.PERFIL) }
                )
                ItemMenu(
                    icone = Icons.Filled.BarChart,
                    texto = "Meu histórico",
                    onClick = { onItemClick(PerfilTelaId.HISTORICO) }
                )
                ItemMenu(
                    icone = Icons.Filled.Groups,
                    texto = "Meus grupos",
                    onClick = { onItemClick(PerfilTelaId.GRUPOS) }
                )
                if (uiState.tipoConta == TipoConta.EMAIL_SENHA) {
                    ItemMenu(
                        icone = Icons.Filled.Lock,
                        texto = "Redefinir senha",
                        onClick = onRedefinirSenhaClick,
                        mostrarChevron = false
                    )
                    if (uiState.mensagemRedefinicaoSenha != null) {
                        Text(
                            text = uiState.mensagemRedefinicaoSenha,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 24.dp)
                        )
                    }
                }
                BotaoSairPerfil(texto = "Sair da conta", onClick = onSairClick)
            }
        }
    }
}

@Composable
private fun BotaoSairPerfil(
    texto: String,
    onClick: () -> Unit
) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 12.dp)
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ExitToApp,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.error
        )
        Spacer(modifier = Modifier.padding(start = 12.dp))
        Text(
            text = texto,
            color = MaterialTheme.colorScheme.error,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun ItemMenu(
    icone: ImageVector,
    texto: String,
    onClick: () -> Unit,
    mostrarChevron: Boolean = true
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(imageVector = icone, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.padding(start = 16.dp))
        Text(text = texto, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        if (mostrarChevron) {
            Icon(
                imageVector = Icons.Filled.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
internal fun AvatarIniciais(nome: String, tamanho: Dp = 72.dp) {
    val inicial = nome.trim().firstOrNull()?.uppercase() ?: "?"
    Box(
        modifier = Modifier
            .size(tamanho)
            .background(MaterialTheme.colorScheme.tertiaryContainer, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = inicial,
            style = if (tamanho >= 56.dp) MaterialTheme.typography.headlineSmall else MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onTertiaryContainer,
            fontWeight = FontWeight.Bold
        )
    }
}
