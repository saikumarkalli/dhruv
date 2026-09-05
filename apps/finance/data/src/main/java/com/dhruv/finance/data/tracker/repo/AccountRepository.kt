package com.dhruv.finance.data.tracker.repo

import com.dhruv.finance.data.tracker.mapper.toDomain
import com.dhruv.finance.data.tracker.mapper.toUpsertDto
import com.dhruv.finance.data.tracker.model.Account
import com.dhruv.finance.data.tracker.model.Category
import com.dhruv.finance.data.tracker.model.Transaction
import com.dhruv.finance.data.tracker.model.TransactionSource
import com.dhruv.finance.data.tracker.model.TransactionType
import com.dhruv.finance.data.tracker.net.MoneyApi
import com.dhruv.finance.data.tracker.net.SupabaseClientFactory
import kotlinx.coroutines.CancellationException
import java.time.Instant
import java.util.UUID
import kotlin.math.abs

/**
 * Money tab accounts (D6/D7, US3) — "spendable now" is read from `finance.v_account_balances`
 * (FR-017, NFR-8), never summed client-side.
 */
interface AccountRepository {
    suspend fun listAccounts(): Result<List<Account>>

    suspend fun createAccount(account: Account): Result<Account>

    suspend fun updateAccount(account: Account): Result<Account>

    suspend fun softDeleteAccount(accountId: String): Result<Unit>

    /**
     * FR-021/research R8: records the user-stated real balance, sets `reconciled_at` (clearing
     * the staleness flag, FR-020), and — when [statedBalancePaise] differs from the account's
     * currently computed balance (`finance.v_account_balances`) — appends a single adjustment
     * [Transaction] (`source = RECONCILE`, the reserved `Adjustment` category, itself
     * `excluded_from_spend`) for exactly that difference. Never edits `opening_balance_paise`
     * directly — the difference is always an explainable, auditable transaction, never a silent
     * overwrite (spec.md Story 3 Acceptance Scenario 5).
     */
    suspend fun reconcileAccount(
        accountId: String,
        statedBalancePaise: Long,
    ): Result<Unit>
}

class AccountRepositoryImpl(
    private val api: MoneyApi,
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository,
) : AccountRepository {
    constructor(
        supabaseClientFactory: SupabaseClientFactory,
        transactionRepository: TransactionRepository,
        categoryRepository: CategoryRepository,
    ) : this(
        supabaseClientFactory.dataRetrofit.create(MoneyApi::class.java),
        transactionRepository,
        categoryRepository,
    )

    @Suppress("TooGenericExceptionCaught")
    override suspend fun listAccounts(): Result<List<Account>> =
        try {
            val accounts = api.listAccounts()
            val balances = api.listAccountBalances().associateBy { it.accountId }
            Result.success(accounts.map { it.toDomain(balances[it.id]) })
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }

    @Suppress("TooGenericExceptionCaught")
    override suspend fun createAccount(account: Account): Result<Account> =
        try {
            val requestId = UUID.randomUUID().toString()
            val created = api.createAccount(account.toUpsertDto(requestId)).first()
            Result.success(created.toDomain())
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }

    @Suppress("TooGenericExceptionCaught")
    override suspend fun updateAccount(account: Account): Result<Account> =
        try {
            val updated = api.updateAccount("eq.${account.id}", account.toUpsertDto()).first()
            Result.success(updated.toDomain())
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }

    @Suppress("TooGenericExceptionCaught")
    override suspend fun softDeleteAccount(accountId: String): Result<Unit> =
        try {
            api.patchAccount("eq.$accountId", mapOf("deleted_at" to Instant.now().toString()))
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }

    @Suppress("TooGenericExceptionCaught", "ReturnCount")
    override suspend fun reconcileAccount(
        accountId: String,
        statedBalancePaise: Long,
    ): Result<Unit> =
        try {
            val currentBalancePaise =
                api.listAccountBalances().firstOrNull { it.accountId == accountId }?.balancePaise ?: 0L
            val diffPaise = statedBalancePaise - currentBalancePaise

            if (diffPaise != 0L) {
                categoryRepository.ensureReservedCategories()
                val adjustmentCategoryId =
                    categoryRepository
                        .listCategories()
                        .getOrElse { return Result.failure(it) }
                        .firstOrNull { it.name == Category.RESERVED_ADJUSTMENT }
                        ?.id
                        ?: return Result.failure(
                            IllegalStateException("Adjustment category is not seeded for this user"),
                        )

                val adjustment =
                    Transaction(
                        id = "",
                        type = if (diffPaise > 0) TransactionType.INCOME else TransactionType.EXPENSE,
                        amountPaise = abs(diffPaise),
                        accountId = accountId,
                        toAccountId = null,
                        categoryId = adjustmentCategoryId,
                        payee = null,
                        note = "Reconciliation adjustment",
                        occurredAt = Instant.now(),
                        cleared = true,
                        receiptPath = null,
                        goalId = null,
                        recurringId = null,
                        splitGroupId = null,
                        source = TransactionSource.RECONCILE,
                    )
                transactionRepository.createTransaction(adjustment).getOrElse { return Result.failure(it) }
            }

            api.patchAccount("eq.$accountId", mapOf("reconciled_at" to Instant.now().toString()))
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
}
