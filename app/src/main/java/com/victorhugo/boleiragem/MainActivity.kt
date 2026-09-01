package com.victorhugo.boleiragem

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.google.firebase.auth.FirebaseAuth
import com.victorhugo.boleiragem.navigation.BoleiragemBottomNavigationBar
import com.victorhugo.boleiragem.navigation.NavDestinations
import com.victorhugo.boleiragem.ui.screens.cadastro.CadastroJogadoresScreen
import com.victorhugo.boleiragem.ui.screens.cadastro.DetalheJogadorScreen
import com.victorhugo.boleiragem.ui.screens.configuracao.ConfiguracaoPontuacaoScreen
import com.victorhugo.boleiragem.ui.screens.configuracao.ConfiguracaoTimesScreen
import com.victorhugo.boleiragem.ui.screens.configuracao.GerenciadorPerfisScreen
import com.victorhugo.boleiragem.ui.screens.grupos.GruposPeladaScreen
import com.victorhugo.boleiragem.ui.screens.historico.HistoricoScreen
import com.victorhugo.boleiragem.ui.screens.login.LoginScreen
import com.victorhugo.boleiragem.ui.screens.sorteio.ResultadoSorteioScreen
import com.victorhugo.boleiragem.ui.screens.sorteio.SorteioTimesScreen
import com.victorhugo.boleiragem.ui.screens.splash.SplashScreen
import com.victorhugo.boleiragem.ui.screens.times.TimesAtuaisScreen
import com.victorhugo.boleiragem.ui.theme.BoleiragemTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    // Flag para acompanhar se as permissões já foram solicitadas
    private var permissionsRequested = false

    // Lançador para solicitar permissões
    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.entries.all { it.value }
        // Permissões solicitadas, independente do resultado
        permissionsRequested = true
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        requestLocationPermissions()
        setContent {
            BoleiragemTheme {
                BoleiragemApp()
            }
        }
    }

    private fun requestLocationPermissions() {
        if (!permissionsRequested) {
            val hasLocationPermission = ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED

            if (!hasLocationPermission) {
                locationPermissionLauncher.launch(
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    )
                )
            }
        }
    }
}

