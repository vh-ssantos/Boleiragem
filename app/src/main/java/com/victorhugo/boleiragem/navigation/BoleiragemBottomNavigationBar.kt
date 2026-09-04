package com.victorhugo.boleiragem.navigation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Rule
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController

@Composable
fun BoleiragemBottomNavigationBar(
    navController: NavController,
    currentTab: Int = 0,
    onTabSelected: (Int) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val items = listOf(
        BottomNavItem(
            route = NavDestinations.CadastroJogadores.route,
            title = "Jogadores",
            icon = Icons.Default.Person
        ),
        BottomNavItem(
            route = NavDestinations.ConfiguracaoTimes.route,
            title = "Regras",
            icon = Icons.Default.Rule
        ),
        BottomNavItem(
            route = NavDestinations.SorteioTimes.route,
            title = "Sorteio",
            icon = Icons.Default.PlayArrow
        ),
        BottomNavItem(
            route = NavDestinations.TimesAtuais.route,
            title = "Jogo",
            icon = Icons.Default.EmojiEvents
        ),
        BottomNavItem(
            route = NavDestinations.Historico.route,
            title = "Estatísticas",
            icon = Icons.Default.BarChart
        )
    )

    NavigationBar(
        modifier = modifier.fillMaxWidth(),
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp
    ) {
        items.forEachIndexed { index, item ->
            val selected = index == currentTab
            NavigationBarItem(
                icon = {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = item.icon,
                            contentDescription = item.title,
                            tint = if (selected)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = item.title,
                            textAlign = TextAlign.Center,
                            fontSize = 12.sp,
                            color = if (selected)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                selected = selected,
                onClick = { onTabSelected(index) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = Color.Transparent, // Controlamos a cor diretamente no ícone
                    unselectedIconColor = Color.Transparent, // Controlamos a cor diretamente no ícone
                    selectedTextColor = Color.Transparent, // Controlamos a cor diretamente no texto
                    unselectedTextColor = Color.Transparent, // Controlamos a cor diretamente no texto
                    indicatorColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                )
            )
        }
    }
}

data class BottomNavItem(
    val route: String,
    val title: String,
    val icon: ImageVector
)
