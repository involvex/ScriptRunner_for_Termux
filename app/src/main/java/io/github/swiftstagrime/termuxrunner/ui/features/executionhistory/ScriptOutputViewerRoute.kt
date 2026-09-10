package io.github.swiftstagrime.termuxrunner.ui.features.executionhistory

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel

@Composable
fun ScriptOutputViewerRoute(
    onBack: () -> Unit,
    executionId: Long,
) {
    val viewModel: ScriptOutputViewerViewModel = hiltViewModel()

    LaunchedEffect(executionId) {
        viewModel.load(executionId)
    }

    ScriptOutputViewerScreen(
        onBack = onBack,
        executionId = executionId,
        viewModel = viewModel,
    )
}
