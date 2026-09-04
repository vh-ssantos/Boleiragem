package com.victorhugo.boleiragem.ui.screens.perfil

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.victorhugo.boleiragem.data.model.PosicaoJogador
import com.victorhugo.boleiragem.ui.common.EmptyState
import com.victorhugo.boleiragem.ui.common.OutlinedTextFieldComAcentos
import com.victorhugo.boleiragem.ui.common.SectionCard
import com.victorhugo.boleiragem.ui.common.StatTile

private fun PosicaoJogador.nomeExibicao(): String =
    name.lowercase().split("_").joinToString(" ") { it.replaceFirstChar(Char::uppercase) }

/**
 * Avatar clicável para o TopAppBar (padrão Nubank) — acessível tanto em "Minhas Peladas" quanto
 * dentro de um grupo, ao contrário da antiga aba "Perfil" que só existia dentro de um grupo. Ao
 * tocar, abre um bottom sheet com os dados do usuário, "Meu Histórico" e "Sair da conta".
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PerfilAvatarButton(
    viewModel: PerfilViewModel = hiltViewModel(),
    onSairClick: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var mostrarMenu by remember { mutableStateOf(false) }
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
                onNomeChange = viewModel::onNomeChange,
                onPosicaoChange = viewModel::onPosicaoChange,
                onIdadeChange = viewModel::onIdadeChange,
                onSalvarClick = viewModel::salvar,
                onSairClick = {
                    mostrarMenu = false
                    onSairClick()
                },
                onFecharClick = { mostrarMenu = false }
            )
        }
    }
}

@Composable
private fun PerfilMenuConteudo(
    uiState: PerfilUiState,
    onNomeChange: (String) -> Unit,
    onPosicaoChange: (PosicaoJogador) -> Unit,
    onIdadeChange: (String) -> Unit,
    onSalvarClick: () -> Unit,
    onSairClick: () -> Unit,
    onFecharClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp)
            .padding(bottom = 24.dp)
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            IconButton(onClick = onFecharClick, modifier = Modifier.align(Alignment.CenterEnd)) {
                Icon(imageVector = Icons.Filled.Close, contentDescription = "Fechar")
            }
        }

        when {
            uiState.carregando -> {
                Box(modifier = Modifier.fillMaxWidth().padding(32.dp)) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
            }
            !uiState.autenticado -> {
                PerfilConvidado()
            }
            else -> {
                PerfilAutenticado(
                    uiState = uiState,
                    onNomeChange = onNomeChange,
                    onPosicaoChange = onPosicaoChange,
                    onIdadeChange = onIdadeChange,
                    onSalvarClick = onSalvarClick,
                    onSairClick = onSairClick
                )
            }
        }
    }
}

@Composable
private fun PerfilConvidado(modifier: Modifier = Modifier) {
    EmptyState(
        icone = Icons.Filled.Info,
        titulo = "Você está usando o Boleiragem sem conta",
        descricao = "Sem conta, não é possível ter um perfil nem um histórico entre peladas — " +
            "só o resultado da pelada mais recente jogada neste aparelho fica disponível. " +
            "Entre com e-mail ou Google para ter perfil e histórico completos.",
        modifier = modifier
    )
}

@Composable
private fun AvatarIniciais(nome: String, tamanho: Dp = 72.dp) {
    val inicial = nome.trim().firstOrNull()?.uppercase() ?: "?"
    Box(
        modifier = Modifier
            .size(tamanho)
            .background(MaterialTheme.colorScheme.primary, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = inicial,
            style = if (tamanho >= 56.dp) MaterialTheme.typography.headlineSmall else MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onPrimary,
            fontWeight = FontWeight.Bold
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PerfilAutenticado(
    uiState: PerfilUiState,
    onNomeChange: (String) -> Unit,
    onPosicaoChange: (PosicaoJogador) -> Unit,
    onIdadeChange: (String) -> Unit,
    onSalvarClick: () -> Unit,
    onSairClick: () -> Unit
) {
    var menuPosicaoAberto by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AvatarIniciais(nome = uiState.nome)
        Spacer(modifier = Modifier.height(12.dp))
        Text(text = uiState.email, style = MaterialTheme.typography.bodyMedium)
        Text(
            text = when (uiState.tipoConta) {
                TipoConta.GOOGLE -> "Conta Google"
                TipoConta.EMAIL_SENHA -> "Conta e-mail e senha"
                TipoConta.CONVIDADO -> "Convidado"
            },
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(24.dp))

        SectionCard(titulo = "Dados do perfil") {
            Column {
                OutlinedTextFieldComAcentos(
                    value = uiState.nome,
                    onValueChange = onNomeChange,
                    label = { Text("Nome") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                ExposedDropdownMenuBox(
                    expanded = menuPosicaoAberto,
                    onExpandedChange = { menuPosicaoAberto = it }
                ) {
                    OutlinedTextFieldComAcentos(
                        value = uiState.posicaoFavorita?.nomeExibicao() ?: "",
                        onValueChange = {},
                        label = { Text("Posição favorita") },
                        enabled = false,
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = menuPosicaoAberto,
                        onDismissRequest = { menuPosicaoAberto = false }
                    ) {
                        PosicaoJogador.entries.forEach { posicao ->
                            DropdownMenuItem(
                                text = { Text(posicao.nomeExibicao()) },
                                onClick = {
                                    onPosicaoChange(posicao)
                                    menuPosicaoAberto = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextFieldComAcentos(
                    value = uiState.idade,
                    onValueChange = onIdadeChange,
                    label = { Text("Idade") },
                    keyboardType = KeyboardType.Number,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                if (uiState.erro != null) {
                    Text(
                        text = uiState.erro,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
                if (uiState.salvoComSucesso) {
                    Text(
                        text = "Perfil salvo!",
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.bodySmall
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                Button(
                    onClick = onSalvarClick,
                    enabled = !uiState.salvando,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (uiState.salvando) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), color = MaterialTheme.colorScheme.onPrimary)
                    } else {
                        Text("Salvar")
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        MeuHistoricoCard(uiState.estatisticas)

        Spacer(modifier = Modifier.height(24.dp))

        OutlinedButton(
            onClick = onSairClick,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(imageVector = Icons.Filled.ExitToApp, contentDescription = null)
            Spacer(modifier = Modifier.height(0.dp))
            Text(text = "  Sair da conta")
        }
    }
}

@Composable
private fun MeuHistoricoCard(estatisticas: com.victorhugo.boleiragem.data.repository.EstatisticasPessoais?) {
    SectionCard(
        titulo = "Meu histórico",
        subtitulo = "Peladas jogadas em todos os grupos, neste aparelho"
    ) {
        if (estatisticas == null || estatisticas.peladasJogadas == 0) {
            Text(
                text = "Você ainda não finalizou nenhuma pelada como jogador vinculado à sua conta.",
                style = MaterialTheme.typography.bodyMedium
            )
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                StatTile(estatisticas.peladasJogadas.toString(), "Peladas")
                StatTile(estatisticas.vitorias.toString(), "Vitórias")
                StatTile(estatisticas.empates.toString(), "Empates")
                StatTile(estatisticas.derrotas.toString(), "Derrotas")
            }
        }
    }
}
