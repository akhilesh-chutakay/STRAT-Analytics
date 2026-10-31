package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.example.data.backup.AutoBackupManager
import com.example.data.drive.GoogleDriveService
import com.example.data.local.AppDatabase
import com.example.data.repository.IdeaRepository
import com.example.data.repository.UserProfileRepository
import com.example.ui.components.MarketUpdateDialog
import com.example.ui.components.UserDpAvatar
import com.example.ui.components.UserProfileDialog
import com.example.ui.navigation.Screen
import com.example.ui.screens.*
import com.example.ui.theme.AccentEmerald
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.PrimaryBlue
import com.example.ui.viewmodel.IdeaViewModel
import com.example.ui.viewmodel.IdeaViewModelFactory

class MainActivity : ComponentActivity() {

    private val database by lazy { AppDatabase.getDatabase(this, lifecycleScope) }
    private val driveService by lazy { GoogleDriveService(this) }
    private val autoBackupManager by lazy { AutoBackupManager(this, driveService) }
    private val repository by lazy { IdeaRepository(database.ideaDao(), database.signalDao(), autoBackupManager) }
    private val userProfileRepository by lazy { UserProfileRepository(this) }
    private val viewModel: IdeaViewModel by viewModels {
        IdeaViewModelFactory(repository, driveService, userProfileRepository, autoBackupManager)
    }

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                val navController = rememberNavController()
                val currentBackStack by navController.currentBackStackEntryAsState()
                val currentRoute = currentBackStack?.destination?.route
                val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()
                val backupSettings by viewModel.backupSettings.collectAsStateWithLifecycle()
                val marketUpdateState by viewModel.marketUpdateState.collectAsStateWithLifecycle()

                var showUserProfileDialog by remember { mutableStateOf(false) }

                // Market Re-Analysis Dialog
                if (marketUpdateState.isScanning || marketUpdateState.newlyAddedIdeas.isNotEmpty() || marketUpdateState.errorMessage != null) {
                    MarketUpdateDialog(
                        state = marketUpdateState,
                        onDismiss = { viewModel.clearMarketUpdateState() },
                        onNavigateToIdea = { id ->
                            navController.navigate(Screen.Detail.createRoute(id))
                        }
                    )
                }

                if (showUserProfileDialog) {
                    UserProfileDialog(
                        viewModel = viewModel,
                        onDismiss = { showUserProfileDialog = false }
                    )
                }

                val isRootScreen = currentRoute == Screen.Home.route ||
                    currentRoute == Screen.Actionable.route ||
                    currentRoute == Screen.Signals.route ||
                    currentRoute == Screen.Analytics.route

