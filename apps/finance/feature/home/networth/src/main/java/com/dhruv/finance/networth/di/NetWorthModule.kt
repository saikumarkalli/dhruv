package com.dhruv.finance.networth.di

import org.koin.dsl.module

// ViewModel bindings land per-screen as Phase 2's user stories build C1-C7 (each gets its own
// viewModel { } entry here, same shape as LoansModule.kt). Stub kept non-empty-Koin-graph safe by
// registering nothing yet rather than a placeholder binding with no consumer.
val netWorthModule =
    module {
    }
