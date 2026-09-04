package com.victorhugo.boleiragem.ui.screens.perfil

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.victorhugo.boleiragem.data.model.PosicaoJogador
import com.victorhugo.boleiragem.ui.common.OutlinedTextFieldComAcentos

/**
 * Tela cheia de edição de perfil (nome/posição/idade) — aberta a partir do item "Meu perfil" do
 * menu (`PerfilMenuSheet`). Antes esse form ficava embutido direto no sheet; virou uma tela de
 * verdade porque um sheet deve ser um menu de opções, não um formulário.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TelaMeuPerfil(
    uiState: PerfilUiState,
    onNomeChange: (String) -> Unit,
    onPosicaoChange: (PosicaoJogador) -> Unit,
    onIdadeChange: (String) -> Unit,
    onSalvarClick: () -> Unit,
    onBackClick: () -> Unit
) {
    var menuPosicaoAberto by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Meu Perfil") },
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
                .verticalScroll(rememberScrollState())
                .padding(24.dp)
        ) {
            OutlinedTextFieldComAcentos(
                value = uiState.nome,
                onValueChange = onNomeChange,
                label = { Text("Nome") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            ExposedDropdownMenuBox(
                expanded = menuPosicaoAberto,
                onExpandedChange = { menuPosicaoAberto = it }
            ) {
                OutlinedTextFieldComAcentos(
                    value = uiState.posicaoFavorita?.nomeExibicao() ?: "",
                    onValueChange = {},
                    label = { Text("Posição favorita") },
                    enabled = false,
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor()
                )
                ExposedDropdownMenu(
                    expanded = menuPosicaoAberto,
                    onDismissRequest = { menuPosicaoAberto = false }
                ) {
                    PosicaoJogador.entries.forEach { posicao ->
                        DropdownMenuItem(
                            text = { Text(posicao.nomeExibicao()) },
                            onClick = {
                                onPosicaoChange(posicao)
                                menuPosicaoAberto = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextFieldComAcentos(
                value = uiState.idade,
                onValueChange = onIdadeChange,
                label = { Text("Idade") },
                keyboardType = KeyboardType.Number,
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (uiState.erro != null) {
                Text(
                    text = uiState.erro,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
            if (uiState.salvoComSucesso) {
                Text(
                    text = "Perfil salvo!",
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.bodySmall
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            Button(
                onClick = onSalvarClick,
                enabled = !uiState.salvando,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (uiState.salvando) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Text("Salvar")
                }
            }
        }
    }
}

internal fun PosicaoJogador.nomeExibicao(): String =
    name.lowercase().split("_").joinToString(" ") { it.replaceFirstChar(Char::uppercase) }
