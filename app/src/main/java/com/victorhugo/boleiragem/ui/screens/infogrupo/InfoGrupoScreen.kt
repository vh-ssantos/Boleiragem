package com.victorhugo.boleiragem.ui.screens.infogrupo

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.victorhugo.boleiragem.data.model.TipoRecorrencia
import com.victorhugo.boleiragem.ui.common.SectionCard
import com.victorhugo.boleiragem.ui.common.StatTile
import com.victorhugo.boleiragem.ui.screens.grupos.ChipPapel
import com.victorhugo.boleiragem.ui.screens.grupos.formatarHorarioGrupo

/**
 * Primeira tela dentro de um grupo — "sente que aquilo é uma comunidade" (pedido do usuário):
 * nome, participantes, local, horário e contador pra próxima pelada. Só leitura nesta v1 — editar
 * o grupo continua sendo feito pela tela de Grupos.
 */
@Composable
fun InfoGrupoScreen(
    grupoId: Long,
    viewModel: InfoGrupoViewModel = hiltViewModel()
) {
    LaunchedEffect(grupoId) {
        viewModel.setGrupoId(grupoId)
    }

    val uiState by viewModel.uiState.collectAsState()
    val grupo = uiState.grupo

    if (uiState.carregando || grupo == null) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            CircularProgressIndicator()
        }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = grupo.nome,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            if (grupo.firestoreId != null) {
                ChipPapel(uiState.papel)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        SectionCard(titulo = "Onde e quando") {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.LocationOn, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(grupo.local, style = MaterialTheme.typography.bodyLarge)
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.DateRange, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(formatarHorarioGrupo(grupo), style = MaterialTheme.typography.bodyLarge)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        SectionCard(titulo = "Participantes") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatTile(uiState.jogadoresCadastrados.toString(), "Jogadores cadastrados")
                uiState.membrosDoGrupo?.let { membros ->
                    StatTile(membros.toString(), "Membros do grupo")
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        SectionCard(titulo = "Próxima pelada") {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.CalendarMonth, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = when {
                        grupo.tipoRecorrencia != TipoRecorrencia.RECORRENTE ->
                            "Pelada esporádica — datas combinadas à parte"
                        uiState.diasAteProximaPelada == null -> "Nenhum dia da semana configurado"
                        uiState.diasAteProximaPelada == 0 -> "Pelada hoje!"
                        uiState.diasAteProximaPelada == 1 -> "Falta 1 dia para a próxima pelada"
                        else -> "Faltam ${uiState.diasAteProximaPelada} dias para a próxima pelada"
                    },
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }
    }
}
