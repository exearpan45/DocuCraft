package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarDefaults
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.ImageToolsScreen
import com.example.ui.screens.PdfToolsScreen
import com.example.ui.screens.RecentHistoryScreen
import com.example.ui.screens.SettingsAndAboutScreen
import com.example.ui.screens.SettingsSection
import com.example.ui.theme.GradientBlueEnd
import com.example.ui.theme.GradientBlueStart
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.HistoryViewModel
import com.example.viewmodel.ImageToolsViewModel
import com.example.viewmodel.PdfToolsViewModel
import kotlinx.coroutines.delay

sealed interface AppScreen {
    data object Home : AppScreen
    data class ImageTools(val mode: String = "SIZE_REDUCER") : AppScreen
    data class PdfTools(val tab: String = "MERGE") : AppScreen
    data object History : AppScreen
    data class Settings(val section: SettingsSection = SettingsSection.SETTINGS) : AppScreen
}

class MainActivity : ComponentActivity() {

    private val imageToolsViewModel by viewModels<ImageToolsViewModel>()
    private val pdfToolsViewModel by viewModels<PdfToolsViewModel>()
    private val historyViewModel by viewModels<HistoryViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                var showSplashScreen by remember { mutableStateOf(true) }
                var currentScreen by remember { mutableStateOf<AppScreen>(AppScreen.Home) }

                // Opening Splash Logo Animation
                if (showSplashScreen) {
                    AppOpeningSplash(
                        onSplashFinished = { showSplashScreen = false }
                    )
                } else {
                    // Handle back press
                    if (currentScreen != AppScreen.Home) {
                        BackHandler {
                            currentScreen = AppScreen.Home
                        }
                    }

                    Scaffold(
                        modifier = Modifier.fillMaxSize(),
                        bottomBar = {
                            DocuCraftBottomBar(
                                currentScreen = currentScreen,
                                onScreenSelected = { screen ->
                                    if (screen == AppScreen.History) {
                                        historyViewModel.loadHistory()
                                    }
                                    currentScreen = screen
                                }
                            )
                        }
                    ) { innerPadding ->
                        // Smooth left-to-right / right-to-left directional sliding
                        AnimatedContent(
                            targetState = currentScreen,
                            transitionSpec = {
                                val isNavigatingBack = targetState is AppScreen.Home
                                if (isNavigatingBack) {
                                    // Smooth slide from left to right when returning to Home
                                    (slideInHorizontally(
                                        animationSpec = spring(
                                            dampingRatio = Spring.DampingRatioLowBouncy,
                                            stiffness = Spring.StiffnessMediumLow
                                        )
                                    ) { -it } + fadeIn(animationSpec = tween(300))).togetherWith(
                                        slideOutHorizontally(
                                            animationSpec = tween(280, easing = FastOutSlowInEasing)
                                        ) { it } + fadeOut(animationSpec = tween(250))
                                    )
                                } else {
                                    // Smooth slide from right to left when entering tools
                                    (slideInHorizontally(
                                        animationSpec = spring(
                                            dampingRatio = Spring.DampingRatioLowBouncy,
                                            stiffness = Spring.StiffnessMediumLow
                                        )
                                    ) { it } + fadeIn(animationSpec = tween(300))).togetherWith(
                                        slideOutHorizontally(
                                            animationSpec = tween(280, easing = FastOutSlowInEasing)
                                        ) { -it } + fadeOut(animationSpec = tween(250))
                                    )
                                }
                            },
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding),
                            label = "screen_smooth_slide_transition"
                        ) { target ->
                            when (target) {
                                is AppScreen.Home -> {
                                    HomeScreen(
                                        onNavigateToImageTools = { mode ->
                                            currentScreen = AppScreen.ImageTools(mode)
                                        },
                                        onNavigateToPdfTools = { tab ->
                                            currentScreen = AppScreen.PdfTools(tab)
                                        },
                                        onNavigateToHistory = {
                                            historyViewModel.loadHistory()
                                            currentScreen = AppScreen.History
                                        },
                                        onNavigateToSettings = { section ->
                                            currentScreen = AppScreen.Settings(section)
                                        }
                                    )
                                }
                                is AppScreen.ImageTools -> {
                                    ImageToolsScreen(
                                        viewModel = imageToolsViewModel,
                                        initialMode = target.mode,
                                        onNavigateBack = { currentScreen = AppScreen.Home }
                                    )
                                }
                                is AppScreen.PdfTools -> {
                                    PdfToolsScreen(
                                        viewModel = pdfToolsViewModel,
                                        initialTab = target.tab,
                                        onNavigateBack = { currentScreen = AppScreen.Home }
                                    )
                                }
                                is AppScreen.History -> {
                                    RecentHistoryScreen(
                                        viewModel = historyViewModel,
                                        onNavigateBack = { currentScreen = AppScreen.Home }
                                    )
                                }
                                is AppScreen.Settings -> {
                                    SettingsAndAboutScreen(
                                        storageManager = imageToolsViewModel.storageManager,
                                        initialSection = target.section,
                                        onNavigateBack = { currentScreen = AppScreen.Home }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AppOpeningSplash(
    onSplashFinished: () -> Unit
) {
    var startAnimation by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        startAnimation = true
        delay(1400)
        onSplashFinished()
    }

    val scale by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0.4f,
        animationSpec = spring(
            dampingRatio = 0.5f,
            stiffness = Spring.StiffnessLow
        ),
        label = "logo_scale"
    )

    val alpha by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0f,
        animationSpec = tween(600),
        label = "content_alpha"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "halo_pulse")
    val haloScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "halo_scale"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    listOf(
                        Color(0xFF1E3A8A), // Deep Royal Sapphire
                        Color(0xFF0F172A), // Slate 900
                        Color(0xFF070B14)  // Midnight Obsidian
                    )
                )
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onSplashFinished() }
            .testTag("app_opening_splash"),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Glowing Halo behind logo
            Box(
                modifier = Modifier
                    .size(140.dp)
                    .scale(haloScale)
                    .clip(CircleShape)
                    .background(Color(0xFF3B82F6).copy(alpha = 0.25f)),
                contentAlignment = Alignment.Center
            ) {
                // Animated Logo Container
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .scale(scale)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                listOf(GradientBlueStart, GradientBlueEnd)
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_app_icon),
                        contentDescription = "DocuCraft Logo",
                        modifier = Modifier
                            .size(76.dp)
                            .clip(CircleShape)
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Brand Title with Smooth Fade
            Text(
                text = "DocuCraft",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
                letterSpacing = 1.sp,
                modifier = Modifier.scale(scale)
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Native Image & PDF Engine",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF93C5FD).copy(alpha = alpha)
            )

            Spacer(modifier = Modifier.height(18.dp))

            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color.White.copy(alpha = 0.12f),
                modifier = Modifier.scale(scale)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = null,
                        tint = Color(0xFF34D399),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "100% Private · On-Device",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                }
            }
        }
    }
}

