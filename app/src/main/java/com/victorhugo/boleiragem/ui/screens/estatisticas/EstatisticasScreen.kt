package com.victorhugo.boleiragem.ui.screens.estatisticas

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.victorhugo.boleiragem.data.model.Jogador

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EstatisticasScreen(
    viewModel: EstatisticasViewModel = hiltViewModel(),
    grupoId: Long = -1L, // Adicionando parâmetro grupoId
    onNavigateBack: () -> Unit = {}
) {
    // Efeito para definir o ID do grupo quando a tela é carregada
    LaunchedEffect(grupoId) {
        viewModel.setGrupoId(grupoId)
    }

    val jogadores by viewModel.jogadores.collectAsState(initial = emptyList())
    val jogadorSelecionado by viewModel.jogadorSelecionado.collectAsState()
    val ranking by viewModel.ranking.collectAsState(initial = emptyList())

    val taxaVitoria by viewModel.taxaVitoria.collectAsState(initial = 0f)
    val taxaEmpate by viewModel.taxaEmpate.collectAsState(initial = 0f)
    val taxaDerrota by viewModel.taxaDerrota.collectAsState(initial = 0f)
    val mediaPontuacao by viewModel.mediaPontuacao.collectAsState(initial = 0f)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Estatísticas") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Voltar"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Tabbed view para diferentes visualizações de estatísticas
            var tabIndex by remember { mutableIntStateOf(0) }
            val tabs = listOf("Ranking", "Jogador Individual")

            TabRow(selectedTabIndex = tabIndex) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        text = { Text(title) },
                        selected = tabIndex == index,
                        onClick = { tabIndex = index }
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                when (tabIndex) {
                    0 -> RankingTab(ranking) {
                        viewModel.selecionarJogador(it)
                        tabIndex = 1
                    }
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
}

@Composable
fun RankingTab(
    jogadores: List<Jogador>,
    onJogadorClick: (Jogador) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = "Ranking de Jogadores",
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Cabeçalho da tabela
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.primaryContainer)
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Text(
                text = "Pos",
                fontWeight = FontWeight.Bold,
                modifier = Modifier.width(40.dp),
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Text(
                text = "Jogador",
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f),
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Text(
                text = "Jogos",
                fontWeight = FontWeight.Bold,
                modifier = Modifier.width(60.dp),
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Text(
                text = "Pontos",
                fontWeight = FontWeight.Bold,
                modifier = Modifier.width(70.dp),
                textAlign = TextAlign.End,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }

        // Lista de jogadores
        LazyColumn {
            items(jogadores) { jogador ->
                val index = jogadores.indexOf(jogador) + 1
                RankingItem(index = index, jogador = jogador, onClick = { onJogadorClick(jogador) })
            }
        }
    }
}

@Composable
fun RankingItem(
    index: Int,
    jogador: Jogador,
    onClick: () -> Unit
) {
    val backgroundColor = when (index) {
        1 -> Color(0xFFFFF9C4) // Ouro para primeiro lugar
        2 -> Color(0xFFF5F5F5) // Prata para segundo lugar
        3 -> Color(0xFFFFECB3) // Bronze para terceiro lugar
        else -> MaterialTheme.colorScheme.surface
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(backgroundColor)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "$index",
            modifier = Modifier.width(40.dp),
            fontWeight = if (index <= 3) FontWeight.Bold else FontWeight.Normal
        )
        Text(
            text = jogador.nome,
            modifier = Modifier.weight(1f),
            fontWeight = if (index <= 3) FontWeight.Bold else FontWeight.Normal
        )
        Text(
            text = "${jogador.totalJogos}",
            modifier = Modifier.width(60.dp),
            textAlign = TextAlign.Center
        )
        Text(
            text = "${jogador.pontuacaoTotal}",
            modifier = Modifier.width(70.dp),
            textAlign = TextAlign.End,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun JogadorIndividualTab(
    jogador: Jogador?,
    taxaVitoria: Float,
    taxaEmpate: Float,
    taxaDerrota: Float,
    mediaPontuacao: Float
) {
    if (jogador == null) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text("Selecione um jogador do ranking para ver as estatísticas")
        }
    } else {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Cabeçalho com nome do jogador
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .padding(16.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = jogador.nome,
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Estatísticas gerais
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                EstatisticaItem(
                    titulo = "Total Jogos",
                    valor = "${jogador.totalJogos}",
                    modifier = Modifier.weight(1f)
                )
                EstatisticaItem(
                    titulo = "Pontuação Total",
                    valor = "${jogador.pontuacaoTotal}",
                    modifier = Modifier.weight(1f)
                )
                EstatisticaItem(
                    titulo = "Média/Jogo",
                    valor = String.format("%.1f", mediaPontuacao),
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Gráfico de desempenho (representação simplificada)
            Text(
                text = "Desempenho",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            DesempenhoChart(
                taxaVitoria = taxaVitoria,
                taxaEmpate = taxaEmpate,
                taxaDerrota = taxaDerrota,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .padding(horizontal = 16.dp)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                LegendaItem("Vitórias", Color(0xFF4CAF50), String.format("%.1f%%", taxaVitoria))
                LegendaItem("Empates", Color(0xFFFFC107), String.format("%.1f%%", taxaEmpate))
                LegendaItem("Derrotas", Color(0xFFF44336), String.format("%.1f%%", taxaDerrota))
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Mais detalhes
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                EstatisticaItem(
                    titulo = "Vitórias",
                    valor = "${jogador.vitorias}",
                    modifier = Modifier.weight(1f)
                )
                EstatisticaItem(
                    titulo = "Empates",
                    valor = "${jogador.empates}",
                    modifier = Modifier.weight(1f)
                )
                EstatisticaItem(
                    titulo = "Derrotas",
                    valor = "${jogador.derrotas}",
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

// Redesign (1ª onda): delega pro StatTile compartilhado (ui/common) em vez de desenhar o próprio
// par valor+rótulo — mesmo componente usado em "Meu Histórico" da tela de Perfil.
@Composable
fun EstatisticaItem(
    titulo: String,
    valor: String,
    modifier: Modifier = Modifier
) {
    com.victorhugo.boleiragem.ui.common.StatTile(
        valor = valor,
        titulo = titulo,
        modifier = modifier.padding(8.dp)
    )
}

@Composable
fun LegendaItem(
    texto: String,
    cor: Color,
    percentual: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(end = 8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .background(cor)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = "$texto ($percentual)",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )
    }
}

@Composable
fun DesempenhoChart(
    taxaVitoria: Float,
    taxaEmpate: Float,
    taxaDerrota: Float,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .weight(taxaVitoria.coerceAtLeast(0.1f))
                .fillMaxHeight()
                .background(Color(0xFF4CAF50))
        )
        Box(
            modifier = Modifier
                .weight(taxaEmpate.coerceAtLeast(0.1f))
                .fillMaxHeight()
                .background(Color(0xFFFFC107))
        )
        Box(
            modifier = Modifier
                .weight(taxaDerrota.coerceAtLeast(0.1f))
                .fillMaxHeight()
                .background(Color(0xFFF44336))
        )
    }
}
