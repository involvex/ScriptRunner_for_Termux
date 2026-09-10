package io.github.swiftstagrime.termuxrunner.domain.model

data class ScriptTemplate(
    val id: String,
    val name: String,
    val description: String,
    val content: String,
    val category: String,
)
