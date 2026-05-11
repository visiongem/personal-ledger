package io.github.visiongem.ledger.core.data.backup

import io.github.visiongem.ledger.core.data.domain.Budget
import java.math.BigDecimal
import java.time.YearMonth
import java.time.format.DateTimeFormatter

object BudgetCsv {

    const val HEADER = "id,categoryId,month,limit,currencyCode"
    private val MONTH_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM")

    fun toCsv(budgets: List<Budget>): String = buildString {
        appendLine(HEADER)
        for (b in budgets) {
            append(b.id).append(',')
            append(b.categoryId).append(',')
            append(b.month.format(MONTH_FORMAT)).append(',')
            append(b.limit.toPlainString()).append(',')
            append(b.currencyCode)
            append('\n')
        }
    }

    fun fromCsv(text: String): List<Budget>? = try {
        val rows = CsvFormat.parseLines(text, HEADER) ?: return null
        rows.map { line ->
            val cols = CsvFormat.splitLine(line)
            require(cols.size >= 5) { "Wrong column count" }
            Budget(
                id = cols[0].toLong(),
                categoryId = cols[1].toLong(),
                month = YearMonth.parse(cols[2], MONTH_FORMAT),
                limit = BigDecimal(cols[3]),
                currencyCode = cols[4],
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
