package com.victorhugo.boleiragem.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.victorhugo.boleiragem.ui.screens.cadastro.DetalheJogadorScreen
import com.victorhugo.boleiragem.ui.screens.configuracao.GerenciadorPerfisScreen
import com.victorhugo.boleiragem.ui.screens.estatisticas.EstatisticasScreen
import com.victorhugo.boleiragem.ui.screens.login.LoginScreen // Import adicionado
import com.victorhugo.boleiragem.ui.screens.sorteio.ResultadoSorteioScreen
import com.victorhugo.boleiragem.ui.screens.splash.SplashScreen

// Definições de NavDestinations
sealed class NavDestinations(val route: String) {
    object Splash : NavDestinations("splash_screen")
    object Login : NavDestinations("login_screen") // Nova rota de Login
    object CadastroJogadores : NavDestinations("cadastro_jogadores_screen") // Rota principal (com ViewPager)
    object ConfiguracaoTimes : NavDestinations("configuracao_times_screen")
    object SorteioTimes : NavDestinations("sorteio_times_screen")
    object TimesAtuais : NavDestinations("times_atuais_screen")
    object Historico : NavDestinations("historico_screen")
    object DetalheJogador : NavDestinations("detalhe_jogador_screen/{jogadorId}") {
        fun createRoute(jogadorId: Long) = "detalhe_jogador_screen/$jogadorId"
    }
    object ResultadoSorteio : NavDestinations("resultado_sorteio_screen/{isSorteioRapido}") {
        fun createRoute(isSorteioRapido: Boolean) = "resultado_sorteio_screen/$isSorteioRapido"
    }
    object GerenciadorPerfis : NavDestinations("gerenciador_perfis_screen/{grupoId}") {
        fun createRoute(grupoId: Long) = "gerenciador_perfis_screen/$grupoId"
    }
    object Estatisticas : NavDestinations("estatisticas_screen")
    object Perfil : NavDestinations("perfil_screen")
}

@Composable
fun BoleiragemNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    showMainScreens: Boolean = true
) {
    NavHost(
        navController = navController,
        startDestination = NavDestinations.Splash.route, // Splash ainda é o início
        modifier = modifier
    ) {
        composable(NavDestinations.Splash.route) {
            SplashScreen(onNavigateToHome = {
                navController.navigate(NavDestinations.Login.route) { // Navega para Login
                    popUpTo(NavDestinations.Splash.route) { inclusive = true }
                    launchSingleTop = true // Evita múltiplas instâncias da tela de login na pilha
                }
            })
        }

        composable(NavDestinations.Login.route) {
            LoginScreen(
                onLoginSucesso = {
                    navController.navigate(NavDestinations.CadastroJogadores.route) {
                        popUpTo(NavDestinations.Login.route) { inclusive = true }
                        launchSingleTop = true
                    }
                },
                onEntrarSemContaClick = {
                    navController.navigate(NavDestinations.CadastroJogadores.route) {
                        popUpTo(NavDestinations.Login.route) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            )
        }

        // Telas principais com navegação por abas - gerenciadas pelo ViewPager na MainActivity
        if (showMainScreens) {
            composable(route = NavDestinations.CadastroJogadores.route) { /* Conteúdo gerenciado pelo ViewPager */ }
            composable(route = NavDestinations.ConfiguracaoTimes.route) { /* Conteúdo gerenciado pelo ViewPager */ }
            composable(route = NavDestinations.SorteioTimes.route) { /* Conteúdo gerenciado pelo ViewPager */ }
            composable(route = NavDestinations.TimesAtuais.route) { /* Conteúdo gerenciado pelo ViewPager */ }
            composable(route = NavDestinations.Historico.route) { /* Conteúdo gerenciado pelo ViewPager */ }
        }

        composable(
            route = NavDestinations.DetalheJogador.route,
            arguments = listOf(navArgument("jogadorId") { type = NavType.LongType })
        ) {
            val jogadorId = it.arguments?.getLong("jogadorId") ?: -1L
            DetalheJogadorScreen(
                jogadorId = jogadorId,
                onBackClick = { navController.navigateUp() }
            )
        }

        composable(
            route = NavDestinations.ResultadoSorteio.route,
            arguments = listOf(navArgument("isSorteioRapido") { type = NavType.BoolType; defaultValue = false })
        ) {
            ResultadoSorteioScreen(
                onBackClick = { navController.navigateUp() }
            )
        }

        composable(
            route = NavDestinations.GerenciadorPerfis.route,
            arguments = listOf(navArgument("grupoId") { type = NavType.LongType })
        ) {
            GerenciadorPerfisScreen(
                onNavigateBack = { navController.navigateUp() }
            )
        }

        composable(route = NavDestinations.Estatisticas.route) {
            EstatisticasScreen(
                onNavigateBack = { navController.navigateUp() }
            )
        }
    }
}
