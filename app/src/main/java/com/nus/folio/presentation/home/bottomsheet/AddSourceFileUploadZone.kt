package com.nus.folio.presentation.home.bottomsheet

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.net.Uri
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.draganddrop.dragAndDropTarget
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draganddrop.DragAndDropEvent
import androidx.compose.ui.draganddrop.DragAndDropTarget
import androidx.compose.ui.draganddrop.mimeTypes
import androidx.compose.ui.draganddrop.toAndroidDragEvent
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nus.folio.R
import com.nus.folio.presentation.home.HomeSheetInputBorder
import com.nus.folio.presentation.home.HomeUploadZoneShape
import com.nus.folio.ui.theme.HomeTextPrimary
import com.nus.folio.ui.theme.HomeTextSecondary
import com.nus.folio.ui.theme.HomeUploadIcon
import java.text.NumberFormat
import java.util.Locale

internal fun Context.findActivityOrNull(): Activity? {
    var current: Context? = this
    while (current is ContextWrapper) {
        if (current is Activity) return current
        current = current.baseContext
    }
    return current as? Activity
}

@Composable
@OptIn(ExperimentalFoundationApi::class)
internal fun FileUploadZone(
    selectedFile: SelectedSourceFile?,
    onClick: () -> Unit,
    onFileDropped: (Uri) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val dropTarget = remember(onFileDropped, context) {
        object : DragAndDropTarget {
            override fun onDrop(event: DragAndDropEvent): Boolean {
                val androidEvent = event.toAndroidDragEvent()
                val clipData = androidEvent.clipData ?: return false
                // Cross-app drops need a temporary grant; persistable URI permission
                // cannot acquire this access.
                context.findActivityOrNull()?.requestDragAndDropPermissions(androidEvent)
                for (index in 0 until clipData.itemCount) {
                    val uri = clipData.getItemAt(index).uri
                    if (uri != null) {
                        onFileDropped(uri)
                        return true
                    }
                }
                return false
            }
        }
    }
    val dashWidth = 1.5.dp
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(HomeUploadZoneShape)
            .dragAndDropTarget(
                shouldStartDragAndDrop = { event ->
                    event.mimeTypes().any { mime ->
                        mime.startsWith("application/") ||
                            mime.startsWith("text/") ||
                            mime == "*/*"
                    }
                },
                target = dropTarget,
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .drawBehind {
                val stroke = Stroke(
                    width = dashWidth.toPx(),
                    pathEffect = PathEffect.dashPathEffect(
                        floatArrayOf(12f, 10f),
                        phase = 0f,
                    ),
                )
                drawRoundRect(
                    color = HomeSheetInputBorder,
                    style = stroke,
                    cornerRadius = CornerRadius(16.dp.toPx()),
                )
            }
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_document),
            contentDescription = null,
            tint = HomeUploadIcon,
            modifier = Modifier.size(28.dp),
        )
        Spacer(modifier = Modifier.height(16.dp))
        if (selectedFile != null) {
            Text(
                text = stringResource(R.string.add_source_file_selected, selectedFile.displayName),
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = HomeTextPrimary,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            val detailLine = buildFileMetaLine(selectedFile)
            if (detailLine.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = detailLine,
                    fontSize = 13.sp,
                    color = HomeTextSecondary,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            selectedFile.author?.takeIf { it.isNotBlank() }?.let { author ->
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.add_source_file_meta_author, author),
                    fontSize = 13.sp,
                    color = HomeTextSecondary,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = stringResource(R.string.add_source_change_file),
                fontSize = 13.sp,
                color = HomeTextSecondary,
                textAlign = TextAlign.Center,
            )
        } else {
            Text(
                text = stringResource(R.string.add_source_upload_hint),
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = HomeTextPrimary,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = stringResource(R.string.add_source_upload_types),
                fontSize = 13.sp,
                color = HomeTextSecondary,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.add_source_upload_limit),
                fontSize = 13.sp,
                color = HomeTextSecondary,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
internal fun buildFileMetaLine(selectedFile: SelectedSourceFile): String {
    val parts = mutableListOf<String>()
    selectedFile.sizeBytes?.let { parts += formatFileSize(it) }
    selectedFile.pageCount?.let { pages ->
        parts += pluralStringResource(R.plurals.add_source_file_meta_pages, pages, pages)
    }
    selectedFile.characterCount?.let { chars ->
        val formatted = NumberFormat.getIntegerInstance(Locale.getDefault()).format(chars)
        parts += pluralStringResource(R.plurals.add_source_file_meta_characters, chars, formatted)
    }
    return parts.joinToString(" · ")
}
