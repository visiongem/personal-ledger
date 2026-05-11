package io.github.visiongem.ledger.core.data.backup

import java.io.InputStream
import java.io.OutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

// Writes and reads a 5-entry ZIP whose names are stable across versions.
// All entries use UTF-8 CSV with the per-entity headers + escape rules from
// CsvFormat. Reads fail loudly if any entry is missing or malformed.
object LedgerZipBackup {

    private const val ACCOUNTS_ENTRY = "accounts.csv"
    private const val CATEGORIES_ENTRY = "categories.csv"
    private const val RECORDS_ENTRY = "records.csv"
    private const val BUDGETS_ENTRY = "budgets.csv"
    private const val RATES_ENTRY = "rates.csv"

    fun writeTo(out: OutputStream, dump: LedgerBackup) {
        ZipOutputStream(out).use { zip ->
            zip.writeEntry(ACCOUNTS_ENTRY, AccountCsv.toCsv(dump.accounts))
            zip.writeEntry(CATEGORIES_ENTRY, CategoryCsv.toCsv(dump.categories))
            zip.writeEntry(RECORDS_ENTRY, RecordCsv.toCsv(dump.records))
            zip.writeEntry(BUDGETS_ENTRY, BudgetCsv.toCsv(dump.budgets))
            zip.writeEntry(RATES_ENTRY, ExchangeRateCsv.toCsv(dump.rates))
        }
    }

    fun readFrom(input: InputStream): Result<LedgerBackup> = runCatching {
        val texts = HashMap<String, String>(5)
        ZipInputStream(input).use { zip ->
            var entry: ZipEntry? = zip.nextEntry
            while (entry != null) {
                val text = zip.readBytes().toString(Charsets.UTF_8)
                texts[entry.name] = text
                zip.closeEntry()
                entry = zip.nextEntry
            }
        }

        LedgerBackup(
            accounts = decode(texts, ACCOUNTS_ENTRY, AccountCsv::fromCsv),
            categories = decode(texts, CATEGORIES_ENTRY, CategoryCsv::fromCsv),
            records = decode(texts, RECORDS_ENTRY, RecordCsv::fromCsv),
            budgets = decode(texts, BUDGETS_ENTRY, BudgetCsv::fromCsv),
            rates = decode(texts, RATES_ENTRY, ExchangeRateCsv::fromCsv),
        )
    }

    private fun ZipOutputStream.writeEntry(name: String, content: String) {
        putNextEntry(ZipEntry(name))
        write(content.toByteArray(Charsets.UTF_8))
        closeEntry()
    }

    private fun <T> decode(
        texts: Map<String, String>,
        name: String,
        parser: (String) -> List<T>?,
    ): List<T> {
        val text = texts[name] ?: error("Missing $name in ZIP")
        return parser(text) ?: error("Malformed $name in ZIP")
    }
}
