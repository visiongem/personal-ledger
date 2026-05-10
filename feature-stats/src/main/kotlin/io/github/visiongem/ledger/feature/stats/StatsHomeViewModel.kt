package io.github.visiongem.ledger.feature.stats

import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.visiongem.ledger.core.base.BaseViewModel
import io.github.visiongem.ledger.core.data.domain.Category
import io.github.visiongem.ledger.core.data.domain.Record
import io.github.visiongem.ledger.core.data.domain.RecordType
import io.github.visiongem.ledger.core.data.local.prefs.UserPreferencesRepository
import io.github.visiongem.ledger.core.data.repo.CategoryRepository
import io.github.visiongem.ledger.core.data.repo.RecordRepository
import java.math.BigDecimal
import java.time.YearMonth
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

@HiltViewModel
class StatsHomeViewModel @Inject constructor(
    recordRepository: RecordRepository,
    categoryRepository: CategoryRepository,
    userPreferencesRepository: UserPreferencesRepository,
) : BaseViewModel() {

    private val month: YearMonth = YearMonth.now()

    val state: StateFlow<StatsHomeUiState> = combine(
        recordRepository.observeInRange(month.atDay(1), month.atEndOfMonth()),
        categoryRepository.observeAll(),
        userPreferencesRepository.flow,
    ) { records, categories, prefs ->
        compose(month, records, categories, prefs.defaultCurrency)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STATE_TIMEOUT_MILLIS),
        initialValue = StatsHomeUiState(month = month),
    )

    private companion object {
        const val STATE_TIMEOUT_MILLIS = 5_000L

        fun compose(
            month: YearMonth,
            records: List<Record>,
            categories: List<Category>,
            defaultCurrency: String,
        ): StatsHomeUiState {
            val incomeTotal = records.asSequence()
                .filter { it.type == RecordType.INCOME }
                .fold(BigDecimal.ZERO) { acc, r -> acc.add(r.amount) }
            val expenseTotal = records.asSequence()
                .filter { it.type == RecordType.EXPENSE }
                .fold(BigDecimal.ZERO) { acc, r -> acc.add(r.amount) }

            val byCategoryName = records.asSequence()
                .filter { it.type == RecordType.EXPENSE && it.categoryId != null }
                .groupBy { it.categoryId!! }
                .map { (catId, group) ->
                    val name = categories.firstOrNull { it.id == catId }?.name ?: "Unknown"
                    val sum = group.fold(BigDecimal.ZERO) { acc, r -> acc.add(r.amount) }
                    CategoryTotal(categoryName = name, total = sum)
                }
                .sortedByDescending { it.total }

            return StatsHomeUiState(
                month = month,
                incomeTotal = incomeTotal,
                expenseTotal = expenseTotal,
                net = incomeTotal.subtract(expenseTotal),
                expenseByCategory = byCategoryName,
                displayCurrency = defaultCurrency,
                loading = false,
            )
        }
    }
}
