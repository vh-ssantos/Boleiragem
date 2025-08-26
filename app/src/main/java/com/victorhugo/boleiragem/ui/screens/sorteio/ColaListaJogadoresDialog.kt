package com.victorhugo.boleiragem.ui.screens.sorteio

import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog

/**
 * Componente de diálogo que mostra uma prancheta para colar lista de jogadores.
 * Possui um botão "COLAR" na parte superior que pega o conteúdo da área de transferência.
 */
@Composable
fun ColaListaJogadoresDialog(
    onDismissRequest: () -> Unit,
    onConfirmar: (String) -> Unit
) {
    val context = LocalContext.current
    var texto by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismissRequest) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(500.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Título
                Text(
                    text = "Lista de Jogadores",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Botão COLAR no topo
                Button(
                    onClick = {
                        try {
                            // Pegar texto da área de transferência com tratamento de erros
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                            if (clipboard != null) {
                                val clip = clipboard.primaryClip
                                if (clip != null && clip.itemCount > 0) {
                                    val itemTexto = clip.getItemAt(0).text
                                    if (itemTexto != null) {
                                        texto = itemTexto.toString()
                                    }
                                }
                            }
                        } catch (e: Exception) {
                            // Registra o erro mas não quebra o aplicativo
                            println("Erro ao acessar área de transferência: ${e.message}")
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentPaste,
                        contentDescription = "Colar"
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("COLAR", fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Campo de texto estilo prancheta (um grande EditText em branco)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        // .background(Color.White) // REMOVIDO
                        .border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(8.dp)
                        )
                ) {
                    TextField(
                        value = texto,
                        onValueChange = { texto = it },
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(8.dp),
                        colors = TextFieldDefaults.colors(
                            // focusedContainerColor = Color.White, // REMOVIDO
                            // unfocusedContainerColor = Color.White, // REMOVIDO
                            // disabledContainerColor = Color.White, // REMOVIDO
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            cursorColor = MaterialTheme.colorScheme.primary
                        ),
                        placeholder = { Text("Digite ou cole os nomes dos jogadores aqui...") }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Botões de ação
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(
                        onClick = onDismissRequest
                    ) {
                        Text("Cancelar")
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        onClick = { onConfirmar(texto) },
                        enabled = texto.isNotBlank()
                    ) {
                        Text("Confirmar")
                    }
                }
            }
        }
    }
}
