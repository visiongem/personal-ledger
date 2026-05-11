package io.github.visiongem.ledger.core.data.backup

import com.google.common.truth.Truth.assertThat
import io.github.visiongem.ledger.core.data.domain.Account
import io.github.visiongem.ledger.core.data.domain.Budget
import io.github.visiongem.ledger.core.data.domain.Category
import io.github.visiongem.ledger.core.data.domain.CategoryType
import io.github.visiongem.ledger.core.data.domain.ExchangeRate
import io.github.visiongem.ledger.core.data.domain.Record
import io.github.visiongem.ledger.core.data.domain.RecordType
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.util.zip.ZipInputStream
import org.junit.jupiter.api.Test

class LedgerZipBackupTest {

    private val sample = LedgerBackup(
        accounts = listOf(
            Account(1L, "Cash", "USD", BigDecimal("100"), false, Instant.parse("2026-05-10T00:00:00Z")),
            Account(2L, "Savings", "EUR", BigDecimal.ZERO, true, Instant.parse("2025-12-31T00:00:00Z")),
        ),
        categories = listOf(
            Category(10L, "Food", CategoryType.EXPENSE, null, 0),
            Category(20L, "Salary", CategoryType.INCOME, null, 1),
        ),
        records = listOf(
            Record(
                id = 100L, accountId = 1L, categoryId = 10L, type = RecordType.EXPENSE,
                amount = BigDecimal("12.50"), occurredOn = LocalDate.of(2026, 5, 10),
                note = "Coffee, \"morning\"",
            ),
            Record(
                id = 101L, accountId = 1L, categoryId = null, type = RecordType.TRANSFER,
                amount = BigDecimal("50"), occurredOn = LocalDate.of(2026, 5, 11),
                note = null, transferToAccountId = 2L, transferAmount = BigDecimal("46.25"),
            ),
        ),
        budgets = listOf(
            Budget(200L, 10L, YearMonth.of(2026, 5), BigDecimal("500"), "USD"),
        ),
        rates = listOf(
            ExchangeRate("USD", "EUR", BigDecimal("0.92"), LocalDate.of(2026, 5, 11)),
            ExchangeRate("USD", "CNY", BigDecimal("7.23"), LocalDate.of(2026, 5, 11)),
        ),
    )

    @Test fun roundTripPreservesAllFiveLists() {
        val out = ByteArrayOutputStream()
        LedgerZipBackup.writeTo(out, sample)
        val read = LedgerZipBackup.readFrom(ByteArrayInputStream(out.toByteArray())).getOrThrow()

        assertThat(read.accounts).isEqualTo(sample.accounts)
        assertThat(read.categories).isEqualTo(sample.categories)
        assertThat(read.records).isEqualTo(sample.records)
        assertThat(read.budgets).isEqualTo(sample.budgets)
        assertThat(read.rates).isEqualTo(sample.rates)
    }

    @Test fun emptyBackupRoundTrips() {
        val empty = LedgerBackup(emptyList(), emptyList(), emptyList(), emptyList(), emptyList())
        val out = ByteArrayOutputStream()
        LedgerZipBackup.writeTo(out, empty)
        val read = LedgerZipBackup.readFrom(ByteArrayInputStream(out.toByteArray())).getOrThrow()
        assertThat(read.totalCount).isEqualTo(0)
    }

    @Test fun missingEntryReturnsFailure() {
        // Build a ZIP that contains accounts.csv only — others missing.
        val out = ByteArrayOutputStream()
        java.util.zip.ZipOutputStream(out).use { zip ->
            zip.putNextEntry(java.util.zip.ZipEntry("accounts.csv"))
            zip.write(AccountCsv.toCsv(sample.accounts).toByteArray())
            zip.closeEntry()
        }

        val result = LedgerZipBackup.readFrom(ByteArrayInputStream(out.toByteArray()))
        assertThat(result.isFailure).isTrue()
        assertThat(result.exceptionOrNull()?.message).contains("Missing")
    }

    @Test fun malformedEntryReturnsFailure() {
        // Build a ZIP whose categories.csv has bogus content.
        val out = ByteArrayOutputStream()
        java.util.zip.ZipOutputStream(out).use { zip ->
            for (entry in listOf("accounts.csv", "categories.csv", "records.csv", "budgets.csv", "rates.csv")) {
                zip.putNextEntry(java.util.zip.ZipEntry(entry))
                zip.write(
                    when (entry) {
                        "categories.csv" -> "not the right header\n".toByteArray()
                        "accounts.csv" -> AccountCsv.toCsv(emptyList()).toByteArray()
                        "records.csv" -> RecordCsv.toCsv(emptyList()).toByteArray()
                        "budgets.csv" -> BudgetCsv.toCsv(emptyList()).toByteArray()
                        else -> ExchangeRateCsv.toCsv(emptyList()).toByteArray()
                    }
                )
                zip.closeEntry()
            }
        }
        val result = LedgerZipBackup.readFrom(ByteArrayInputStream(out.toByteArray()))
        assertThat(result.isFailure).isTrue()
        assertThat(result.exceptionOrNull()?.message).contains("Malformed")
    }

    @Test fun outputZipContainsAllFiveEntries() {
        val out = ByteArrayOutputStream()
        LedgerZipBackup.writeTo(out, sample)
        val entryNames = mutableListOf<String>()
        ZipInputStream(ByteArrayInputStream(out.toByteArray())).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                entryNames.add(entry.name)
                zip.closeEntry()
                entry = zip.nextEntry
            }
        }
        assertThat(entryNames).containsExactly(
            "accounts.csv", "categories.csv", "records.csv", "budgets.csv", "rates.csv",
        )
    }
}
