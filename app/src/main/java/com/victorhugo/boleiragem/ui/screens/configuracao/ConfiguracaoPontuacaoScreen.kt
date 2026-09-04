package com.victorhugo.boleiragem.ui.screens.configuracao

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConfiguracaoPontuacaoScreen(
    viewModel: ConfiguracaoPontuacaoViewModel = hiltViewModel(),
    grupoId: Long = -1L,
    podeEditar: Boolean = true,
    onBackClick: () -> Unit
) {
    LaunchedEffect(grupoId) {
        viewModel.setGrupoId(grupoId)
    }

    val configuracao by viewModel.configuracaoPontuacao.collectAsState(null)
    val salvandoConfiguracao by viewModel.salvandoConfiguracao.collectAsState()
    val configuracaoSalva by viewModel.configuracaoSalva.collectAsState()

    // Estados locais para controlar os valores dos campos de texto
    var pontosPorVitoria by remember { mutableStateOf("10") }
    var pontosPorDerrota by remember { mutableStateOf("-10") }
    var pontosPorEmpate by remember { mutableStateOf("-5") }

    // Estado para verificar se os valores foram alterados
    var valoresAlterados by remember { mutableStateOf(false) }

    // Atualiza os estados locais quando a configuração mudar e não for nula
    LaunchedEffect(configuracao) {
        if (configuracao != null) {
            pontosPorVitoria = configuracao!!.pontosPorVitoria.toString()
            pontosPorDerrota = configuracao!!.pontosPorDerrota.toString()
            pontosPorEmpate = configuracao!!.pontosPorEmpate.toString()
        }
    }

    // Verifica se os valores foram alterados apenas se configuracao não for nula
    LaunchedEffect(pontosPorVitoria, pontosPorDerrota, pontosPorEmpate, configuracao) {
        if (configuracao != null) {
            valoresAlterados = (pontosPorVitoria != configuracao!!.pontosPorVitoria.toString() ||
                    pontosPorDerrota != configuracao!!.pontosPorDerrota.toString() ||
                    pontosPorEmpate != configuracao!!.pontosPorEmpate.toString())
        }
    }

    // Efeito para voltar automaticamente quando a configuração for salva com sucesso
    LaunchedEffect(configuracaoSalva) {
        if (configuracaoSalva) {
            // Volta para a tela anterior após salvar com sucesso
            onBackClick()
        }
    }

    val snackbarHostState = remember { SnackbarHostState() }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Configuração de Pontuação") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Voltar")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    if (!podeEditar) return@FloatingActionButton
                    // Converte os valores de texto para números e atualiza o ViewModel
                    viewModel.atualizarPontosPorVitoria(pontosPorVitoria.toIntOrNull() ?: 10)
                    viewModel.atualizarPontosPorDerrota(pontosPorDerrota.toIntOrNull() ?: -10)
                    viewModel.atualizarPontosPorEmpate(pontosPorEmpate.toIntOrNull() ?: -5)

                    // Salva a configuração
                    viewModel.salvarConfiguracao()
                },
                containerColor = if (podeEditar) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                contentColor = if (podeEditar) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
            ) {
                Icon(Icons.Default.Check, contentDescription = "Salvar")
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (salvandoConfiguracao) {
                // Mostra um indicador de progresso enquanto salva
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "Configure a pontuação dos jogadores",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    Text(
                        text = "Defina quantos pontos cada resultado vale para um jogador. " +
                              "Valores negativos reduzem a pontuação.",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(bottom = 24.dp)
                    )

                    // Alerta de recálculo quando valores forem alterados
                    if (valoresAlterados) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = "Informação",
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(end = 8.dp)
                                )
                                Text(
                                    text = "Ao salvar, a pontuação de todos os jogadores será recalculada com base nos novos valores.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }

                    PontuacaoInputField(
                        label = "Pontos por vitória",
                        value = pontosPorVitoria,
                        onValueChange = { pontosPorVitoria = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp)
                    )

                    PontuacaoInputField(
                        label = "Pontos por derrota",
                        value = pontosPorDerrota,
                        onValueChange = { pontosPorDerrota = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp)
                    )

                    PontuacaoInputField(
                        label = "Pontos por empate",
                        value = pontosPorEmpate,
                        onValueChange = { pontosPorEmpate = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 24.dp)
                    )

                    Text(
                        text = "Observação: Ao alterar esses valores, a pontuação de todos os " +
                               "jogadores será recalculada automaticamente com base no " +
                               "histórico de resultados.",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PontuacaoInputField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = value,
        onValueChange = {
            // Aceita apenas números, sinal de negativo no início e sem espaços
            val filteredValue = it.replace(Regex("[^0-9-]"), "")

            // Garante que o sinal de negativo só aparece no início
            val finalValue = if (filteredValue.count { it == '-' } > 1) {
                filteredValue.replace("-", "").let { v -> if (filteredValue.startsWith("-")) "-$v" else v }
            } else {
                filteredValue
            }

            onValueChange(finalValue)
        },
        label = { Text(label) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        singleLine = true,
        modifier = modifier
    )
}
