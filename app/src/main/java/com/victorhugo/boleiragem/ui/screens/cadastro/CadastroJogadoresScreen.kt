package com.victorhugo.boleiragem.ui.screens.cadastro

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.victorhugo.boleiragem.data.model.Jogador
import com.victorhugo.boleiragem.data.model.PosicaoJogador

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CadastroJogadoresScreen(
viewModel: CadastroJogadoresViewModel = hiltViewModel(),
grupoId: Long, // Adicionado este parâmetro
podeEditar: Boolean = true,
onNavigateToDetalheJogador: (Long) -> Unit
) {
    // Defina o grupoId no ViewModel
    viewModel.setGrupoId(grupoId)

    val jogadores by viewModel.jogadores.collectAsState(initial = emptyList())
    var showAddJogadorDialog by remember { mutableStateOf(false) }
    var showOrdenarDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        TopAppBar(
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Cadastro de Jogadores")
                    Spacer(modifier = Modifier.width(8.dp))
                    // Contador de jogadores
                    Badge(
                        containerColor = MaterialTheme.colorScheme.onPrimary,
                        contentColor = MaterialTheme.colorScheme.primary,
                    ) {
                        Text(
                            text = "${jogadores.size}",
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }
            },
            actions = {
                // Botão de ordenação
                IconButton(onClick = { showOrdenarDialog = true }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Sort,
                        contentDescription = "Ordenar jogadores",
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.primary,
                titleContentColor = MaterialTheme.colorScheme.onPrimary
            )
        )

        Box(
            modifier = Modifier.weight(1f)
        ) {
            if (jogadores.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Nenhum jogador cadastrado.",
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Clique no botão + abaixo para adicionar jogadores.",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = 16.dp,
                        end = 16.dp,
                        top = 16.dp,
                        // Adiciona padding extra embaixo para o botão não sobrepor os itens
                        bottom = 80.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(jogadores) { jogador ->
                        JogadorItem(
                            jogador = jogador,
                            onEditClick = { onNavigateToDetalheJogador(jogador.id) },
                            onDeleteClick = { viewModel.deletarJogador(jogador) },
                            podeEditar = podeEditar
                        )
                    }
                }
            }

            // Botão flutuante no canto inferior da tela, mas dentro da Box. Desabilitado (não
            // escondido) pra quem não é Dono/Responsável — ExtendedFloatingActionButton não tem
            // parâmetro `enabled`, então o corte é feito no próprio onClick + cores esmaecidas.
            ExtendedFloatingActionButton(
                onClick = { if (podeEditar) showAddJogadorDialog = true },
                icon = { Icon(Icons.Default.Add, contentDescription = "Adicionar") },
                text = { Text("Adicionar Jogador") },
                containerColor = if (podeEditar) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                contentColor = if (podeEditar) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 16.dp)
            )
        }
    }

    if (showAddJogadorDialog) {
        JogadorDialog(
            onDismiss = { showAddJogadorDialog = false },
            onConfirm = { nome, posicaoPrincipal, posicaoSecundaria, notaPrincipal, notaSecundaria ->
                val novoJogador = Jogador(
                    grupoId = 0, // Será definido pelo ViewModel baseado no grupo atual
                    nome = nome,
                    posicaoPrincipal = posicaoPrincipal,
                    posicaoSecundaria = posicaoSecundaria,
                    notaPosicaoPrincipal = notaPrincipal,
                    notaPosicaoSecundaria = notaSecundaria
                )
                viewModel.inserirJogador(novoJogador)
                showAddJogadorDialog = false
            }
        )
    }

    // Chamada do diálogo de ordenação
    if (showOrdenarDialog) {
        OrdenarDialog(
            onDismiss = { showOrdenarDialog = false },
            criterioAtual = viewModel.criterioOrdenacao,
            onOrdenar = { criterio ->
                viewModel.mudarCriterioOrdenacao(criterio)
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JogadorItem(
    jogador: Jogador,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
    podeEditar: Boolean = true
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        onClick = onEditClick
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Informações básicas do jogador
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = jogador.nome,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Principal: ${jogador.posicaoPrincipal.name} (${jogador.notaPosicaoPrincipal}⭐)",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    jogador.posicaoSecundaria?.let { posicao ->
                        Text(
                            text = "Secundária: ${posicao.name} (${jogador.notaPosicaoSecundaria}⭐)",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }

                IconButton(onClick = onEditClick, enabled = podeEditar) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Editar",
                        tint = if (podeEditar) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                IconButton(onClick = onDeleteClick, enabled = podeEditar) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Excluir",
                        tint = if (podeEditar) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Estatísticas do jogador
            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(Modifier, DividerDefaults.Thickness, DividerDefaults.color)
            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Estatísticas",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = "Jogos", style = MaterialTheme.typography.bodySmall)
                    Text(text = "${jogador.totalJogos}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = "Vitórias", style = MaterialTheme.typography.bodySmall)
                    Text(text = "${jogador.vitorias}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = "Empates", style = MaterialTheme.typography.bodySmall)
                    Text(text = "${jogador.empates}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.tertiary)
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = "Derrotas", style = MaterialTheme.typography.bodySmall)
                    Text(text = "${jogador.derrotas}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.EmojiEvents,
                    contentDescription = "Pontuação",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Pontuação: ${jogador.pontuacaoTotal}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun JogadorDialog(
    jogadorExistente: Jogador? = null,
    onDismiss: () -> Unit,
    onConfirm: (String, PosicaoJogador, PosicaoJogador?, Int, Int?) -> Unit
) {
    var nome by remember { mutableStateOf(jogadorExistente?.nome ?: "") }
    var posicaoPrincipal by remember {
        mutableStateOf(jogadorExistente?.posicaoPrincipal ?: PosicaoJogador.MEIO_CAMPO)
    }
    var posicaoSecundaria by remember {
        mutableStateOf(jogadorExistente?.posicaoSecundaria)
    }
    var notaPrincipal by remember {
        mutableIntStateOf(jogadorExistente?.notaPosicaoPrincipal ?: 3)
    }
    var notaSecundaria by remember {
        mutableStateOf(jogadorExistente?.notaPosicaoSecundaria ?: 3)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = if (jogadorExistente == null) "Novo Jogador" else "Editar Jogador") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
            ) {
                OutlinedTextField(
                    value = nome,
                    onValueChange = { nome = it },
                    label = { Text("Nome do Jogador") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text("Posição Principal:", fontWeight = FontWeight.Bold)
                PosicaoDropdown(
                    posicaoSelecionada = posicaoPrincipal,
                    onPosicaoSelecionada = { posicaoPrincipal = it }
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text("Nota para posição principal:", fontWeight = FontWeight.Bold)
                NotaRatingBar(
                    nota = notaPrincipal,
                    onNotaChange = { notaPrincipal = it }
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text("Posição Secundária (opcional):", fontWeight = FontWeight.Bold)
                PosicaoDropdown(
                    posicaoSelecionada = posicaoSecundaria,
                    onPosicaoSelecionada = { posicaoSecundaria = it },
                    opcional = true
                )

                if (posicaoSecundaria != null) {
                    Spacer(modifier = Modifier.height(8.dp))

                    Text("Nota para posição secundária:", fontWeight = FontWeight.Bold)
                    NotaRatingBar(
                        nota = notaSecundaria,
                        onNotaChange = { notaSecundaria = it }
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(
                        nome,
                        posicaoPrincipal,
                        posicaoSecundaria,
                        notaPrincipal,
                        if (posicaoSecundaria != null) notaSecundaria else null
                    )
                },
                enabled = nome.isNotBlank()
            ) {
                Text("Confirmar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PosicaoDropdown(
    posicaoSelecionada: PosicaoJogador?,
    onPosicaoSelecionada: (PosicaoJogador) -> Unit,
    opcional: Boolean = false
) {
    var expanded by remember { mutableStateOf(false) }
    val posicoes = PosicaoJogador.entries.toTypedArray()

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
    ) {
        OutlinedTextField(
            value = posicaoSelecionada?.name ?: if (opcional) "Nenhuma" else "",
            onValueChange = {},
            readOnly = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth()
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            if (opcional) {
                DropdownMenuItem(
                    text = { Text("Nenhuma") },
                    onClick = {
                        onPosicaoSelecionada(posicoes[0]) // Valor temporário
                        expanded = false
                    }
                )
            }

            posicoes.forEach { posicao ->
                DropdownMenuItem(
                    text = { Text(posicao.name) },
                    onClick = {
                        onPosicaoSelecionada(posicao)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
fun NotaRatingBar(
    nota: Int,
    onNotaChange: (Int) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        for (i in 1..5) {
            IconButton(
                onClick = { onNotaChange(i) },
            ) {
                Icon(
                    imageVector = androidx.compose.material.icons.Icons.Filled.Star,
                    contentDescription = "Estrela $i",
                    tint = if (i <= nota) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                )
            }
        }
    }
}
