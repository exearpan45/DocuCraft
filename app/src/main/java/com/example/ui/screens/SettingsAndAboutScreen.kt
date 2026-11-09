package com.example.ui.screens

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.FileStorageManager
import com.example.model.formatFileSize
import com.example.ui.theme.GradientBlueEnd
import com.example.ui.theme.GradientBlueStart
import java.io.File

enum class SettingsSection(val title: String, val icon: ImageVector) {
    SETTINGS("Settings", Icons.Default.Settings),
    HOW_TO_USE("How to Use", Icons.Default.HelpOutline),
    PRIVACY_POLICY("Privacy Policy", Icons.Default.Security),
    ABOUT("About", Icons.Default.Info)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsAndAboutScreen(
    storageManager: FileStorageManager,
    initialSection: SettingsSection = SettingsSection.SETTINGS,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableStateOf(initialSection) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "App Hub & Settings",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("settings_back_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            ScrollableTabRow(
                selectedTabIndex = selectedTab.ordinal,
                edgePadding = 16.dp,
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                SettingsSection.entries.forEach { section ->
                    Tab(
                        selected = selectedTab == section,
                        onClick = { selectedTab = section },
                        text = { Text(section.title, fontWeight = FontWeight.SemiBold) },
                        icon = {
                            Icon(
                                imageVector = section.icon,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        modifier = Modifier.testTag("tab_${section.name.lowercase()}")
                    )
                }
            }

            when (selectedTab) {
                SettingsSection.SETTINGS -> SettingsTabContent(context = context, storageManager = storageManager)
                SettingsSection.HOW_TO_USE -> HowToUseTabContent()
                SettingsSection.PRIVACY_POLICY -> PrivacyPolicyTabContent()
                SettingsSection.ABOUT -> AboutTabContent()
            }
        }
    }
}

@Composable
fun SettingsTabContent(
    context: Context,
    storageManager: FileStorageManager
) {
    fun calculateCacheBytes(): Long {
        return try {
            val cacheDir = context.cacheDir
            cacheDir.walkTopDown().filter { it.isFile }.map { it.length() }.sum()
        } catch (_: Exception) {
            0L
        }
    }

    var cacheBytes by remember { mutableLongStateOf(calculateCacheBytes()) }
    var cleanSuccess by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("settings_tab_content"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CleaningServices,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Temporary Cache & Storage",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Fast on-device rendering scratchpad",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Current Temp Cache Size",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = formatFileSize(cacheBytes),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            storageManager.clearTempFiles()
                            cacheBytes = calculateCacheBytes()
                            cleanSuccess = true
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                            contentColor = MaterialTheme.colorScheme.onErrorContainer
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("clear_cache_button")
                    ) {
                        Icon(Icons.Default.DeleteSweep, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Clear Temporary Cache Files", fontWeight = FontWeight.Bold)
                    }

                    AnimatedVisibility(visible = cleanSuccess) {
                        Column {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "✓ Temporary processing cache cleaned successfully!",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }

        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    Text(
                        text = "Engine Preferences",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "DocuCraft uses hardware-accelerated local rendering. No external server round-trips or subscriptions are required.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    SettingInfoRow(
                        title = "Storage Architecture",
                        subtitle = "Android App-Specific Scoped Directory & System Storage Access Framework (SAF)"
                    )
                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
                    SettingInfoRow(
                        title = "PDF Rendering Pipeline",
                        subtitle = "Native android.graphics.pdf.PdfRenderer & PdfDocument with DirectSeek FileDescriptors"
                    )
                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
                    SettingInfoRow(
                        title = "Image Processing Backend",
                        subtitle = "Native Bitmap hardware decoding with inSampleSize and ColorMatrix math"
                    )
                }
            }
        }
    }
}

