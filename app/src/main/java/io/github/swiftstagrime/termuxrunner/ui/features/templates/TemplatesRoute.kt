package io.github.swiftstagrime.termuxrunner.ui.features.templates

import androidx.compose.runtime.Composable
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel

@Composable
fun TemplatesRoute(
    onBack: () -> Unit,
    onTemplateSelected: (String) -> Unit = {},
) {
    val viewModel: TemplatesViewModel = hiltViewModel()

    TemplatesScreen(
        onBack = onBack,
        onTemplateSelected = onTemplateSelected,
        viewModel = viewModel,
    )
}
