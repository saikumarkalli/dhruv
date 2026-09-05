package com.dhruv.finance.money

import androidx.lifecycle.viewModelScope
import com.dhruv.core.observability.CrashReporter
import com.dhruv.core.observability.FeatureViewModel
import com.dhruv.core.observability.PerformanceTracer
import com.dhruv.finance.data.tracker.model.Category
import com.dhruv.finance.data.tracker.model.CategoryKind
import com.dhruv.finance.data.tracker.repo.CategoryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.YearMonth

/** One D8 row (spec.md Story 5, MNY-UI-007). [subtitle] already encodes the two special-case
 * renderings the task calls for — an excluded category's "Excluded from spend" label and the
 * reserved Uncategorised category's "N need a category" count — so the screen renders it as an
 * ordinary [com.dhruv.core.ui.components.ListGroupRow] subtitle with no further branching. */
data class CategoryRow(
    val id: String,
    val name: String,
    val kind: CategoryKind,
    val excludedFromSpend: Boolean,
    val spendPaise: Long?,
    /** Tenths-of-a-percent (425 = 42.5%) — Article VII/DAT-BR-008 forbids floating-point numeric
     * types anywhere under the tracker package. */
    val sharePercentTenths: Int?,
    val subtitle: String?,
    val isReservedUncategorised: Boolean,
    /** FR-026b — the two reserved categories (Uncategorised, Adjustment) are never deletable. */
    val isReserved: Boolean,
)

sealed interface CategoriesUiState {
    data object Loading : CategoriesUiState

    data class Loaded(
        val expenseRows: List<CategoryRow>,
        val incomeRows: List<CategoryRow>,
    ) : CategoriesUiState {
        val expenseCount: Int get() = expenseRows.size
        val incomeCount: Int get() = incomeRows.size
    }

    data class Error(val message: String) : CategoriesUiState
}

/** D8's merge confirmation (FR-024, MNY-BR-004). [Confirm] carries the exact, all-time counts
 * fetched *before* asking — [CategoriesScreen] must be able to state the real number that will
 * move, never a guess, so [CategoriesViewModel.requestMerge] resolves both counts first and only
 * then surfaces the prompt. */
sealed interface MergePrompt {
    data object None : MergePrompt

    data class Confirm(
        val sourceId: String,
        val sourceName: String,
        val sourceCount: Int,
        val targetId: String,
        val targetName: String,
        val targetCount: Int,
    ) : MergePrompt
}

/** D8's delete confirmation (FR-026b). A category with linked transactions is not directly
 * deletable — [Blocked] tells the user to merge it first (FR-024) rather than silently refusing. */
sealed interface DeletePrompt {
    data object None : DeletePrompt

    data class Confirm(val categoryId: String, val categoryName: String) : DeletePrompt

    data class Blocked(val categoryName: String, val transactionCount: Int) : DeletePrompt
}

/**
 * D8 (categories, US5) — Expense/Income separation with counts, per-row spend/share, safe
 * rename, excluded-from-spend toggling, and the irreversible merge flow (spec.md Story 5,
 * FR-022..FR-026). Seeds the two reserved categories (T062) on every load, matching
 * [CategoryRepository.ensureReservedCategories]'s own "idempotent, safe on every open" contract.
 */
