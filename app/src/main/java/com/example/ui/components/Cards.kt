package com.example.ui.components

import android.graphics.RectF
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PhotoSizeSelectActual
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.model.CropAspectRatio
import com.example.model.ImageDetails
import com.example.model.PdfDetails

@Composable
fun ToolActionCard(
    title: String,
    description: String,
    icon: ImageVector,
    iconBackgroundColor: Color,
    iconColor: Color = Color.White,
    tag: String = "",
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag(tag)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(iconBackgroundColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(26.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
        }
    }
}

@Composable
fun ImageDetailsCard(
    details: ImageDetails,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(details.uri)
                    .crossfade(true)
                    .build(),
                contentDescription = "Image preview",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(60.dp)
                    .clip(RoundedCornerShape(12.dp))
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = details.fileName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    DetailChip(label = details.formattedDimensions)
                    DetailChip(label = details.formattedSize)
                    DetailChip(label = details.aspectRatioString)
                }
            }
        }
    }
}

@Composable
fun PdfDetailsCard(
    details: PdfDetails,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Description,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = details.fileName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    DetailChip(label = "${details.pageCount} pages")
                    DetailChip(label = details.formattedSize)
                }
            }
        }
    }
}

@Composable
fun DetailChip(label: String) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun InteractiveCropOverlay(
    imageAspect: Float, // width / height of source image
    cropAspect: CropAspectRatio,
    cropRect: RectF, // 0f..1f normalized
    onCropRectChanged: (RectF) -> Unit,
    modifier: Modifier = Modifier
) {
    var internalRect by remember(cropAspect, imageAspect) {
        val initial = if (cropAspect.ratio != null) {
            val targetRatio = cropAspect.ratio!!
            if (targetRatio > imageAspect) {
                // Crop is wider than image: fit width, shrink height
                val h = imageAspect / targetRatio
                val top = (1f - h) / 2f
                RectF(0f, top, 1f, top + h)
            } else {
                // Crop is taller than image: fit height, shrink width
                val w = targetRatio / imageAspect
                val left = (1f - w) / 2f
                RectF(left, 0f, left + w, 1f)
            }
        } else {
            RectF(0.05f, 0.05f, 0.95f, 0.95f)
        }
        onCropRectChanged(initial)
        mutableStateOf(initial)
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(imageAspect.coerceIn(0.5f, 2.5f))
            .clip(RoundedCornerShape(12.dp))
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(cropAspect) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        val w = size.width.toFloat()
                        val h = size.height.toFloat()
                        if (w <= 0 || h <= 0) return@detectDragGestures

                        val dx = dragAmount.x / w
                        val dy = dragAmount.y / h

                        val currentW = internalRect.width()
                        val currentH = internalRect.height()

                        var newLeft = (internalRect.left + dx).coerceIn(0f, 1f - currentW)
                        var newTop = (internalRect.top + dy).coerceIn(0f, 1f - currentH)
                        var newRight = newLeft + currentW
                        var newBottom = newTop + currentH

                        val updated = RectF(newLeft, newTop, newRight, newBottom)
                        internalRect = updated
                        onCropRectChanged(updated)
                    }
                }
        ) {
            val w = size.width
            val h = size.height

            val leftPx = internalRect.left * w
            val topPx = internalRect.top * h
            val rightPx = internalRect.right * w
            val bottomPx = internalRect.bottom * h
            val rectW = rightPx - leftPx
            val rectH = bottomPx - topPx

            // Draw darkened semi-transparent dimming outside crop rect
            // Top dim
            drawRect(Color.Black.copy(alpha = 0.5f), Offset(0f, 0f), Size(w, topPx))
            // Bottom dim
            drawRect(Color.Black.copy(alpha = 0.5f), Offset(0f, bottomPx), Size(w, h - bottomPx))
            // Left dim
            drawRect(Color.Black.copy(alpha = 0.5f), Offset(0f, topPx), Size(leftPx, rectH))
            // Right dim
            drawRect(Color.Black.copy(alpha = 0.5f), Offset(rightPx, topPx), Size(w - rightPx, rectH))

            // White border around crop area
            drawRect(
                color = Color.White,
                topLeft = Offset(leftPx, topPx),
                size = Size(rectW, rectH),
                style = Stroke(width = 3.dp.toPx())
            )

            // Rule of thirds grid lines
            val thirdW = rectW / 3f
            val thirdH = rectH / 3f
            val gridColor = Color.White.copy(alpha = 0.6f)
            val stroke = Stroke(width = 1.dp.toPx())

            // Vertical thirds
            drawLine(gridColor, Offset(leftPx + thirdW, topPx), Offset(leftPx + thirdW, bottomPx), stroke.width)
            drawLine(gridColor, Offset(leftPx + 2 * thirdW, topPx), Offset(leftPx + 2 * thirdW, bottomPx), stroke.width)

            // Horizontal thirds
            drawLine(gridColor, Offset(leftPx, topPx + thirdH), Offset(rightPx, topPx + thirdH), stroke.width)
            drawLine(gridColor, Offset(leftPx, topPx + 2 * thirdH), Offset(rightPx, topPx + 2 * thirdH), stroke.width)
        }
    }
}
