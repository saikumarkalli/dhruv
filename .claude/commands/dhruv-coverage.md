---
description: Improve JVM unit-test coverage for a Dhruv module and verify the measured % rose (JaCoCo).
argument-hint: "[module path or class — omit for the whole aggregated report]"
---

Use the **dhruv-coverage-booster** subagent to raise measured line coverage for `$ARGUMENTS`.

If `$ARGUMENTS` is empty, baseline the whole aggregated report (`./gradlew jacocoAggregatedReport`) and target the lowest-covered, most-testable classes first.

Follow the project conventions: JUnit4 + kotlinx-coroutines-test + Turbine + hand-written fakes (no MockK/Truth); no Compose-UI or Robolectric-SQLite/Room tests (they don't move the JVM number / fail on Windows). Report the before→after coverage %, the tests added, and the `globalLineFloor` ratchet — measured from the JaCoCo XML, never estimated.
