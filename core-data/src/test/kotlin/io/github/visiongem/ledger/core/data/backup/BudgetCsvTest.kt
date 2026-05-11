package io.github.visiongem.ledger.core.data.backup

import com.google.common.truth.Truth.assertThat
import io.github.visiongem.ledger.core.data.domain.Budget
import java.math.BigDecimal
import java.time.YearMonth
import org.junit.jupiter.api.Test

class BudgetCsvTest {

    private val mayFood = Budget(
        id = 1L,
        categoryId = 10L,
        month = YearMonth.of(2026, 5),
        limit = BigDecimal("500.00"),
        currencyCode = "USD",
    )

    private val juneTransport = Budget(
        id = 2L,
        categoryId = 20L,
        month = YearMonth.of(2026, 6),
        limit = BigDecimal("300"),
        currencyCode = "EUR",
    )

    @Test fun roundTripPreservesFields() {
        val csv = BudgetCsv.toCsv(listOf(mayFood, juneTransport))
        val parsed = BudgetCsv.fromCsv(csv)
        assertThat(parsed).containsExactly(mayFood, juneTransport).inOrder()
    }

    @Test fun singleDigitMonthZeroPadded() {
        val january = mayFood.copy(month = YearMonth.of(2026, 1))
        val csv = BudgetCsv.toCsv(listOf(january))
        assertThat(csv).contains(",2026-01,")
    }

    @Test fun emptyListProducesHeaderOnly() {
        assertThat(BudgetCsv.toCsv(emptyList()).trim()).isEqualTo(BudgetCsv.HEADER)
        assertThat(BudgetCsv.fromCsv(BudgetCsv.toCsv(emptyList()))).isEmpty()
    }

    @Test fun missingHeaderReturnsNull() {
        assertThat(BudgetCsv.fromCsv("1,10,2026-05,500,USD\n")).isNull()
    }

    @Test fun malformedMonthReturnsNull() {
        val bad = "${BudgetCsv.HEADER}\n1,10,not-a-month,500,USD\n"
        assertThat(BudgetCsv.fromCsv(bad)).isNull()
    }
}
