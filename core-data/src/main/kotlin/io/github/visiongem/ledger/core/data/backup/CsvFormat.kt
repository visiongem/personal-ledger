package io.github.visiongem.ledger.core.data.backup

// RFC 4180-style CSV primitives shared by all entity-specific formatters.
internal object CsvFormat {

    fun escape(value: String): String {
        if (value.isEmpty()) return ""
        val needsQuoting = value.contains(',') || value.contains('"') || value.contains('\n')
        if (!needsQuoting) return value
        return buildString {
            append('"')
            for (c in value) {
                if (c == '"') append('"')
                append(c)
            }
            append('"')
        }
    }

    // Splits a CSV line into unquoted column values. Quoted fields handle commas + escaped quotes.
    fun splitLine(line: String): List<String> {
        val result = mutableListOf<String>()
        val sb = StringBuilder()
        var inQuotes = false
        var i = 0
        while (i < line.length) {
            val c = line[i]
            when {
                inQuotes -> {
                    if (c == '"') {
                        if (i + 1 < line.length && line[i + 1] == '"') {
                            sb.append('"')
                            i++
                        } else {
                            inQuotes = false
                        }
                    } else {
                        sb.append(c)
                    }
                }
                c == ',' -> {
                    result.add(sb.toString())
                    sb.clear()
                }
                c == '"' && sb.isEmpty() -> inQuotes = true
                else -> sb.append(c)
            }
            i++
        }
        result.add(sb.toString())
        return result
    }

    // Returns the data rows (header excluded). null = header missing or wrong.
    fun parseLines(text: String, header: String): List<String>? {
        val lines = text.lines().filter { it.isNotBlank() }
        if (lines.isEmpty()) return emptyList()
        if (lines[0] != header) return null
        return lines.drop(1)
    }
}
