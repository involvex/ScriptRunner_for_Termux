package io.github.swiftstagrime.termuxrunner.data.template

import io.github.swiftstagrime.termuxrunner.domain.model.ScriptTemplate
import io.github.swiftstagrime.termuxrunner.domain.repository.ScriptTemplateRepository
import javax.inject.Inject

class ScriptTemplateRepositoryImpl
    @Inject
    constructor() : ScriptTemplateRepository {
        private val allTemplates = BUILTIN_TEMPLATES

        override suspend fun getTemplates(): List<ScriptTemplate> = allTemplates

        override suspend fun getTemplate(id: String): ScriptTemplate? = allTemplates.find { it.id == id }

        override suspend fun searchTemplates(query: String): List<ScriptTemplate> {
            if (query.isBlank()) return allTemplates
            val lowerQuery = query.lowercase()
            return allTemplates.filter {
                it.name.lowercase().contains(lowerQuery) ||
                    it.description.lowercase().contains(lowerQuery) ||
                    it.category.lowercase().contains(lowerQuery)
            }
        }
    }
