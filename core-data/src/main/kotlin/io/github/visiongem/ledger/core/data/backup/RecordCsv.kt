package io.github.visiongem.ledger.core.data.backup

import io.github.visiongem.ledger.core.data.domain.Record
import io.github.visiongem.ledger.core.data.domain.RecordType
import java.math.BigDecimal
import java.time.LocalDate

// RFC 4180-style CSV: quote fields containing commas, quotes, or newlines; escape
// embedded quotes by doubling them. Header is required on read.
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
            append(escape(r.note.orEmpty())).append(',')
            append(r.transferToAccountId?.toString().orEmpty()).append(',')
            append(r.transferAmount?.toPlainString().orEmpty())
            append('\n')
        }
    }

    // Returns null on parse failure (missing/wrong header, bad column count, malformed types).
    fun fromCsv(text: String): List<Record>? = try {
        parseLines(text)
    } catch (_: IllegalArgumentException) {
        null
    } catch (_: NumberFormatException) {
        null
    } catch (_: Exception) {
        null
    }

    private fun parseLines(text: String): List<Record>? {
        val lines = text.lines().filter { it.isNotBlank() }
        if (lines.isEmpty()) return emptyList()
        if (lines[0] != HEADER) return null
        return lines.drop(1).map { line ->
            val cols = splitLine(line)
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
    }

    private fun escape(value: String): String {
        if (value.isEmpty()) return ""
        val needsQuoting = value.contains(',') || value.contains('"') || value.contains('\n')
        if (!needsQuoting) return value
        return buildString {
            append('"')
            for (c in value) {
                if (c == '"') append('"')
                append(c)
            }
            append('"')
        }
    }

    // Splits a CSV line into unquoted column values. Quoted fields handle commas + escaped quotes.
    private fun splitLine(line: String): List<String> {
        val result = mutableListOf<String>()
        val sb = StringBuilder()
        var inQuotes = false
        var i = 0
        while (i < line.length) {
            val c = line[i]
            when {
                inQuotes -> {
                    if (c == '"') {
                        if (i + 1 < line.length && line[i + 1] == '"') {
                            sb.append('"')
                            i++
                        } else {
                            inQuotes = false
                        }
                    } else {
                        sb.append(c)
                    }
                }
                c == ',' -> {
                    result.add(sb.toString())
                    sb.clear()
                }
                c == '"' && sb.isEmpty() -> inQuotes = true
                else -> sb.append(c)
            }
            i++
        }
        result.add(sb.toString())
        return result
    }
}
