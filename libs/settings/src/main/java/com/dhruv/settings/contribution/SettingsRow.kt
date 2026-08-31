package com.dhruv.settings.contribution

import androidx.annotation.StringRes
import com.dhruv.core.navigation.NavTarget
import kotlinx.coroutines.flow.Flow

/**
 * The closed row vocabulary a [SettingsContribution] may use (contract §2). Every variant carries
 * [key] (the persisted preference key it reads/writes — append-only, constitution IX), [label] and
 * [description] (string resources, never literals — FR-043, design system §10), and [enabled].
 *
 * No Compose type may appear in this file (contract §5, `SET-ARCH-004`) — that is what keeps a
 * contribution testable as plain data and keeps every module's styling inside the shell's one
 * renderer instead of drifting per module.
 */
sealed interface SettingsRow {
    val key: String

    @get:StringRes
    val label: Int

    @get:StringRes
    val description: Int
    val enabled: Boolean

    /** On/off preference or alert toggle. Renders as a switch row. */
    data class Toggle(
        override val key: String,
        @StringRes override val label: Int,
        @StringRes override val description: Int,
        override val enabled: Boolean = true,
        val value: Flow<Boolean>,
        val onChange: suspend (Boolean) -> Unit,
    ) : SettingsRow

    /** Enumerated preference. Renders as a segmented row at <= 3 options, a selection sheet above that. */
    data class Choice(
        override val key: String,
        @StringRes override val label: Int,
        @StringRes override val description: Int,
        override val enabled: Boolean = true,
        val options: List<ChoiceOption>,
        val selected: Flow<String>,
        val onSelect: suspend (String) -> Unit,
    ) : SettingsRow

    /** Bounded numeric preference. Renders as a stepper row. */
    data class Stepper(
        override val key: String,
        @StringRes override val label: Int,
        @StringRes override val description: Int,
        override val enabled: Boolean = true,
        val value: Flow<Int>,
        val range: IntRange,
        val step: Int,
        val onChange: suspend (Int) -> Unit,
    ) : SettingsRow

    /** A tappable action (clear, reset, run-now). Destructive rows carry [confirm] and render distinctly. */
    data class Action(
        override val key: String,
        @StringRes override val label: Int,
        @StringRes override val description: Int,
        override val enabled: Boolean = true,
        val onInvoke: suspend () -> Result<Unit>,
        val confirm: ConfirmSpec? = null,
        val destructive: Boolean = false,
    ) : SettingsRow

    /**
     * The only escape hatch to bespoke UI (contract rule 8) — a module needing a custom control
     * points here at its own screen via the existing [NavTarget] vocabulary rather than modelling
     * a fake [Choice] or [Action]. Renders as a chevron row.
     */
    data class Navigate(
        override val key: String,
        @StringRes override val label: Int,
        @StringRes override val description: Int,
        override val enabled: Boolean = true,
        val target: NavTarget,
    ) : SettingsRow

    /** Read-only value (version, status, last-run). Renders as a value row, never interactive. */
    data class Info(
        override val key: String,
        @StringRes override val label: Int,
        @StringRes override val description: Int,
        override val enabled: Boolean = true,
        val value: Flow<String>,
    ) : SettingsRow

    /**
     * A user-supplied secret (FR-038, `SET-BR-012`) — never shown in full after entry. [value]
     * emits `null` when unset. Renders masked with a single-action remove when set; an editable
     * field with an explicit save when unset. This is the one row type FR-042's "no save action"
     * does not apply to: persisting an encrypted secret character-by-character on every keystroke
     * is a worse property than one explicit confirm, so entry is deliberately save-then-persist
     * rather than persist-immediately (data-model.md / spec.md Implementation record, T097 note).
     */
    data class SecretText(
        override val key: String,
        @StringRes override val label: Int,
        @StringRes override val description: Int,
        override val enabled: Boolean = true,
        val value: Flow<String?>,
        val onSave: suspend (String) -> Unit,
        val onRemove: suspend () -> Unit,
    ) : SettingsRow
}

/** One option of a [SettingsRow.Choice]. [id] is a persisted value and therefore append-only (contract rule 6). */
data class ChoiceOption(
    val id: String,
    @StringRes val label: Int,
)

/** Confirmation shown before a destructive [SettingsRow.Action] executes. */
data class ConfirmSpec(
    @StringRes val title: Int,
    @StringRes val body: Int,
    @StringRes val confirmLabel: Int,
    val typeToConfirm: Boolean = false,
)
