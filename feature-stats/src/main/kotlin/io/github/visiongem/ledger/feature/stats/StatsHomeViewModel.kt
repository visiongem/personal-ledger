package io.github.visiongem.ledger.feature.stats

import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.visiongem.ledger.core.base.BaseViewModel
import io.github.visiongem.ledger.core.data.domain.Account
import io.github.visiongem.ledger.core.data.domain.Category
import io.github.visiongem.ledger.core.data.domain.Record
import io.github.visiongem.ledger.core.data.domain.RecordType
import io.github.visiongem.ledger.core.data.local.prefs.UserPreferencesRepository
import io.github.visiongem.ledger.core.data.repo.AccountRepository
import io.github.visiongem.ledger.core.data.repo.CategoryRepository
import io.github.visiongem.ledger.core.data.repo.ExchangeRateRepository
import io.github.visiongem.ledger.core.data.repo.RecordRepository
import java.math.BigDecimal
import java.time.YearMonth
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class StatsHomeViewModel @Inject constructor(
    private val recordRepository: RecordRepository,
    private val categoryRepository: CategoryRepository,
    private val accountRepository: AccountRepository,
    private val exchangeRateRepository: ExchangeRateRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
) : BaseViewModel() {

    private val selectedMonth = MutableStateFlow(YearMonth.now())

    val state: StateFlow<StatsHomeUiState> = selectedMonth.flatMapLatest { month ->
        combine(
            recordRepository.observeInRange(month.atDay(1), month.atEndOfMonth()),
            accountRepository.observeAll(),
            categoryRepository.observeAll(),
            userPreferencesRepository.flow,
        ) { records, accounts, categories, prefs ->
            val accountById = accounts.associateBy { it.id }
            val defaultCurrency = prefs.defaultCurrency
            val rates = buildRates(accounts, defaultCurrency)
            compose(month, records, categories, accountById, defaultCurrency, rates)
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STATE_TIMEOUT_MILLIS),
        initialValue = StatsHomeUiState(month = selectedMonth.value),
    )

    fun goPrevMonth() {
        selectedMonth.update { it.minusMonths(1) }
    }

    fun goNextMonth() {
        // Cap forward navigation at the current real-world month — there's no
        // data past today, future-month stats would always be empty + confusing.
        selectedMonth.update { current ->
            val now = YearMonth.now()
            if (current >= now) current else current.plusMonths(1)
        }
    }

    private suspend fun buildRates(
        accounts: List<Account>,
        defaultCurrency: String,
    ): Map<String, BigDecimal?> {
        val needed = accounts.map { it.currencyCode }.toHashSet() - defaultCurrency
        val result = HashMap<String, BigDecimal?>(needed.size)
        for (source in needed) {
            result[source] = exchangeRateRepository
                .observeLatest(base = source, quote = defaultCurrency)
                .first()
                ?.rate
        }
        return result
    }

    private companion object {
        const val STATE_TIMEOUT_MILLIS = 5_000L

        fun compose(
            month: YearMonth,
            records: List<Record>,
            categories: List<Category>,
            accountById: Map<Long, Account>,
            defaultCurrency: String,
            rates: Map<String, BigDecimal?>,
        ): StatsHomeUiState {
            var incomeTotal = BigDecimal.ZERO
            var expenseTotal = BigDecimal.ZERO
            var unconvertedCount = 0
            val categoryTotals = HashMap<Long, BigDecimal>()

            for (record in records) {
                if (record.type == RecordType.TRANSFER) continue
                val sourceCurrency = accountById[record.accountId]?.currencyCode
                val converted: BigDecimal? = when {
                    sourceCurrency == null -> null
                    sourceCurrency == defaultCurrency -> record.amount
                    else -> rates[sourceCurrency]?.let { rate -> record.amount.multiply(rate) }
                }
                if (converted == null) {
                    unconvertedCount++
                    continue
                }
                when (record.type) {
                    RecordType.INCOME -> incomeTotal = incomeTotal.add(converted)
                    RecordType.EXPENSE -> {
                        expenseTotal = expenseTotal.add(converted)
                        record.categoryId?.let { categoryId ->
                            categoryTotals.merge(categoryId, converted, BigDecimal::add)
                        }
                    }
                    RecordType.TRANSFER -> Unit
                }
            }

            val expenseByCategoryList = categoryTotals.entries
                .map { (categoryId, total) ->
                    val name = categories.firstOrNull { it.id == categoryId }?.name ?: "Unknown"
                    CategoryTotal(categoryName = name, total = total)
                }
                .sortedByDescending { it.total }

            return StatsHomeUiState(
                month = month,
                incomeTotal = incomeTotal,
                expenseTotal = expenseTotal,
                net = incomeTotal.subtract(expenseTotal),
                expenseByCategory = expenseByCategoryList,
                displayCurrency = defaultCurrency,
                unconvertedCount = unconvertedCount,
                loading = false,
            )
        }
    }
}
