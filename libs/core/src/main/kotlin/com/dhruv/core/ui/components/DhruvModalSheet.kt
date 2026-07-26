package com.dhruv.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.dhruv.core.ui.theme.LocalDhruvNextColors

/**
 * DhruvNext's bottom-sheet convention (§5): 42%-alpha scrim, 26dp top radius, 38×4 drag handle.
 * Backs `consent`/`shell`/`addtxn` (ADR-0024).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DhruvModalSheet(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    sheetState: SheetState = rememberModalBottomSheetState(),
    content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit,
) {
    val colors = LocalDhruvNextColors.current
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        sheetState = sheetState,
        containerColor = colors.surf,
        scrimColor = Color.Black.copy(alpha = SCRIM_ALPHA),
        shape = RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp),
        dragHandle = { DhruvDragHandle() },
        content = content,
    )
}

@Composable
private fun DhruvDragHandle() {
    val colors = LocalDhruvNextColors.current
    Box(
        modifier = Modifier.padding(vertical = 12.dp),
    ) {
        Box(
            modifier =
                Modifier
                    .size(width = 38.dp, height = 4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(colors.line),
        )
    }
}

private const val SCRIM_ALPHA = 0.42f
