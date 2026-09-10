package io.github.swiftstagrime.termuxrunner.ui.features.templates
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.swiftstagrime.termuxrunner.di.IoDispatcher
import io.github.swiftstagrime.termuxrunner.domain.model.ScriptTemplate
import io.github.swiftstagrime.termuxrunner.domain.repository.ScriptTemplateRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TemplatesViewModel
    @Inject
    constructor(
        private val templateRepository: ScriptTemplateRepository,
        @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow<TemplatesUiState>(TemplatesUiState.Loading)
        val uiState = _uiState.asStateFlow()

        init {
            loadTemplates()
        }

        fun loadTemplates() {
            viewModelScope.launch(ioDispatcher) {
                _uiState.value = TemplatesUiState.Success(templateRepository.getTemplates())
            }
        }

        fun search(query: String) {
            if (query.isBlank()) {
                loadTemplates()
                return
            }
            viewModelScope.launch(ioDispatcher) {
                val results = templateRepository.searchTemplates(query)
                _uiState.value = TemplatesUiState.Success(results)
            }
        }
    }

sealed interface TemplatesUiState {
    data object Loading : TemplatesUiState

    data class Success(
        val templates: List<ScriptTemplate>,
        val searchQuery: String = "",
    ) : TemplatesUiState

    data class Error(
        val message: String,
    ) : TemplatesUiState
}
