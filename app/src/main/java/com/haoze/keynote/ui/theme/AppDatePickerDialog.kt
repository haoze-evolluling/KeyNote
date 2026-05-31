package com.haoze.keynote.ui.theme

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.window.DialogProperties

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppDatePickerDialog(
    onDismissRequest: () -> Unit,
    confirmButton: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    dismissButton: @Composable (() -> Unit)? = null,
    shape: Shape = DatePickerDefaults.shape,
    tonalElevation: Dp = DatePickerDefaults.TonalElevation,
    properties: DialogProperties = DialogProperties(),
    content: @Composable ColumnScope.() -> Unit
) {
    DatePickerDialog(
        onDismissRequest = onDismissRequest,
        confirmButton = confirmButton,
        modifier = modifier,
        dismissButton = dismissButton,
        shape = shape,
        tonalElevation = tonalElevation,
        properties = properties,
        content = content
    )
}
