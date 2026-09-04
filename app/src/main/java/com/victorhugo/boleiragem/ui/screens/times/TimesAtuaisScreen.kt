package com.victorhugo.boleiragem.ui.screens.times

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Sports
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.victorhugo.boleiragem.data.model.HistoricoTime
import com.victorhugo.boleiragem.ui.common.Dimensions
import com.victorhugo.boleiragem.ui.screens.historico.HistoricoTimesViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimesAtuaisScreen(
    viewModel: HistoricoTimesViewModel = hiltViewModel(),
    grupoId: Long = -1L, // Adicionando parâmetro grupoId
    podeEditar: Boolean = true
) {
    // Efeito para definir o ID do grupo quando a tela é carregada
    LaunchedEffect(grupoId) {
        viewModel.setGrupoId(grupoId)
    }

    val historicoTimes by viewModel.historicoTimes.collectAsState(initial = emptyList())
    val peladaFinalizada by viewModel.peladaFinalizada.collectAsState()
    val jogadoresPorTime by viewModel.jogadoresPorTime.collectAsState()
    val mostrarDialogoTransferencia by viewModel.mostrarDialogoTransferencia.collectAsState()
    val mostrarComponenteConfronto by viewModel.mostrarComponenteConfronto.collectAsState()

    // Estado para controlar se o diálogo de confirmação está sendo mostrado
    var showDialogConfirmacao by remember { mutableStateOf(false) }

    // Estado para controlar se o diálogo de confirmação para apagar está sendo mostrado
    var showDialogCancelar by remember { mutableStateOf(false) }

    // Diálogo de confirmação para finalizar a pelada
    if (showDialogConfirmacao) {
        AlertDialog(
            onDismissRequest = { showDialogConfirmacao = false },
            title = { Text("Finalizar Pelada") },
            text = {
                Text(
                    "Tem certeza que deseja finalizar a pelada? " +
                            "As estatísticas dos jogadores serão atualizadas com os resultados atuais " +
                            "e você não poderá mais editar os resultados desse sorteio."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.finalizarPelada()
                        showDialogConfirmacao = false
                    }
                ) {
                    Text("Confirmar")
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showDialogConfirmacao = false }
                ) {
                    Text("Cancelar")
                }
            }
        )
    }

    // Diálogo de confirmação para apagar a pelada
    if (showDialogCancelar) {
        AlertDialog(
            onDismissRequest = { showDialogCancelar = false },
            title = { Text("Cancelar Pelada") },
            text = {
                Text(
                    "Tem certeza que deseja cancelar esta pelada? " +
                            "Todos os times e resultados serão perdidos e não poderão ser recuperados."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.cancelarPeladaAtual()
                        showDialogCancelar = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Cancelar")
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showDialogCancelar = false }
                ) {
                    Text("Cancelar")
                }
            }
        )
    }

    // Diálogo para transferência de jogadores entre times
    if (mostrarDialogoTransferencia) {
        TransferenciaJogadorDialog(
            times = historicoTimes,
            jogadoresPorTime = jogadoresPorTime,
            modoSubstituicaoTimeReserva = viewModel.modoSubstituicaoTimeReserva.collectAsState().value,
            onDismiss = { viewModel.fecharDialogoTransferencia() },
            onTransferirJogador = { jogador, timeOrigemId, timeDestinoId ->
                viewModel.transferirJogador(jogador, timeOrigemId, timeDestinoId)
            }
        )
    }

    // SnackBar para confirmar finalização da pelada
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(peladaFinalizada) {
        if (peladaFinalizada) {
            snackbarHostState.showSnackbar(
                message = "Pelada finalizada com sucesso!",
                duration = SnackbarDuration.Short
            )
        }
    }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        TopAppBar(
            title = { Text("Pelada Atual") },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.primary,
                titleContentColor = MaterialTheme.colorScheme.onPrimary
            ),
            actions = {
                // Botão para exibir o seletor de confrontos
                if (historicoTimes.size > 1) {
                    IconButton(onClick = { viewModel.mostrarComponenteConfronto() }, enabled = podeEditar) {
                        Icon(
                            imageVector = Icons.Default.Sports,
                            contentDescription = "Confronto entre Times",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }

                    // Botão para transferir jogadores entre times
                    IconButton(onClick = { viewModel.mostrarDialogoTransferencia() }, enabled = podeEditar) {
                        Icon(
                            imageVector = Icons.Default.SwapHoriz,
                            contentDescription = "Transferir Jogadores",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
            }
        )

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            if (historicoTimes.isEmpty()) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "Nenhum time sorteado ainda.",
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Faça um sorteio na aba 'Sorteio' para começar a registrar o histórico dos times.",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                // Exibe o componente de confronto se estiver ativado
                if (mostrarComponenteConfronto) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp)
                    ) {
                        ConfrontoTimesComponent(
                            times = historicoTimes,
                            onFinalizarConfronto = { timeA, timeB, resultado ->
                                viewModel.finalizarConfronto(timeA, timeB, resultado)
                            },
                            onCancelarConfronto = { viewModel.ocultarComponenteConfronto() }
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        contentPadding = PaddingValues(bottom = 140.dp), // Aumentando o padding inferior para não esconder o último item
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(historicoTimes) { time ->
                            TimeCard(
                                time = time,
                                jogadores = jogadoresPorTime[time.id] ?: emptyList(),
                                onVitoriaClick = { viewModel.adicionarVitoria(time) },
                                onDerrotaClick = { viewModel.adicionarDerrota(time) },
                                onEmpateClick = { viewModel.adicionarEmpate(time) },
                                onDiminuirVitoriaClick = { viewModel.diminuirVitoria(time) },
                                onDiminuirDerrotaClick = { viewModel.diminuirDerrota(time) },
                                onDiminuirEmpateClick = { viewModel.diminuirEmpate(time) },
                                onSubstituicaoClick = { viewModel.mostrarDialogoTransferencia() },
                                podeEditar = podeEditar
                            )
                        }
                    }
                }

                // Botões fixos na parte inferior
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Só mostra os botões quando o componente de confronto não estiver visível
                    if (!mostrarComponenteConfronto) {
                        // Usando as constantes globais de tamanho de botão

                        // Row para alinhar os botões lado a lado
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Botão para cancelar a pelada
                            Button(
                                onClick = { showDialogCancelar = true },
                                enabled = podeEditar,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(Dimensions.standardButtonHeight),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.error,
                                    contentColor = MaterialTheme.colorScheme.onError
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Cancelar Pelada",
                                    modifier = Modifier.size(20.dp),
                                    tint = MaterialTheme.colorScheme.onError
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    "CANCELAR",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            // Botão para finalizar a pelada
                            Button(
                                onClick = { showDialogConfirmacao = true },
                                enabled = podeEditar,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(Dimensions.standardButtonHeight),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary
                                )
                            ) {
                                Icon(
                                    Icons.Default.CheckCircle,
                                    contentDescription = "Finalizar Pelada",
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    "FINALIZAR",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            // Host do Snackbar
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }
}

@Composable
fun TimeCard(
    time: HistoricoTime,
    jogadores: List<com.victorhugo.boleiragem.data.model.Jogador>,
    onVitoriaClick: () -> Unit,
    onDerrotaClick: () -> Unit,
    onEmpateClick: () -> Unit,
    onDiminuirVitoriaClick: () -> Unit = {},
    onDiminuirDerrotaClick: () -> Unit = {},
    onDiminuirEmpateClick: () -> Unit = {},
    onSubstituicaoClick: () -> Unit = {}, // Adicionando o parâmetro para substituição
    podeEditar: Boolean = true
) {
    val dateFormatter = remember { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()) }
    val dataFormatada = remember(time.dataUltimoSorteio) {
        dateFormatter.format(Date(time.dataUltimoSorteio))
    }

    // Estado para controlar se o card está expandido
    var expandido by remember { mutableStateOf(false) }

    // Estado para controlar se o modo de edição está ativado
    var modoEdicao by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Cabeçalho do time - clicável para expandir/contrair
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expandido = !expandido },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (time.ehTimeReserva) "Time Reserva" else time.nome,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = dataFormatada,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Informações sobre médias do time - sempre visíveis
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text(
                        text = "Estrelas: ${String.format("%.1f", time.mediaEstrelas)}★",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }

                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.secondaryContainer
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.EmojiEvents,
                            contentDescription = "Pontuação",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Text(
                            text = "Pontuação: ${String.format("%.1f", time.mediaPontuacao)}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            }

            // Para times de reserva, não exibimos estatísticas de vitórias/derrotas/empates
            // Em vez disso, mostramos um botão de substituição
            if (time.ehTimeReserva) {
                // Seção especial para Time Reserva com botão de substituição
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Time de Reserva",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = "Este time está disponível para substituições",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Botão de substituição
                    Button(
                        onClick = onSubstituicaoClick,
                        enabled = podeEditar,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondary,
                            contentColor = MaterialTheme.colorScheme.onSecondary
                        ),
                        modifier = Modifier.size(56.dp), // Aumentando o tamanho para caber o ícone
                        contentPadding = PaddingValues(0.dp) // Remove padding interno
                    ) {
                        Icon(
                            imageVector = Icons.Default.SwapHoriz,
                            contentDescription = "Substituição",
                            modifier = Modifier.size(28.dp) // Ícone um pouco maior
                        )
                    }
                }
            } else if (!time.ehTimeReserva) {
                // Estatísticas com botão de edição
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Coluna de estatísticas
                    Column {
                        Text(
                            text = "Estatísticas:",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Text(
                                text = "V: ${time.vitorias}",
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )

                            Text(
                                text = "D: ${time.derrotas}",
                                color = MaterialTheme.colorScheme.error,
                                fontWeight = FontWeight.Bold
                            )

                            Text(
                                text = "E: ${time.empates}",
                                color = MaterialTheme.colorScheme.tertiary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Botão de edição para mostrar/esconder os controles
                    IconButton(
                        onClick = { modoEdicao = !modoEdicao },
                        enabled = podeEditar
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Editar estatísticas",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                // Controles para editar estatísticas (visíveis apenas quando modoEdicao é verdadeiro)
                AnimatedVisibility(
                    visible = modoEdicao,
                    enter = fadeIn(initialAlpha = 0.3f),
                    exit = fadeOut(animationSpec = tween(250))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                    ) {
                        // Controles de vitória
                        EstatisticaControle(
                            titulo = "Vitórias:",
                            valor = time.vitorias,
                            corTexto = MaterialTheme.colorScheme.primary,
                            onAdicionar = { onVitoriaClick() },
                            onDiminuir = { onDiminuirVitoriaClick() }
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Controles de derrota
                        EstatisticaControle(
                            titulo = "Derrotas:",
                            valor = time.derrotas,
                            corTexto = MaterialTheme.colorScheme.error,
                            onAdicionar = { onDerrotaClick() },
                            onDiminuir = { onDiminuirDerrotaClick() }
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Controles de empate
                        EstatisticaControle(
                            titulo = "Empates:",
                            valor = time.empates,
                            corTexto = MaterialTheme.colorScheme.tertiary,
                            onAdicionar = { onEmpateClick() },
                            onDiminuir = { onDiminuirEmpateClick() }
                        )
                    }
                }
            }

            // Jogadores - visíveis apenas quando o card estiver expandido
            AnimatedVisibility(
                visible = expandido,
                enter = fadeIn(initialAlpha = 0.3f),
                exit = fadeOut(animationSpec = tween(250))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp)
                ) {
                    Text(
                        text = "Jogadores:",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    jogadores.forEach { jogador ->
                        Text(
                            text = "${jogador.nome} (${jogador.posicaoPrincipal.sigla})",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun EstatisticaControle(
    titulo: String,
    valor: Int,
    corTexto: Color,
    onAdicionar: () -> Unit,
    onDiminuir: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = titulo,
            style = MaterialTheme.typography.bodyMedium
        )

        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Botão para diminuir
            IconButton(
                onClick = onDiminuir,
                enabled = valor > 0
            ) {
                Icon(
                    imageVector = Icons.Default.Remove,
                    contentDescription = "Diminuir",
                    tint = if (valor > 0) corTexto else MaterialTheme.colorScheme.outline
                )
            }

            // Valor atual
            Text(
                text = valor.toString(),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = corTexto,
                modifier = Modifier.padding(horizontal = 8.dp)
            )

            // Botão para adicionar
            IconButton(
                onClick = onAdicionar
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Adicionar",
                    tint = corTexto
                )
            }
        }
    }
}
