package com.victorhugo.boleiragem.ui.screens.historico

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.SportsSoccer
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.victorhugo.boleiragem.data.model.HistoricoPelada
import com.victorhugo.boleiragem.ui.screens.estatisticas.EstatisticasViewModel
import com.victorhugo.boleiragem.ui.screens.estatisticas.RankingTab
import com.victorhugo.boleiragem.ui.screens.estatisticas.JogadorIndividualTab
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoricoScreen(
    historicoViewModel: HistoricoViewModel = hiltViewModel(),
    estatisticasViewModel: EstatisticasViewModel = hiltViewModel(),
    grupoId: Long = -1L, // Adicionando parâmetro grupoId
    podeEditar: Boolean = true
) {
    // Efeito para definir o ID do grupo quando a tela é carregada
    LaunchedEffect(grupoId) {
        historicoViewModel.setGrupoId(grupoId)
        estatisticasViewModel.setGrupoId(grupoId)
    }

    // Histórico
    val historicoPartidas by historicoViewModel.historicoPartidas.collectAsState(initial = emptyList())
    val ordenacaoAtual by historicoViewModel.ordenacaoAtual.collectAsState()

    // Estatísticas
    val jogadores by estatisticasViewModel.jogadores.collectAsState(initial = emptyList())
    val jogadorSelecionado by estatisticasViewModel.jogadorSelecionado.collectAsState()
    val ranking by estatisticasViewModel.ranking.collectAsState(initial = emptyList())
    val taxaVitoria by estatisticasViewModel.taxaVitoria.collectAsState(initial = 0f)
    val taxaEmpate by estatisticasViewModel.taxaEmpate.collectAsState(initial = 0f)
    val taxaDerrota by estatisticasViewModel.taxaDerrota.collectAsState(initial = 0f)
    val mediaPontuacao by estatisticasViewModel.mediaPontuacao.collectAsState(initial = 0f)

    // Estado para controlar qual guia está ativa (Histórico ou Estatísticas)
    var selectedTab by remember { mutableStateOf(0) }

    // Estado para controlar qual subaba de Estatísticas está selecionada
    var estatisticasTabIndex by remember { mutableIntStateOf(0) }

    // Estado para controlar a exibição do diálogo de confirmação para exclusão do histórico
    var showConfirmacaoExclusao by remember { mutableStateOf(false) }

    // Diálogo de confirmação para excluir o histórico
    if (showConfirmacaoExclusao) {
        AlertDialog(
            onDismissRequest = { showConfirmacaoExclusao = false },
            title = { Text("Apagar Histórico") },
            text = {
                Text(
                    "Tem certeza que deseja apagar todo o histórico de peladas? " +
                    "Esta ação não pode ser desfeita."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        historicoViewModel.apagarHistorico()
                        showConfirmacaoExclusao = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Apagar")
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showConfirmacaoExclusao = false }
                ) {
                    Text("Cancelar")
                }
            }
        )
    }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // TopAppBar que muda conforme a guia selecionada
        TopAppBar(
            title = {
                Text(if (selectedTab == 0) "Histórico de Peladas" else "Estatísticas")
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.primary,
                titleContentColor = MaterialTheme.colorScheme.onPrimary
            ),
            actions = {
                // Botões de navegação entre Histórico e Estatísticas
                IconButton(
                    onClick = { selectedTab = 0 }
                ) {
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = "Histórico",
                        tint = if (selectedTab == 0)
                            MaterialTheme.colorScheme.onPrimary
                        else
                            MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f)
                    )
                }

                IconButton(
                    onClick = { selectedTab = 1 }
                ) {
                    Icon(
                        imageVector = Icons.Default.BarChart,
                        contentDescription = "Estatísticas",
                        tint = if (selectedTab == 1)
                            MaterialTheme.colorScheme.onPrimary
                        else
                            MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f)
                    )
                }

                // Botões específicos por tab
                if (selectedTab == 0) {
                    // Botões para o Histórico
                    IconButton(
                        onClick = { historicoViewModel.alternarOrdenacao() }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Sort,
                            contentDescription = "Ordenar por data",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }

                    IconButton(
                        onClick = { showConfirmacaoExclusao = true },
                        enabled = podeEditar
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Limpar Histórico",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
            }
        )

        // Conteúdo principal que muda conforme a guia selecionada
        when (selectedTab) {
            0 -> HistoricoContent(
                historicoPartidas = historicoPartidas,
                ordenacaoAtual = ordenacaoAtual
            )
            1 -> EstatisticasContent(
                estatisticasTabIndex = estatisticasTabIndex,
                onTabChange = { estatisticasTabIndex = it },
                ranking = ranking,
                jogadorSelecionado = jogadorSelecionado,
                taxaVitoria = taxaVitoria,
                taxaEmpate = taxaEmpate,
                taxaDerrota = taxaDerrota,
                mediaPontuacao = mediaPontuacao,
                onJogadorClick = { estatisticasViewModel.selecionarJogador(it); estatisticasTabIndex = 1 }
            )
        }
    }
}

