package io.github.visiongem.ledger.core.data.backup

import com.google.common.truth.Truth.assertThat
import io.github.visiongem.ledger.core.data.domain.Account
import java.math.BigDecimal
import java.time.Instant
import org.junit.jupiter.api.Test

class AccountCsvTest {

    private val cash = Account(
        id = 1L,
        name = "Cash",
        currencyCode = "USD",
        openingBalance = BigDecimal("100.50"),
        archived = false,
        createdAt = Instant.parse("2026-05-10T00:00:00Z"),
    )

    private val archived = Account(
        id = 2L,
        name = "Old, account \"savings\"",
        currencyCode = "EUR",
        openingBalance = BigDecimal.ZERO,
        archived = true,
        createdAt = Instant.parse("2025-12-31T23:59:59Z"),
    )

    @Test fun roundTripPreservesFields() {
        val csv = AccountCsv.toCsv(listOf(cash, archived))
        val parsed = AccountCsv.fromCsv(csv)
        assertThat(parsed).hasSize(2)
        assertThat(parsed!![0]).isEqualTo(cash)
        assertThat(parsed[1]).isEqualTo(archived)
    }

    @Test fun emptyListProducesHeaderOnly() {
        val csv = AccountCsv.toCsv(emptyList())
        assertThat(csv.trim()).isEqualTo(AccountCsv.HEADER)
        assertThat(AccountCsv.fromCsv(csv)).isEmpty()
    }

    @Test fun nameWithCommaAndQuoteRoundTrips() {
        val withCommas = cash.copy(name = "Savings, Inc. \"main\"")
        val csv = AccountCsv.toCsv(listOf(withCommas))
        assertThat(AccountCsv.fromCsv(csv)).containsExactly(withCommas)
    }

    @Test fun missingHeaderReturnsNull() {
        val bad = "1,Cash,USD,0,0,2026-05-10T00:00:00Z\n"
        assertThat(AccountCsv.fromCsv(bad)).isNull()
    }

    @Test fun malformedRowReturnsNull() {
        val bad = "${AccountCsv.HEADER}\nnot-a-long,Cash,USD,0,0,2026-05-10T00:00:00Z\n"
        assertThat(AccountCsv.fromCsv(bad)).isNull()
    }

    @Test fun emptyInputReturnsEmptyList() {
        assertThat(AccountCsv.fromCsv("")).isEmpty()
    }

    @Test fun archivedFlagRoundTrips() {
        val csv = AccountCsv.toCsv(listOf(cash, archived))
        val parsed = AccountCsv.fromCsv(csv)!!
        assertThat(parsed[0].archived).isFalse()
        assertThat(parsed[1].archived).isTrue()
    }
}
