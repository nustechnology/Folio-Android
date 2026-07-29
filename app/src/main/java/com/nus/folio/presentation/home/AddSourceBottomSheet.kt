package com.nus.folio.presentation.home

import android.util.Patterns
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nus.folio.R
import com.nus.folio.ui.theme.FolioAndroidTheme
import com.nus.folio.ui.theme.HomeCardBackground
import com.nus.folio.ui.theme.HomeChipBorder
import com.nus.folio.ui.theme.HomeHeader
import com.nus.folio.ui.theme.HomeSheetBackground
import com.nus.folio.ui.theme.HomeSheetHandle
import com.nus.folio.ui.theme.HomeTextPrimary
import com.nus.folio.ui.theme.HomeTextSecondary
import com.nus.folio.ui.theme.HomeUploadDash
import com.nus.folio.ui.theme.HomeStatusFailedText
import com.nus.folio.ui.theme.HomeUploadIcon
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val AddSourceContentHeight = 160.dp
private val AddSourceScrim = Color.Black.copy(alpha = 0.32f)
private val AddSourceButtonShape = RoundedCornerShape(12.dp)

enum class AddSourceTab {
    PDF,
    WEB,
    TEXT,
}

@Composable
internal fun AddSourceBottomSheet(
    onDismiss: () -> Unit,
    onSubmit: (AddSourceTab, Uri?, String) -> Unit = { _, _, _ -> },
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var selectedPdfUriString by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedPdfName by rememberSaveable { mutableStateOf<String?>(null) }
    val selectedPdfUri = selectedPdfUriString?.let(Uri::parse)

    val pdfPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        val uriString = uri.toString()
        selectedPdfUriString = uriString
        selectedPdfName = uri.lastPathSegment
        scope.launch {
            val resolvedName = withContext(Dispatchers.IO) {
                runCatching {
                    context.contentResolver.takePersistableUriPermission(
                        uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION,
                    )
                }
                uri.displayName(context) ?: uri.lastPathSegment
            }
            if (selectedPdfUriString == uriString) {
                selectedPdfName = resolvedName
            }
        }
    }

    // Let Compose own IME insets — avoid window resize + padding stacking.
    DisposableEffect(Unit) {
        val window = context.findActivityOrNull()?.window
            ?: return@DisposableEffect onDispose {}
        val previousSoftInputMode = window.attributes.softInputMode
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING)
        onDispose {
            window.setSoftInputMode(previousSoftInputMode)
        }
    }

    BackHandler(onBack = onDismiss)

    Box(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(AddSourceScrim)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {},
                ),
        )
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(HomeSheetBackground, HomeSheetShape)
                .windowInsetsPadding(WindowInsets.navigationBars.union(WindowInsets.ime))
                .padding(horizontal = 24.dp)
                .padding(bottom = 20.dp),
        ) {
            AddSourceDragHandle()
            AddSourceSheetContent(
                selectedPdfUri = selectedPdfUri,
                selectedPdfName = selectedPdfName,
                onCancelClick = onDismiss,
                onUploadPdfClick = {
                    pdfPicker.launch(arrayOf("application/pdf"))
                },
                onSubmit = { tab, textValue ->
                    onSubmit(tab, selectedPdfUri, textValue)
                },
            )
        }
    }
}

private fun Context.findActivityOrNull(): Activity? {
    var current: Context? = this
    while (current is ContextWrapper) {
        if (current is Activity) return current
        current = current.baseContext
    }
    return current as? Activity
}

