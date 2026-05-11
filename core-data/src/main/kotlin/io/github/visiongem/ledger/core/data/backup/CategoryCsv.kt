package io.github.visiongem.ledger.core.data.backup

import io.github.visiongem.ledger.core.data.domain.Category
import io.github.visiongem.ledger.core.data.domain.CategoryType

object CategoryCsv {

    const val HEADER = "id,name,type,iconKey,sortOrder"

    fun toCsv(categories: List<Category>): String = buildString {
        appendLine(HEADER)
        for (c in categories) {
            append(c.id).append(',')
            append(CsvFormat.escape(c.name)).append(',')
            append(c.type.name).append(',')
            append(CsvFormat.escape(c.iconKey.orEmpty())).append(',')
            append(c.sortOrder)
            append('\n')
        }
    }

    fun fromCsv(text: String): List<Category>? = try {
        val rows = CsvFormat.parseLines(text, HEADER) ?: return null
        rows.map { line ->
            val cols = CsvFormat.splitLine(line)
            require(cols.size >= 5) { "Wrong column count" }
            Category(
                id = cols[0].toLong(),
                name = cols[1],
                type = CategoryType.valueOf(cols[2]),
                iconKey = cols[3].takeIf { it.isNotEmpty() },
                sortOrder = cols[4].toInt(),
            )
        }
    } catch (_: IllegalArgumentException) {
        null
    } catch (_: NumberFormatException) {
        null
    } catch (_: Exception) {
        null
    }
}
