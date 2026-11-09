package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.Flip
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.PhotoSizeSelectSmall
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Rotate90DegreesCcw
import androidx.compose.material.icons.filled.Rotate90DegreesCw
import androidx.compose.material.icons.filled.Tune
import com.example.model.TargetSizePreset
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.model.CropAspectRatio
import com.example.model.ImageFormat
import com.example.model.UiProcessingState
import com.example.model.formatFileSize
import com.example.ui.components.ErrorAlertDialog
import com.example.ui.components.ImageDetailsCard
import com.example.ui.components.InteractiveCropOverlay
import com.example.ui.components.ProcessingDialog
import com.example.ui.components.SuccessResultDialog
import com.example.ui.theme.SuccessGreen
import com.example.viewmodel.ImageToolsViewModel
import java.io.File
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImageToolsScreen(
    viewModel: ImageToolsViewModel,
    initialMode: String = "RESIZE_COMPRESS",
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var selectedTabIndex by remember(initialMode) {
        mutableIntStateOf(
            when (initialMode) {
                "CROP_ROTATE" -> 2
                "RESIZE_COMPRESS" -> 1
                else -> 0
            }
        )
    }

    // Photo picker launcher
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            viewModel.selectImage(uri)
        }
    }

    // Document picker fallback
    val docPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            viewModel.selectImage(uri)
        }
    }

    // Save to custom destination SAF launcher
    var pendingSaveFile by remember { mutableStateOf<File?>(null) }
    val saveFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("image/*")
    ) { destUri ->
        if (destUri != null && pendingSaveFile != null) {
            viewModel.exportToDestination(pendingSaveFile!!, destUri)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Image Tools", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack, modifier = Modifier.testTag("image_tools_back_button")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (uiState.selectedImageUri != null) {
                        IconButton(onClick = {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        }) {
                            Icon(Icons.Default.Refresh, contentDescription = "Pick Different Image")
                        }
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
            // Mode Tabs: Resize & Compress vs Crop & Rotate
            TabRow(
                selectedTabIndex = selectedTabIndex,
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                Tab(
                    selected = selectedTabIndex == 0,
                    onClick = { selectedTabIndex = 0 },
                    text = { Text("Size Reducer") },
                    icon = { Icon(Icons.Default.PhotoSizeSelectSmall, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    modifier = Modifier.testTag("tab_size_reducer")
                )
                Tab(
                    selected = selectedTabIndex == 1,
                    onClick = { selectedTabIndex = 1 },
                    text = { Text("Resize & Compress") },
                    icon = { Icon(Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    modifier = Modifier.testTag("tab_resize_compress")
                )
                Tab(
                    selected = selectedTabIndex == 2,
                    onClick = { selectedTabIndex = 2 },
                    text = { Text("Crop & Rotate") },
                    icon = { Icon(Icons.Default.Crop, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    modifier = Modifier.testTag("tab_crop_rotate")
                )
            }

            if (uiState.selectedImageUri == null) {
                // Empty state: Select an Image
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AddPhotoAlternate,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(36.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Text(
                                text = "Choose an Image to Edit",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            Text(
                                text = "Pick any JPEG, PNG, or WEBP photo from your device. Processing is executed securely on your phone.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )

                            Spacer(modifier = Modifier.height(24.dp))

                            Button(
                                onClick = {
                                    photoPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("pick_image_button"),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Choose from Photos")
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            OutlinedButton(
                                onClick = {
                                    docPickerLauncher.launch(arrayOf("image/*"))
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Browse Device Files")
                            }
                        }
                    }
                }
            } else {
                // Image Loaded: Show preview and controls
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Image details card
                    item {
                        if (uiState.imageDetails != null) {
                            ImageDetailsCard(details = uiState.imageDetails!!)
                        } else if (uiState.isLoadingDetails) {
                            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator()
                            }
                        }
                    }

                    when (selectedTabIndex) {
                        0 -> {
                            // TAB 0: Size Reducer
                            item {
                                SizeReducerControlsSection(viewModel = viewModel, uiState = uiState)
                            }
                        }
                        1 -> {
                            // TAB 1: Resize & Compress
                            item {
                                ResizeControlsSection(viewModel = viewModel, uiState = uiState)
                            }
                            item {
                                CompressControlsSection(viewModel = viewModel, uiState = uiState)
                            }
                        }
                        else -> {
                            // TAB 2: Crop & Rotate
                            item {
                                CropRotateControlsSection(viewModel = viewModel, uiState = uiState)
                            }
                        }
                    }

                    // Process and Save CTA Button
                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = {
                                if (selectedTabIndex == 0) {
                                    viewModel.executeReduceImageSize()
                                } else {
                                    viewModel.processImage()
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .testTag("process_image_button"),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Text(
                                text = when (selectedTabIndex) {
                                    0 -> "Reduce Image to Target Size"
                                    1 -> "Process & Save Image"
                                    else -> "Apply Crop, Rotate & Save"
                                },
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(24.dp))
                    }
                }
            }
        }
    }

    // Processing Dialog
    when (val state = uiState.processingState) {
        is UiProcessingState.Processing -> {
            ProcessingDialog(state = state, onCancel = { viewModel.cancelActiveJob() })
        }
        is UiProcessingState.Success -> {
            SuccessResultDialog(
                state = state,
                onSaveToDevice = { file ->
                    pendingSaveFile = file
                    saveFileLauncher.launch(file.name)
                },
                onShare = { file ->
                    val intent = viewModel.storageManager.createShareIntent(file, uiState.outputFormat.mimeType)
                    context.startActivity(Intent.createChooser(intent, "Share Image"))
                },
                onOpenFile = { file ->
                    try {
                        val intent = viewModel.storageManager.createViewIntent(file, uiState.outputFormat.mimeType)
                        context.startActivity(intent)
                    } catch (_: Exception) {}
                },
                onDismiss = { viewModel.dismissProcessingState() }
            )
        }
        is UiProcessingState.Error -> {
            ErrorAlertDialog(state = state, onDismiss = { viewModel.dismissProcessingState() })
        }
        UiProcessingState.Idle -> {}
    }
}

@Composable
fun ResizeControlsSection(
    viewModel: ImageToolsViewModel,
    uiState: com.example.viewmodel.ImageToolsUiState
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Resize & Dimensions",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (uiState.preserveAspectRatio) Icons.Default.Lock else Icons.Default.LockOpen,
                        contentDescription = null,
                        tint = if (uiState.preserveAspectRatio) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Lock Aspect",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Switch(
                        checked = uiState.preserveAspectRatio,
                        onCheckedChange = { viewModel.togglePreserveAspectRatio() },
                        modifier = Modifier.testTag("lock_aspect_switch")
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Percentage Presets
            Text(
                text = "Preset Scales",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(25, 50, 75, 100).forEach { percent ->
                    FilterChip(
                        selected = uiState.selectedScalePercent == percent,
                        onClick = { viewModel.setScalePercent(percent) },
                        label = { Text("$percent%") },
                        modifier = Modifier.weight(1f),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Custom width and height fields
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = if (uiState.targetWidth > 0) uiState.targetWidth.toString() else "",
                    onValueChange = { str ->
                        val intVal = str.toIntOrNull() ?: 0
                        viewModel.onWidthChanged(intVal)
                    },
                    label = { Text("Width (px)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("width_input"),
                    singleLine = true
                )

                OutlinedTextField(
                    value = if (uiState.targetHeight > 0) uiState.targetHeight.toString() else "",
                    onValueChange = { str ->
                        val intVal = str.toIntOrNull() ?: 0
                        viewModel.onHeightChanged(intVal)
                    },
                    label = { Text("Height (px)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("height_input"),
                    singleLine = true
                )
            }
        }
    }
}

@Composable
fun CompressControlsSection(
    viewModel: ImageToolsViewModel,
    uiState: com.example.viewmodel.ImageToolsUiState
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Format & Compression",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Format Selection
            Text(
                text = "Target Format",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ImageFormat.entries.forEach { fmt ->
                    FilterChip(
                        selected = uiState.outputFormat == fmt,
                        onClick = { viewModel.setOutputFormat(fmt) },
                        label = { Text(fmt.name) },
                        modifier = Modifier.weight(1f),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Quality Slider (for JPEG / WEBP)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Compression Quality",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "${uiState.compressionQuality}%",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Slider(
                value = uiState.compressionQuality.toFloat(),
                onValueChange = { viewModel.setCompressionQuality(it.roundToInt()) },
                valueRange = 10f..100f,
                steps = 17,
                modifier = Modifier.fillMaxWidth().testTag("quality_slider")
            )

            // Estimated file size preview
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Estimated File Size",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = if (uiState.isEstimatingSize) "Estimating..." else formatFileSize(uiState.estimatedSizeBytes),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    val origSize = uiState.imageDetails?.sizeBytes ?: 0L
                    if (origSize > 0 && uiState.estimatedSizeBytes > 0 && uiState.estimatedSizeBytes < origSize) {
                        val pct = (((origSize - uiState.estimatedSizeBytes).toDouble() / origSize) * 100).roundToInt()
                        Text(
                            text = "~$pct% smaller",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = SuccessGreen
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Color Filter & Document Enhancer
            Text(
                text = "Color Mode & Document Filter",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                com.example.model.ImageFilterMode.entries.forEach { mode ->
                    FilterChip(
                        selected = uiState.filterMode == mode,
                        onClick = { viewModel.setFilterMode(mode) },
                        label = { Text(mode.displayName, fontSize = 11.sp) },
                        modifier = Modifier.weight(1f),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.tertiaryContainer
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Watermark Controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Add Text Watermark",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Switch(
                    checked = uiState.watermarkConfig.isEnabled,
                    onCheckedChange = { checked ->
                        viewModel.setWatermarkConfig(uiState.watermarkConfig.copy(isEnabled = checked))
                    }
                )
            }

            if (uiState.watermarkConfig.isEnabled) {
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = uiState.watermarkConfig.text,
                    onValueChange = { text ->
                        viewModel.setWatermarkConfig(uiState.watermarkConfig.copy(text = text))
                    },
                    label = { Text("Watermark Text (e.g. CONFIDENTIAL)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Watermark Opacity: ${(uiState.watermarkConfig.alpha * 100).roundToInt()}%",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Slider(
                    value = uiState.watermarkConfig.alpha,
                    onValueChange = { a ->
                        viewModel.setWatermarkConfig(uiState.watermarkConfig.copy(alpha = a))
                    },
                    valueRange = 0.1f..0.9f
                )
            }
        }
    }
}

@Composable
fun CropRotateControlsSection(
    viewModel: ImageToolsViewModel,
    uiState: com.example.viewmodel.ImageToolsUiState
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Crop & Orientation",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Aspect Presets
            Text(
                text = "Aspect Ratio",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                CropAspectRatio.entries.take(4).forEach { aspect ->
                    FilterChip(
                        selected = uiState.selectedCropAspect == aspect,
                        onClick = { viewModel.setCropAspect(aspect) },
                        label = { Text(aspect.label) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                CropAspectRatio.entries.drop(4).forEach { aspect ->
                    FilterChip(
                        selected = uiState.selectedCropAspect == aspect,
                        onClick = { viewModel.setCropAspect(aspect) },
                        label = { Text(aspect.label) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Interactive Crop Preview
            val details = uiState.imageDetails
            val aspect = if (details != null && details.height > 0) {
                details.width.toFloat() / details.height.toFloat()
            } else 1.0f

            InteractiveCropOverlay(
                imageAspect = aspect,
                cropAspect = uiState.selectedCropAspect,
                cropRect = uiState.cropRect,
                onCropRectChanged = { viewModel.updateCropRect(it) }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Rotation & Flip buttons
            Text(
                text = "Rotate & Flip",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { viewModel.rotateCounterClockwise() },
                    modifier = Modifier.weight(1f).testTag("rotate_ccw_button")
                ) {
                    Icon(Icons.Default.Rotate90DegreesCcw, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("-90°")
                }

                OutlinedButton(
                    onClick = { viewModel.rotateClockwise() },
                    modifier = Modifier.weight(1f).testTag("rotate_cw_button")
                ) {
                    Icon(Icons.Default.Rotate90DegreesCw, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("+90°")
                }

                OutlinedButton(
                    onClick = { viewModel.toggleFlipHorizontal() },
                    modifier = Modifier.weight(1f).testTag("flip_horizontal_button")
                ) {
                    Icon(Icons.Default.Flip, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Flip")
                }
            }

            if (uiState.rotationDegrees != 0 || uiState.flipHorizontal || uiState.flipVertical) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Current orientation: ${uiState.rotationDegrees}° rotation" +
                            if (uiState.flipHorizontal) " · Flipped Horizontal" else "",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
fun SizeReducerControlsSection(
    viewModel: ImageToolsViewModel,
    uiState: com.example.viewmodel.ImageToolsUiState
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Target File Size Limit",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Select a target maximum size. The compression algorithm automatically calculates optimal resolution downscaling and lossy encoding to compress under this limit.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Presets
            Text(
                text = "Select Target Limit",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf(
                    TargetSizePreset.UNDER_100_KB,
                    TargetSizePreset.UNDER_200_KB,
                    TargetSizePreset.UNDER_500_KB
                ).forEach { preset ->
                    FilterChip(
                        selected = uiState.targetSizePreset == preset,
                        onClick = { viewModel.setTargetSizePreset(preset) },
                        label = { Text(preset.label) },
                        modifier = Modifier.weight(1f),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf(
                    TargetSizePreset.UNDER_1_MB,
                    TargetSizePreset.CUSTOM
                ).forEach { preset ->
                    FilterChip(
                        selected = uiState.targetSizePreset == preset,
                        onClick = { viewModel.setTargetSizePreset(preset) },
                        label = { Text(preset.label) },
                        modifier = Modifier.weight(1f),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    )
                }
            }

            if (uiState.targetSizePreset == TargetSizePreset.CUSTOM) {
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = uiState.customTargetKb.toString(),
                    onValueChange = { str ->
                        val kb = str.toIntOrNull() ?: 100
                        viewModel.setCustomTargetKb(kb)
                    },
                    label = { Text("Custom Target Size (KB)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("custom_target_kb_input"),
                    singleLine = true
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Format Selection
            Text(
                text = "Encoding Format",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(com.example.model.ImageFormat.JPEG, com.example.model.ImageFormat.WEBP).forEach { fmt ->
                    FilterChip(
                        selected = uiState.outputFormat == fmt,
                        onClick = { viewModel.setOutputFormat(fmt) },
                        label = { Text(fmt.displayName) },
                        modifier = Modifier.weight(1f),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Info Card
            val origSize = uiState.imageDetails?.sizeBytes ?: 0L
            val targetBytes = if (uiState.targetSizePreset == TargetSizePreset.CUSTOM) {
                uiState.customTargetKb * 1024L
            } else {
                uiState.targetSizePreset.targetBytes
            }

            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Original Size",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = formatFileSize(origSize),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "Target Max Size",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = formatFileSize(targetBytes),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}

