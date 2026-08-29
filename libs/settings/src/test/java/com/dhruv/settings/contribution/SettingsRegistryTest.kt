package com.dhruv.settings.contribution

import com.dhruv.core.flags.FeatureFlagResolver
import org.junit.Assert.assertEquals
import org.junit.Test

private class FakeResolver(
    private val enabled: Set<String>,
) : FeatureFlagResolver {
    override fun isEnabled(key: String): Boolean = key in enabled
}

private fun contribution(
    moduleKey: String,
    order: Int,
    title: Int,
) = SettingsContribution(moduleKey = moduleKey, title = title, summary = 0, order = order, groups = emptyList())

/** Deterministic fake — resource ids in tests carry no real string, so title text is derived from the id. */
private val fakeTitleOf: (Int) -> String = { id -> "title-$id" }

class SettingsRegistryTest {
    private val registry = SettingsRegistry()

    // SET-ARCH-001: every registered contribution is returned by type; the registry holds no
    // module names of its own — it only ever reflects what it was handed.
    @Test
    fun `resolve returns every enabled contribution it was given`() {
        val a = contribution("a", order = 0, title = 1)
        val b = contribution("b", order = 1, title = 2)
        val resolved = registry.resolve(listOf(a, b), FakeResolver(setOf("a", "b")), fakeTitleOf)
        assertEquals(listOf("a", "b"), resolved.map { it.moduleKey })
    }

    // SET-BR-001: a module whose flag is disabled is absent — not greyed out, not present.
    @Test
    fun `resolve drops a contribution whose moduleKey is disabled`() {
        val disabled = contribution("disabled", order = 0, title = 1)
        val enabled = contribution("enabled", order = 1, title = 2)
        val resolved = registry.resolve(listOf(disabled, enabled), FakeResolver(setOf("enabled")), fakeTitleOf)
        assertEquals(listOf("enabled"), resolved.map { it.moduleKey })
    }

    // SET-BR-002: matches how the resolver already gates routes on minVersion — this test only
    // proves the registry defers entirely to FeatureFlagResolver.isEnabled, which already encodes
    // the minVersion check (HardcodedFeatureFlagResolver); the registry has no separate version logic.
    @Test
    fun `resolve defers version-gating to the resolver, adding no logic of its own`() {
        val belowMinVersion = contribution("gated", order = 0, title = 1)
        val resolved = registry.resolve(listOf(belowMinVersion), FakeResolver(emptySet()), fakeTitleOf)
        assertEquals(emptyList<String>(), resolved.map { it.moduleKey })
    }

    // SET-BR-003: enabling a module makes its entry appear with no other change to Settings — the
    // registry's own resolve() output for a still-disabled set is simply a superset once the flag flips.
    @Test
    fun `a contribution appears once its moduleKey resolves enabled, nothing else changes`() {
        val a = contribution("a", order = 0, title = 1)
        val newlyEnabled = contribution("newlyEnabled", order = 1, title = 2)

        val before = registry.resolve(listOf(a, newlyEnabled), FakeResolver(setOf("a")), fakeTitleOf)
        val after = registry.resolve(listOf(a, newlyEnabled), FakeResolver(setOf("a", "newlyEnabled")), fakeTitleOf)

        assertEquals(listOf("a"), before.map { it.moduleKey })
        assertEquals(listOf("a", "newlyEnabled"), after.map { it.moduleKey })
    }

    // SET-BR-004: ordering is `order` then title, identical across two resolutions, regardless of
    // registration order.
    @Test
    fun `resolve orders by order then title, deterministically across repeated calls`() {
        val zTitleFirstOrder = contribution("z", order = 0, title = 99)
        val aTitleFirstOrder = contribution("a", order = 0, title = 1)
        val secondOrder = contribution("mid", order = 1, title = 50)
        val unsorted = listOf(secondOrder, zTitleFirstOrder, aTitleFirstOrder)
        val enabled = FakeResolver(setOf("z", "a", "mid"))

        val first = registry.resolve(unsorted, enabled, fakeTitleOf)
        val second = registry.resolve(unsorted.shuffled(), enabled, fakeTitleOf)

        // order 0: title "title-1" (a) sorts before "title-99" (z); order 1: "mid" last.
        assertEquals(listOf("a", "z", "mid"), first.map { it.moduleKey })
        assertEquals(first.map { it.moduleKey }, second.map { it.moduleKey })
    }

    // SET-ARCH-005: every registered contribution's moduleKey exists in the flag file.
    @Test
    fun `unknownModuleKeys reports a contribution whose moduleKey is not a real flag key`() {
        val real = contribution("calculator", order = 0, title = 1)
        val typo = contribution("calcuator", order = 1, title = 2)
        val result = registry.unknownModuleKeys(listOf(real, typo), knownFlagKeys = setOf("calculator", "currency"))
        assertEquals(listOf("calcuator"), result)
    }
}
