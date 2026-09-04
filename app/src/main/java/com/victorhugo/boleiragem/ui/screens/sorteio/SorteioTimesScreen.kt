package com.victorhugo.boleiragem.ui.screens.sorteio

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.victorhugo.boleiragem.data.model.ConfiguracaoSorteio
import com.victorhugo.boleiragem.data.model.Jogador
import com.victorhugo.boleiragem.ui.common.Dimensions.standardButtonHeight
import com.victorhugo.boleiragem.ui.common.Dimensions.standardButtonWidth

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SorteioTimesScreen(
    viewModel: SorteioTimesViewModel = hiltViewModel(),
    grupoId: Long = -1L, // Adicionando o parâmetro grupoId
    podeEditar: Boolean = true,
    onSorteioRealizado: () -> Unit = {},
    onNavigateToHistorico: () -> Unit = {}
) {
    // Efeito para definir o ID do grupo quando a tela é carregada
    LaunchedEffect(grupoId) {
        viewModel.setGrupoId(grupoId)
    }

    val jogadores by viewModel.jogadores.collectAsState(initial = emptyList())
    val configuracao by viewModel.configuracao.collectAsState(initial = null)
    val configuracaoSelecionada by viewModel.configuracaoSelecionada.collectAsState()
    val loading by viewModel.loading.collectAsState()
    val sorteioRealizado by viewModel.sorteioRealizado.collectAsState()
    val mostrarDialogConfirmacao by viewModel.mostrarDialogConfirmacao.collectAsState()
    val temPeladaAtiva by viewModel.temPeladaAtiva.collectAsState(initial = false)
    val botaoSorteioHabilitado by viewModel.botaoSorteioHabilitado.collectAsState(initial = true)

    // Estado para controlar a expansão do dropdown
    var dropdownExpandido by remember { mutableStateOf(false) }

    LaunchedEffect(sorteioRealizado) {
        if (sorteioRealizado) {
            // Primeiro mostramos a tela de resultado
            onSorteioRealizado()
            // Depois navegamos para a aba de histórico (Times)
            onNavigateToHistorico()
            viewModel.resetSorteioRealizado()
        }
    }

    // Dialog de confirmação para novo sorteio
    if (mostrarDialogConfirmacao) {
        AlertDialog(
            onDismissRequest = { viewModel.cancelarNovoSorteio() },
            title = { Text("Pelada em Andamento") },
            text = {
                Text(
                    "Existe uma pelada em andamento que ainda não foi registrada " +
                            "(vitórias/derrotas/empates ainda não foram contabilizados). " +
                            "\n\nSe continuar, o sorteio anterior será descartado e um novo será realizado. " +
                            "\n\nDeseja continuar mesmo assim?"
                )
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.confirmarNovoSorteio() }
                ) {
                    Text("Sim, fazer novo sorteio")
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { viewModel.cancelarNovoSorteio() }
                ) {
                    Text("Não, manter pelada atual")
                }
            }
        )
    }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        TopAppBar(
            title = { Text("Sorteio de Times") },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.primary,
                titleContentColor = MaterialTheme.colorScheme.onPrimary
            )
        )

        Box(
            modifier = Modifier.weight(1f)
        ) {
            // Conteúdo principal
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                // Aviso de pelada ativa
                if (temPeladaAtiva) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = "Informação",
                                tint = MaterialTheme.colorScheme.tertiary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(
                                text = "Há uma pelada em andamento. Registre o resultado para fazer um novo sorteio.",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Substituir o Card de Configuração por um Dropdown de perfis
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "Configuração do Sorteio",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Dropdown para seleção de configuração
                        ExposedDropdownMenuBox(
                            expanded = dropdownExpandido,
                            onExpandedChange = {
                                // Corrigindo o problema: sempre permitir expandir o dropdown
                                // se não houver pelada ativa
                                if (!temPeladaAtiva) dropdownExpandido = !dropdownExpandido
                            }
                        ) {
                            OutlinedTextField(
                                value = configuracaoSelecionada?.nome ?: configuracao?.nome ?: "Selecione uma configuração",
                                onValueChange = { },
                                readOnly = true,
                                enabled = !temPeladaAtiva,
                                trailingIcon = {
                                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = dropdownExpandido)
                                },
                                modifier = Modifier
                                    .menuAnchor()
                                    .fillMaxWidth(),
                                colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
                            )

                            ExposedDropdownMenu(
                                expanded = dropdownExpandido,
                                onDismissRequest = { dropdownExpandido = false }
                            ) {
                                // Mostra os perfis reais de configuração
                                viewModel.configuracoesDisponiveis.forEach { config ->
                                    DropdownMenuItem(
                                        text = {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text(config.nome)
                                                if (config.isPadrao) {
                                                    Text(
                                                        text = "(Padrão)",
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = MaterialTheme.colorScheme.primary
                                                    )
                                                }
                                            }
                                        },
                                        onClick = {
                                            viewModel.selecionarConfiguracao(config)
                                            dropdownExpandido = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Seletor de jogadores
                Text(
                    text = "Selecione os jogadores para o sorteio:",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Botões para selecionar/desmarcar todos
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp) // Reduzido o espaçamento
                ) {
                    OutlinedButton(
                        onClick = { viewModel.selecionarDisponiveis() },
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp), // Ajustado o padding interno
                        enabled = !temPeladaAtiva
                    ) {
                        Text(
                            "Disp.", // Abreviado para evitar quebra de linha
                            maxLines = 1, // Força texto em uma única linha
                            overflow = TextOverflow.Ellipsis // Adiciona elipses caso o texto não caiba
                        )
                    }

                    OutlinedButton(
                        onClick = { viewModel.selecionarTodos() },
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp),
                        enabled = !temPeladaAtiva
                    ) {
                        Text(
                            "Todos",
                            maxLines = 1
                        )
                    }

                    OutlinedButton(
                        onClick = { viewModel.desmarcarTodos() },
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp),
                        enabled = !temPeladaAtiva
                    ) {
                        Text(
                            "Nenhum",
                            maxLines = 1
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Lista de jogadores com checkboxes
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(bottom = 80.dp) // Espaço para o botão no final
                ) {
                    items(jogadores) { jogador ->
                        JogadorItemSelecao(
                            jogador = jogador,
                            selecionado = viewModel.jogadoresSelecionados.contains(jogador.id),
                            onSelecaoChange = { selecionado ->
                                viewModel.toggleJogadorSelecionado(jogador.id, selecionado)
                            },
                            enabled = !temPeladaAtiva
                        )
                    }
                }
            }

            // Sobreposição de carregamento
            if (loading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        modifier = Modifier
                            .widthIn(max = 300.dp)
                            .wrapContentHeight()
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(56.dp)
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            Text(
                                text = "Sorteando os times...",
                                style = MaterialTheme.typography.bodyLarge,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }

            // Botão fixo na parte inferior com estilo melhorado
            Button(
                onClick = { viewModel.verificarESortearTimes() },
                enabled = podeEditar && viewModel.jogadoresSelecionados.isNotEmpty() && !loading && botaoSorteioHabilitado,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 16.dp, vertical = 16.dp)
                    .width(standardButtonWidth)
                    .height(standardButtonHeight),
                colors = ButtonDefaults.buttonColors(
                    // Verde quando habilitado, cinza quando desabilitado
                    containerColor = if (viewModel.jogadoresSelecionados.isNotEmpty() && !loading && botaoSorteioHabilitado)
                        Color(0xFF4CAF50) // Verde
                    else
                        Color(0xFFBDBDBD), // Cinza
                    contentColor = Color.White,
                    disabledContainerColor = Color(0xFFE0E0E0),
                    disabledContentColor = Color(0xFF9E9E9E)
                )
            ) {
                Text(
                    "SORTEAR TIMES",
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun JogadorItemSelecao(
    jogador: Jogador,
    selecionado: Boolean,
    onSelecaoChange: (Boolean) -> Unit,
    enabled: Boolean = true
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = jogador.nome,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${jogador.posicaoPrincipal.name} (${jogador.notaPosicaoPrincipal}⭐)",
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Checkbox(
                checked = selecionado,
                onCheckedChange = onSelecaoChange,
                enabled = enabled
            )
        }
    }
}
