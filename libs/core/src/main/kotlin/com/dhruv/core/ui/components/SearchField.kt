package com.dhruv.core.ui.components

import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dhruv.core.ui.theme.LocalDhruvNextColors

/** DhruvNext §6.4/§6.7's search field — e.g. "Search 11 planners", calculator-history search. */
@Composable
fun SearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "Search",
) {
    val colors = LocalDhruvNextColors.current
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier.fillMaxWidth().defaultMinSize(minHeight = MIN_HIT_TARGET),
        placeholder = { Text(placeholder, color = colors.tx3) },
        leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = null, tint = colors.tx3) },
        singleLine = true,
        shape = RoundedCornerShape(14.dp),
        colors =
            OutlinedTextFieldDefaults.colors(
                focusedBorderColor = colors.acc,
                unfocusedBorderColor = colors.line,
                focusedTextColor = colors.tx,
                unfocusedTextColor = colors.tx,
            ),
    )
}

private val MIN_HIT_TARGET = 48.dp
