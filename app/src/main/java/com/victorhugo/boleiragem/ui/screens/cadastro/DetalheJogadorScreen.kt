package com.victorhugo.boleiragem.ui.screens.cadastro

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.victorhugo.boleiragem.data.model.PosicaoJogador

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetalheJogadorScreen(
    jogadorId: Long,
    onBackClick: () -> Unit,
    podeEditar: Boolean = true,
    viewModel: DetalheJogadorViewModel = hiltViewModel()
) {
    val jogadorState = viewModel.jogador.collectAsState(null)
    val salvoState = viewModel.salvo.collectAsState()

    // Resetar o estado de salvo quando a composição entra
    DisposableEffect(Unit) {
        viewModel.resetarSalvo()
        onDispose { }
    }

    LaunchedEffect(jogadorId) {
        viewModel.carregarJogador(jogadorId)
    }

    LaunchedEffect(salvoState.value) {
        if (salvoState.value) {
            // Só fecha a tela se o jogador foi realmente salvo
            onBackClick()
            viewModel.resetarSalvo() // Garante que o estado seja resetado após navegar de volta
        }
    }

    val jogador = jogadorState.value

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (jogadorId > 0) "Editar Jogador" else "Novo Jogador") },
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
                    if (podeEditar) viewModel.salvarJogador()
                },
                containerColor = if (podeEditar) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                contentColor = if (podeEditar) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
            ) {
                Icon(Icons.Default.Check, contentDescription = "Salvar")
            }
        }
    ) { paddingValues ->
        if (jogadorState.value != null || jogadorId <= 0) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = viewModel.nome,
                    onValueChange = { viewModel.nome = it },
                    label = { Text("Nome") },
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Posição Principal", fontWeight = FontWeight.Bold)
                PosicaoDropdown(
                    posicaoSelecionada = viewModel.posicaoPrincipal,
                    onPosicaoSelecionada = { viewModel.posicaoPrincipal = it }
                )

                Text("Nota Posição Principal", fontWeight = FontWeight.Bold)
                NotaRatingBar(
                    nota = viewModel.notaPrincipal,
                    onNotaChange = { viewModel.notaPrincipal = it }
                )

                Text("Posição Secundária (opcional)", fontWeight = FontWeight.Bold)
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = viewModel.possuiSecundaria,
                        onCheckedChange = { viewModel.possuiSecundaria = it }
                    )
                    Text("Possui posição secundária")
                }

                if (viewModel.possuiSecundaria) {
                    PosicaoDropdown(
                        posicaoSelecionada = viewModel.posicaoSecundaria ?: PosicaoJogador.PIVO,
                        onPosicaoSelecionada = { viewModel.posicaoSecundaria = it }
                    )

                    Text("Nota Posição Secundária", fontWeight = FontWeight.Bold)
                    NotaRatingBar(
                        nota = viewModel.notaSecundaria ?: 3,
                        onNotaChange = { viewModel.notaSecundaria = it }
                    )
                }
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }
    }
}
