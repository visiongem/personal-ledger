package io.github.visiongem.ledger.core.data.local.converter

import androidx.room.TypeConverter
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

object LedgerTypeConverters {

    private val yearMonthFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM")

    @TypeConverter
    @JvmStatic
    fun bigDecimalToString(value: BigDecimal?): String? = value?.toPlainString()

    @TypeConverter
    @JvmStatic
    fun stringToBigDecimal(value: String?): BigDecimal? = value?.let(::BigDecimal)

    @TypeConverter
    @JvmStatic
    fun localDateToLong(value: LocalDate?): Long? = value?.toEpochDay()

    @TypeConverter
    @JvmStatic
    fun longToLocalDate(value: Long?): LocalDate? = value?.let(LocalDate::ofEpochDay)

    @TypeConverter
    @JvmStatic
    fun instantToLong(value: Instant?): Long? = value?.toEpochMilli()

    @TypeConverter
    @JvmStatic
    fun longToInstant(value: Long?): Instant? = value?.let(Instant::ofEpochMilli)

    @TypeConverter
    @JvmStatic
    fun yearMonthToString(value: YearMonth?): String? = value?.format(yearMonthFormatter)

    @TypeConverter
    @JvmStatic
    fun stringToYearMonth(value: String?): YearMonth? =
        value?.let { YearMonth.parse(it, yearMonthFormatter) }
}
