package com.victorhugo.boleiragem.ui.screens.perfil

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.victorhugo.boleiragem.data.model.HistoricoPelada
import com.victorhugo.boleiragem.data.repository.EstatisticasPessoais
import com.victorhugo.boleiragem.ui.common.EmptyState
import com.victorhugo.boleiragem.ui.common.StatTile
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Tela cheia "Meu Histórico" — aberta a partir do menu de perfil. Mostra os agregados (que antes
 * ficavam direto no sheet) e, abaixo, uma lista com cada pelada finalizada em que o usuário
 * participou (via `HistoricoRepository.observarPeladasDoUsuario`).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TelaMeuHistorico(
    usuarioUid: String,
    estatisticas: EstatisticasPessoais?,
    peladas: List<HistoricoPelada>,
    onBackClick: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Meu Histórico") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                }
            )
        }
    ) { innerPadding ->
        if (peladas.isEmpty()) {
            EmptyState(
                icone = Icons.Filled.Info,
                titulo = "Nenhuma pelada ainda",
                descricao = "Você ainda não finalizou nenhuma pelada como jogador vinculado à sua conta.",
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            )
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp)
        ) {
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        StatTile((estatisticas?.peladasJogadas ?: 0).toString(), "Peladas")
                        StatTile((estatisticas?.vitorias ?: 0).toString(), "Vitórias")
                        StatTile((estatisticas?.empates ?: 0).toString(), "Empates")
                        StatTile((estatisticas?.derrotas ?: 0).toString(), "Derrotas")
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Peladas",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            items(peladas) { pelada ->
                PeladaItem(pelada = pelada, usuarioUid = usuarioUid)
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun PeladaItem(pelada: HistoricoPelada, usuarioUid: String) {
    val meuTime = pelada.times.firstOrNull { it.usuariosUids?.contains(usuarioUid) == true }
    val formatador = remember(pelada.id) { SimpleDateFormat("dd/MM/yyyy", Locale("pt", "BR")) }

    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = formatador.format(Date(pelada.dataFinalizacao)),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = meuTime?.nome ?: "Time",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (meuTime != null) {
                Text(
                    text = "${meuTime.vitorias}V ${meuTime.empates}E ${meuTime.derrotas}D",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}
