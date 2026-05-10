package io.github.visiongem.ledger.core.data.repo

import io.github.visiongem.ledger.core.data.domain.Account
import io.github.visiongem.ledger.core.data.local.dao.AccountDao
import io.github.visiongem.ledger.core.data.local.mapper.toDomain
import io.github.visiongem.ledger.core.data.local.mapper.toEntity
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class AccountRepositoryImpl @Inject constructor(
    private val dao: AccountDao,
) : AccountRepository {

    override fun observeAll(): Flow<List<Account>> =
        dao.observeAll().map { list -> list.map { it.toDomain() } }

    override fun observeActive(): Flow<List<Account>> =
        dao.observeActive().map { list -> list.map { it.toDomain() } }

    override fun observeById(id: Long): Flow<Account?> =
        dao.observeById(id).map { it?.toDomain() }

    override suspend fun upsert(account: Account): Long =
        dao.upsert(account.toEntity())

    override suspend fun deleteById(id: Long) {
        dao.deleteById(id)
    }
}
