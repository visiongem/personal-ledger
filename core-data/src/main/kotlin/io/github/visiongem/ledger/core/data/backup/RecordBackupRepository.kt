package io.github.visiongem.ledger.core.data.backup

import io.github.visiongem.ledger.core.data.repo.RecordRepository
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.flow.first

interface RecordBackupRepository {
    suspend fun exportToCsv(): String
    suspend fun importFromCsv(text: String): Result<Int>
}

class RecordBackupRepositoryImpl @Inject constructor(
    private val recordRepository: RecordRepository,
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

    private companion object {
        val WIDE_START: LocalDate = LocalDate.of(1900, 1, 1)
        val WIDE_END: LocalDate = LocalDate.of(2200, 12, 31)
    }
}
