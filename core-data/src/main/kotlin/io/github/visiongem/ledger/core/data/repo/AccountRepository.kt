package io.github.visiongem.ledger.core.data.repo

import io.github.visiongem.ledger.core.data.domain.Account
import kotlinx.coroutines.flow.Flow

interface AccountRepository {
    fun observeAll(): Flow<List<Account>>
    fun observeActive(): Flow<List<Account>>
    fun observeById(id: Long): Flow<Account?>
    suspend fun upsert(account: Account): Long
    suspend fun deleteById(id: Long)
}
