package io.github.visiongem.ledger.core.data.backup

import com.google.common.truth.Truth.assertThat
import io.github.visiongem.ledger.core.data.domain.ExchangeRate
import java.math.BigDecimal
import java.time.LocalDate
import org.junit.jupiter.api.Test

class ExchangeRateCsvTest {

    private val usdCny = ExchangeRate(
        baseCurrency = "USD",
        quoteCurrency = "CNY",
        rate = BigDecimal("7.2345"),
        asOf = LocalDate.of(2026, 5, 10),
    )

    private val usdEur = ExchangeRate(
        baseCurrency = "USD",
        quoteCurrency = "EUR",
        rate = BigDecimal("0.92"),
        asOf = LocalDate.of(2026, 5, 11),
    )

    @Test fun roundTripPreservesFields() {
        val csv = ExchangeRateCsv.toCsv(listOf(usdCny, usdEur))
        assertThat(ExchangeRateCsv.fromCsv(csv)).containsExactly(usdCny, usdEur).inOrder()
    }

    @Test fun preservesBigDecimalScale() {
        val csv = ExchangeRateCsv.toCsv(listOf(usdCny))
        val parsed = ExchangeRateCsv.fromCsv(csv)!!
        assertThat(parsed[0].rate).isEqualTo(BigDecimal("7.2345"))
    }

    @Test fun emptyListProducesHeaderOnly() {
        assertThat(ExchangeRateCsv.toCsv(emptyList()).trim()).isEqualTo(ExchangeRateCsv.HEADER)
        assertThat(ExchangeRateCsv.fromCsv(ExchangeRateCsv.toCsv(emptyList()))).isEmpty()
    }

    @Test fun missingHeaderReturnsNull() {
        assertThat(ExchangeRateCsv.fromCsv("USD,CNY,7.2345,2026-05-10\n")).isNull()
    }

    @Test fun malformedDateReturnsNull() {
        val bad = "${ExchangeRateCsv.HEADER}\nUSD,CNY,7.2345,not-a-date\n"
        assertThat(ExchangeRateCsv.fromCsv(bad)).isNull()
    }
}
