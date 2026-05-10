package io.github.visiongem.ledger.core.data.repo

import io.github.visiongem.ledger.core.data.domain.Record
import io.github.visiongem.ledger.core.data.domain.RecordType
import io.github.visiongem.ledger.core.data.local.dao.RecordDao
import io.github.visiongem.ledger.core.data.local.mapper.toDomain
import io.github.visiongem.ledger.core.data.local.mapper.toEntity
import java.math.BigDecimal
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RecordRepositoryImpl @Inject constructor(
    private val dao: RecordDao,
) : RecordRepository {

    override fun observeById(id: Long): Flow<Record?> =
        dao.observeById(id).map { it?.toDomain() }

    override fun observeByAccount(accountId: Long): Flow<List<Record>> =
        dao.observeByAccount(accountId).map { list -> list.map { it.toDomain() } }

    override fun observeInRange(start: LocalDate, end: LocalDate): Flow<List<Record>> =
        dao.observeInRange(start, end).map { list -> list.map { it.toDomain() } }

    override fun observeByCategoryInRange(
        categoryId: Long,
        start: LocalDate,
        end: LocalDate,
    ): Flow<List<Record>> =
        dao.observeByCategoryInRange(categoryId, start, end)
            .map { list -> list.map { it.toDomain() } }

    override fun observeCategoryTotalsInRange(
        type: RecordType,
        start: LocalDate,
        end: LocalDate,
    ): Flow<Map<Long, BigDecimal>> =
        dao.observeInRange(start, end).map { list ->
            list.asSequence()
                .filter { it.type == type }
                .filter { it.categoryId != null }
                .groupBy { it.categoryId!! }
                .mapValues { (_, group) ->
                    group.fold(BigDecimal.ZERO) { acc, r -> acc.add(r.amount) }
                }
        }

    override suspend fun upsert(record: Record): Long = dao.upsert(record.toEntity())

    override suspend fun deleteById(id: Long) {
        dao.deleteById(id)
    }
}
