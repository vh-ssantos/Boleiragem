package com.victorhugo.boleiragem.ui.screens.grupos

import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import com.victorhugo.boleiragem.R
import com.victorhugo.boleiragem.data.model.DiaSemana
import com.victorhugo.boleiragem.data.model.GrupoPelada
import com.victorhugo.boleiragem.data.model.GrupoRemoto
import com.victorhugo.boleiragem.data.model.PapelGrupo
import com.victorhugo.boleiragem.data.model.TipoRecorrencia
import com.victorhugo.boleiragem.data.model.UsuarioPerfil
import com.victorhugo.boleiragem.ui.screens.compartilhar.CompartilharPeladaScreen
import com.victorhugo.boleiragem.ui.screens.grupos.dialogs.SorteioRapidoDialog
import com.victorhugo.boleiragem.ui.screens.sorteio.ColaListaJogadoresDialog
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GruposPeladaScreen(
    viewModel: GruposPeladaViewModel = hiltViewModel(),
    convidadoSessaoId: String? = null,
    onGrupoSelecionado: (Long, String) -> Unit = { _, _ -> },
    onNavigateToSorteioResultado: (isSorteioRapido: Boolean) -> Unit,
    onSairClick: () -> Unit
) {
    val grupos by viewModel.grupos.collectAsState()
    val carregando by viewModel.carregando.collectAsState()
    val mostrarDialogoGrupo by viewModel.mostrarDialogoGrupo.collectAsState()
    val grupoEmEdicao by viewModel.grupoEmEdicao.collectAsState()
    val context = LocalContext.current

    var mostrarDialogoSelecionarGrupoParaSorteioRapido by remember { mutableStateOf(false) }
    var mostrarDialogoColaLista by remember { mutableStateOf(false) }
    var mostrarDialogoOpcoesSorteio by remember { mutableStateOf(false) } // Novo estado
    // Cronômetro solto (sem depender de grupo) — pedido pra funcionar mesmo antes de criar peladas
    var mostrarCronometro by remember { mutableStateOf(false) }

    val navegarParaDetalheGrupo: (GrupoPelada) -> Unit = {
        grupo -> onGrupoSelecionado(grupo.id, grupo.nome)
    }

    fun compartilharTexto(texto: String, context: Context) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, texto)
        }
        context.startActivity(Intent.createChooser(intent, "Compartilhar via"))
    }

    val mostrarTelaCompartilhamento by viewModel.mostrarTelaCompartilhamento.collectAsState()
    val grupoParaCompartilhar by viewModel.grupoParaCompartilhar.collectAsState()
    val grupoParaCompartilharNaoNulo = grupoParaCompartilhar

    val mostrarDialogoConfigSorteioRapido by viewModel.mostrarDialogoConfigSorteioRapido.collectAsState()
    val grupoSelecionadoParaSorteioRapido by viewModel.grupoSelecionadoParaSorteioRapido.collectAsState()
    val jogadoresAtivosParaDialogoSorteioRapido by viewModel.jogadoresAtivosParaDialogoSorteioRapido.collectAsState()
    val perfisConfiguracaoSorteio by viewModel.perfisConfiguracaoSorteio.collectAsState()
    val usarPerfilExistenteSorteioRapido by viewModel.usarPerfilExistenteSorteioRapido.collectAsState()
    val perfilConfigSelecionadoSorteioRapido by viewModel.perfilConfigSelecionadoSorteioRapido.collectAsState()
    val jogadoresPorTimeSorteioRapido by viewModel.jogadoresPorTimeSorteioRapido.collectAsState()
    val numeroDeTimesSorteioRapido by viewModel.numeroDeTimesSorteioRapido.collectAsState()
    val navegarParaResultadoSorteio by viewModel.navegarParaResultadoSorteio.collectAsState()
    val erroSorteioRapidoAtual by viewModel.erroSorteioRapido.collectAsState()
    val podeRealizarSorteioRapido by viewModel.podeRealizarSorteioRapido.collectAsState()

    // Grupos compartilhados (Firestore)
    val papeisGrupos by viewModel.papeisGrupos.collectAsState()
    val mostrarDialogoEntrarComCodigo by viewModel.mostrarDialogoEntrarComCodigo.collectAsState()
    val erroEntrarComCodigo by viewModel.erroEntrarComCodigo.collectAsState()
    val carregandoEntrarComCodigo by viewModel.carregandoEntrarComCodigo.collectAsState()
    val convitePendente by viewModel.convitePendente.collectAsState()
    val grupoParaConvidar by viewModel.grupoParaConvidar.collectAsState()
    val carregandoConvite by viewModel.carregandoConvite.collectAsState()
    val grupoParaGerenciarMembros by viewModel.grupoParaGerenciarMembros.collectAsState()
    val perfisMembros by viewModel.perfisMembros.collectAsState()

    LaunchedEffect(convidadoSessaoId) {
        viewModel.definirSessaoConvidado(convidadoSessaoId)
    }

    LaunchedEffect(navegarParaResultadoSorteio) {
        if (navegarParaResultadoSorteio == true) {
            viewModel.onNavegacaoParaResultadoSorteioRealizada()
            try {
                onNavigateToSorteioResultado(true)
            } catch (e: Exception) {
                Log.e("GruposPeladaScreen", "Erro ao solicitar navegação para ResultadoSorteio", e)
            }
        }
    }

    if (mostrarTelaCompartilhamento && grupoParaCompartilharNaoNulo != null) {
        CompartilharPeladaScreen(
            grupo = grupoParaCompartilharNaoNulo,
            onCompartilhar = { texto -> compartilharTexto(texto, context) },
            onVoltar = { viewModel.ocultarTelaCompartilhamento() }
        )
    } else {
        Scaffold(
            topBar = {},
            floatingActionButton = {
                ExtendedFloatingActionButton(
                    onClick = { viewModel.mostrarDialogoCriarGrupo() },
                    icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                    text = { Text("Criar grupo") },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            }
        ) { paddingValues ->
            HomeMinhasPeladas(
                grupos = grupos,
                papeisGrupos = papeisGrupos,
                carregando = carregando,
                paddingValues = paddingValues,
                onSairClick = onSairClick,
                onGrupoClick = navegarParaDetalheGrupo,
                onEditarClick = { viewModel.mostrarDialogoEditarGrupo(it) },
                onExcluirClick = { viewModel.excluirGrupo(it) },
                onConvidarClick = { viewModel.abrirConvite(it) },
                onSairDoGrupoClick = { grupo -> grupo.firestoreId?.let { viewModel.sairDeGrupoCompartilhadoPorId(it) } },
                onCriarGrupoClick = { viewModel.mostrarDialogoCriarGrupo() },
                onEntrarComCodigoClick = { viewModel.abrirDialogoEntrarComCodigo() },
                onSorteioClick = { mostrarDialogoOpcoesSorteio = true },
                onCronometroClick = { mostrarCronometro = true }
            )
        }
    }

    if (mostrarDialogoGrupo) {
        DialogoAdicionarEditarGrupo(
            grupo = grupoEmEdicao,
            onDismissRequest = { viewModel.fecharDialogoGrupo() },
            onSalvar = { nome, local, horario, imagemUrl, descricao, tipoRecorrencia, diaSemana ->
                viewModel.salvarGrupo(
                    nome, local, horario, imagemUrl, descricao,
                    tipoRecorrencia, diaSemana, null, null,
                    null, null, emptyList()
                )
            }
        )
    }

    if (mostrarDialogoColaLista) {
        ColaListaJogadoresDialog(
            onDismissRequest = { mostrarDialogoColaLista = false },
            onConfirmar = { textoColado ->
                mostrarDialogoColaLista = false
                val nomesParseados = viewModel.parsearListaDeJogadores(textoColado)
                if (nomesParseados.isNotEmpty()) {
                    viewModel.prepararSorteioDeListaColada(nomesParseados)
                } else {
                    Toast.makeText(context, "Nenhum nome válido encontrado na lista.", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }

    if (mostrarCronometro) {
        Dialog(
            onDismissRequest = { mostrarCronometro = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            com.victorhugo.boleiragem.ui.screens.cronometro.CronometroScreen(
                onBackClick = { mostrarCronometro = false }
            )
        }
    }

    // Novo diálogo de Opções de Sorteio
    if (mostrarDialogoOpcoesSorteio) {
        DialogoOpcoesSorteio(
            onDismissRequest = { mostrarDialogoOpcoesSorteio = false },
            onColarListaClick = {
                mostrarDialogoOpcoesSorteio = false
                mostrarDialogoColaLista = true
            },
            onSelecionarGrupoClick = {
                mostrarDialogoOpcoesSorteio = false
                if (grupos.isNotEmpty()) {
                    mostrarDialogoSelecionarGrupoParaSorteioRapido = true
                } else {
                    Toast.makeText(context, "Nenhum grupo cadastrado. Crie um grupo primeiro.", Toast.LENGTH_LONG).show()
                }
            },
            temGrupos = grupos.isNotEmpty()
        )
    }

    SorteioRapidoDialog(
        showDialog = mostrarDialogoConfigSorteioRapido,
        grupoPelada = grupoSelecionadoParaSorteioRapido,
        jogadoresAtivosNoGrupo = jogadoresAtivosParaDialogoSorteioRapido,
        perfisConfiguracao = perfisConfiguracaoSorteio,
        usarPerfilExistente = usarPerfilExistenteSorteioRapido,
        perfilSelecionado = perfilConfigSelecionadoSorteioRapido,
        jogadoresPorTimeManual = jogadoresPorTimeSorteioRapido,
        numeroDeTimesManual = numeroDeTimesSorteioRapido,
        erroAtual = erroSorteioRapidoAtual,
        podeRealizarSorteio = podeRealizarSorteioRapido,
        isSorteioListaColada = viewModel.isSorteioDeListaColada.collectAsState().value,
        onDismissRequest = { viewModel.onFecharDialogoSorteioRapido() },
        onUsarPerfilChanged = { viewModel.onUsarPerfilExistenteSorteioRapidoChanged(it) },
        onPerfilSelecionadoChanged = { viewModel.onPerfilConfigSorteioRapidoSelecionado(it) },
        onJogadoresPorTimeManualChanged = { viewModel.onJogadoresPorTimeSorteioRapidoChanged(it.toIntOrNull() ?: 0) },
        onNumeroDeTimesManualChanged = { viewModel.onNumeroDeTimesSorteioRapidoChanged(it.toIntOrNull() ?: 0) },
        onConfirmSorteio = { viewModel.onConfirmarSorteioRapido() },
        onClearError = { viewModel.limparErroSorteioRapido() }
    )

    if (mostrarTelaCompartilhamento && grupoParaCompartilharNaoNulo != null) {
        CompartilharPeladaScreen(
            grupo = grupoParaCompartilharNaoNulo,
            onCompartilhar = { texto ->
                compartilharTexto(texto, context)
                viewModel.ocultarTelaCompartilhamento()
            },
            onVoltar = { viewModel.ocultarTelaCompartilhamento() }
        )
    }

    if (mostrarDialogoSelecionarGrupoParaSorteioRapido) {
        SelecionarGrupoParaSorteioRapidoDialog(
            showDialog = mostrarDialogoSelecionarGrupoParaSorteioRapido,
            grupos = grupos,
            onDismissRequest = { mostrarDialogoSelecionarGrupoParaSorteioRapido = false },
            onGrupoSelecionado = { grupo ->
                viewModel.onAbrirDialogoSorteioRapido(grupo)
                mostrarDialogoSelecionarGrupoParaSorteioRapido = false
            }
        )
    }

    if (mostrarDialogoEntrarComCodigo) {
        DialogoEntrarComCodigo(
            convitePendente = convitePendente,
            erro = erroEntrarComCodigo,
            carregando = carregandoEntrarComCodigo,
            onBuscarCodigo = { viewModel.buscarGrupoPorCodigo(it) },
            onConfirmarEntrar = { viewModel.confirmarEntrarNoGrupo() },
            onDismissRequest = { viewModel.fecharDialogoEntrarComCodigo() }
        )
    }

    if (grupoParaConvidar != null) {
        DialogoConvite(
            grupo = grupoParaConvidar!!,
            uidAtual = viewModel.uidAtual,
            onCompartilhar = { texto -> compartilharTexto(texto, context) },
            onGerenciarMembrosClick = { viewModel.abrirGerenciarMembros(grupoParaConvidar!!) },
            onPermiteConviteChanged = { viewModel.atualizarPermiteConviteDeMembros(grupoParaConvidar!!, it) },
            onDismissRequest = { viewModel.fecharConvite() }
        )
    }

    if (carregandoConvite) {
        Dialog(onDismissRequest = {}) {
            Card { Box(Modifier.padding(32.dp)) { CircularProgressIndicator() } }
        }
    }

    if (grupoParaGerenciarMembros != null) {
        DialogoGerenciarMembros(
            grupo = grupoParaGerenciarMembros!!,
            uidAtual = viewModel.uidAtual,
            perfis = perfisMembros,
            onPromoverEditor = { viewModel.promoverParaEditor(grupoParaGerenciarMembros!!, it) },
            onRemoverEditor = { viewModel.removerEditor(grupoParaGerenciarMembros!!, it) },
            onRemoverMembro = { viewModel.removerMembro(grupoParaGerenciarMembros!!, it) },
            onDismissRequest = { viewModel.fecharGerenciarMembros() }
        )
    }
}


@Composable
fun DialogoEntrarComCodigo(
    convitePendente: GrupoRemoto?,
    erro: String?,
    carregando: Boolean,
    onBuscarCodigo: (String) -> Unit,
    onConfirmarEntrar: () -> Unit,
    onDismissRequest: () -> Unit
) {
    var codigo by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismissRequest) {
        Card(modifier = Modifier.fillMaxWidth().padding(16.dp), shape = RoundedCornerShape(16.dp)) {
            Column(modifier = Modifier.fillMaxWidth().padding(20.dp)) {
                if (convitePendente == null) {
                    Text("Entrar em um grupo", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Digite o código de convite que você recebeu:", style = MaterialTheme.typography.bodyMedium)
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = codigo,
                        onValueChange = { codigo = it.uppercase() },
                        label = { Text("Código") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (erro != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(erro, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = onDismissRequest) { Text("Cancelar") }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(onClick = { onBuscarCodigo(codigo) }, enabled = !carregando) {
                            if (carregando) CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                            else Text("Buscar")
                        }
                    }
                } else {
                    Text("Convite para grupo", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        "${convitePendente.donoNome} te convidou para participar de \"${convitePendente.nome}\". Deseja aceitar?",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Você vai poder ver jogadores, histórico e estatísticas do grupo.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    if (erro != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(erro, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = onDismissRequest) { Text("Recusar") }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(onClick = onConfirmarEntrar, enabled = !carregando) {
                            if (carregando) CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                            else Text("Aceitar")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DialogoConvite(
    grupo: GrupoRemoto,
    uidAtual: String?,
    onCompartilhar: (String) -> Unit,
    onGerenciarMembrosClick: () -> Unit,
    onPermiteConviteChanged: (Boolean) -> Unit,
    onDismissRequest: () -> Unit
) {
    val ehDono = uidAtual != null && uidAtual == grupo.donoId
    Dialog(onDismissRequest = onDismissRequest) {
        Card(modifier = Modifier.fillMaxWidth().padding(16.dp), shape = RoundedCornerShape(16.dp)) {
            Column(modifier = Modifier.fillMaxWidth().padding(20.dp)) {
                Text("Convidar para \"${grupo.nome}\"", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(16.dp))
                Text("Código de convite", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    grupo.codigoConvite,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = {
                        onCompartilhar(
                            "🏆 Você foi convidado para a pelada \"${grupo.nome}\"!\n\n" +
                                "Abra o Boleiragem, toque em \"Entrar em um grupo\" e digite o código: ${grupo.codigoConvite}"
                        )
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Share, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Compartilhar convite")
                }
                if (ehDono) {
                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Membros também podem convidar", style = MaterialTheme.typography.bodyMedium)
                            Text(
                                "Se desligado, só você e os editores podem compartilhar o código",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                        Switch(checked = grupo.permiteConviteDeMembros, onCheckedChange = onPermiteConviteChanged)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(onClick = onGerenciarMembrosClick, modifier = Modifier.fillMaxWidth()) {
                        Text("Gerenciar membros (${grupo.membrosIds.size})")
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismissRequest) { Text("Fechar") }
                }
            }
        }
    }
}

@Composable
fun DialogoGerenciarMembros(
    grupo: GrupoRemoto,
    uidAtual: String?,
    perfis: Map<String, UsuarioPerfil>,
    onPromoverEditor: (String) -> Unit,
    onRemoverEditor: (String) -> Unit,
    onRemoverMembro: (String) -> Unit,
    onDismissRequest: () -> Unit
) {
    Dialog(onDismissRequest = onDismissRequest) {
        Card(modifier = Modifier.fillMaxWidth().padding(16.dp), shape = RoundedCornerShape(16.dp)) {
            Column(modifier = Modifier.fillMaxWidth().padding(20.dp).heightIn(max = 500.dp)) {
                Text("Membros de \"${grupo.nome}\"", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(12.dp))
                LazyColumn(modifier = Modifier.weight(1f, fill = false)) {
                    items(grupo.membrosIds.filter { it != grupo.donoId }) { uid ->
                        val nome = perfis[uid]?.nome ?: uid.take(8)
                        val ehEditor = grupo.editoresIds.contains(uid)
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(nome, style = MaterialTheme.typography.bodyMedium)
                                Text(if (ehEditor) "Responsável" else "Membro", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                            }
                            if (ehEditor) {
                                TextButton(onClick = { onRemoverEditor(uid) }) { Text("Rebaixar") }
                            } else {
                                TextButton(onClick = { onPromoverEditor(uid) }) { Text("Tornar responsável") }
                            }
                            IconButton(onClick = { onRemoverMembro(uid) }) {
                                Icon(Icons.Default.PersonRemove, contentDescription = "Remover do grupo")
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismissRequest) { Text("Fechar") }
                }
            }
        }
    }
}

// Novo Composable para o diálogo de opções de sorteio
@Composable
fun DialogoOpcoesSorteio(
    onDismissRequest: () -> Unit,
    onColarListaClick: () -> Unit,
    onSelecionarGrupoClick: () -> Unit,
    temGrupos: Boolean
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text("Iniciar Novo Sorteio") },
        text = { Text("Como você deseja fornecer os jogadores?") },
        confirmButton = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Button(
                    onClick = onColarListaClick,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                ) {
                    Text("Colar Lista de Jogadores")
                }
                Button(
                    onClick = onSelecionarGrupoClick,
                    enabled = temGrupos, // Desabilita se não houver grupos
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Selecionar Grupo Existente")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text("Cancelar")
            }
        }
    )
}


@Composable
fun SelecionarGrupoParaSorteioRapidoDialog(
    showDialog: Boolean,
    grupos: List<GrupoPelada>,
    onDismissRequest: () -> Unit,
    onGrupoSelecionado: (GrupoPelada) -> Unit
) {
    if (!showDialog) return

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text("Selecionar Grupo para Sorteio") },
        text = {
            if (grupos.isEmpty()) {
                Text("Nenhum grupo cadastrado para selecionar.")
            } else {
                LazyColumn {
                    items(grupos) { grupo ->
                        Text(
                            text = grupo.nome,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onGrupoSelecionado(grupo) }
                                .padding(vertical = 12.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismissRequest) {
                Text("Cancelar")
            }
        }
    )
}


@Composable
fun ListaVisualizacao(
    grupos: List<GrupoPelada>,
    papeisGrupos: Map<String, PapelGrupo> = emptyMap(),
    onGrupoClick: (GrupoPelada) -> Unit,
    onEditarClick: (GrupoPelada) -> Unit,
    onExcluirClick: (GrupoPelada) -> Unit,
    onSorteioRapidoClick: (GrupoPelada) -> Unit,
    onConvidarClick: (GrupoPelada) -> Unit = {},
    onSairDoGrupoClick: (GrupoPelada) -> Unit = {}
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(grupos) { grupo ->
            GrupoItemLista(
                grupo = grupo,
                papel = grupo.firestoreId?.let { papeisGrupos[it] } ?: PapelGrupo.DONO,
                onClick = { onGrupoClick(grupo) },
                onEditarClick = { onEditarClick(grupo) },
                onExcluirClick = { onExcluirClick(grupo) },
                onSorteioRapidoClick = { onSorteioRapidoClick(grupo) }, // Este click pode ser removido dos itens individuais se o FAB for a única entrada
                onConvidarClick = { onConvidarClick(grupo) },
                onSairDoGrupoClick = { onSairDoGrupoClick(grupo) }
            )
        }
    }
}

@Composable
fun CardsVisualizacao(
    grupos: List<GrupoPelada>,
    papeisGrupos: Map<String, PapelGrupo> = emptyMap(),
    onGrupoClick: (GrupoPelada) -> Unit,
    onEditarClick: (GrupoPelada) -> Unit,
    onExcluirClick: (GrupoPelada) -> Unit,
    onSorteioRapidoClick: (GrupoPelada) -> Unit,
    onConvidarClick: (GrupoPelada) -> Unit = {},
    onSairDoGrupoClick: (GrupoPelada) -> Unit = {}
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(grupos) { grupo ->
            GrupoItemCard(
                grupo = grupo,
                papel = grupo.firestoreId?.let { papeisGrupos[it] } ?: PapelGrupo.DONO,
                onClick = { onGrupoClick(grupo) },
                onEditarClick = { onEditarClick(grupo) },
                onExcluirClick = { onExcluirClick(grupo) },
                onSorteioRapidoClick = { onSorteioRapidoClick(grupo) }, // Idem
                onConvidarClick = { onConvidarClick(grupo) },
                onSairDoGrupoClick = { onSairDoGrupoClick(grupo) }
            )
        }
    }
}

@Composable
fun MinimalistaVisualizacao(
    grupos: List<GrupoPelada>,
    papeisGrupos: Map<String, PapelGrupo> = emptyMap(),
    onGrupoClick: (GrupoPelada) -> Unit,
    onEditarClick: (GrupoPelada) -> Unit,
    onExcluirClick: (GrupoPelada) -> Unit,
    onSorteioRapidoClick: (GrupoPelada) -> Unit,
    onConvidarClick: (GrupoPelada) -> Unit = {},
    onSairDoGrupoClick: (GrupoPelada) -> Unit = {}
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(grupos) { grupo ->
            GrupoItemMinimalista(
                grupo = grupo,
                papel = grupo.firestoreId?.let { papeisGrupos[it] } ?: PapelGrupo.DONO,
                onClick = { onGrupoClick(grupo) },
                onEditarClick = { onEditarClick(grupo) },
                onExcluirClick = { onExcluirClick(grupo) },
                onSorteioRapidoClick = { onSorteioRapidoClick(grupo) }, // Idem
                onConvidarClick = { onConvidarClick(grupo) },
                onSairDoGrupoClick = { onSairDoGrupoClick(grupo) }
            )
        }
    }
}

@Composable
fun GrupoItemLista(
    grupo: GrupoPelada,
    onClick: () -> Unit,
    onEditarClick: () -> Unit,
    onExcluirClick: () -> Unit,
    onSorteioRapidoClick: () -> Unit, // Considerar remover se o FAB é a única entrada para sorteio
    onConvidarClick: () -> Unit = {},
    papel: PapelGrupo = PapelGrupo.DONO,
    onSairDoGrupoClick: () -> Unit = {}
) {
    var showMenu by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(64.dp).clip(RoundedCornerShape(8.dp)).background(MaterialTheme.colorScheme.primaryContainer)
            ) {
                val resourceId = when(grupo.imagemUrl) {
                    "ic_pelada_default_1" -> R.drawable.ic_pelada_default_1
                    "ic_pelada_default_2" -> R.drawable.ic_pelada_default_2
                    "logo_boleiragem" -> R.drawable.logo_boleiragem
                    else -> R.drawable.ic_pelada_default_1
                }
                Image(painter = painterResource(id = resourceId), "Imagem do grupo", Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(grupo.nome, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    if (grupo.firestoreId != null) {
                        Spacer(modifier = Modifier.width(8.dp))
                        ChipPapel(papel)
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.LocationOn, "Local", Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(grupo.local, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                }
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.DateRange, "Horário", Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = formatarHorarioGrupo(grupo),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
            }
            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(Icons.Default.MoreVert, "Mais opções")
                }
                DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                    if (papel == PapelGrupo.DONO) {
                        DropdownMenuItem(text = { Text(if (grupo.firestoreId != null) "Convidar / código" else "Convidar") }, onClick = {
                            onConvidarClick()
                            showMenu = false
                        })
                        DropdownMenuItem(text = { Text("Editar") }, onClick = {
                            onEditarClick()
                            showMenu = false
                        })
                        DropdownMenuItem(text = { Text("Excluir") }, onClick = {
                            onExcluirClick()
                            showMenu = false
                        })
                    } else {
                        DropdownMenuItem(text = { Text("Sair do grupo") }, onClick = {
                            onSairDoGrupoClick()
                            showMenu = false
                        })
                    }
                }
            }
        }
    }
}

@Composable
fun ChipPapel(papel: PapelGrupo) {
    val texto = when (papel) {
        PapelGrupo.DONO -> "Dono"
        PapelGrupo.EDITOR -> "Responsável"
        PapelGrupo.MEMBRO -> "Membro"
    }
    AssistChip(onClick = {}, label = { Text(texto, style = MaterialTheme.typography.labelSmall) }, modifier = Modifier.height(24.dp))
}

@Composable
fun GrupoItemCard(
    grupo: GrupoPelada,
    onClick: () -> Unit,
    onEditarClick: () -> Unit,
    onExcluirClick: () -> Unit,
    onSorteioRapidoClick: () -> Unit, // Considerar remover
    onConvidarClick: () -> Unit = {},
    papel: PapelGrupo = PapelGrupo.DONO,
    onSairDoGrupoClick: () -> Unit = {}
) {
    var showMenu by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column {
            Box(modifier = Modifier.fillMaxWidth().height(120.dp).background(MaterialTheme.colorScheme.primaryContainer)) {
                val resourceId = when(grupo.imagemUrl) {
                    "ic_pelada_default_1" -> R.drawable.ic_pelada_default_1
                    "ic_pelada_default_2" -> R.drawable.ic_pelada_default_2
                    "logo_boleiragem" -> R.drawable.logo_boleiragem
                    else -> R.drawable.ic_pelada_default_1
                }
                Image(painter = painterResource(id = resourceId), "Imagem do grupo", Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                Box(modifier = Modifier.align(Alignment.TopEnd).padding(8.dp)) {
                    IconButton(
                        onClick = { showMenu = true },
                        modifier = Modifier.size(32.dp).background(color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f), shape = CircleShape)
                    ) {
                        Icon(Icons.Default.MoreVert, "Mais opções", Modifier.size(16.dp))
                    }
                    DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                        if (papel == PapelGrupo.DONO) {
                            DropdownMenuItem(text = { Text(if (grupo.firestoreId != null) "Convidar / código" else "Convidar") }, onClick = {
                                onConvidarClick()
                                showMenu = false
                            })
                            DropdownMenuItem(text = { Text("Editar") }, onClick = {
                                onEditarClick()
                                showMenu = false
                            })
                            DropdownMenuItem(text = { Text("Excluir") }, onClick = {
                                onExcluirClick()
                                showMenu = false
                            })
                        } else {
                            DropdownMenuItem(text = { Text("Sair do grupo") }, onClick = {
                                onSairDoGrupoClick()
                                showMenu = false
                            })
                        }
                    }
                }
            }
            Column(modifier = Modifier.padding(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(grupo.nome, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
                    if (grupo.firestoreId != null) {
                        Spacer(modifier = Modifier.width(8.dp))
                        ChipPapel(papel)
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.LocationOn, "Local", Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(grupo.local, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.DateRange, "Horário", Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = formatarHorarioGrupo(grupo),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
fun GrupoItemMinimalista(
    grupo: GrupoPelada,
    onClick: () -> Unit,
    onEditarClick: () -> Unit,
    onExcluirClick: () -> Unit,
    onSorteioRapidoClick: () -> Unit, // Considerar remover
    onConvidarClick: () -> Unit = {},
    papel: PapelGrupo = PapelGrupo.DONO,
    onSairDoGrupoClick: () -> Unit = {}
) {
    var showMenu by remember { mutableStateOf(false) }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Box {
            Box(
                modifier = Modifier.size(80.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer)
                    .border(width = 2.dp, color = MaterialTheme.colorScheme.primary, shape = CircleShape)
            ) {
                val resourceId = when(grupo.imagemUrl) {
                    "ic_pelada_default_1" -> R.drawable.ic_pelada_default_1
                    "ic_pelada_default_2" -> R.drawable.ic_pelada_default_2
                    "logo_boleiragem" -> R.drawable.logo_boleiragem
                    else -> R.drawable.ic_pelada_default_1
                }
                Image(painter = painterResource(id = resourceId), "Imagem do grupo", Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                Box(
                    modifier = Modifier.align(Alignment.BottomEnd).padding(4.dp).size(24.dp).clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary).clickable { showMenu = true }
                ) {
                    Icon(Icons.Default.Menu, "Opções", Modifier.align(Alignment.Center).size(16.dp), tint = MaterialTheme.colorScheme.onPrimary)
                }
                DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                    if (papel == PapelGrupo.DONO) {
                        DropdownMenuItem(text = { Text(if (grupo.firestoreId != null) "Convidar / código" else "Convidar") }, onClick = {
                            onConvidarClick()
                            showMenu = false
                        })
                        DropdownMenuItem(text = { Text("Editar") }, onClick = {
                            onEditarClick()
                            showMenu = false
                        })
                        DropdownMenuItem(text = { Text("Excluir") }, onClick = {
                            onExcluirClick()
                            showMenu = false
                        })
                    } else {
                        DropdownMenuItem(text = { Text("Sair do grupo") }, onClick = {
                            onSairDoGrupoClick()
                            showMenu = false
                        })
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(grupo.nome, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.width(100.dp))
        if (grupo.firestoreId != null) {
            ChipPapel(papel)
        }
        Text(
            text = formatarHorarioGrupo(grupo),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.width(100.dp)
        )
    }
}

@Composable
fun formatarHorarioGrupo(grupo: GrupoPelada): String {
    return try {
        val formatter = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
        val date = formatter.parse(grupo.horario)
        if (date != null) {
            val horarioFormatado = SimpleDateFormat("HH:mm", Locale.getDefault()).format(date)
            if (grupo.tipoRecorrencia == TipoRecorrencia.RECORRENTE && grupo.diasSemana.isNotEmpty()) {
                val diasStr = grupo.diasSemana.joinToString(", ") { dia ->
                    when (dia) {
                        DiaSemana.DOMINGO -> "Dom"
                        DiaSemana.SEGUNDA -> "Seg"
                        DiaSemana.TERCA -> "Ter"
                        DiaSemana.QUARTA -> "Qua"
                        DiaSemana.QUINTA -> "Qui"
                        DiaSemana.SEXTA -> "Sex"
                        DiaSemana.SABADO -> "Sáb"
                    }
                }
                "$diasStr às $horarioFormatado"
            } else {
                horarioFormatado
            }
        } else {
            grupo.horario
        }
    } catch (_: Exception) {
        grupo.horario
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DialogoAdicionarEditarGrupo(
    grupo: GrupoPelada?,
    onDismissRequest: () -> Unit,
    onSalvar: (nome: String, local: String, horario: String, imagemUrl: String?, descricao: String?,
              tipoRecorrencia: TipoRecorrencia, diaSemana: DiaSemana?) -> Unit
) {
    val titulo = if (grupo == null) "Novo Grupo de Pelada" else "Editar Grupo"
    var nome by remember { mutableStateOf(grupo?.nome ?: "") }
    var local by remember { mutableStateOf(grupo?.local ?: "") }
    val timePickerState = rememberTimePickerState(
        initialHour = grupo?.horario?.let { try { SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()).parse(it)?.let { d -> SimpleDateFormat("HH", Locale.getDefault()).format(d).toInt() } } catch (_: Exception) { 19 } } ?: 19,
        initialMinute = grupo?.horario?.let { try { SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()).parse(it)?.let { d -> SimpleDateFormat("mm", Locale.getDefault()).format(d).toInt() } } catch (_: Exception) { 0 } } ?: 0
    )
    var mostrarTimePicker by remember { mutableStateOf(false) }
    var usarModoRelogio by remember { mutableStateOf(true) }
    var descricao by remember { mutableStateOf(grupo?.descricao ?: "") }
    var tipoRecorrenciaState by remember { mutableStateOf(grupo?.tipoRecorrencia ?: TipoRecorrencia.ESPORADICA) }
    val diasSelecionados = remember { mutableStateListOf<DiaSemana>().apply { addAll(grupo?.diasSemana ?: emptyList()) } }
    var imagemSelecionada by remember { mutableStateOf(grupo?.imagemUrl ?: "ic_pelada_default_1") }

    if (mostrarTimePicker) {
        BasicAlertDialog(
            onDismissRequest = { mostrarTimePicker = false },
            properties = DialogProperties(usePlatformDefaultWidth = false),
            modifier = Modifier.fillMaxWidth(0.9f)
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(28.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = AlertDialogDefaults.TonalElevation
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Selecione o horário", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                        ToggleButton(selected = usarModoRelogio, onClick = { usarModoRelogio = true }, content = { Text("Relógio") })
                        Spacer(modifier = Modifier.width(8.dp))
                        ToggleButton(selected = !usarModoRelogio, onClick = { usarModoRelogio = false }, content = { Text("Teclado") })
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    if (usarModoRelogio) {
                        TimePicker(state = timePickerState, modifier = Modifier.padding(vertical = 8.dp))
                    } else {
                        TimeInput(state = timePickerState, modifier = Modifier.padding(vertical = 8.dp))
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = { mostrarTimePicker = false }) { Text("Cancelar") }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(onClick = { mostrarTimePicker = false }) { Text("OK") }
                    }
                }
            }
        }
    }

    Dialog(onDismissRequest = onDismissRequest) {
        Card(modifier = Modifier.fillMaxWidth().padding(16.dp), shape = RoundedCornerShape(16.dp)) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(16.dp).heightIn(max = 600.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(titulo, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(16.dp))
                Column(modifier = Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState())) {
                    Text("Selecione uma imagem:", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Medium)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        listOf("ic_pelada_default_1", "ic_pelada_default_2", "logo_boleiragem").forEach { imagem ->
                            ImagemOpcao(imagemResource = imagem, selecionada = imagemSelecionada == imagem, onClick = { imagemSelecionada = imagem })
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(value = nome, onValueChange = { nome = it }, label = { Text("Nome do Grupo") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(value = local, onValueChange = { local = it }, label = { Text("Local da Pelada") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = tipoRecorrenciaState == TipoRecorrencia.RECORRENTE, onCheckedChange = { isChecked ->
                            tipoRecorrenciaState = if (isChecked) TipoRecorrencia.RECORRENTE else TipoRecorrencia.ESPORADICA
                            if (!isChecked) diasSelecionados.clear()
                        })
                        Text("Evento Recorrente (Dia Fixo)?")
                    }
                    if (tipoRecorrenciaState == TipoRecorrencia.RECORRENTE) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Selecione os dias da semana:", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Medium)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            DiaSemana.values().forEach { dia ->
                                DiaSemanaChip(dia = dia, selecionado = diasSelecionados.contains(dia)) {
                                    if (diasSelecionados.contains(dia)) diasSelecionados.remove(dia) else diasSelecionados.add(dia)
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Horário da Pelada:", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Medium)
                    Spacer(modifier = Modifier.height(8.dp))
                    Card(modifier = Modifier.fillMaxWidth().clickable { mostrarTimePicker = true }, elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)) {
                        Column(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("%02d:%02d".format(timePickerState.hour, timePickerState.minute), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(value = descricao, onValueChange = { descricao = it }, label = { Text("Descrição (opcional)") }, modifier = Modifier.fillMaxWidth(), minLines = 2)
                }
                Spacer(modifier = Modifier.height(16.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    OutlinedButton(onClick = onDismissRequest) { Text("Cancelar") }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val dataAtual = java.time.LocalDate.now()
                            val horarioCompleto = "$dataAtual" + "T" + "%02d:%02d:00".format(timePickerState.hour, timePickerState.minute)
                            onSalvar(
                                nome, local, horarioCompleto, imagemSelecionada, descricao,
                                tipoRecorrenciaState,
                                if (tipoRecorrenciaState == TipoRecorrencia.RECORRENTE) diasSelecionados.firstOrNull() else null
                            )
                        },
                        enabled = nome.isNotBlank() && local.isNotBlank() && (tipoRecorrenciaState == TipoRecorrencia.ESPORADICA || diasSelecionados.isNotEmpty())
                    ) { Text("Salvar") }
                }
            }
        }
    }
}

@Composable
fun DiaSemanaChip(dia: DiaSemana, selecionado: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = selecionado,
        onClick = onClick,
        label = { Text(dia.name.take(3).replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }) },
        modifier = Modifier.padding(horizontal = 2.dp)
    )
}

@Composable
fun ToggleButton(
    selected: Boolean,
    onClick: () -> Unit,
    content: @Composable () -> Unit
) {
    Surface(
        modifier = Modifier.clip(RoundedCornerShape(50)).clickable(onClick = onClick),
        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
        contentColor = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
    ) {
        Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp), contentAlignment = Alignment.Center) {
            content()
        }
    }
}

@Composable
fun HorarioSelector(
    value: Int,
    range: IntRange,
    step: Int = 1,
    onValueChange: (Int) -> Unit,
    formatValue: (Int) -> String = { it.toString() }
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        IconButton(onClick = { onValueChange((value + step).coerceAtMost(range.last)) }) {
            Icon(Icons.Default.Add, "Aumentar", tint = MaterialTheme.colorScheme.primary)
        }
        Card(modifier = Modifier.width(60.dp).height(48.dp), elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(formatValue(value), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            }
        }
        IconButton(onClick = { onValueChange((value - step).coerceAtLeast(range.first)) }) {
            Icon(Icons.Default.Add, "Diminuir", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.rotate(180f))
        }
    }
}

@Composable
fun DiaSemanaOpcao(
    sigla: String,
    selecionado: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier.size(36.dp).clip(CircleShape)
            .background(if (selecionado) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
            .border(width = 1.dp, color = if (selecionado) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), shape = CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(sigla, color = if (selecionado) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium, fontWeight = if (selecionado) FontWeight.Bold else FontWeight.Normal)
    }
}

@Composable
fun ImagemOpcao(
    imagemResource: String,
    selecionada: Boolean,
    onClick: () -> Unit
) {
    Box(modifier = Modifier.size(70.dp).padding(4.dp)) {
        val resourceId = when(imagemResource) {
            "ic_pelada_default_1" -> R.drawable.ic_pelada_default_1
            "ic_pelada_default_2" -> R.drawable.ic_pelada_default_2
            "logo_boleiragem" -> R.drawable.logo_boleiragem
            else -> R.drawable.ic_pelada_default_1
        }
        Box(
            modifier = Modifier.size(60.dp).clip(RoundedCornerShape(8.dp)).background(MaterialTheme.colorScheme.surfaceVariant)
                .clickable(onClick = onClick)
                .border(width = if (selecionada) 2.dp else 0.dp, color = if (selecionada) MaterialTheme.colorScheme.primary else Color.Transparent, shape = RoundedCornerShape(8.dp))
        ) {
            Image(painter = painterResource(id = resourceId), "Imagem $imagemResource", Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
        }
        if (selecionada) {
            Box(modifier = Modifier.size(20.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary).align(Alignment.BottomEnd)) {
                Icon(Icons.Default.Check, "Selecionada", tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(14.dp).align(Alignment.Center))
            }
        }
    }
}
