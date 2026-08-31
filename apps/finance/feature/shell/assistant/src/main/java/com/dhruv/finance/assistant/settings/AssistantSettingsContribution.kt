package com.dhruv.finance.assistant.settings

import com.dhruv.finance.assistant.R
import com.dhruv.settings.SettingsRepository
import com.dhruv.settings.contribution.SettingsContribution
import com.dhruv.settings.contribution.SettingsGroup
import com.dhruv.settings.contribution.SettingsRow
import kotlinx.coroutines.flow.map

private const val MODULE_KEY = "assistant"

/**
 * The assistant's Settings entry (T096): consent status + re-consent (FR-036/FR-037), and the
 * personal AI key (FR-038, `SET-BR-012`) via [SettingsRow.SecretText]. `consentGranted` is left at
 * its default (always granted) deliberately — this entry's own job **is** the consent control, so
 * gating it behind itself would be circular; FR-035's consent gate is for a module whose *other*
 * controls need a consent this entry doesn't own.
 */
fun assistantSettingsContribution(settingsRepository: SettingsRepository): SettingsContribution =
    SettingsContribution(
        moduleKey = MODULE_KEY,
        title = R.string.settings_assistant_title,
        summary = R.string.settings_assistant_summary,
        order = 2,
        // Optional (FR-032): the Ask detail route, not a tab's content — safe to hide.
        optional = true,
        groups =
            listOf(
                SettingsGroup(
                    label = null,
                    rows =
                        listOf(
                            SettingsRow.Toggle(
                                key = "assistant_consent_granted",
                                label = R.string.settings_assistant_consent_label,
                                description = R.string.settings_assistant_consent_description,
                                value = settingsRepository.observe().map { it.assistantConsentGranted },
                                onChange = { granted -> settingsRepository.update { copy(assistantConsentGranted = granted) } },
                            ),
                            SettingsRow.SecretText(
                                key = "gemini_api_key",
                                label = R.string.settings_assistant_key_label,
                                description = R.string.settings_assistant_key_description,
                                value = settingsRepository.observe().map { it.geminiApiKey },
                                onSave = { key -> settingsRepository.update { copy(geminiApiKey = key) } },
                                onRemove = { settingsRepository.clearGeminiKey() },
                            ),
                        ),
                ),
            ),
    )
