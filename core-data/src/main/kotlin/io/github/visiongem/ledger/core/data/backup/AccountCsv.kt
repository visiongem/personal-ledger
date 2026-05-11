package io.github.visiongem.ledger.core.data.backup

import io.github.visiongem.ledger.core.data.domain.Account
import java.math.BigDecimal
import java.time.Instant

object AccountCsv {

    const val HEADER = "id,name,currencyCode,openingBalance,archived,createdAt"

    fun toCsv(accounts: List<Account>): String = buildString {
        appendLine(HEADER)
        for (a in accounts) {
            append(a.id).append(',')
            append(CsvFormat.escape(a.name)).append(',')
            append(a.currencyCode).append(',')
            append(a.openingBalance.toPlainString()).append(',')
            append(if (a.archived) "1" else "0").append(',')
            append(a.createdAt.toString())
            append('\n')
        }
    }

    fun fromCsv(text: String): List<Account>? = try {
        val rows = CsvFormat.parseLines(text, HEADER) ?: return null
        rows.map { line ->
            val cols = CsvFormat.splitLine(line)
            require(cols.size >= 6) { "Wrong column count" }
            Account(
                id = cols[0].toLong(),
                name = cols[1],
                currencyCode = cols[2],
                openingBalance = BigDecimal(cols[3]),
                archived = cols[4] == "1",
                createdAt = Instant.parse(cols[5]),
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