@Composable
fun SettingInfoRow(title: String, subtitle: String) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun HowToUseTabContent() {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("how_to_use_tab_content"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            GuideHeaderCard(
                title = "Welcome to DocuCraft Guide",
                description = "Learn how to get the most out of your native Image and PDF processing tools with step-by-step instructions."
            )
        }

        item {
            GuideFeatureCard(
                icon = Icons.Default.Image,
                iconTint = Color(0xFF0284C7),
                title = "1. Image Tools & Size Reducer",
                steps = listOf(
                    "Size Reducer: Tap 'Size Reducer' and select a target limit (< 100 KB, < 200 KB, or custom KB). The engine calculates optimal lossy downscaling automatically.",
                    "Resize & Compress: Adjust target width and height while locking the aspect ratio. Choose between JPEG, PNG, or WEBP output formats.",
                    "Color Filters & Scanner: Choose Doc Scan High Contrast to whiten backgrounds and darken text, Grayscale, Sepia, or Inverted modes.",
                    "Watermarks: Toggle 'Add Text Watermark' and input your custom text and transparency.",
                    "Crop & Rotate: Use the interactive touch box to freely crop or lock to 1:1, 4:3, 16:9, and rotate/flip as desired."
                )
            )
        }

        item {
            GuideFeatureCard(
                icon = Icons.Default.Description,
                iconTint = Color(0xFF6366F1),
                title = "2. PDF Tools",
                steps = listOf(
                    "Merge PDFs: Tap 'Add PDF' to pick multiple documents, drag or adjust their order, and tap 'Merge Selected PDFs'.",
                    "Split & Extract: Enter comma-separated pages or ranges (e.g., '1-3, 5, 8') to export specific pages into a new PDF.",
                    "Reorder & Delete: Browse page thumbnail previews, tap to remove unnecessary pages, and save the updated file.",
                    "PDF Size Reducer: Choose between Scanned / Heavy PDF compression or Standard compression to reduce file sizes for email and portal submissions.",
                    "Rotate Pages: Correct orientation of upside-down or sideways pages by rotating 90°, 180°, or 270°.",
                    "PDF Watermark: Stamp confidential or custom text diagonally across every page.",
                    "Images to PDF: Select multiple photos and export as high-resolution A4, Letter, or Fit-Image PDF documents."
                )
            )
        }

        item {
            GuideFeatureCard(
                icon = Icons.Default.AutoAwesome,
                iconTint = Color(0xFF10B981),
                title = "3. Saving & Sharing Documents",
                steps = listOf(
                    "Every completed operation allows you to preview the file size before and after processing.",
                    "Tap 'Save to Custom Folder' to pick any folder on your phone or Google Drive via Android's Storage Access Framework.",
                    "Tap 'Share' to instantly send the processed file to WhatsApp, Gmail, Drive, or nearby devices.",
                    "Use the 'Document Library' tab to search by name or date, filter by PDF or Image, and manage past exports."
                )
            )
        }
    }
}

@Composable
fun GuideHeaderCard(title: String, description: String) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
            )
        }
    }
}

@Composable
fun GuideFeatureCard(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    steps: List<String>
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(iconTint.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            steps.forEachIndexed { index, step ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Text(
                        text = "•",
                        style = MaterialTheme.typography.titleMedium,
                        color = iconTint,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Text(
                        text = step,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

@Composable
fun PrivacyPolicyTabContent() {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("privacy_policy_tab_content"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(22.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.VerifiedUser,
                            contentDescription = null,
                            tint = Color(0xFF34D399),
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "100% On-Device Privacy Guarantee",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "DocuCraft operates with zero server dependencies. Your confidential contracts, receipts, identity documents, and personal photos are NEVER uploaded, transmitted, or monitored.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFFCBD5E1)
                    )
                }
            }
        }

        item {
            PrivacyPolicyCard(
                icon = Icons.Default.Lock,
                title = "Local Sandboxing & Zero Cloud Transmissions",
                body = "All image transformations, cropping, rotations, size reduction, and PDF operations occur strictly inside the device's CPU, GPU, and local RAM memory space. No analytics, tracking beacons, or telemetries are installed."
            )
        }

        item {
            PrivacyPolicyCard(
                icon = Icons.Default.PhoneAndroid,
                title = "Scoped Storage & Zero Invasive Permissions",
                body = "DocuCraft does NOT request dangerous broad storage permissions (READ_EXTERNAL_STORAGE). It relies exclusively on the standard Android Photo Picker and Storage Access Framework (SAF), accessing only the single document you explicitly select."
            )
        }

        item {
            PrivacyPolicyCard(
                icon = Icons.Default.Speed,
                title = "Deterministic Output & No Silent Data Changes",
                body = "Your original documents are never modified or overwritten without your explicit choice. All generated files are placed in an app-private cache or your designated export folder."
            )
        }
    }
}

@Composable
fun PrivacyPolicyCard(
    icon: ImageVector,
    title: String,
    body: String
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = body,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun AboutTabContent() {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("about_tab_content"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        brush = Brush.linearGradient(
                            listOf(GradientBlueStart, GradientBlueEnd)
                        ),
                        shape = RoundedCornerShape(24.dp)
                    )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(70.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.WorkspacePremium,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(38.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "DocuCraft",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )

                    Text(
                        text = "Native Image & PDF Engine",
                        style = MaterialTheme.typography.titleSmall,
                        color = Color.White.copy(alpha = 0.85f)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color.White.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = "Version 1.0.0 (Release Build)",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                        )
                    }
                }
            }
        }

        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    Text(
                        text = "Engineering & Authorship",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column {
                            Text(
                                text = "Lead Developer",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Arpan Goswami",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(16.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.secondaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Code,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column {
                            Text(
                                text = "Core Platform Architecture",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "100% Native Kotlin & Jetpack Compose",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }

        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    Text(
                        text = "Technical Highlights",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    val highlights = listOf(
                        "Zero third-party network SDKs or telemetry",
                        "High-contrast doc scanning & custom text watermarking",
                        "Multi-page PDF merge, page extraction & reordering",
                        "Lossless & lossy JPEG, WEBP, and PNG processing",
                        "Modern Material 3 dynamic theming & smooth spring physics"
                    )

                    highlights.forEach { highlight ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("✦", color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(end = 8.dp))
                            Text(highlight, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
    }
}
