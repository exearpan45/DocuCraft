package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.BrandingWatermark
import androidx.compose.material.icons.filled.CallSplit
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Compress
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.MergeType
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Reorder
import androidx.compose.material.icons.filled.Rotate90DegreesCw
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.domain.PdfCompressionLevel
import com.example.model.OutputFileType
import com.example.model.PdfPageSize
import com.example.model.UiProcessingState
import com.example.model.formatFileSize
import com.example.ui.components.DetailChip
import com.example.ui.components.ErrorAlertDialog
import com.example.ui.components.PdfDetailsCard
import com.example.ui.components.ProcessingDialog
import com.example.ui.components.SuccessResultDialog
import com.example.viewmodel.PdfToolTab
import com.example.viewmodel.PdfToolsViewModel
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PdfToolsScreen(
    viewModel: PdfToolsViewModel,
    initialTab: String = "MERGE",
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // Initialize tab once from initialTab
    remember(initialTab) {
        val tab = when (initialTab) {
            "SPLIT" -> PdfToolTab.SPLIT
            "REORDER" -> PdfToolTab.REORDER
            "COMPRESS" -> PdfToolTab.COMPRESS
            "IMAGES_TO_PDF" -> PdfToolTab.IMAGES_TO_PDF
            else -> PdfToolTab.MERGE
        }
        viewModel.selectTab(tab)
        true
    }

    // PDF Pickers
    val singlePdfPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            viewModel.selectSinglePdf(uri)
        }
    }

    val multiPdfPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        if (uris.isNotEmpty()) {
            viewModel.addPdfsForMerge(uris)
        }
    }

    // Images for PDF picker
    val imagesPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia()
    ) { uris ->
        if (uris.isNotEmpty()) {
            viewModel.addImagesForPdf(uris)
        }
    }

    // SAF Destination Save
    var pendingSaveFile by remember { mutableStateOf<File?>(null) }
    val saveFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/pdf")
    ) { destUri ->
        if (destUri != null && pendingSaveFile != null) {
            viewModel.exportToDestination(pendingSaveFile!!, destUri)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("PDF Tools", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack, modifier = Modifier.testTag("pdf_tools_back_button")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.clearAll() }) {
                        Icon(Icons.Default.Clear, contentDescription = "Clear All")
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
            // Scrollable tabs
            ScrollableTabRow(
                selectedTabIndex = uiState.currentTab.ordinal,
                edgePadding = 12.dp,
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                PdfToolTab.entries.forEach { tab ->
                    Tab(
                        selected = uiState.currentTab == tab,
                        onClick = { viewModel.selectTab(tab) },
                        text = {
                            Text(
                                when (tab) {
                                    PdfToolTab.MERGE -> "Merge"
                                    PdfToolTab.SPLIT -> "Split"
                                    PdfToolTab.REORDER -> "Reorder"
                                    PdfToolTab.COMPRESS -> "Compress"
                                    PdfToolTab.IMAGES_TO_PDF -> "Images to PDF"
                                    PdfToolTab.ROTATE_PAGES -> "Rotate Pages"
                                    PdfToolTab.WATERMARK_PDF -> "Watermark"
                                }
                            )
                        },
                        icon = {
                            Icon(
                                imageVector = when (tab) {
                                    PdfToolTab.MERGE -> Icons.Default.MergeType
                                    PdfToolTab.SPLIT -> Icons.Default.CallSplit
                                    PdfToolTab.REORDER -> Icons.Default.Reorder
                                    PdfToolTab.COMPRESS -> Icons.Default.Compress
                                    PdfToolTab.IMAGES_TO_PDF -> Icons.Default.PhotoLibrary
                                    PdfToolTab.ROTATE_PAGES -> Icons.Default.Rotate90DegreesCw
                                    PdfToolTab.WATERMARK_PDF -> Icons.Default.BrandingWatermark
                                },
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        modifier = Modifier.testTag("tab_${tab.name.lowercase()}")
                    )
                }
            }

            // Tab Content
            when (uiState.currentTab) {
                PdfToolTab.MERGE -> {
                    MergePdfsTabContent(
                        viewModel = viewModel,
                        uiState = uiState,
                        onAddPdfsClick = { multiPdfPicker.launch(arrayOf("application/pdf")) }
                    )
                }
                PdfToolTab.SPLIT -> {
                    SplitPdfTabContent(
                        viewModel = viewModel,
                        uiState = uiState,
                        onSelectPdfClick = { singlePdfPicker.launch(arrayOf("application/pdf")) }
                    )
                }
                PdfToolTab.REORDER -> {
                    ReorderPdfTabContent(
                        viewModel = viewModel,
                        uiState = uiState,
                        onSelectPdfClick = { singlePdfPicker.launch(arrayOf("application/pdf")) }
                    )
                }
                PdfToolTab.COMPRESS -> {
                    CompressPdfTabContent(
                        viewModel = viewModel,
                        uiState = uiState,
                        onSelectPdfClick = { singlePdfPicker.launch(arrayOf("application/pdf")) }
                    )
                }
                PdfToolTab.IMAGES_TO_PDF -> {
                    ImagesToPdfTabContent(
                        viewModel = viewModel,
                        uiState = uiState,
                        onAddImagesClick = {
                            imagesPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        }
                    )
                }
                PdfToolTab.ROTATE_PAGES -> {
                    RotatePdfTabContent(
                        viewModel = viewModel,
                        uiState = uiState,
                        onSelectPdfClick = { singlePdfPicker.launch(arrayOf("application/pdf")) }
                    )
                }
                PdfToolTab.WATERMARK_PDF -> {
                    WatermarkPdfTabContent(
                        viewModel = viewModel,
                        uiState = uiState,
                        onSelectPdfClick = { singlePdfPicker.launch(arrayOf("application/pdf")) }
                    )
                }
            }
        }
    }

    // Processing Dialogs
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
                    val intent = viewModel.storageManager.createShareIntent(file, "application/pdf")
                    context.startActivity(Intent.createChooser(intent, "Share PDF"))
                },
                onOpenFile = { file ->
                    try {
                        val intent = viewModel.storageManager.createViewIntent(file, "application/pdf")
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

// ---------------- TAB 1: MERGE ----------------

@Composable
fun MergePdfsTabContent(
    viewModel: PdfToolsViewModel,
    uiState: com.example.viewmodel.PdfToolsUiState,
    onAddPdfsClick: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Merge PDF Files",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Select 2 or more PDF documents to merge into one organized PDF file. Adjust ordering using the up/down controls.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = onAddPdfsClick,
                        modifier = Modifier.fillMaxWidth().testTag("add_pdfs_merge_button"),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Add PDF Files")
                    }
                }
            }
        }

        if (uiState.isInspectingMergeFile) {
            item {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(modifier = Modifier.size(32.dp))
                }
            }
        }

        // List of files to merge
        itemsIndexed(uiState.mergePdfs) { index, item ->
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${index + 1}",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = item.details.fileName,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "${item.details.pageCount} pages",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "·",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = item.details.formattedSize,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    IconButton(
                        onClick = { viewModel.moveMergeItemUp(index) },
                        enabled = index > 0,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Default.ArrowUpward, contentDescription = "Move Up", modifier = Modifier.size(18.dp))
                    }

                    IconButton(
                        onClick = { viewModel.moveMergeItemDown(index) },
                        enabled = index < uiState.mergePdfs.size - 1,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Default.ArrowDownward, contentDescription = "Move Down", modifier = Modifier.size(18.dp))
                    }

                    IconButton(
                        onClick = { viewModel.removeMergeItem(index) },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "Remove", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }

        if (uiState.mergePdfs.size >= 2) {
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = { viewModel.executeMerge() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("execute_merge_button"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.MergeType, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Merge ${uiState.mergePdfs.size} PDFs (${uiState.mergePdfs.sumOf { it.details.pageCount }} pages)",
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

// ---------------- TAB 2: SPLIT ----------------

@Composable
fun SplitPdfTabContent(
    viewModel: PdfToolsViewModel,
    uiState: com.example.viewmodel.PdfToolsUiState,
    onSelectPdfClick: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        if (uiState.selectedPdfUri == null) {
            item {
                SelectPdfPromptCard(
                    title = "Choose PDF to Split",
                    description = "Extract individual pages or page ranges (e.g. 1-3, 5) into a new document.",
                    onSelectClick = onSelectPdfClick
                )
            }
        } else {
            item {
                if (uiState.pdfDetails != null) {
                    PdfDetailsCard(details = uiState.pdfDetails!!)
                }
            }

            item {
                // Range input
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Page Selection",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Enter comma-separated page ranges or check/uncheck pages below.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = uiState.splitRangeText,
                                onValueChange = { viewModel.setSplitRangeText(it) },
                                label = { Text("e.g. 1-3, 5") },
                                modifier = Modifier.weight(1f).testTag("split_range_input"),
                                singleLine = true
                            )
                            Button(
                                onClick = { viewModel.applyRangeSelection(uiState.splitRangeText) },
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("Apply")
                            }
                        }
                    }
                }
            }

            if (uiState.isLoadingPages) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
            } else if (uiState.pdfPages.isNotEmpty()) {
                item {
                    Text(
                        text = "Pages Preview (${uiState.pdfPages.count { it.isSelected }} of ${uiState.pdfPages.size} selected)",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Grid of pages
                item {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 100.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(300.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        itemsIndexed(uiState.pdfPages) { _, page ->
                            PageThumbnailCard(
                                page = page,
                                isSelectable = true,
                                onToggle = { viewModel.togglePageSelection(page.originalPageIndex) }
                            )
                        }
                    }
                }
            }

            item {
                val selectedCount = uiState.pdfPages.count { it.isSelected }
                Button(
                    onClick = { viewModel.executeSplit() },
                    enabled = selectedCount > 0,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("execute_split_button"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.CallSplit, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Extract $selectedCount Pages", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// ---------------- TAB 3: REORDER & REMOVE ----------------

@Composable
fun ReorderPdfTabContent(
    viewModel: PdfToolsViewModel,
    uiState: com.example.viewmodel.PdfToolsUiState,
    onSelectPdfClick: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        if (uiState.selectedPdfUri == null) {
            item {
                SelectPdfPromptCard(
                    title = "Choose PDF to Reorder or Trim",
                    description = "Rearrange the order of pages or remove pages from the document.",
                    onSelectClick = onSelectPdfClick
                )
            }
        } else {
            item {
                if (uiState.pdfDetails != null) {
                    PdfDetailsCard(details = uiState.pdfDetails!!)
                }
            }

            if (uiState.isLoadingPages) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
            } else {
                item {
                    Text(
                        text = "Current Pages (${uiState.pdfPages.size} pages remaining)",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }

                itemsIndexed(uiState.pdfPages) { index, page ->
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (page.thumbnail != null) {
                                Image(
                                    bitmap = page.thumbnail.asImageBitmap(),
                                    contentDescription = "Page thumbnail",
                                    modifier = Modifier
                                        .size(48.dp, 64.dp)
                                        .clip(RoundedCornerShape(6.dp)),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .size(48.dp, 64.dp)
                                        .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(6.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Description, contentDescription = null)
                                }
                            }

                            Spacer(modifier = Modifier.width(16.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Position: Page ${index + 1}",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Original Page: #${page.displayPageNumber}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            IconButton(
                                onClick = { viewModel.movePageLeft(index) },
                                enabled = index > 0,
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(Icons.Default.ArrowUpward, contentDescription = "Move earlier", modifier = Modifier.size(18.dp))
                            }

                            IconButton(
                                onClick = { viewModel.movePageRight(index) },
                                enabled = index < uiState.pdfPages.size - 1,
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(Icons.Default.ArrowDownward, contentDescription = "Move later", modifier = Modifier.size(18.dp))
                            }

                            IconButton(
                                onClick = { viewModel.deletePage(index) },
                                enabled = uiState.pdfPages.size > 1,
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete Page", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = { viewModel.executeReorderAndSave() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("execute_reorder_button"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Save Reordered PDF (${uiState.pdfPages.size} pages)", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// ---------------- TAB 4: COMPRESS ----------------

@Composable
fun CompressPdfTabContent(
    viewModel: PdfToolsViewModel,
    uiState: com.example.viewmodel.PdfToolsUiState,
    onSelectPdfClick: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        if (uiState.selectedPdfUri == null) {
            item {
                SelectPdfPromptCard(
                    title = "Choose PDF to Compress",
                    description = "Optimize and shrink large scanned or graphic-dense PDF files on your device.",
                    onSelectClick = onSelectPdfClick
                )
            }
        } else {
            item {
                if (uiState.pdfDetails != null) {
                    PdfDetailsCard(details = uiState.pdfDetails!!)
                }
            }

            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Compression Profile",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        PdfCompressionLevel.entries.forEach { level ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable { viewModel.setCompressionLevel(level) }
                                    .padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = uiState.compressionLevel == level,
                                    onClick = { viewModel.setCompressionLevel(level) }
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = level.title,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = level.description,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item {
                Button(
                    onClick = { viewModel.executeCompressPdf() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("execute_compress_pdf_button"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Compress, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Compress PDF Document", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// ---------------- TAB 5: IMAGES TO PDF ----------------

@Composable
fun ImagesToPdfTabContent(
    viewModel: PdfToolsViewModel,
    uiState: com.example.viewmodel.PdfToolsUiState,
    onAddImagesClick: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Convert Images to PDF",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Compile photos, scans, or receipts into a multipage PDF document.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = onAddImagesClick,
                        modifier = Modifier.fillMaxWidth().testTag("add_images_to_pdf_button"),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Select Images (${uiState.selectedImageUris.size} selected)")
                    }
                }
            }
        }

        // Layout options
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Page Format",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        PdfPageSize.entries.forEach { size ->
                            FilterChip(
                                selected = uiState.imagesToPdfPageSize == size,
                                onClick = { viewModel.setImagesToPdfPageSize(size) },
                                label = { Text(size.displayName.take(12)) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }

        // Selected images list
        itemsIndexed(uiState.selectedImageUris) { index, uri ->
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(uri)
                            .crossfade(true)
                            .build(),
                        contentDescription = "Image thumbnail",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(52.dp)
                            .clip(RoundedCornerShape(8.dp))
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    Text(
                        text = "Page ${index + 1}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f)
                    )

                    IconButton(
                        onClick = { viewModel.moveImageUp(index) },
                        enabled = index > 0,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Default.ArrowUpward, contentDescription = "Move Up", modifier = Modifier.size(16.dp))
                    }

                    IconButton(
                        onClick = { viewModel.moveImageDown(index) },
                        enabled = index < uiState.selectedImageUris.size - 1,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Default.ArrowDownward, contentDescription = "Move Down", modifier = Modifier.size(16.dp))
                    }

                    IconButton(
                        onClick = { viewModel.removeImageFromPdf(index) },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "Remove", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }

        if (uiState.selectedImageUris.isNotEmpty()) {
            item {
                Button(
                    onClick = { viewModel.executeConvertImagesToPdf() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("execute_images_to_pdf_button"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Generate PDF (${uiState.selectedImageUris.size} Pages)", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// ---------------- COMMON COMPONENTS ----------------

@Composable
fun SelectPdfPromptCard(
    title: String,
    description: String,
    onSelectClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Description,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(30.dp)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )

            Spacer(modifier = Modifier.height(18.dp))

            Button(
                onClick = onSelectClick,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.testTag("select_pdf_button")
            ) {
                Text("Select PDF Document")
            }
        }
    }
}

@Composable
fun PageThumbnailCard(
    page: com.example.model.PdfPageItem,
    isSelectable: Boolean,
    onToggle: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (page.isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f) else MaterialTheme.colorScheme.surface
        ),
        border = androidx.compose.foundation.BorderStroke(
            width = if (page.isSelected) 2.dp else 1.dp,
            color = if (page.isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle() }
    ) {
        Column(
            modifier = Modifier.padding(6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(0.75f)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color.White),
                contentAlignment = Alignment.Center
            ) {
                if (page.thumbnail != null) {
                    Image(
                        bitmap = page.thumbnail.asImageBitmap(),
                        contentDescription = "Page ${page.displayPageNumber}",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                    )
                } else {
                    Text(
                        text = "${page.displayPageNumber}",
                        fontWeight = FontWeight.Bold,
                        color = Color.DarkGray
                    )
                }

                if (isSelectable) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(4.dp)
                    ) {
                        Checkbox(
                            checked = page.isSelected,
                            onCheckedChange = { onToggle() },
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Page ${page.displayPageNumber}",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

// ---------------- TAB 6: ROTATE PAGES ----------------

@Composable
fun RotatePdfTabContent(
    viewModel: PdfToolsViewModel,
    uiState: com.example.viewmodel.PdfToolsUiState,
    onSelectPdfClick: () -> Unit
) {
    var selectedDegrees by remember { androidx.compose.runtime.mutableIntStateOf(90) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        if (uiState.selectedPdfUri == null) {
            item {
                SelectPdfPromptCard(
                    title = "Choose PDF to Rotate",
                    description = "Fix upside-down or sideways pages by rotating pages 90°, 180°, or 270°.",
                    onSelectClick = onSelectPdfClick
                )
            }
        } else {
            item {
                if (uiState.pdfDetails != null) {
                    PdfDetailsCard(details = uiState.pdfDetails!!)
                }
            }

            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Rotation Angle",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Select orientation to apply clockwise across all pages.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(90 to "90° CW", 180 to "180° Flip", 270 to "270° (90° CCW)").forEach { (deg, label) ->
                                FilterChip(
                                    selected = selectedDegrees == deg,
                                    onClick = { selectedDegrees = deg },
                                    label = { Text(label) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }

            item {
                Button(
                    onClick = { viewModel.executeRotatePdf(selectedDegrees) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("execute_rotate_pdf_button"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Rotate90DegreesCw, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Rotate Document by $selectedDegrees°", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// ---------------- TAB 7: WATERMARK PDF ----------------

@Composable
fun WatermarkPdfTabContent(
    viewModel: PdfToolsViewModel,
    uiState: com.example.viewmodel.PdfToolsUiState,
    onSelectPdfClick: () -> Unit
) {
    var watermarkText by remember { mutableStateOf("CONFIDENTIAL") }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        if (uiState.selectedPdfUri == null) {
            item {
                SelectPdfPromptCard(
                    title = "Choose PDF to Watermark",
                    description = "Stamp custom security, privacy, or branding watermarks across every page of your PDF document.",
                    onSelectClick = onSelectPdfClick
                )
            }
        } else {
            item {
                if (uiState.pdfDetails != null) {
                    PdfDetailsCard(details = uiState.pdfDetails!!)
                }
            }

            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Watermark Text",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf("CONFIDENTIAL", "DRAFT", "COPY", "ORIGINAL").forEach { preset ->
                                FilterChip(
                                    selected = watermarkText == preset,
                                    onClick = { watermarkText = preset },
                                    label = { Text(preset) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = watermarkText,
                            onValueChange = { watermarkText = it },
                            label = { Text("Custom Watermark Text") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("watermark_text_input"),
                            singleLine = true
                        )
                    }
                }
            }

            item {
                Button(
                    onClick = { viewModel.executeWatermarkPdf(watermarkText) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("execute_watermark_pdf_button"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.BrandingWatermark, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Apply Watermark & Save PDF", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