@Composable
fun HistoricoContent(
    historicoPartidas: List<HistoricoPelada>,
    ordenacaoAtual: OrdenacaoHistorico
) {
    Column(modifier = Modifier.fillMaxSize()) {
        // Mostra a ordem de classificação atual
        Text(
            text = "Ordenação: ${if (ordenacaoAtual == OrdenacaoHistorico.MAIS_RECENTES) "Mais Recentes" else "Mais Antigas"}",
            modifier = Modifier.padding(8.dp),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )

        if (historicoPartidas.isEmpty()) {
            // Exibe uma mensagem quando não há itens no histórico
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.SportsSoccer,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Nenhuma pelada registrada ainda",
                        style = MaterialTheme.typography.titleMedium,
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "As peladas salvas aparecerão aqui",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
            }
        } else {
            // Lista as peladas do histórico
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                items(historicoPartidas) { historicoPelada ->
                    HistoricoItem(historicoPelada = historicoPelada)
                }
            }
        }
    }
}

@Composable
fun EstatisticasContent(
    estatisticasTabIndex: Int,
    onTabChange: (Int) -> Unit,
    ranking: List<com.victorhugo.boleiragem.data.model.Jogador>,
    jogadorSelecionado: com.victorhugo.boleiragem.data.model.Jogador?,
    taxaVitoria: Float,
    taxaEmpate: Float,
    taxaDerrota: Float,
    mediaPontuacao: Float,
    onJogadorClick: (com.victorhugo.boleiragem.data.model.Jogador) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        // Abas para navegação entre ranking e jogador individual
        TabRow(selectedTabIndex = estatisticasTabIndex) {
            Tab(
                text = { Text("Ranking") },
                selected = estatisticasTabIndex == 0,
                onClick = { onTabChange(0) }
            )
            Tab(
                text = { Text("Jogador Individual") },
                selected = estatisticasTabIndex == 1,
                onClick = { onTabChange(1) }
            )
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            when (estatisticasTabIndex) {
                0 -> RankingTab(ranking) { onJogadorClick(it) }
                1 -> JogadorIndividualTab(
                    jogadorSelecionado,
                    taxaVitoria,
                    taxaEmpate,
                    taxaDerrota,
                    mediaPontuacao
                )
            }
        }
    }
}

@Composable
fun HistoricoItem(historicoPelada: HistoricoPelada) {
    val dateFormatter = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
    val dataFormatada = dateFormatter.format(Date(historicoPelada.dataFinalizacao))

    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Cabeçalho da pelada
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val peladaTitulo = "Pelada ${historicoPelada.id}"
                Text(
                    text = peladaTitulo,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = dataFormatada,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Divider(modifier = Modifier.padding(vertical = 8.dp))

            // Lista de times que participaram
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                historicoPelada.times.forEach { time ->
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = MaterialTheme.shapes.small
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = time.nome,
                                style = MaterialTheme.typography.bodyLarge
                            )

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // Sempre exibir o contador de vitórias
                                val vitoriasTxt = "${time.vitorias}V"
                                Badge(
                                    containerColor = MaterialTheme.colorScheme.primary
                                ) {
                                    Text(
                                        text = vitoriasTxt,
                                        modifier = Modifier.padding(horizontal = 4.dp)
                                    )
                                }

                                // Sempre exibir o contador de empates
                                val empatesTxt = "${time.empates}E"
                                Badge(
                                    containerColor = MaterialTheme.colorScheme.tertiary
                                ) {
                                    Text(
                                        text = empatesTxt,
                                        modifier = Modifier.padding(horizontal = 4.dp)
                                    )
                                }

                                // Sempre exibir o contador de derrotas
                                val derrotasTxt = "${time.derrotas}D"
                                Badge(
                                    containerColor = MaterialTheme.colorScheme.error
                                ) {
                                    Text(
                                        text = derrotasTxt,
                                        modifier = Modifier.padding(horizontal = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