@Composable
internal fun AddSourceSheetContent(
    onUploadPdfClick: () -> Unit,
    onSubmit: (AddSourceTab, String) -> Unit,
    onCancelClick: () -> Unit = {},
    modifier: Modifier = Modifier,
    initialTab: AddSourceTab = AddSourceTab.PDF,
    selectedPdfUri: Uri? = null,
    selectedPdfName: String? = null,
) {
    var selectedTab by rememberSaveable { mutableStateOf(initialTab) }
    var webUrl by rememberSaveable { mutableStateOf("") }
    var textContent by rememberSaveable { mutableStateOf("") }
    var webUrlTouched by rememberSaveable { mutableStateOf(false) }

    val webUrlValid = isValidHttpUrl(webUrl)
    val showWebError = selectedTab == AddSourceTab.WEB && webUrlTouched && webUrl.isNotBlank() && !webUrlValid

    val canSubmit = when (selectedTab) {
        AddSourceTab.PDF -> selectedPdfUri != null
        AddSourceTab.WEB -> webUrlValid
        AddSourceTab.TEXT -> textContent.isNotBlank()
    }

    Column(modifier = modifier) {
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.add_source_title),
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = HomeTextPrimary,
        )
        Spacer(modifier = Modifier.height(20.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            AddSourceTabChip(
                label = stringResource(R.string.add_source_tab_pdf),
                selected = selectedTab == AddSourceTab.PDF,
                onClick = { selectedTab = AddSourceTab.PDF },
                modifier = Modifier.weight(1f),
            )
            AddSourceTabChip(
                label = stringResource(R.string.add_source_tab_web),
                selected = selectedTab == AddSourceTab.WEB,
                onClick = { selectedTab = AddSourceTab.WEB },
                modifier = Modifier.weight(1f),
            )
            AddSourceTabChip(
                label = stringResource(R.string.add_source_tab_text),
                selected = selectedTab == AddSourceTab.TEXT,
                onClick = { selectedTab = AddSourceTab.TEXT },
                modifier = Modifier.weight(1f),
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(AddSourceContentHeight),
        ) {
            when (selectedTab) {
                AddSourceTab.PDF -> PdfUploadZone(
                    selectedFileName = selectedPdfName,
                    onClick = onUploadPdfClick,
                    modifier = Modifier.fillMaxSize(),
                )
                AddSourceTab.WEB, AddSourceTab.TEXT -> {
                    val isTextTab = selectedTab == AddSourceTab.TEXT
                    Column(modifier = Modifier.fillMaxSize()) {
                        AddSourceTextField(
                            value = if (isTextTab) textContent else webUrl,
                            onValueChange = { value ->
                                if (isTextTab) {
                                    textContent = value
                                } else {
                                    webUrl = value
                                    webUrlTouched = true
                                }
                            },
                            hint = stringResource(
                                if (isTextTab) {
                                    R.string.add_source_text_hint
                                } else {
                                    R.string.add_source_web_hint
                                },
                            ),
                            singleLine = !isTextTab,
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                        )
                        if (showWebError) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = stringResource(R.string.add_source_web_invalid_url),
                                fontSize = 12.sp,
                                color = HomeStatusFailedText,
                            )
                        }
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            AddSourceCancelButton(
                onClick = onCancelClick,
                modifier = Modifier.weight(1f),
            )
            AddSourceSubmitButton(
                enabled = canSubmit,
                onClick = {
                    val value = when (selectedTab) {
                        AddSourceTab.PDF -> selectedPdfName.orEmpty()
                        AddSourceTab.WEB -> webUrl.trim()
                        AddSourceTab.TEXT -> textContent.trim()
                    }
                    onSubmit(selectedTab, value)
                },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun AddSourceCancelButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.height(52.dp),
        shape = AddSourceButtonShape,
        border = BorderStroke(1.dp, HomeChipBorder),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = HomeCardBackground,
            contentColor = HomeTextPrimary,
        ),
    ) {
        Text(
            text = stringResource(R.string.add_source_cancel),
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
        )
    }
}

@Composable
private fun AddSourceSubmitButton(
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(52.dp),
        shape = AddSourceButtonShape,
        colors = ButtonDefaults.buttonColors(
            containerColor = HomeHeader,
            contentColor = Color.White,
            disabledContainerColor = HomeHeader.copy(alpha = 0.35f),
            disabledContentColor = Color.White.copy(alpha = 0.7f),
        ),
    ) {
        Text(
            text = stringResource(R.string.add_source_submit),
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
        )
    }
}

@Composable
private fun AddSourceDragHandle() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 10.dp, bottom = 4.dp),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .width(40.dp)
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(HomeSheetHandle),
        )
    }
}

@Composable
private fun AddSourceTabChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val background = if (selected) HomeHeader else HomeCardBackground
    val border = if (selected) Color.Transparent else HomeChipBorder
    val contentColor = if (selected) Color.White else HomeTextPrimary
    Box(
        modifier = modifier
            .clip(HomeSheetTabShape)
            .background(background)
            .border(1.dp, border, HomeSheetTabShape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            fontSize = 14.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            color = contentColor,
        )
    }
}

@Composable
private fun PdfUploadZone(
    selectedFileName: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val dashWidth = 1.5.dp
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(HomeUploadZoneShape)
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
                    color = HomeUploadDash,
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
        if (selectedFileName != null) {
            Text(
                text = stringResource(R.string.add_source_pdf_selected, selectedFileName),
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = HomeTextPrimary,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = stringResource(R.string.add_source_change_pdf),
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
                text = stringResource(R.string.add_source_upload_limit),
                fontSize = 13.sp,
                color = HomeTextSecondary,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun AddSourceTextField(
    value: String,
    onValueChange: (String) -> Unit,
    hint: String,
    singleLine: Boolean,
    modifier: Modifier = Modifier,
) {
    val scrollState = rememberScrollState()
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(HomeUploadZoneShape)
            .border(1.dp, HomeChipBorder, HomeUploadZoneShape)
            .background(HomeCardBackground)
            .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        if (value.isEmpty()) {
            Text(
                text = hint,
                color = HomeTextSecondary,
                fontSize = 15.sp,
            )
        }
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = singleLine,
            textStyle = TextStyle(color = HomeTextPrimary, fontSize = 15.sp),
            cursorBrush = SolidColor(HomeTextPrimary),
            modifier = Modifier
                .fillMaxSize()
                .then(
                    if (singleLine) {
                        Modifier
                    } else {
                        Modifier.verticalScroll(scrollState)
                    },
                ),
        )
    }
}

private fun isValidHttpUrl(input: String): Boolean {
    val trimmed = input.trim()
    if (trimmed.isEmpty()) return false
    val uri = Uri.parse(trimmed)
    val scheme = uri.scheme?.lowercase()
    return (scheme == "http" || scheme == "https") &&
        !uri.host.isNullOrBlank() &&
        Patterns.WEB_URL.matcher(trimmed).matches()
}

private fun Uri.displayName(context: android.content.Context): String? {
    val resolver = context.contentResolver
    return resolver.query(this, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
        ?.use { cursor ->
            val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (index >= 0 && cursor.moveToFirst()) cursor.getString(index) else null
        }
}

@Preview(showBackground = true, widthDp = 393, heightDp = 852, backgroundColor = 0xFFF7F1E6)
@Composable
private fun AddSourceSheetContentPreview() {
    FolioAndroidTheme(dynamicColor = false) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(HomeSheetBackground, HomeSheetShape)
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 20.dp),
            ) {
                AddSourceSheetContent(
                    onUploadPdfClick = {},
                    onSubmit = { _, _ -> },
                )
            }
        }
    }
}