@Composable
fun BoleiragemApp() {
    var showSplashScreen by remember { mutableStateOf(true) }
    var showLoginScreen by remember { mutableStateOf(false) }
    var showGruposScreen by remember { mutableStateOf(false) }
    var showResultadoSorteioRapido by remember { mutableStateOf(false) } // Nova flag de estado
    var grupoSelecionadoId by remember { mutableStateOf(-1L) }
    var grupoSelecionadoNome by remember { mutableStateOf("") }

    when {
        showSplashScreen -> {
            SplashScreen(onNavigateToHome = {
                showSplashScreen = false
                // Se já existe uma sessão do Firebase Auth ativa, pula a tela de login
                if (FirebaseAuth.getInstance().currentUser != null) {
                    showGruposScreen = true
                } else {
                    showLoginScreen = true
                }
            })
        }
        showLoginScreen -> {
            LoginScreen(
                onLoginSucesso = {
                    showLoginScreen = false
                    showGruposScreen = true
                },
                onEntrarSemContaClick = {
                    showLoginScreen = false
                    showGruposScreen = true
                }
            )
        }
        showGruposScreen -> {
            GruposPeladaScreen(
                onGrupoSelecionado = { grupoId, grupoNome ->
                    grupoSelecionadoId = grupoId
                    grupoSelecionadoNome = grupoNome
                    showGruposScreen = false
                    // MainScreen será mostrada no 'else'
                },
                onNavigateToSorteioResultado = { isSorteioRapidoValue ->
                    if (isSorteioRapidoValue) {
                        showGruposScreen = false
                        showResultadoSorteioRapido = true
                    }
                },
                onSairClick = { // Implementação do onSairClick
                    FirebaseAuth.getInstance().signOut()
                    showGruposScreen = false
                    showLoginScreen = true
                    showResultadoSorteioRapido = false // Garante que outras telas sejam resetadas
                    // grupoSelecionadoId e grupoSelecionadoNome não precisam ser resetados aqui
                }
            )
        }
        showResultadoSorteioRapido -> {
            val tempNavController = rememberNavController()
            NavHost(
                navController = tempNavController,
                startDestination = "placeholder_resultado_sorteio" // Rota inicial temporária
            ) {
                composable("placeholder_resultado_sorteio") {
                    Box(modifier = Modifier.fillMaxSize())
                }
                composable(
                    route = NavDestinations.ResultadoSorteio.route, 
                    arguments = listOf(navArgument("isSorteioRapido") { type = NavType.BoolType })
                ) {
                    ResultadoSorteioScreen(
                        onBackClick = {
                            showResultadoSorteioRapido = false
                            showGruposScreen = true 
                        }
                    )
                }
            }
            LaunchedEffect(Unit) {
                tempNavController.navigate(NavDestinations.ResultadoSorteio.createRoute(isSorteioRapido = true)) {
                    popUpTo("placeholder_resultado_sorteio") { inclusive = true } 
                }
            }
        }
        else -> {
            MainScreen(
                grupoId = grupoSelecionadoId,
                grupoNome = grupoSelecionadoNome,
                onVoltarParaGerenciamento = {
                    showGruposScreen = true
                    showResultadoSorteioRapido = false
                }
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    grupoId: Long = -1L,
    grupoNome: String = "",
    onVoltarParaGerenciamento: () -> Unit = {}
) {
    val navController = rememberNavController() 
    val scope = rememberCoroutineScope()
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val pagerState = rememberPagerState(
        initialPage = selectedTabIndex,
        pageCount = { 5 } // Jogadores, Regras, Sorteio, Jogo, Estatísticas
    )

    LaunchedEffect(pagerState.currentPage) {
        selectedTabIndex = pagerState.currentPage
    }

    // val navigateToTab = { tabIndex: Int -> ... } // Não é mais usado diretamente aqui

    var isSecondaryScreen by remember { mutableStateOf(false) }
    var secondaryScreenContent by remember { mutableStateOf<@Composable () -> Unit>({}) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            if (!isSecondaryScreen) {
                TopAppBar(
                    title = {
                        Text(
                            text = grupoNome.ifBlank { "Boleiragem" },
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Start
                        )
                    },
                    navigationIcon = {
                        androidx.compose.material3.IconButton(onClick = onVoltarParaGerenciamento) {
                            androidx.compose.material3.Icon(
                                imageVector = Icons.Filled.ArrowBack,
                                contentDescription = "Voltar para gerenciamento",
                                tint = Color.White
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        titleContentColor = MaterialTheme.colorScheme.onPrimary,
                        navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    modifier = Modifier.height(56.dp)
                )
            }
        },
        bottomBar = {
            if (!isSecondaryScreen) {
                BoleiragemBottomNavigationBar(
                    navController = navController, // Este navController é para as abas
                    currentTab = selectedTabIndex,
                    onTabSelected = { index ->
                        selectedTabIndex = index
                        scope.launch {
                            pagerState.animateScrollToPage(index)
                        }
                    }
                )
            }
        }
    ) { innerPadding ->
        if (isSecondaryScreen) {
            secondaryScreenContent()
        } else {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding) // Aplicar innerPadding aqui
            ) { page ->
                when (page) {
                    0 -> CadastroJogadoresScreen(
                        grupoId = grupoId,
                        onNavigateToDetalheJogador = { jogadorId ->
                            isSecondaryScreen = true
                            secondaryScreenContent = {
                                DetalheJogadorScreen(
                                    jogadorId = jogadorId,
                                    onBackClick = { isSecondaryScreen = false }
                                )
                            }
                        }
                    )
                    1 -> ConfiguracaoTimesScreen(
                        grupoId = grupoId,
                        onNavigateToConfiguracaoPontuacao = {
                            isSecondaryScreen = true
                            secondaryScreenContent = {
                                ConfiguracaoPontuacaoScreen(
                                    onBackClick = { isSecondaryScreen = false }
                                )
                            }
                        },
                        onNavigateToGerenciadorPerfis = {
                            isSecondaryScreen = true
                            secondaryScreenContent = {
                                GerenciadorPerfisScreen(
                                    grupoId = grupoId,
                                    onNavigateBack = { isSecondaryScreen = false }
                                )
                            }
                        }
                    )
                    2 -> SorteioTimesScreen(
                        grupoId = grupoId,
                        onSorteioRealizado = {
                            scope.launch { pagerState.animateScrollToPage(3) }
                        },
                        onNavigateToHistorico = {
                            scope.launch { pagerState.animateScrollToPage(4) }
                        }
                    )
                    3 -> TimesAtuaisScreen(grupoId = grupoId)
                    4 -> HistoricoScreen(grupoId = grupoId)
                }
            }
        }
    }
}
