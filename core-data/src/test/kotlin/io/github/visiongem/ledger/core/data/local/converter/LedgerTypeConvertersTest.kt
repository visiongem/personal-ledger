package io.github.visiongem.ledger.core.data.local.converter

import com.google.common.truth.Truth.assertThat
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import org.junit.jupiter.api.Test

class LedgerTypeConvertersTest {

    // ===== BigDecimal =====

    @Test fun bigDecimalRoundTripPreservesScale() {
        val original = BigDecimal("123.45")
        val storedAs = LedgerTypeConverters.bigDecimalToString(original)
        val restored = LedgerTypeConverters.stringToBigDecimal(storedAs)
        assertThat(restored).isEqualTo(original)
        assertThat(restored?.scale()).isEqualTo(2)
    }

    @Test fun bigDecimalNullRoundTrip() {
        assertThat(LedgerTypeConverters.bigDecimalToString(null)).isNull()
        assertThat(LedgerTypeConverters.stringToBigDecimal(null)).isNull()
    }

    @Test fun bigDecimalLargeNumberSafe() {
        val original = BigDecimal("9999999999999999.99")
        val restored = LedgerTypeConverters.stringToBigDecimal(
            LedgerTypeConverters.bigDecimalToString(original)
        )
        assertThat(restored).isEqualTo(original)
    }

    // ===== LocalDate =====

    @Test fun localDateRoundTrip() {
        val original = LocalDate.of(2026, 5, 10)
        val stored = LedgerTypeConverters.localDateToLong(original)
        assertThat(LedgerTypeConverters.longToLocalDate(stored)).isEqualTo(original)
    }

    @Test fun localDateEpochIsZero() {
        val epoch = LocalDate.of(1970, 1, 1)
        assertThat(LedgerTypeConverters.localDateToLong(epoch)).isEqualTo(0L)
    }

    @Test fun localDateNullRoundTrip() {
        assertThat(LedgerTypeConverters.localDateToLong(null)).isNull()
        assertThat(LedgerTypeConverters.longToLocalDate(null)).isNull()
    }

    // ===== Instant =====

    @Test fun instantRoundTrip() {
        val original = Instant.parse("2026-05-10T12:34:56Z")
        val stored = LedgerTypeConverters.instantToLong(original)
        assertThat(LedgerTypeConverters.longToInstant(stored)).isEqualTo(original)
    }

    @Test fun instantNullRoundTrip() {
        assertThat(LedgerTypeConverters.instantToLong(null)).isNull()
        assertThat(LedgerTypeConverters.longToInstant(null)).isNull()
    }

    // ===== YearMonth =====

    @Test fun yearMonthRoundTrip() {
        val original = YearMonth.of(2026, 5)
        val stored = LedgerTypeConverters.yearMonthToString(original)
        assertThat(stored).isEqualTo("2026-05")
        assertThat(LedgerTypeConverters.stringToYearMonth(stored)).isEqualTo(original)
    }

    @Test fun yearMonthSingleDigitMonthZeroPadded() {
        val january = YearMonth.of(2026, 1)
        assertThat(LedgerTypeConverters.yearMonthToString(january)).isEqualTo("2026-01")
    }

    @Test fun yearMonthNullRoundTrip() {
        assertThat(LedgerTypeConverters.yearMonthToString(null)).isNull()
        assertThat(LedgerTypeConverters.stringToYearMonth(null)).isNull()
    }
}
