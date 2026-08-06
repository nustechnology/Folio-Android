package com.nus.folio.presentation.home.bottomsheet

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nus.folio.R
import com.nus.folio.domain.util.AddSourceInputRules
import com.nus.folio.presentation.home.HomeSheetInputBorder
import com.nus.folio.presentation.home.HomeUploadZoneShape
import com.nus.folio.ui.theme.HomeCardBackground
import com.nus.folio.ui.theme.HomeChipBorder
import com.nus.folio.ui.theme.HomeStatusFailedText
import com.nus.folio.ui.theme.HomeTextPrimary
import com.nus.folio.ui.theme.HomeTextSecondary
import com.nus.folio.ui.theme.LoginCopper
import java.text.NumberFormat
import java.util.Locale

internal val AddSourceContentHeight = 160.dp

@Composable
internal fun AddSourceWebFields(
    url: String,
    title: String,
    author: String,
    showUrlError: Boolean,
    onUrlChange: (String) -> Unit,
    onTitleChange: (String) -> Unit,
    onAuthorChange: (String) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            AddSourceLabeledField(
                label = stringResource(R.string.add_source_web_url_label),
                value = url,
                onValueChange = onUrlChange,
                placeholder = stringResource(R.string.add_source_web_url_placeholder),
            )
            if (showUrlError) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = stringResource(R.string.add_source_web_invalid_url),
                    fontSize = 12.sp,
                    color = HomeStatusFailedText,
                )
            }
        }
        AddSourceLabeledField(
            label = stringResource(R.string.add_source_web_title_label),
            value = title,
            onValueChange = onTitleChange,
            placeholder = stringResource(R.string.add_source_web_title_placeholder),
        )
        AddSourceLabeledField(
            label = stringResource(R.string.add_source_web_author_label),
            value = author,
            onValueChange = onAuthorChange,
            placeholder = stringResource(R.string.add_source_web_author_placeholder),
        )
    }
}

@Composable
internal fun AddSourceTextFields(
    title: String,
    author: String,
    content: String,
    showContentError: Boolean,
    contentError: AddSourceInputRules.ContentValidationError?,
    onTitleChange: (String) -> Unit,
    onAuthorChange: (String) -> Unit,
    onContentChange: (String) -> Unit,
) {
    val numberFormat = remember { NumberFormat.getIntegerInstance(Locale.getDefault()) }
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        AddSourceLabeledField(
            label = stringResource(R.string.add_source_text_title_label),
            value = title,
            onValueChange = onTitleChange,
            placeholder = stringResource(R.string.add_source_text_title_placeholder),
        )
        AddSourceLabeledField(
            label = stringResource(R.string.add_source_text_author_label),
            value = author,
            onValueChange = onAuthorChange,
            placeholder = stringResource(R.string.add_source_text_author_placeholder),
        )
        Column(modifier = Modifier.fillMaxWidth()) {
            AddSourceLabeledField(
                label = stringResource(R.string.add_source_text_content_label),
                value = content,
                onValueChange = onContentChange,
                placeholder = stringResource(R.string.add_source_text_content_placeholder),
                singleLine = false,
                fieldModifier = Modifier
                    .fillMaxWidth()
                    .height(AddSourceContentHeight),
                showResizeHint = true,
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = stringResource(
                    R.string.add_source_text_content_counter,
                    numberFormat.format(content.length),
                    numberFormat.format(AddSourceInputRules.MAX_CONTENT_LENGTH),
                ),
                fontSize = 12.sp,
                color = if (content.length > AddSourceInputRules.MAX_CONTENT_LENGTH) {
                    HomeStatusFailedText
                } else {
                    HomeTextSecondary
                },
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.End,
            )
            if (showContentError && contentError != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = when (contentError) {
                        AddSourceInputRules.ContentValidationError.TOO_SHORT ->
                            stringResource(R.string.add_source_text_content_too_short)
                        AddSourceInputRules.ContentValidationError.TOO_LONG ->
                            stringResource(R.string.add_source_text_content_too_long)
                    },
                    fontSize = 12.sp,
                    color = HomeStatusFailedText,
                )
            }
        }
    }
}

@Composable
internal fun AddSourceLabeledField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    singleLine: Boolean = true,
    fieldModifier: Modifier = Modifier.fillMaxWidth(),
    showResizeHint: Boolean = false,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            color = LoginCopper,
        )
        Spacer(modifier = Modifier.height(8.dp))
        AddSourceTextField(
            value = value,
            onValueChange = onValueChange,
            hint = placeholder,
            singleLine = singleLine,
            showResizeHint = showResizeHint,
            modifier = fieldModifier,
        )
    }
}

@Composable
internal fun AddSourceTextField(
    value: String,
    onValueChange: (String) -> Unit,
    hint: String,
    singleLine: Boolean,
    modifier: Modifier = Modifier,
    showResizeHint: Boolean = false,
) {
    val scrollState = rememberScrollState()
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(HomeUploadZoneShape)
            .border(1.dp, HomeSheetInputBorder, HomeUploadZoneShape)
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
                .fillMaxWidth()
                .then(
                    if (singleLine) {
                        Modifier
                    } else {
                        Modifier
                            .fillMaxSize()
                            .verticalScroll(scrollState)
                    },
                ),
        )
        if (showResizeHint) {
            AddSourceResizeHint(modifier = Modifier.align(Alignment.BottomEnd))
        }
    }
}

@Composable
internal fun AddSourceResizeHint(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(2.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            Box(
                modifier = Modifier
                    .width(8.dp)
                    .height(1.dp)
                    .background(HomeChipBorder),
            )
            Box(
                modifier = Modifier
                    .width(8.dp)
                    .height(1.dp)
                    .background(HomeChipBorder),
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            Box(
                modifier = Modifier
                    .width(8.dp)
                    .height(1.dp)
                    .background(HomeChipBorder),
            )
            Box(
                modifier = Modifier
                    .width(8.dp)
                    .height(1.dp)
                    .background(HomeChipBorder),
            )
        }
    }
}
