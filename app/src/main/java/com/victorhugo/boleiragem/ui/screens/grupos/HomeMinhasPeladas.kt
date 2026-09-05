package com.victorhugo.boleiragem.ui.screens.grupos

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.SportsSoccer
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.victorhugo.boleiragem.R
import com.victorhugo.boleiragem.data.model.GrupoPelada
import com.victorhugo.boleiragem.data.model.PapelGrupo

@Composable
fun HomeMinhasPeladas(
    grupos: List<GrupoPelada>,
    papeisGrupos: Map<String, PapelGrupo>,
    carregando: Boolean,
    paddingValues: PaddingValues,
    onSairClick: () -> Unit,
    onGrupoClick: (GrupoPelada) -> Unit,
    onEditarClick: (GrupoPelada) -> Unit,
    onExcluirClick: (GrupoPelada) -> Unit,
    onConvidarClick: (GrupoPelada) -> Unit,
    onSairDoGrupoClick: (GrupoPelada) -> Unit,
    onCriarGrupoClick: () -> Unit,
    onEntrarComCodigoClick: () -> Unit,
    onSorteioClick: () -> Unit,
    onCronometroClick: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
                        MaterialTheme.colorScheme.background,
                        MaterialTheme.colorScheme.background
                    )
                )
            )
            .padding(paddingValues),
        contentPadding = PaddingValues(start = 16.dp, top = 20.dp, end = 16.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { HomeHeader(onSairClick = onSairClick) }
        item {
            AtalhosHome(
                onCriarGrupoClick = onCriarGrupoClick,
                onEntrarComCodigoClick = onEntrarComCodigoClick,
                onSorteioClick = onSorteioClick,
                onCronometroClick = onCronometroClick
            )
        }

        when {
            carregando -> item {
                Box(modifier = Modifier.fillMaxWidth().height(220.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            grupos.isEmpty() -> item {
                EmptyHomeCard(
                    onCriarGrupoClick = onCriarGrupoClick,
                    onSorteioClick = onSorteioClick
                )
            }
            else -> {
                item { ProximaPeladaCard(grupo = grupos.first()) }
                item {
                    Text(
                        text = "Seus grupos",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
                items(grupos) { grupo ->
                    GrupoHomeCard(
                        grupo = grupo,
                        papel = grupo.firestoreId?.let { papeisGrupos[it] } ?: PapelGrupo.DONO,
                        onClick = { onGrupoClick(grupo) },
                        onEditarClick = { onEditarClick(grupo) },
                        onExcluirClick = { onExcluirClick(grupo) },
                        onConvidarClick = { onConvidarClick(grupo) },
                        onSairDoGrupoClick = { onSairDoGrupoClick(grupo) }
                    )
                }
            }
        }
    }
}

@Composable
private fun HomeHeader(onSairClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Boleiragem",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Minhas peladas",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Black
            )
        }
        Box(
            modifier = Modifier
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.tertiaryContainer)
        ) {
            com.victorhugo.boleiragem.ui.screens.perfil.PerfilAvatarButton(onSairClick = onSairClick)
        }
    }
}

@Composable
private fun AtalhosHome(
    onCriarGrupoClick: () -> Unit,
    onEntrarComCodigoClick: () -> Unit,
    onSorteioClick: () -> Unit,
    onCronometroClick: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            AtalhoHome(
                icone = Icons.Filled.Add,
                texto = "Criar",
                cor = MaterialTheme.colorScheme.primaryContainer,
                onClick = onCriarGrupoClick,
                modifier = Modifier.weight(1f)
            )
            AtalhoHome(
                icone = Icons.Filled.GroupAdd,
                texto = "Entrar",
                cor = MaterialTheme.colorScheme.secondaryContainer,
                onClick = onEntrarComCodigoClick,
                modifier = Modifier.weight(1f)
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            AtalhoHome(
                icone = Icons.Filled.ContentPaste,
                texto = "Sortear",
                cor = MaterialTheme.colorScheme.tertiaryContainer,
                onClick = onSorteioClick,
                modifier = Modifier.weight(1f)
            )
            AtalhoHome(
                icone = Icons.Filled.Timer,
                texto = "Cronômetro",
                cor = MaterialTheme.colorScheme.surfaceVariant,
                onClick = onCronometroClick,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun AtalhoHome(
    icone: ImageVector,
    texto: String,
    cor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .height(52.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        color = cor,
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(imageVector = icone, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = texto, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun ProximaPeladaCard(grupo: GrupoPelada) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Icon(
                imageVector = Icons.Filled.SportsSoccer,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.10f),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(12.dp)
                    .size(96.dp)
            )
            Column(modifier = Modifier.padding(18.dp)) {
                Text(
                    text = "Próxima pelada",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.78f)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = grupo.nome,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Spacer(modifier = Modifier.height(12.dp))
                LinhaInfoGrupo(Icons.Default.LocationOn, grupo.local, MaterialTheme.colorScheme.onPrimaryContainer)
                Spacer(modifier = Modifier.height(6.dp))
                LinhaInfoGrupo(Icons.Default.DateRange, formatarHorarioGrupo(grupo), MaterialTheme.colorScheme.onPrimaryContainer)
            }
        }
    }
}

@Composable
private fun EmptyHomeCard(
    onCriarGrupoClick: () -> Unit,
    onSorteioClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(22.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_pelada_empty),
                contentDescription = null,
                modifier = Modifier.size(110.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Sua primeira pelada começa aqui",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Crie um grupo fixo ou cole uma lista para fazer um sorteio rápido.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(18.dp))
            Button(onClick = onCriarGrupoClick, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Criar grupo")
            }
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedButton(onClick = onSorteioClick, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Filled.ContentPaste, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Sortear lista")
            }
        }
    }
}

@Composable
private fun GrupoHomeCard(
    grupo: GrupoPelada,
    papel: PapelGrupo,
    onClick: () -> Unit,
    onEditarClick: () -> Unit,
    onExcluirClick: () -> Unit,
    onConvidarClick: () -> Unit,
    onSairDoGrupoClick: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(62.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer)
            ) {
                val resourceId = when (grupo.imagemUrl) {
                    "ic_pelada_default_1" -> R.drawable.ic_pelada_default_1
                    "ic_pelada_default_2" -> R.drawable.ic_pelada_default_2
                    "logo_boleiragem" -> R.drawable.logo_boleiragem
                    else -> R.drawable.ic_pelada_default_1
                }
                Image(
                    painter = painterResource(id = resourceId),
                    contentDescription = "Imagem do grupo",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = grupo.nome,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (grupo.firestoreId != null) {
                        Spacer(modifier = Modifier.width(8.dp))
                        ChipPapel(papel)
                    }
                }
                Spacer(modifier = Modifier.height(7.dp))
                LinhaInfoGrupo(Icons.Default.LocationOn, grupo.local, MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(4.dp))
                LinhaInfoGrupo(Icons.Default.DateRange, formatarHorarioGrupo(grupo), MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "Mais opções")
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
private fun LinhaInfoGrupo(
    icone: ImageVector,
    texto: String,
    cor: Color
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icone, contentDescription = null, modifier = Modifier.size(15.dp), tint = cor.copy(alpha = 0.82f))
        Spacer(modifier = Modifier.width(5.dp))
        Text(
            text = texto,
            style = MaterialTheme.typography.bodySmall,
            color = cor.copy(alpha = 0.86f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
