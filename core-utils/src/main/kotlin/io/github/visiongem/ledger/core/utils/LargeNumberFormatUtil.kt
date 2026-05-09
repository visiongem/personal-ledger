package io.github.visiongem.ledger.core.utils

import java.math.BigDecimal
import java.math.RoundingMode

object LargeNumberFormatUtil {

    private val THOUSAND = BigDecimal(1000)
    private val UNITS = listOf("", "K", "M", "G", "T", "P")

    fun format(value: String?): String {
        var bd = value.toSafeBigDecimal()
        if (bd.signum() == 0) return "0"

        val negative = bd.signum() < 0
        if (negative) bd = bd.abs()

        var unitIndex = 0
        while (bd >= THOUSAND && unitIndex < UNITS.lastIndex) {
            bd = bd.divide(THOUSAND, 10, RoundingMode.HALF_UP)
            unitIndex++
        }

        // Format to 1 decimal place, then strip trailing zeros so "1.0" → "1".
        val text = bd.setScale(1, RoundingMode.HALF_UP)
            .stripTrailingZeros()
            .toPlainString()

        val sign = if (negative) "-" else ""
        return "$sign$text${UNITS[unitIndex]}"
    }
}
