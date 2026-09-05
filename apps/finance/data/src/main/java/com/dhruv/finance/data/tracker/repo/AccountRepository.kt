package com.dhruv.finance.data.tracker.repo

import com.dhruv.finance.data.tracker.mapper.toDomain
import com.dhruv.finance.data.tracker.mapper.toUpsertDto
import com.dhruv.finance.data.tracker.model.Account
import com.dhruv.finance.data.tracker.net.MoneyApi
import com.dhruv.finance.data.tracker.net.SupabaseClientFactory
import kotlinx.coroutines.CancellationException
import java.time.Instant
import java.util.UUID

/**
 * Money tab accounts (D6/D7, US3) — "spendable now" is read from `finance.v_account_balances`
 * (FR-017, NFR-8), never summed client-side.
 */
interface AccountRepository {
    suspend fun listAccounts(): Result<List<Account>>

    suspend fun createAccount(account: Account): Result<Account>

    suspend fun updateAccount(account: Account): Result<Account>

    suspend fun softDeleteAccount(accountId: String): Result<Unit>

    /** FR-021: sets `reconciled_at`; any adjustment for a stated-balance difference is written by
     * the caller as a separate RECONCILE-source transaction (research R8), not by this call. */
    suspend fun markReconciled(accountId: String): Result<Unit>
}

class AccountRepositoryImpl(
    private val api: MoneyApi,
) : AccountRepository {
    constructor(supabaseClientFactory: SupabaseClientFactory) : this(
        supabaseClientFactory.dataRetrofit.create(MoneyApi::class.java),
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

    @Suppress("TooGenericExceptionCaught")
    override suspend fun markReconciled(accountId: String): Result<Unit> =
        try {
            api.patchAccount("eq.$accountId", mapOf("reconciled_at" to Instant.now().toString()))
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
}
