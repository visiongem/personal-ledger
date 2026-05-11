package io.github.visiongem.ledger.core.data.backup

import io.github.visiongem.ledger.core.data.local.dao.AccountDao
import io.github.visiongem.ledger.core.data.local.dao.BudgetDao
import io.github.visiongem.ledger.core.data.local.dao.CategoryDao
import io.github.visiongem.ledger.core.data.local.dao.ExchangeRateDao
import io.github.visiongem.ledger.core.data.local.dao.RecordDao
import io.github.visiongem.ledger.core.data.local.mapper.toDomain
import io.github.visiongem.ledger.core.data.local.mapper.toEntity
import io.github.visiongem.ledger.core.data.repo.RecordRepository
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.flow.first

interface RecordBackupRepository {
    // Records-only CSV (legacy single-table format, kept for callers that want it)
    suspend fun exportToCsv(): String
    suspend fun importFromCsv(text: String): Result<Int>

    // Full-schema ZIP (preferred from v2 on)
    suspend fun exportZip(): ByteArray
    suspend fun importZip(bytes: ByteArray): Result<Int>
}

class RecordBackupRepositoryImpl @Inject constructor(
    private val recordRepository: RecordRepository,
    private val accountDao: AccountDao,
    private val categoryDao: CategoryDao,
    private val recordDao: RecordDao,
    private val budgetDao: BudgetDao,
    private val exchangeRateDao: ExchangeRateDao,
) : RecordBackupRepository {

    override suspend fun exportToCsv(): String {
        val all = recordRepository.observeInRange(WIDE_START, WIDE_END).first()
        return RecordCsv.toCsv(all)
    }

    override suspend fun importFromCsv(text: String): Result<Int> = runCatching {
        val parsed = RecordCsv.fromCsv(text) ?: error("Invalid CSV")
        for (record in parsed) {
            recordRepository.upsert(record)
        }
        parsed.size
    }

    override suspend fun exportZip(): ByteArray {
        val dump = LedgerBackup(
            accounts = accountDao.getAll().map { it.toDomain() },
            categories = categoryDao.getAll().map { it.toDomain() },
            records = recordDao.getAll().map { it.toDomain() },
            budgets = budgetDao.getAll().map { it.toDomain() },
            rates = exchangeRateDao.getAll().map { it.toDomain() },
        )
        val out = ByteArrayOutputStream()
        LedgerZipBackup.writeTo(out, dump)
        return out.toByteArray()
    }

    override suspend fun importZip(bytes: ByteArray): Result<Int> {
        val read = LedgerZipBackup.readFrom(ByteArrayInputStream(bytes))
        val dump = read.getOrElse { return Result.failure(it) }
        return runCatching {
            // FK order: independent tables first, then dependents.
            dump.accounts.forEach { accountDao.upsert(it.toEntity()) }
            dump.categories.forEach { categoryDao.upsert(it.toEntity()) }
            dump.records.forEach { recordDao.upsert(it.toEntity()) }
            dump.budgets.forEach { budgetDao.upsert(it.toEntity()) }
            exchangeRateDao.upsertAll(dump.rates.map { it.toEntity() })
            dump.totalCount
        }
    }

    private companion object {
        val WIDE_START: LocalDate = LocalDate.of(1900, 1, 1)
        val WIDE_END: LocalDate = LocalDate.of(2200, 12, 31)
    }
}
