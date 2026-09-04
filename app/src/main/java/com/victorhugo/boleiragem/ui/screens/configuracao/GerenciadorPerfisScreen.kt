package com.victorhugo.boleiragem.ui.screens.configuracao

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import com.victorhugo.boleiragem.data.model.ConfiguracaoSorteio
import com.victorhugo.boleiragem.data.model.CriterioSorteio

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GerenciadorPerfisScreen(
    viewModel: GerenciadorPerfisViewModel = hiltViewModel(),
    grupoId: Long = 0L, // Adicionando o parâmetro grupoId com valor padrão
    podeEditar: Boolean = true,
    onNavigateBack: () -> Unit
) {
    val perfis by viewModel.perfis.collectAsState()
    val perfilEmEdicao by viewModel.perfilEmEdicao.collectAsState()
    val perfilParaExcluir by viewModel.perfilParaExcluir.collectAsState()

    // Efeito para definir o ID do grupo quando a tela é carregada
    LaunchedEffect(grupoId) {
        viewModel.setGrupoId(grupoId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Perfis de Configuração") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Voltar"
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { if (podeEditar) viewModel.criarNovoPerfil() },
                containerColor = if (podeEditar) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
            ) {
                Icon(Icons.Default.Add, contentDescription = "Adicionar Perfil")
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (perfis.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Nenhum perfil configurado.\nClique no + para adicionar um novo perfil.",
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = paddingValues
                ) {
                    items(perfis) { perfil ->
                        PerfilItem(
                            perfil = perfil,
                            onEdit = { viewModel.editarPerfil(perfil) },
                            onDelete = { viewModel.confirmarExclusao(perfil) },
                            onSetDefault = { viewModel.definirComoPadrao(perfil.id) },
                            isUnicoPerfil = perfis.size <= 1, // Desabilita a exclusão quando há apenas um perfil
                            podeEditar = podeEditar
                        )
                    }
                }
            }
        }
    }

    // Diálogo de edição/criação de perfil
    perfilEmEdicao?.let { perfil ->
        PerfilEditDialog(
            perfil = perfil,
            onSave = { viewModel.salvarPerfil(it) },
            onCancel = { viewModel.cancelarEdicao() }
        )
    }

    // Diálogo de confirmação de exclusão
    perfilParaExcluir?.let { perfil ->
        AlertDialog(
            onDismissRequest = { viewModel.cancelarExclusao() },
            title = { Text("Excluir Perfil") },
            text = { Text("Tem certeza que deseja excluir o perfil '${perfil.nome}'?") },
            confirmButton = {
                Button(onClick = { viewModel.excluirPerfil() }) {
                    Text("Confirmar")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.cancelarExclusao() }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
fun PerfilItem(
    perfil: ConfiguracaoSorteio,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onSetDefault: () -> Unit,
    isUnicoPerfil: Boolean = false, // Novo parâmetro para verificar se é o único perfil
    podeEditar: Boolean = true
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = perfil.nome,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )

                    if (perfil.isPadrao) {
                        Box(
                            modifier = Modifier
                                .padding(start = 8.dp)
                                .background(
                                    color = MaterialTheme.colorScheme.primary,
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "Padrão",
                                color = Color.White,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }

                Row {
                    IconButton(onClick = onEdit, enabled = podeEditar) {
                        Icon(Icons.Default.Edit, contentDescription = "Editar")
                    }
                    // Desativar o botão de exclusão se for o único perfil ou o usuário não puder editar
                    IconButton(
                        onClick = onDelete,
                        enabled = !isUnicoPerfil && podeEditar
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Excluir",
                            tint = if (isUnicoPerfil || !podeEditar) Color.Gray else MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Times: ${perfil.qtdTimes} × ${perfil.qtdJogadoresPorTime} jogadores",
                style = MaterialTheme.typography.bodyMedium
            )

            Text(
                text = "Sorteio: ${if (perfil.aleatorio) "Aleatório" else "Balanceado"}",
                style = MaterialTheme.typography.bodyMedium
            )

            if (!perfil.isPadrao) {
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = onSetDefault,
                    enabled = podeEditar,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("Definir como Padrão")
                }
            }
        }
    }
}

@Composable
fun PerfilEditDialog(
    perfil: ConfiguracaoSorteio,
    onSave: (ConfiguracaoSorteio) -> Unit,
    onCancel: () -> Unit
) {
    var nome by remember { mutableStateOf(perfil.nome) }
    var qtdJogadoresPorTime by remember { mutableIntStateOf(perfil.qtdJogadoresPorTime) }
    var qtdTimes by remember { mutableIntStateOf(perfil.qtdTimes) }
    var aleatorio by remember { mutableStateOf(perfil.aleatorio) }

    Dialog(onDismissRequest = onCancel) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = if (perfil.id == 0L) "Novo Perfil" else "Editar Perfil",
                    style = MaterialTheme.typography.titleLarge
                )

                OutlinedTextField(
                    value = nome,
                    onValueChange = { nome = it },
                    label = { Text("Nome do Perfil") },
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Número de Times: $qtdTimes")
                Slider(
                    value = qtdTimes.toFloat(),
                    onValueChange = { qtdTimes = it.toInt() },
                    valueRange = 2f..6f,
                    steps = 3,
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Jogadores por Time: $qtdJogadoresPorTime")
                Slider(
                    value = qtdJogadoresPorTime.toFloat(),
                    onValueChange = { qtdJogadoresPorTime = it.toInt() },
                    valueRange = 3f..11f,
                    steps = 7,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Sorteio Aleatório")
                    Switch(
                        checked = aleatorio,
                        onCheckedChange = { aleatorio = it }
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onCancel) {
                        Text("Cancelar")
                    }

                    Button(
                        onClick = {
                            onSave(
                                perfil.copy(
                                    nome = nome,
                                    qtdJogadoresPorTime = qtdJogadoresPorTime,
                                    qtdTimes = qtdTimes,
                                    aleatorio = aleatorio
                                )
                            )
                        }
                    ) {
                        Text("Salvar")
                    }
                }
            }
        }
    }
}
