package com.nus.folio.presentation.home

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.nus.folio.domain.model.NoteFilter
import com.nus.folio.domain.model.NoteOrigin
import com.nus.folio.domain.model.SourceFilter
import com.nus.folio.domain.model.SourceType
import com.nus.folio.ui.theme.HomeTypeFileBackground
import com.nus.folio.ui.theme.HomeTypeFileText
import com.nus.folio.ui.theme.HomeTypeNoteBackground
import com.nus.folio.ui.theme.HomeTypeNoteSavedBackground
import com.nus.folio.ui.theme.HomeTypeNoteSavedText
import com.nus.folio.ui.theme.HomeTypeNoteText
import com.nus.folio.ui.theme.HomeTypeTextBackground
import com.nus.folio.ui.theme.HomeTypeTextText
import com.nus.folio.ui.theme.HomeTypeWebBackground
import com.nus.folio.ui.theme.HomeTypeWebText
import com.nus.folio.ui.theme.LoginCopper

internal val HomeCardShape = RoundedCornerShape(12.dp)
internal val HomeChipShape = RoundedCornerShape(50)
internal val HomeSourceFilterChipSelected = Color(0xFFE3EDF7)
internal val HomeSourceFilterChipSelectedBorder = Color(0xFFB8C9DC)
internal val HomeFilterActiveDot = Color(0xFFE24B4A)
internal val HomeBadgeShape = RoundedCornerShape(8.dp)
internal val HomeStatusShape = RoundedCornerShape(8.dp)
internal val HomeNavPillShape = RoundedCornerShape(14.dp)
internal val HomeBottomNavClearance = 72.dp
internal val HomeSheetShape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
internal val HomeSheetTabShape = RoundedCornerShape(12.dp)
internal val HomeUploadZoneShape = RoundedCornerShape(16.dp)
internal val HomeSheetInputBorder = LoginCopper
internal val HomeNavItemTint = Color(0xFFD8C7A2)
internal val HomeNavTopLine = Color(0xFF1F3D36)
internal val AskSuggestionShape = RoundedCornerShape(12.dp)
internal val AskInputShape = RoundedCornerShape(16.dp)
internal val AskSourceChipShape = RoundedCornerShape(12.dp)
internal val AskSubmitShape = RoundedCornerShape(10.dp)
internal val AskSparkleCircleShape = CircleShape
internal val AskButtonBackground = Color(0xFF2D3E4E)
internal val AskSourceChipBackground = Color(0xFFE8EEF2)
internal val AskFeedbackChoiceBackground = Color(0xFFFAF4EA)
internal val AskFeedbackChoiceSelectedBackground = Color(0xFFE2D8C6)
internal val AskFeedbackNoSelectedBackground = Color(0xFFF9E7E5)
internal val AskFeedbackNoSelectedText = Color(0xFFB53D35)
internal val AskSparkleBorder = Color(0xFFD9D9D9)
internal val AskInputMinHeight = 24.dp
internal val AskInputMaxHeight = 176.dp

internal data class SourceTypeBadgeColors(
    val background: Color,
    val content: Color,
)

internal fun sourceTypeBadgeColors(type: SourceType): SourceTypeBadgeColors = when (type) {
    SourceType.FILE, SourceType.BOOK -> SourceTypeBadgeColors(
        background = HomeTypeFileBackground,
        content = HomeTypeFileText,
    )
    SourceType.WEB -> SourceTypeBadgeColors(
        background = HomeTypeWebBackground,
        content = HomeTypeWebText,
    )
    SourceType.TEXT -> SourceTypeBadgeColors(
        background = HomeTypeTextBackground,
        content = HomeTypeTextText,
    )
}

internal fun sourceFilterBadgeColors(filter: SourceFilter): SourceTypeBadgeColors? = when (filter) {
    SourceFilter.ALL -> null
    SourceFilter.FILE -> sourceTypeBadgeColors(SourceType.FILE)
    SourceFilter.WEB -> sourceTypeBadgeColors(SourceType.WEB)
    SourceFilter.TEXT -> sourceTypeBadgeColors(SourceType.TEXT)
}

internal fun noteOriginBadgeColors(origin: NoteOrigin): SourceTypeBadgeColors = when (origin) {
    NoteOrigin.USER_CREATED -> SourceTypeBadgeColors(
        background = HomeTypeNoteBackground,
        content = HomeTypeNoteText,
    )
    NoteOrigin.SAVED_ANSWER -> SourceTypeBadgeColors(
        background = HomeTypeNoteSavedBackground,
        content = HomeTypeNoteSavedText,
    )
}

internal fun noteFilterBadgeColors(filter: NoteFilter): SourceTypeBadgeColors? = when (filter) {
    NoteFilter.ALL -> null
    NoteFilter.USER_CREATED -> noteOriginBadgeColors(NoteOrigin.USER_CREATED)
    NoteFilter.SAVED_ANSWER -> noteOriginBadgeColors(NoteOrigin.SAVED_ANSWER)
}

internal val NoteIconBadgeColors = SourceTypeBadgeColors(
    background = HomeTypeNoteBackground,
    content = HomeTypeNoteText,
)

internal val ConversationIconBadgeColors = SourceTypeBadgeColors(
    background = HomeTypeNoteSavedBackground,
    content = HomeTypeNoteSavedText,
)
