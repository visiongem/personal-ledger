package io.github.visiongem.ledger.core.utils

import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Currency
import java.util.Locale

object CurrencyFormatter {

    // Formats amount with thousand separators and currency-specific decimal scale,
    // prefixed by the ISO 4217 code. JPY and other zero-decimal currencies render no
    // decimals. Falls back to scale=2 if the code isn't recognised by JDK.
    fun format(
        amount: BigDecimal,
        currencyCode: String,
        locale: Locale = Locale.getDefault(),
    ): String {
        val scale = defaultScaleFor(currencyCode)
        val rounded = amount.setScale(scale, RoundingMode.HALF_UP)
        val formatter = makeFormatter(scale, locale)
        return "$currencyCode ${formatter.format(rounded)}"
    }

    // Number-only variant for cases where the currency code is rendered separately.
    fun formatAmount(
        amount: BigDecimal,
        scale: Int = 2,
        locale: Locale = Locale.getDefault(),
    ): String {
        val rounded = amount.setScale(scale, RoundingMode.HALF_UP)
        return makeFormatter(scale, locale).format(rounded)
    }

    fun defaultScaleFor(currencyCode: String): Int =
        runCatching { Currency.getInstance(currencyCode).defaultFractionDigits }
            .getOrNull()
            ?.coerceAtLeast(0)
            ?: DEFAULT_SCALE

    private fun makeFormatter(scale: Int, locale: Locale): DecimalFormat {
        val pattern = buildString {
            append("#,##0")
            if (scale > 0) {
                append('.')
                repeat(scale) { append('0') }
            }
        }
        return DecimalFormat(pattern, DecimalFormatSymbols(locale))
    }

    private const val DEFAULT_SCALE = 2
}
