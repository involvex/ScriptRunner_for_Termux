package io.github.swiftstagrime.termuxrunner.ui.features.executionhistory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.swiftstagrime.termuxrunner.domain.model.ScriptExecution
import io.github.swiftstagrime.termuxrunner.domain.repository.ScriptExecutionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ScriptOutputViewerViewModel
    @Inject
    constructor(
        private val scriptExecutionRepository: ScriptExecutionRepository,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow<ScriptOutputUiState>(ScriptOutputUiState.Loading)
        val uiState = _uiState.asStateFlow()

        fun load(executionId: Long) {
            viewModelScope.launch {
                val execution = scriptExecutionRepository.getExecutionById(executionId)
                if (execution != null) {
                    _uiState.value = ScriptOutputUiState.Success(execution)
                } else {
                    _uiState.value = ScriptOutputUiState.Error("Execution not found")
                }
            }
        }
    }

sealed interface ScriptOutputUiState {
    data object Loading : ScriptOutputUiState

    data class Success(
        val execution: ScriptExecution,
    ) : ScriptOutputUiState

    data class Error(
        val message: String,
    ) : ScriptOutputUiState
}