class CategoriesViewModel(
    private val categoryRepository: CategoryRepository,
    crashReporter: CrashReporter,
    private val performanceTracer: PerformanceTracer,
) : FeatureViewModel(crashReporter, "money") {
    private val _uiState = MutableStateFlow<CategoriesUiState>(CategoriesUiState.Loading)
    val uiState: StateFlow<CategoriesUiState> = _uiState.asStateFlow()

    private val _mergePrompt = MutableStateFlow<MergePrompt>(MergePrompt.None)
    val mergePrompt: StateFlow<MergePrompt> = _mergePrompt.asStateFlow()

    private val _mergeError = MutableStateFlow<String?>(null)
    val mergeError: StateFlow<String?> = _mergeError.asStateFlow()

    private val _deletePrompt = MutableStateFlow<DeletePrompt>(DeletePrompt.None)
    val deletePrompt: StateFlow<DeletePrompt> = _deletePrompt.asStateFlow()

    init {
        load()
    }

    fun load() {
        performanceTracer.trace("money_categories_load") { _uiState.value = CategoriesUiState.Loading }
        viewModelScope.launch(exceptionHandler) {
            categoryRepository.ensureReservedCategories()
            categoryRepository
                .listCategoriesWithSpend(YearMonth.now())
                .onSuccess { categories -> _uiState.value = buildRows(categories) }
                .onFailure { error ->
                    _uiState.value = CategoriesUiState.Error(error.message ?: "Couldn't load categories")
                }
        }
    }

    fun refresh() = load()

    private suspend fun buildRows(categories: List<Category>): CategoriesUiState.Loaded {
        val uncategorised = categories.firstOrNull { it.name == Category.RESERVED_UNCATEGORISED }
        val uncategorisedCount =
            uncategorised?.let { categoryRepository.countTransactionsForCategory(it.id).getOrNull() }

        val rows =
            categories.map { category ->
                val isUncategorised = category.id == uncategorised?.id
                CategoryRow(
                    id = category.id,
                    name = category.name,
                    kind = category.kind,
                    excludedFromSpend = category.excludedFromSpend,
                    spendPaise = category.spendPaise,
                    sharePercentTenths = category.sharePercentTenths,
                    subtitle = subtitleFor(category, isUncategorised, uncategorisedCount),
                    isReservedUncategorised = isUncategorised,
                    isReserved =
                        category.name == Category.RESERVED_UNCATEGORISED ||
                            category.name == Category.RESERVED_ADJUSTMENT,
                )
            }
        return CategoriesUiState.Loaded(
            expenseRows = rows.filter { it.kind == CategoryKind.EXPENSE },
            incomeRows = rows.filter { it.kind == CategoryKind.INCOME },
        )
    }

    private fun subtitleFor(
        category: Category,
        isUncategorised: Boolean,
        uncategorisedCount: Int?,
    ): String? =
        when {
            category.excludedFromSpend -> "Excluded from spend"
            isUncategorised && uncategorisedCount != null -> "$uncategorisedCount need a category"
            else -> null
        }

    fun rename(
        categoryId: String,
        newName: String,
    ) {
        if (newName.isBlank()) return
        viewModelScope.launch(exceptionHandler) {
            categoryRepository.renameCategory(categoryId, newName).onSuccess { load() }
        }
    }

    fun setExcludedFromSpend(
        categoryId: String,
        excluded: Boolean,
    ) {
        viewModelScope.launch(exceptionHandler) {
            categoryRepository.setExcludedFromSpend(categoryId, excluded).onSuccess { load() }
        }
    }

    /** Resolves the exact, all-time transaction counts for both sides *before* prompting
     * (FR-024) — the dialog must never estimate. */
    fun requestMerge(
        sourceId: String,
        sourceName: String,
        targetId: String,
        targetName: String,
    ) {
        if (sourceId == targetId) return // Edge Cases: merging a category into itself is a no-op, not an action.
        _mergeError.value = null
        viewModelScope.launch(exceptionHandler) {
            val sourceCount = categoryRepository.countTransactionsForCategory(sourceId)
            val targetCount = categoryRepository.countTransactionsForCategory(targetId)
            if (sourceCount.isSuccess && targetCount.isSuccess) {
                _mergePrompt.value =
                    MergePrompt.Confirm(
                        sourceId = sourceId,
                        sourceName = sourceName,
                        sourceCount = sourceCount.getOrThrow(),
                        targetId = targetId,
                        targetName = targetName,
                        targetCount = targetCount.getOrThrow(),
                    )
            } else {
                _mergeError.value = "Couldn't check how many transactions would move. Try again."
            }
        }
    }

    fun confirmMerge() {
        val prompt = _mergePrompt.value as? MergePrompt.Confirm ?: return
        viewModelScope.launch(exceptionHandler) {
            categoryRepository
                .mergeCategories(prompt.sourceId, prompt.targetId)
                .onSuccess {
                    _mergePrompt.value = MergePrompt.None
                    load()
                }.onFailure { error ->
                    _mergeError.value = error.message ?: "Couldn't merge those categories. Try again."
                }
        }
    }

    fun dismissMergePrompt() {
        _mergePrompt.value = MergePrompt.None
    }

    /** FR-026a. */
    fun createCategory(
        name: String,
        kind: CategoryKind,
    ) {
        if (name.isBlank()) return
        viewModelScope.launch(exceptionHandler) {
            categoryRepository
                .createCategory(
                    Category(id = "", name = name, kind = kind, parentId = null, icon = null, excludedFromSpend = false),
                ).onSuccess { load() }
        }
    }

    /** FR-026b — resolves the exact transaction count first, same pattern [requestMerge] already
     * uses: a category with zero linked transactions confirms the delete; one or more routes to
     * [DeletePrompt.Blocked] naming merge (FR-024) as the way to empty it first. */
    fun requestDelete(
        categoryId: String,
        categoryName: String,
    ) {
        _mergeError.value = null
        viewModelScope.launch(exceptionHandler) {
            categoryRepository.countTransactionsForCategory(categoryId).onSuccess { count ->
                _deletePrompt.value =
                    if (count == 0) {
                        DeletePrompt.Confirm(categoryId, categoryName)
                    } else {
                        DeletePrompt.Blocked(categoryName, count)
                    }
            }.onFailure {
                _mergeError.value = "Couldn't check whether this category can be deleted. Try again."
            }
        }
    }

    fun confirmDelete() {
        val prompt = _deletePrompt.value as? DeletePrompt.Confirm ?: return
        viewModelScope.launch(exceptionHandler) {
            categoryRepository
                .softDeleteCategory(prompt.categoryId)
                .onSuccess {
                    _deletePrompt.value = DeletePrompt.None
                    load()
                }.onFailure { error ->
                    _mergeError.value = error.message ?: "Couldn't delete this category. Try again."
                }
        }
    }

    fun dismissDeletePrompt() {
        _deletePrompt.value = DeletePrompt.None
    }
}
