package com.victorhugo.boleiragem.ui.screens.cronometro

import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
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
import com.victorhugo.boleiragem.ui.common.OutlinedTextFieldComAcentos
import kotlin.math.ceil

private fun formatarTempo(ms: Long): String {
    val totalSegundos = ceil(ms / 1000.0).toLong().coerceAtLeast(0)
    val horas = totalSegundos / 3600
    val minutos = (totalSegundos % 3600) / 60
    val segundos = totalSegundos % 60
    return if (horas > 0) {
        "%02d:%02d:%02d".format(horas, minutos, segundos)
    } else {
        "%02d:%02d".format(minutos, segundos)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CronometroScreen(
    viewModel: CronometroViewModel = hiltViewModel(),
    onBackClick: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var minutosCustom by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Cronômetro") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                SegmentedButton(
                    selected = uiState.modo == ModoCronometro.PROGRESSIVO,
                    onClick = { viewModel.selecionarModo(ModoCronometro.PROGRESSIVO) },
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                ) {
                    Text("Progressivo")
                }
                SegmentedButton(
                    selected = uiState.modo == ModoCronometro.REGRESSIVO,
                    onClick = { viewModel.selecionarModo(ModoCronometro.REGRESSIVO) },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                ) {
                    Text("Regressivo")
                }
            }

            Spacer(modifier = Modifier.height(40.dp))

            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = formatarTempo(uiState.tempoMs),
                    fontSize = 64.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (uiState.esgotado) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                )
            }

            if (uiState.esgotado) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Tempo esgotado!",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            if (uiState.modo == ModoCronometro.REGRESSIVO && !uiState.rodando) {
                Text(
                    text = "Duração",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(5, 10, 15, 20, 45).forEach { minutos ->
                        AssistChip(
                            onClick = { viewModel.definirDuracaoRegressiva(minutos) },
                            label = { Text("${minutos}min") }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextFieldComAcentos(
                        value = minutosCustom,
                        onValueChange = { novo -> if (novo.length <= 3 && novo.all(Char::isDigit)) minutosCustom = novo },
                        label = { Text("Minutos") },
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Number,
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    OutlinedButton(onClick = {
                        minutosCustom.toIntOrNull()?.let { viewModel.definirDuracaoRegressiva(it) }
                    }) {
                        Text("Definir")
                    }
                }
                Spacer(modifier = Modifier.height(32.dp))
            }

            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                if (uiState.rodando) {
                    Button(onClick = viewModel::pausar) {
                        Icon(Icons.Filled.Pause, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Pausar")
                    }
                } else {
                    Button(onClick = viewModel::iniciar, enabled = !uiState.esgotado) {
                        Icon(Icons.Filled.PlayArrow, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Iniciar")
                    }
                }
                OutlinedButton(
                    onClick = viewModel::reiniciar,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurface)
                ) {
                    Icon(Icons.Filled.Replay, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Reiniciar")
                }
            }
        }
    }
}