@Composable
fun DocuCraftBottomBar(
    currentScreen: AppScreen,
    onScreenSelected: (AppScreen) -> Unit
) {
    NavigationBar(
        windowInsets = WindowInsets.navigationBars,
        tonalElevation = 6.dp,
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = Modifier.testTag("main_navigation_bar")
    ) {
        NavigationBarItem(
            selected = currentScreen is AppScreen.Home,
            onClick = { onScreenSelected(AppScreen.Home) },
            icon = {
                Icon(
                    imageVector = if (currentScreen is AppScreen.Home) Icons.Filled.Home else Icons.Outlined.Home,
                    contentDescription = "Home"
                )
            },
            label = { Text("Home", fontSize = 11.sp, fontWeight = if (currentScreen is AppScreen.Home) FontWeight.Bold else FontWeight.Normal) },
            modifier = Modifier.testTag("nav_home")
        )

        NavigationBarItem(
            selected = currentScreen is AppScreen.ImageTools,
            onClick = { onScreenSelected(AppScreen.ImageTools()) },
            icon = {
                Icon(
                    imageVector = if (currentScreen is AppScreen.ImageTools) Icons.Filled.Image else Icons.Outlined.Image,
                    contentDescription = "Image Tools"
                )
            },
            label = { Text("Image", fontSize = 11.sp, fontWeight = if (currentScreen is AppScreen.ImageTools) FontWeight.Bold else FontWeight.Normal) },
            modifier = Modifier.testTag("nav_image_tools")
        )

        NavigationBarItem(
            selected = currentScreen is AppScreen.PdfTools,
            onClick = { onScreenSelected(AppScreen.PdfTools()) },
            icon = {
                Icon(
                    imageVector = if (currentScreen is AppScreen.PdfTools) Icons.Filled.Description else Icons.Outlined.Description,
                    contentDescription = "PDF Tools"
                )
            },
            label = { Text("PDF", fontSize = 11.sp, fontWeight = if (currentScreen is AppScreen.PdfTools) FontWeight.Bold else FontWeight.Normal) },
            modifier = Modifier.testTag("nav_pdf_tools")
        )

        NavigationBarItem(
            selected = currentScreen is AppScreen.History,
            onClick = { onScreenSelected(AppScreen.History) },
            icon = {
                Icon(
                    imageVector = if (currentScreen is AppScreen.History) Icons.Filled.History else Icons.Outlined.History,
                    contentDescription = "Document Library"
                )
            },
            label = { Text("Library", fontSize = 11.sp, fontWeight = if (currentScreen is AppScreen.History) FontWeight.Bold else FontWeight.Normal) },
            modifier = Modifier.testTag("nav_history")
        )

        NavigationBarItem(
            selected = currentScreen is AppScreen.Settings,
            onClick = { onScreenSelected(AppScreen.Settings()) },
            icon = {
                Icon(
                    imageVector = if (currentScreen is AppScreen.Settings) Icons.Filled.Settings else Icons.Outlined.Settings,
                    contentDescription = "Settings"
                )
            },
            label = { Text("Settings", fontSize = 11.sp, fontWeight = if (currentScreen is AppScreen.Settings) FontWeight.Bold else FontWeight.Normal) },
            modifier = Modifier.testTag("nav_settings")
        )
    }
}
