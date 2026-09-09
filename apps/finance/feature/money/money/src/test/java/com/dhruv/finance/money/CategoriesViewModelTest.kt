package com.dhruv.finance.money

import com.dhruv.core.observability.NoOpCrashReporter
import com.dhruv.core.observability.NoOpPerformanceTracer
import com.dhruv.finance.data.tracker.model.Category
import com.dhruv.finance.data.tracker.model.CategoryKind
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/** T057 / MNY-UI-007: Expense/Income tab counts, per-row spend and share, the excluded-category
 * label, and the Uncategorised row's "N need a category" count. */
@OptIn(ExperimentalCoroutinesApi::class)
class CategoriesViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    private val groceries =
        Category(
            id = "cat-groceries",
            name = "Groceries",
            kind = CategoryKind.EXPENSE,
            parentId = null,
            icon = null,
            excludedFromSpend = false,
            spendPaise = 340_00,
            sharePercentTenths = 425,
        )
    private val investment =
        Category(
            id = "cat-investment",
            name = "Investment",
            kind = CategoryKind.EXPENSE,
            parentId = null,
            icon = null,
            excludedFromSpend = true,
            spendPaise = 500_00,
            sharePercentTenths = 0,
        )
    private val uncategorised =
        Category(
            id = "cat-uncategorised",
            name = Category.RESERVED_UNCATEGORISED,
            kind = CategoryKind.EXPENSE,
            parentId = null,
            icon = null,
            excludedFromSpend = false,
        )
    private val salary =
        Category(
            id = "cat-salary",
            name = "Salary",
            kind = CategoryKind.INCOME,
            parentId = null,
            icon = null,
            excludedFromSpend = false,
            spendPaise = 1_000_00,
            sharePercentTenths = 1000,
        )

    private fun viewModel(categoryRepository: FakeCategoryRepository) =
        CategoriesViewModel(categoryRepository, NoOpCrashReporter, NoOpPerformanceTracer)

    @Test
    fun `load reports Expense and Income tab counts separately`() =
        runTest {
            val repo = FakeCategoryRepository(listOf(groceries, investment, uncategorised, salary))
            val vm = viewModel(repo)

            advanceUntilIdle()

            val state = vm.uiState.value as CategoriesUiState.Loaded
            assertEquals(3, state.expenseCount) // groceries, investment, uncategorised
            assertEquals(1, state.incomeCount) // salary
        }

    @Test
    fun `a normal category row exposes the server-computed spend and share unchanged`() =
        runTest {
            val repo = FakeCategoryRepository(listOf(groceries, salary))
            val vm = viewModel(repo)

            advanceUntilIdle()

            val state = vm.uiState.value as CategoriesUiState.Loaded
            val row = state.expenseRows.single { it.id == "cat-groceries" }
            assertEquals(340_00L, row.spendPaise)
            assertEquals(425, row.sharePercentTenths)
            assertNull(row.subtitle)
        }

    @Test
    fun `an excluded category's subtitle states it is excluded from spend`() =
        runTest {
            val repo = FakeCategoryRepository(listOf(investment))
            val vm = viewModel(repo)

            advanceUntilIdle()

            val state = vm.uiState.value as CategoriesUiState.Loaded
            val row = state.expenseRows.single { it.id == "cat-investment" }
            assertEquals("Excluded from spend", row.subtitle)
            assertTrue(row.excludedFromSpend)
        }

    @Test
    fun `the Uncategorised row states the exact count of transactions needing a category`() =
        runTest {
            val repo =
                FakeCategoryRepository(
                    categories = listOf(uncategorised, groceries),
                    transactionCounts = mapOf("cat-uncategorised" to 4),
                )
            val vm = viewModel(repo)

            advanceUntilIdle()

            val state = vm.uiState.value as CategoriesUiState.Loaded
            val row = state.expenseRows.single { it.isReservedUncategorised }
            assertEquals("4 need a category", row.subtitle)
        }

    @Test
    fun `load ensures the reserved categories exist before listing`() =
        runTest {
            val repo = FakeCategoryRepository(listOf(groceries))
            val vm = viewModel(repo)

            advanceUntilIdle()

            assertEquals(1, repo.ensureReservedCalls.size)
        }

    @Test
    fun `requestMerge resolves both categories' exact counts before prompting`() =
        runTest {
            val repo =
                FakeCategoryRepository(
                    categories = listOf(groceries, investment),
                    transactionCounts = mapOf("cat-groceries" to 12, "cat-investment" to 7),
                )
            val vm = viewModel(repo)
            advanceUntilIdle()

            vm.requestMerge("cat-groceries", "Groceries", "cat-investment", "Investment")
            advanceUntilIdle()

            val prompt = vm.mergePrompt.value as MergePrompt.Confirm
            assertEquals(12, prompt.sourceCount)
            assertEquals(7, prompt.targetCount)
        }

    @Test
    fun `confirmMerge calls the repository and reloads on success`() =
        runTest {
            val repo =
                FakeCategoryRepository(
                    categories = listOf(groceries, investment),
                    mergeResult = 19,
                )
            val vm = viewModel(repo)
            advanceUntilIdle()
            vm.requestMerge("cat-groceries", "Groceries", "cat-investment", "Investment")
            advanceUntilIdle()

            vm.confirmMerge()
            advanceUntilIdle()

            assertEquals(listOf("cat-groceries" to "cat-investment"), repo.mergeCalls)
            assertEquals(MergePrompt.None, vm.mergePrompt.value)
        }

    @Test
    fun `merging a category into itself is rejected without a repository call`() =
        runTest {
            val repo = FakeCategoryRepository(listOf(groceries))
            val vm = viewModel(repo)
            advanceUntilIdle()

            vm.requestMerge("cat-groceries", "Groceries", "cat-groceries", "Groceries")
            advanceUntilIdle()

            assertEquals(MergePrompt.None, vm.mergePrompt.value)
        }

    @Test
    fun `rename reloads the list on success`() =
        runTest {
            val repo = FakeCategoryRepository(listOf(groceries))
            val vm = viewModel(repo)
            advanceUntilIdle()

            vm.rename("cat-groceries", "Groceries & Household")
            advanceUntilIdle()

            val state = vm.uiState.value as CategoriesUiState.Loaded
            assertEquals("Groceries & Household", state.expenseRows.single().name)
        }

    // FR-026a
    @Test
    fun `createCategory adds the new category and reloads`() =
        runTest {
            val repo = FakeCategoryRepository(listOf(groceries))
            val vm = viewModel(repo)
            advanceUntilIdle()

            vm.createCategory("Subscriptions", CategoryKind.EXPENSE)
            advanceUntilIdle()

            val state = vm.uiState.value as CategoriesUiState.Loaded
            assertTrue(state.expenseRows.any { it.name == "Subscriptions" })
        }

    @Test
    fun `createCategory with a blank name is a no-op`() =
        runTest {
            val repo = FakeCategoryRepository(listOf(groceries))
            val vm = viewModel(repo)
            advanceUntilIdle()

            vm.createCategory("   ", CategoryKind.EXPENSE)
            advanceUntilIdle()

            val state = vm.uiState.value as CategoriesUiState.Loaded
            assertEquals(1, state.expenseRows.size)
        }

    // FR-026b: zero linked transactions confirms straight away.
    @Test
    fun `requestDelete confirms directly when the category has no transactions`() =
        runTest {
            val repo = FakeCategoryRepository(listOf(groceries), transactionCounts = mapOf("cat-groceries" to 0))
            val vm = viewModel(repo)
            advanceUntilIdle()

            vm.requestDelete("cat-groceries", "Groceries")
            advanceUntilIdle()

            assertEquals(DeletePrompt.Confirm("cat-groceries", "Groceries"), vm.deletePrompt.value)
        }

    // FR-026b: a category with linked transactions blocks with the exact count, naming merge.
    @Test
    fun `requestDelete blocks with the exact count when the category has transactions`() =
        runTest {
            val repo = FakeCategoryRepository(listOf(groceries), transactionCounts = mapOf("cat-groceries" to 3))
            val vm = viewModel(repo)
            advanceUntilIdle()

            vm.requestDelete("cat-groceries", "Groceries")
            advanceUntilIdle()

            assertEquals(DeletePrompt.Blocked("Groceries", 3), vm.deletePrompt.value)
        }

    @Test
    fun `confirmDelete calls the repository and reloads`() =
        runTest {
            val repo = FakeCategoryRepository(listOf(groceries), transactionCounts = mapOf("cat-groceries" to 0))
            val vm = viewModel(repo)
            advanceUntilIdle()

            vm.requestDelete("cat-groceries", "Groceries")
            advanceUntilIdle()
            vm.confirmDelete()
            advanceUntilIdle()

            assertEquals(listOf("cat-groceries"), repo.deletedIds)
            assertEquals(DeletePrompt.None, vm.deletePrompt.value)
            val state = vm.uiState.value as CategoriesUiState.Loaded
            assertTrue(state.expenseRows.none { it.id == "cat-groceries" })
        }

    @Test
    fun `the two reserved categories are never marked deletable`() =
        runTest {
            val repo = FakeCategoryRepository(listOf(groceries, uncategorised))
            val vm = viewModel(repo)
            advanceUntilIdle()

            val state = vm.uiState.value as CategoriesUiState.Loaded
            assertTrue(state.expenseRows.first { it.name == Category.RESERVED_UNCATEGORISED }.isReserved)
            assertTrue(
                state.expenseRows
                    .first { it.name == "Groceries" }
                    .isReserved
                    .not(),
            )
        }
}
