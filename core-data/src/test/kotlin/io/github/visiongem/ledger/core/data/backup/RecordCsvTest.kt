package io.github.visiongem.ledger.core.data.backup

import com.google.common.truth.Truth.assertThat
import io.github.visiongem.ledger.core.data.domain.Record
import io.github.visiongem.ledger.core.data.domain.RecordType
import java.math.BigDecimal
import java.time.LocalDate
import org.junit.jupiter.api.Test

class RecordCsvTest {

    private val expense = Record(
        id = 1L,
        accountId = 10L,
        categoryId = 20L,
        type = RecordType.EXPENSE,
        amount = BigDecimal("12.50"),
        occurredOn = LocalDate.of(2026, 5, 10),
        note = "Coffee",
    )

    private val transfer = Record(
        id = 2L,
        accountId = 10L,
        categoryId = null,
        type = RecordType.TRANSFER,
        amount = BigDecimal("100"),
        occurredOn = LocalDate.of(2026, 5, 11),
        note = null,
        transferToAccountId = 11L,
        transferAmount = BigDecimal("92.50"),
    )

    @Test fun roundTripPreservesFields() {
        val csv = RecordCsv.toCsv(listOf(expense, transfer))
        val parsed = RecordCsv.fromCsv(csv)
        assertThat(parsed).hasSize(2)
        assertThat(parsed!![0]).isEqualTo(expense)
        assertThat(parsed[1]).isEqualTo(transfer)
    }

    @Test fun emptyListProducesHeaderOnly() {
        val csv = RecordCsv.toCsv(emptyList())
        assertThat(csv.trim()).isEqualTo(RecordCsv.HEADER)
        assertThat(RecordCsv.fromCsv(csv)).isEmpty()
    }

    @Test fun noteWithCommaAndQuoteRoundTrips() {
        val record = expense.copy(note = "lunch, \"two\" coffees")
        val csv = RecordCsv.toCsv(listOf(record))
        val parsed = RecordCsv.fromCsv(csv)
        assertThat(parsed).hasSize(1)
        assertThat(parsed!![0].note).isEqualTo("lunch, \"two\" coffees")
    }

    @Test fun missingHeaderReturnsNull() {
        val bad = "1,2026-05-10,1,1,EXPENSE,10,foo,,\n"
        assertThat(RecordCsv.fromCsv(bad)).isNull()
    }

    @Test fun malformedRowReturnsNull() {
        val bad = "${RecordCsv.HEADER}\nnot-a-long,2026-05-10,1,1,EXPENSE,10,foo,,\n"
        assertThat(RecordCsv.fromCsv(bad)).isNull()
    }

    @Test fun emptyInputReturnsEmptyList() {
        assertThat(RecordCsv.fromCsv("")).isEmpty()
    }
}
