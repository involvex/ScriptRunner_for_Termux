package io.github.swiftstagrime.termuxrunner.domain.repository

import io.github.swiftstagrime.termuxrunner.domain.model.ScriptTemplate

interface ScriptTemplateRepository {
    suspend fun getTemplates(): List<ScriptTemplate>

    suspend fun getTemplate(id: String): ScriptTemplate?

    suspend fun searchTemplates(query: String): List<ScriptTemplate>
}
