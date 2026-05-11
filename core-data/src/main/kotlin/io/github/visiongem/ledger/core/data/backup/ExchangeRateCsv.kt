package io.github.visiongem.ledger.core.data.backup

import io.github.visiongem.ledger.core.data.domain.ExchangeRate
import java.math.BigDecimal
import java.time.LocalDate

object ExchangeRateCsv {

    const val HEADER = "baseCurrency,quoteCurrency,rate,asOf"

    fun toCsv(rates: List<ExchangeRate>): String = buildString {
        appendLine(HEADER)
        for (r in rates) {
            append(r.baseCurrency).append(',')
            append(r.quoteCurrency).append(',')
            append(r.rate.toPlainString()).append(',')
            append(r.asOf.toString())
            append('\n')
        }
    }

    fun fromCsv(text: String): List<ExchangeRate>? = try {
        val rows = CsvFormat.parseLines(text, HEADER) ?: return null
        rows.map { line ->
            val cols = CsvFormat.splitLine(line)
            require(cols.size >= 4) { "Wrong column count" }
            ExchangeRate(
                baseCurrency = cols[0],
                quoteCurrency = cols[1],
                rate = BigDecimal(cols[2]),
                asOf = LocalDate.parse(cols[3]),
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
