package io.github.visiongem.ledger.core.utils

import com.google.common.truth.Truth.assertThat
import java.math.BigDecimal
import java.util.Locale
import org.junit.jupiter.api.Test

class CurrencyFormatterTest {

    private val locale = Locale.US

    @Test fun usdWithCentsFormatsTwoDecimalPlaces() {
        val out = CurrencyFormatter.format(BigDecimal("1234.5"), "USD", locale)
        assertThat(out).isEqualTo("USD 1,234.50")
    }

    @Test fun usdRoundsHalfUp() {
        val out = CurrencyFormatter.format(BigDecimal("1234.567"), "USD", locale)
        assertThat(out).isEqualTo("USD 1,234.57")
    }

    @Test fun jpyHasNoDecimals() {
        val out = CurrencyFormatter.format(BigDecimal("1234"), "JPY", locale)
        assertThat(out).isEqualTo("JPY 1,234")
    }

    @Test fun zeroFormatsWithScale() {
        val out = CurrencyFormatter.format(BigDecimal.ZERO, "USD", locale)
        assertThat(out).isEqualTo("USD 0.00")
    }

    @Test fun negativeRendersSign() {
        val out = CurrencyFormatter.format(BigDecimal("-50.5"), "USD", locale)
        assertThat(out).isEqualTo("USD -50.50")
    }

    @Test fun largeNumberHasThousandSeparator() {
        val out = CurrencyFormatter.format(BigDecimal("1234567.89"), "USD", locale)
        assertThat(out).isEqualTo("USD 1,234,567.89")
    }

    @Test fun unknownCurrencyDefaultsToTwoDecimals() {
        val out = CurrencyFormatter.format(BigDecimal("100"), "XYZ", locale)
        assertThat(out).isEqualTo("XYZ 100.00")
    }

    @Test fun formatAmountSkipsCurrencyPrefix() {
        val out = CurrencyFormatter.formatAmount(BigDecimal("1234.5"), scale = 2, locale = locale)
        assertThat(out).isEqualTo("1,234.50")
    }

    @Test fun defaultScaleMatchesIso4217() {
        assertThat(CurrencyFormatter.defaultScaleFor("USD")).isEqualTo(2)
        assertThat(CurrencyFormatter.defaultScaleFor("JPY")).isEqualTo(0)
    }
}
