package io.github.visiongem.ledger.core.data.backup

import io.github.visiongem.ledger.core.data.domain.Record
import io.github.visiongem.ledger.core.data.domain.RecordType
import java.math.BigDecimal
import java.time.LocalDate

object RecordCsv {

    const val HEADER = "id,occurredOn,accountId,categoryId,type,amount,note,transferToAccountId,transferAmount"

    fun toCsv(records: List<Record>): String = buildString {
        appendLine(HEADER)
        for (r in records) {
            append(r.id).append(',')
            append(r.occurredOn.toString()).append(',')
            append(r.accountId).append(',')
            append(r.categoryId?.toString().orEmpty()).append(',')
            append(r.type.name).append(',')
            append(r.amount.toPlainString()).append(',')
            append(CsvFormat.escape(r.note.orEmpty())).append(',')
            append(r.transferToAccountId?.toString().orEmpty()).append(',')
            append(r.transferAmount?.toPlainString().orEmpty())
            append('\n')
        }
    }

    fun fromCsv(text: String): List<Record>? = try {
        val rows = CsvFormat.parseLines(text, HEADER) ?: return null
        rows.map { line ->
            val cols = CsvFormat.splitLine(line)
            require(cols.size >= 9) { "Wrong column count" }
            Record(
                id = cols[0].toLong(),
                accountId = cols[2].toLong(),
                categoryId = cols[3].takeIf { it.isNotEmpty() }?.toLong(),
                type = RecordType.valueOf(cols[4]),
                amount = BigDecimal(cols[5]),
                occurredOn = LocalDate.parse(cols[1]),
                note = cols[6].takeIf { it.isNotEmpty() },
                transferToAccountId = cols[7].takeIf { it.isNotEmpty() }?.toLong(),
                transferAmount = cols[8].takeIf { it.isNotEmpty() }?.let { BigDecimal(it) },
            )
        }
    } catch (_: IllegalArgumentException) {
        null
    } catch (_: NumberFormatException) {
        null
    } catch (_: Exception) {
        null
    }
}
