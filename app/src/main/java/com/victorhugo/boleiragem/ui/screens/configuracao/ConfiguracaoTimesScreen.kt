package com.victorhugo.boleiragem.ui.screens.configuracao

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConfiguracaoTimesScreen(
    viewModel: ConfiguracaoTimesViewModel = hiltViewModel(),
    grupoId: Long = -1L, // Adicionando parâmetro grupoId
    podeEditar: Boolean = true,
    onNavigateToConfiguracaoPontuacao: () -> Unit = {},
    onNavigateToGerenciadorPerfis: () -> Unit = {}
) {
    // Efeito para definir o ID do grupo quando a tela é carregada
    LaunchedEffect(grupoId) {
        viewModel.setGrupoId(grupoId)
    }

    val configuracao by viewModel.configuracao.collectAsState(null)
    val todasConfiguracoes by viewModel.todasConfiguracoes.collectAsState()
    val configuracaoSelecionadaId by viewModel.configuracaoSelecionadaId.collectAsState()
    val scrollState = rememberScrollState()
    val configSalva by viewModel.configSalva.collectAsState()
    val navegarParaGerenciadorPerfis by viewModel.navegarParaGerenciadorPerfis.collectAsState()
    val mostrarDialogoSobrescrever by viewModel.mostrarDialogoSobrescrever.collectAsState()
    val mostrarDialogoNomeConfiguracao by viewModel.mostrarDialogoNomeConfiguracao.collectAsState()

    // Estado do dropdown de configurações
    var dropdownExpandido by remember { mutableStateOf(false) }

    // Estado para guardar o nome da configuração
    var nomeConfiguracao by remember { mutableStateOf("") }

    // SnackBar para confirmação de salvamento
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(configSalva) {
        if (configSalva) {
            snackbarHostState.showSnackbar(
                message = "Configurações salvas com sucesso!",
                duration = SnackbarDuration.Short
            )
        }
    }

    // Efeito para navegação para a tela de gerenciamento de perfis
    LaunchedEffect(navegarParaGerenciadorPerfis) {
        if (navegarParaGerenciadorPerfis) {
            onNavigateToGerenciadorPerfis() // Usando o callback em vez de navegar diretamente
            viewModel.onNavegacaoRealizada()
        }
    }

    // Diálogo para confirmar sobrescrita de configuração existente
    if (mostrarDialogoSobrescrever) {
        AlertDialog(
            onDismissRequest = { viewModel.cancelarSobrescrita() },
            title = { Text("Configuração já existe") },
            text = { Text("Já existe uma configuração com as mesmas características. Deseja sobrescrevê-la com um novo nome?") },
            confirmButton = {
                TextButton(onClick = { viewModel.confirmarSobrescrita() }) {
                    Text("Sim")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.cancelarSobrescrita() }) {
                    Text("Não")
                }
            }
        )
    }

    // Diálogo para nomear a configuração
    if (mostrarDialogoNomeConfiguracao) {
        AlertDialog(
            onDismissRequest = { viewModel.cancelarNomeConfiguracao() },
            title = { Text("Nome da Configuração") },
            text = {
                OutlinedTextField(
                    value = nomeConfiguracao,
                    onValueChange = { nomeConfiguracao = it },
                    label = { Text("Nome") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (nomeConfiguracao.isNotBlank()) {
                            viewModel.confirmarNomeESalvar(nomeConfiguracao)
                        }
                    },
                    enabled = nomeConfiguracao.isNotBlank()
                ) {
                    Text("Salvar")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.cancelarNomeConfiguracao() }) {
                    Text("Cancelar")
                }
            }
        )
    }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        TopAppBar(
            title = { Text("Configuração de Times") },
            actions = {
                // Ícone de engrenagem movido para o topo da tela
                IconButton(onClick = { viewModel.navegarParaGerenciadorPerfis() }, enabled = podeEditar) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Gerenciar Perfis de Configuração",
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.primary,
                titleContentColor = MaterialTheme.colorScheme.onPrimary
            )
        )

        if (configuracao != null) {
            Box(
                modifier = Modifier.weight(1f)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(scrollState)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Seletor de configurações
                    Text(
                        text = "Perfil de Configuração",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )

                    // Dropdown para selecionar perfil
                    Box(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { dropdownExpandido = true }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val perfilAtual = todasConfiguracoes.find { it.id == configuracaoSelecionadaId }
                                Text(
                                    text = perfilAtual?.nome ?: "Selecione um perfil",
                                    fontWeight = FontWeight.Medium
                                )

                                Icon(
                                    imageVector = Icons.Default.ArrowDropDown,
                                    contentDescription = "Selecionar perfil"
                                )
                            }
                        }

                        DropdownMenu(
                            expanded = dropdownExpandido,
                            onDismissRequest = { dropdownExpandido = false },
                            modifier = Modifier.fillMaxWidth(0.9f)
                        ) {
                            todasConfiguracoes.forEach { config ->
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
                                        viewModel.selecionarConfiguracao(config.id)
                                        dropdownExpandido = false
                                    }
                                )
                            }

                            // Botão para gerenciar perfis
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = "Gerenciar perfis...",
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                },
                                onClick = {
                                    dropdownExpandido = false
                                    viewModel.navegarParaGerenciadorPerfis()
                                }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Estrutura dos Times",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Card(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text("Jogadores por Time", fontWeight = FontWeight.Medium)

                            Slider(
                                value = viewModel.jogadoresPorTime.toFloat(),
                                onValueChange = { viewModel.jogadoresPorTime = it.toInt() },
                                valueRange = 3f..11f,
                                steps = 7,
                                enabled = podeEditar,
                                modifier = Modifier.fillMaxWidth()
                            )

                            Text(
                                text = "${viewModel.jogadoresPorTime} jogadores",
                                modifier = Modifier.align(Alignment.End)
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Text("Quantidade de Times", fontWeight = FontWeight.Medium)

                            Slider(
                                value = viewModel.quantidadeTimes.toFloat(),
                                onValueChange = { viewModel.quantidadeTimes = it.toInt() },
                                valueRange = 2f..6f,
                                steps = 3,
                                enabled = podeEditar,
                                modifier = Modifier.fillMaxWidth()
                            )

                            Text(
                                text = "${viewModel.quantidadeTimes} times",
                                modifier = Modifier.align(Alignment.End)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Critérios de Sorteio",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )

                    // Usando o componente atualizado de critérios de sorteio com navegação
                    // Fornecendo a lista completa de configurações e a configuração selecionada atual
                    CriteriosSorteioCard(
                        aleatorio = viewModel.aleatorio,
                        criteriosExtras = viewModel.criteriosExtras,
                        onAleatorioChange = { viewModel.toggleAleatorio(it) },
                        onCriterioExtraToggle = { viewModel.toggleCriterioExtra(it) },
                        onGerenciarPerfisClick = { viewModel.navegarParaGerenciadorPerfis() },
                        configuracoesDisponiveis = todasConfiguracoes,
                        configSelecionada = todasConfiguracoes.find { it.id == configuracaoSelecionadaId },
                        onConfiguracaoSelecionada = { viewModel.selecionarConfiguracao(it.id) }
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Configurações Adicionais",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = onNavigateToConfiguracaoPontuacao
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = "Configuração de Pontuação",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )

                                Text(
                                    text = "Defina pontos por vitória, derrota e empate",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Configurar pontuação",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    // Adicionar um espaço no final para garantir que todos os elementos sejam visíveis após o scroll
                    Spacer(modifier = Modifier.height(80.dp))
                }

                // Área de botões na parte inferior da tela
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .padding(horizontal = 16.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Botão "SALVAR COMO NOVO PERFIL"
                    Button(
                        onClick = {
                            // Iniciar processo de salvamento com nome
                            nomeConfiguracao = viewModel.nomeConfiguracao
                            viewModel.iniciarSalvamentoConfiguracao()
                        },
                        enabled = podeEditar,
                        modifier = Modifier
                            .weight(1f)
                            .height(com.victorhugo.boleiragem.ui.common.Dimensions.standardButtonHeight)
                            .padding(end = 8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = androidx.compose.ui.graphics.Color(0xFFF5A623) // Cor amarelo/laranja
                        )
                    ) {
                        Text(
                            text = "NOVO PERFIL",
                            fontSize = 14.sp // Texto menor para caber melhor
                        )
                    }

                    // Botão "SALVAR CONFIGURAÇÕES"
                    Button(
                        onClick = {
                            viewModel.salvarConfiguracoes()
                        },
                        enabled = podeEditar,
                        modifier = Modifier
                            .weight(1f)
                            .height(com.victorhugo.boleiragem.ui.common.Dimensions.standardButtonHeight)
                            .padding(start = 8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text(
                            text = "SALVAR",
                            fontSize = 14.sp // Texto menor para caber melhor
                        )
                    }
                }

                // Host do Snackbar
                SnackbarHost(
                    hostState = snackbarHostState,
                    modifier = Modifier.align(Alignment.BottomCenter)
                )
            }
        } else {
            // Estado de carregamento
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }
    }
}
