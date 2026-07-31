package com.nus.folio.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nus.folio.presentation.home.AddSourceDragHandle
import com.nus.folio.ui.theme.CormorantGaramond
import com.nus.folio.ui.theme.FolioAndroidTheme
import com.nus.folio.ui.theme.FolioSheetShape
import com.nus.folio.ui.theme.HomeCardBackground
import com.nus.folio.ui.theme.HomeChipBorder
import com.nus.folio.ui.theme.HomeSheetBackground
import com.nus.folio.ui.theme.HomeStatusFailedBackground
import com.nus.folio.ui.theme.HomeStatusFailedText
import com.nus.folio.ui.theme.HomeTextPrimary

private val ItemOptionsButtonShape = RoundedCornerShape(12.dp)

enum class ItemOptionStyle {
    Default,
    Destructive,
}

data class ItemOptionAction(
    val label: String,
    val style: ItemOptionStyle = ItemOptionStyle.Default,
    val onClick: () -> Unit,
)

@Composable
internal fun ItemOptionsBottomSheet(
    title: String,
    actions: List<ItemOptionAction>,
    onDismiss: () -> Unit,
) {
    AnimatedModalSheet(
        onDismiss = onDismiss,
        dismissOnScrimClick = true,
    ) { requestDismiss ->
        AddSourceDragHandle()
        ItemOptionsSheetContent(
            title = title,
            actions = actions.map { action ->
                action.copy(onClick = { requestDismiss(after = action.onClick) })
            },
        )
    }
}

@Composable
private fun ItemOptionsSheetContent(
    title: String,
    actions: List<ItemOptionAction>,
) {
    Column {
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = title,
            fontFamily = CormorantGaramond,
            fontSize = 28.sp,
            fontWeight = FontWeight.SemiBold,
            color = HomeTextPrimary,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(modifier = Modifier.height(24.dp))
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            actions.forEach { action ->
                ItemOptionButton(action = action)
            }
        }
    }
}

@Composable
private fun ItemOptionButton(action: ItemOptionAction) {
    val isDestructive = action.style == ItemOptionStyle.Destructive
    OutlinedButton(
        onClick = action.onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp),
        shape = ItemOptionsButtonShape,
        border = BorderStroke(
            1.dp,
            if (isDestructive) HomeStatusFailedText else HomeChipBorder,
        ),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = if (isDestructive) {
                HomeStatusFailedBackground
            } else {
                HomeCardBackground
            },
            contentColor = if (isDestructive) {
                HomeStatusFailedText
            } else {
                HomeTextPrimary
            },
        ),
    ) {
        Text(
            text = action.label,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Preview(showBackground = true, widthDp = 393, heightDp = 852, backgroundColor = 0xFFF7F1E6, name = "Options — space")
@Composable
private fun ItemOptionsSheetSpacePreview() {
    FolioAndroidTheme(dynamicColor = false) {
        ItemOptionsSheetPreviewScaffold(
            title = "Dissertation Research",
            actions = listOf(
                ItemOptionAction("Rename") {},
                ItemOptionAction("Delete space", ItemOptionStyle.Destructive) {},
            ),
        )
    }
}

@Preview(showBackground = true, widthDp = 393, heightDp = 852, backgroundColor = 0xFFF7F1E6, name = "Options — source")
@Composable
private fun ItemOptionsSheetSourcePreview() {
    FolioAndroidTheme(dynamicColor = false) {
        ItemOptionsSheetPreviewScaffold(
            title = "Alan Turing: Computing Machinery",
            actions = listOf(
                ItemOptionAction("Edit details") {},
                ItemOptionAction("Delete source", ItemOptionStyle.Destructive) {},
            ),
        )
    }
}

@Preview(showBackground = true, widthDp = 393, heightDp = 852, backgroundColor = 0xFFF7F1E6, name = "Options — note")
@Composable
private fun ItemOptionsSheetNotePreview() {
    FolioAndroidTheme(dynamicColor = false) {
        ItemOptionsSheetPreviewScaffold(
            title = "Research Question Draft",
            actions = listOf(
                ItemOptionAction("View") {},
                ItemOptionAction("Edit") {},
                ItemOptionAction("Convert to source") {},
                ItemOptionAction("Delete", ItemOptionStyle.Destructive) {},
            ),
        )
    }
}

@Composable
private fun ItemOptionsSheetPreviewScaffold(
    title: String,
    actions: List<ItemOptionAction>,
) {
    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(HomeSheetBackground, FolioSheetShape)
                .padding(horizontal = 24.dp)
                .padding(bottom = 20.dp),
        ) {
            AddSourceDragHandle()
            ItemOptionsSheetContent(
                title = title,
                actions = actions,
            )
        }
    }
}
