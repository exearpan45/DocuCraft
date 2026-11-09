package com.example.ui.screens

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CallSplit
import androidx.compose.material.icons.filled.Compress
import androidx.compose.material.icons.filled.CropRotate
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MergeType
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.PhotoSizeSelectSmall
import androidx.compose.material.icons.filled.Reorder
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import com.example.R
import com.example.ui.components.ToolActionCard
import com.example.ui.theme.GradientBlueEnd
import com.example.ui.theme.GradientBlueStart
import com.example.ui.theme.GradientGoldEnd
import com.example.ui.theme.GradientGoldStart
import com.example.ui.theme.GradientPurpleEnd
import com.example.ui.theme.GradientPurpleStart
import com.example.ui.theme.GradientTealEnd
import com.example.ui.theme.GradientTealStart
import com.example.ui.theme.PrimaryLight
import com.example.ui.theme.SecondaryLight
import com.example.ui.theme.TertiaryLight

@Composable
fun HomeScreen(
    onNavigateToImageTools: (initialMode: String) -> Unit,
    onNavigateToPdfTools: (initialTab: String) -> Unit,
    onNavigateToHistory: () -> Unit,
    onNavigateToSettings: (section: com.example.ui.screens.SettingsSection) -> Unit
) {
    // Subtle pulsing animation for the app logo
    val infiniteTransition = rememberInfiniteTransition(label = "logo_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("home_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Hero Header & Animated Logo with Premium Gradient
        item {
            Card(
                shape = RoundedCornerShape(26.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        brush = Brush.linearGradient(
                            listOf(
                                MaterialTheme.colorScheme.primaryContainer,
                                MaterialTheme.colorScheme.surfaceVariant
                            )
                        ),
                        shape = RoundedCornerShape(26.dp)
                    )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(22.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "DocuCraft",
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Icon(
                                    imageVector = Icons.Default.WorkspacePremium,
                                    contentDescription = null,
                                    tint = Color(0xFFF59E0B),
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Professional Image & PDF Engine",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                            )
                        }

                        // Animated Glowing Logo
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .scale(pulseScale)
                                .clip(CircleShape)
                                .background(
                                    Brush.radialGradient(
                                        listOf(
                                            MaterialTheme.colorScheme.primary,
                                            GradientBlueEnd
                                        )
                                    )
                                )
                                .border(
                                    width = 2.dp,
                                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.4f),
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Security,
                                contentDescription = "DocuCraft Logo",
                                tint = Color.White,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                        shadowElevation = 2.dp
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "100% On-Device · Zero Cloud Uploads · Private & Fast",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }

        // Section: Dedicated Size Reducer Tools Spotlight
        item {
            SectionHeader(
                title = "Size Reducer Tools",
                subtitle = "Reduce photos and documents to target KB sizes or compression profiles"
            )
        }

        item {
            ToolActionCard(
                title = "Image Size Reducer",
                description = "Automatically shrink photos to exact targets: < 100 KB, < 500 KB, or custom KB for forms and portals",
                icon = Icons.Default.PhotoSizeSelectSmall,
                iconBackgroundColor = Color(0xFF0284C7), // Sky Blue
                tag = "home_image_size_reducer_button",
                onClick = { onNavigateToImageTools("SIZE_REDUCER") }
            )
        }

        item {
            ToolActionCard(
                title = "PDF Size Reducer",
                description = "Compress large scanned or image-dense PDFs with optimized DPI and JPEG stream reduction",
                icon = Icons.Default.Compress,
                iconBackgroundColor = Color(0xFFE11D48), // Rose Red
                tag = "home_pdf_size_reducer_button",
                onClick = { onNavigateToPdfTools("COMPRESS") }
            )
        }

        // Section: Image Tools
        item {
            Spacer(modifier = Modifier.height(4.dp))
            SectionHeader(title = "Image Tools", subtitle = "Resize, compress, convert, crop and rotate")
        }

        item {
            ToolActionCard(
                title = "Resize & Compress",
                description = "Adjust dimensions with aspect lock, reduce file size, or convert between JPEG, PNG, WEBP",
                icon = Icons.Default.Tune,
                iconBackgroundColor = PrimaryLight,
                tag = "home_image_resize_compress_button",
                onClick = { onNavigateToImageTools("RESIZE_COMPRESS") }
            )
        }

        item {
            ToolActionCard(
                title = "Crop & Rotate",
                description = "Rotate 90°/180°, flip horizontally/vertically, and crop with free or preset aspect ratios",
                icon = Icons.Default.CropRotate,
                iconBackgroundColor = SecondaryLight,
                tag = "home_image_crop_rotate_button",
                onClick = { onNavigateToImageTools("CROP_ROTATE") }
            )
        }

        // Section: PDF Tools
        item {
            Spacer(modifier = Modifier.height(4.dp))
            SectionHeader(title = "PDF Tools", subtitle = "Merge, split, reorder, compress, and convert")
        }

        item {
            ToolActionCard(
                title = "Merge PDFs",
                description = "Combine multiple PDF documents into a single organized file in custom order",
                icon = Icons.Default.MergeType,
                iconBackgroundColor = Color(0xFF6366F1), // Indigo
                tag = "home_pdf_merge_button",
                onClick = { onNavigateToPdfTools("MERGE") }
            )
        }

        item {
            ToolActionCard(
                title = "Split & Extract Pages",
                description = "Extract specific pages or page ranges into a brand new PDF",
                icon = Icons.Default.CallSplit,
                iconBackgroundColor = Color(0xFF0EA5E9), // Sky Blue
                tag = "home_pdf_split_button",
                onClick = { onNavigateToPdfTools("SPLIT") }
            )
        }

        item {
            ToolActionCard(
                title = "Reorder & Delete Pages",
                description = "Visually view pages, reorder them, and remove unwanted pages",
                icon = Icons.Default.Reorder,
                iconBackgroundColor = Color(0xFF8B5CF6), // Purple
                tag = "home_pdf_reorder_button",
                onClick = { onNavigateToPdfTools("REORDER") }
            )
        }

        item {
            ToolActionCard(
                title = "Images to PDF",
                description = "Assemble multiple photos into an A4, Letter, or Fit-Image PDF document",
                icon = Icons.Default.PhotoLibrary,
                iconBackgroundColor = Color(0xFF10B981), // Emerald
                tag = "home_images_to_pdf_button",
                onClick = { onNavigateToPdfTools("IMAGES_TO_PDF") }
            )
        }

        // Section: Saved Files & History
        item {
            Spacer(modifier = Modifier.height(4.dp))
            ToolActionCard(
                title = "Document Library & History",
                description = "Search by file name or date, filter, view, and share your processed documents",
                icon = Icons.Default.History,
                iconBackgroundColor = MaterialTheme.colorScheme.surfaceVariant,
                iconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                tag = "home_history_button",
                onClick = onNavigateToHistory
            )
        }

        // Section: App Hub, Privacy & Info
        item {
            Spacer(modifier = Modifier.height(4.dp))
            SectionHeader(
                title = "App Hub & Privacy",
                subtitle = "Settings, Privacy Policy, How to Use guide, and About DocuCraft"
            )
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onNavigateToSettings(com.example.ui.screens.SettingsSection.HOW_TO_USE) }
                        .testTag("home_how_to_use_card")
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Icon(
                            imageVector = Icons.Default.HelpOutline,
                            contentDescription = null,
                            tint = Color(0xFF6366F1),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "How to Use",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Step-by-step guide",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onNavigateToSettings(com.example.ui.screens.SettingsSection.PRIVACY_POLICY) }
                        .testTag("home_privacy_policy_card")
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = null,
                            tint = Color(0xFF10B981),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Privacy Policy",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "100% on-device",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onNavigateToSettings(com.example.ui.screens.SettingsSection.ABOUT) }
                        .testTag("home_about_card")
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Icon(
                            imageVector = Icons.Default.WorkspacePremium,
                            contentDescription = null,
                            tint = Color(0xFFF59E0B),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "About",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "v1.0.0 · Arpan",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun SectionHeader(title: String, subtitle: String) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