                Scaffold(
                    topBar = {
                        if (isRootScreen) {
                            Surface(
                                color = MaterialTheme.colorScheme.background,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .statusBarsPadding()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // App Title
                                    Column {
                                        Text(
                                            text = "Strat Analytics",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary,
                                            letterSpacing = (-0.5).sp
                                        )
                                        Text(
                                            text = "Venture Radar • ${userProfile.name.split(" ").firstOrNull() ?: "Founder"}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontSize = 10.sp
                                        )
                                    }

                                    // Action Controls: Update Ideas, Settings Controls, Profile Avatar
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        // 1. Update Ideas / Market Re-Analysis Button
                                        var showUpdateMenu by remember { mutableStateOf(false) }
                                        Box {
                                            Surface(
                                                shape = CircleShape,
                                                color = Color(0xFF1E1E26),
                                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF2E2E38)),
                                                modifier = Modifier.size(38.dp)
                                            ) {
                                                IconButton(
                                                    onClick = { showUpdateMenu = true },
                                                    modifier = Modifier
                                                        .fillMaxSize()
                                                        .testTag("update_ideas_top_button")
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Outlined.Sync,
                                                        contentDescription = "Update Market Intelligence",
                                                        tint = Color(0xFFE2E8F0),
                                                        modifier = Modifier.size(19.dp)
                                                    )
                                                }
                                            }

                                            DropdownMenu(
                                                expanded = showUpdateMenu,
                                                onDismissRequest = { showUpdateMenu = false },
                                                modifier = Modifier.background(Color(0xFF1E1E26))
                                            ) {
                                                DropdownMenuItem(
                                                    text = {
                                                        Column {
                                                            Text("🌊 Blue Ocean SaaS Scan", fontWeight = FontWeight.Bold, color = Color.White)
                                                            Text("Scan founder in-need & high-margin gaps", fontSize = 11.sp, color = Color(0xFF9E9EA8))
                                                        }
                                                    },
                                                    leadingIcon = {
                                                        Icon(Icons.Outlined.Sync, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(20.dp))
                                                    },
                                                    onClick = {
                                                        showUpdateMenu = false
                                                        viewModel.updateMarketIdeas()
                                                    }
                                                )

                                                Divider(color = Color(0xFF2E2E38))

                                                DropdownMenuItem(
                                                    text = {
                                                        Column {
                                                            Text("💰 Internet \$0 Capital Scout", fontWeight = FontWeight.Bold, color = Color(0xFF6EE7B7))
                                                            Text("Global web scan for \$0 & no-code cashflow", fontSize = 11.sp, color = Color(0xFF9E9EA8))
                                                        }
                                                    },
                                                    leadingIcon = {
                                                        Icon(Icons.Default.MonetizationOn, contentDescription = null, tint = Color(0xFF34D399), modifier = Modifier.size(20.dp))
                                                    },
                                                    onClick = {
                                                        showUpdateMenu = false
                                                        viewModel.scanInternetForZeroInvestmentOpportunities()
                                                    }
                                                )
                                            }
                                        }

                                        // 2. Web Companion & Standalone Website Platform Button
                                        Surface(
                                            shape = CircleShape,
                                            color = Color(0xFF1E1E26),
                                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.5f)),
                                            modifier = Modifier.size(38.dp)
                                        ) {
                                            IconButton(
                                                onClick = { navController.navigate(Screen.WebCompanion.route) },
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .testTag("top_web_companion_button")
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Outlined.Language,
                                                    contentDescription = "Open Web Platform",
                                                    tint = Color(0xFF34D399),
                                                    modifier = Modifier.size(19.dp)
                                                )
                                            }
                                        }

                                        // 3. Sophisticated Settings & System Controls Button
                                        Surface(
                                            shape = CircleShape,
                                            color = Color(0xFF1E1E26),
                                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF2E2E38)),
                                            modifier = Modifier.size(38.dp)
                                        ) {
                                            IconButton(
                                                onClick = { navController.navigate(Screen.Settings.route) },
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .testTag("top_settings_button")
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Outlined.Tune,
                                                    contentDescription = "Settings & Controls",
                                                    tint = Color(0xFFE2E8F0),
                                                    modifier = Modifier.size(19.dp)
                                                )
                                            }
                                        }

                                        // 3. Interactive User DP Avatar
                                        UserDpAvatar(
                                            user = userProfile,
                                            size = 38.dp,
                                            showIndicator = true,
                                            borderWidth = 1.5.dp,
                                            borderColor = MaterialTheme.colorScheme.primary,
                                            onClick = { showUserProfileDialog = true },
                                            modifier = Modifier.testTag("home_user_dp_button")
                                        )
                                    }
                                }
                            }
                        }
                    },
                    bottomBar = {
                        if (isRootScreen) {
                            Surface(
                                color = MaterialTheme.colorScheme.surface,
                                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                NavigationBar(
                                    modifier = Modifier
                                        .windowInsetsPadding(WindowInsets.navigationBars)
                                        .testTag("main_bottom_nav"),
                                    containerColor = MaterialTheme.colorScheme.surface,
                                    tonalElevation = 0.dp
                                ) {
                                    val navItems = listOf(
                                        Screen.Home to (Icons.Filled.GridView to Icons.Outlined.GridView),
                                        Screen.Actionable to (Icons.Filled.Bolt to Icons.Outlined.Bolt),
                                        Screen.Signals to (Icons.Filled.Psychology to Icons.Outlined.Psychology),
                                        Screen.Analytics to (Icons.Filled.QueryStats to Icons.Outlined.QueryStats)
                                    )

                                    navItems.forEach { (screen, iconPair) ->
                                        val isSelected = currentRoute == screen.route
                                        NavigationBarItem(
                                            selected = isSelected,
                                            onClick = {
                                                if (currentRoute != screen.route) {
                                                    navController.navigate(screen.route) {
                                                        popUpTo(navController.graph.findStartDestination().id) {
                                                            saveState = true
                                                        }
                                                        launchSingleTop = true
                                                        restoreState = true
                                                    }
                                                }
                                            },
                                            icon = {
                                                Icon(
                                                    imageVector = if (isSelected) iconPair.first else iconPair.second,
                                                    contentDescription = screen.title
                                                )
                                            },
                                            label = {
                                                Text(
                                                    text = screen.title,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontSize = 10.sp,
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                                )
                                            },
                                            colors = NavigationBarItemDefaults.colors(
                                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                                indicatorColor = MaterialTheme.colorScheme.primaryContainer
                                            ),
                                            modifier = Modifier.testTag("nav_item_${screen.route}")
                                        )
                                    }
                                }
                            }
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                ) { innerPadding ->
                    NavHost(
                        navController = navController,
                        startDestination = Screen.Home.route,
                        modifier = Modifier.padding(innerPadding)
                    ) {
                        composable(Screen.Home.route) {
                            HomeScreen(
                                viewModel = viewModel,
                                onNavigateToDetail = { id ->
                                    navController.navigate(Screen.Detail.createRoute(id))
                                },
                                onNavigateToAdd = {
                                    navController.navigate(Screen.AddEdit.createRoute(null))
                                },
                                onNavigateToWebCompanion = {
                                    navController.navigate(Screen.WebCompanion.route)
                                }
                            )
                        }

                        composable(Screen.Actionable.route) {
                            ActionableIdeasScreen(
                                viewModel = viewModel,
                                onNavigateToDetail = { id ->
                                    navController.navigate(Screen.Detail.createRoute(id))
                                },
                                onNavigateToAdd = {
                                    navController.navigate(Screen.AddEdit.createRoute(null))
                                }
                            )
                        }

                        composable(Screen.Signals.route) {
                            SignalsDiscoveryScreen(
                                viewModel = viewModel,
                                onNavigateToAddWithDraft = { draftedIdea ->
                                    viewModel.saveIdea(draftedIdea) { savedId ->
                                        navController.navigate(Screen.Detail.createRoute(savedId))
                                    }
                                }
                            )
                        }

                        composable(Screen.Analytics.route) {
                            StatisticsScreen(viewModel = viewModel)
                        }

                        composable(Screen.Settings.route) {
                            SettingsScreen(
                                viewModel = viewModel,
                                onNavigateBack = { navController.popBackStack() },
                                onNavigateToWebCompanion = { navController.navigate(Screen.WebCompanion.route) }
                            )
                        }

                        composable(Screen.WebCompanion.route) {
                            WebCompanionScreen(
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }

                        composable(
                            route = Screen.Detail.route,
                            arguments = listOf(navArgument("ideaId") { type = NavType.LongType })
                        ) { backStackEntry ->
                            val ideaId = backStackEntry.arguments?.getLong("ideaId") ?: 0L
                            IdeaDetailScreen(
                                ideaId = ideaId,
                                viewModel = viewModel,
                                onNavigateBack = { navController.popBackStack() },
                                onNavigateToEdit = { id ->
                                    navController.navigate(Screen.AddEdit.createRoute(id))
                                }
                            )
                        }

                        composable(
                            route = Screen.AddEdit.route,
                            arguments = listOf(navArgument("ideaId") {
                                type = NavType.LongType
                                defaultValue = 0L
                            })
                        ) { backStackEntry ->
                            val ideaId = backStackEntry.arguments?.getLong("ideaId")
                            AddEditIdeaScreen(
                                ideaId = if (ideaId != null && ideaId > 0) ideaId else null,
                                viewModel = viewModel,
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }
                    }
                }
            }
        }
    }
}

