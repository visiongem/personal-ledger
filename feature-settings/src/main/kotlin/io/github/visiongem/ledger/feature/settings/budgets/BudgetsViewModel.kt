package io.github.visiongem.ledger.feature.settings.budgets

import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.visiongem.ledger.core.base.BaseViewModel
import io.github.visiongem.ledger.core.data.domain.Budget
import io.github.visiongem.ledger.core.data.domain.CategoryType
import io.github.visiongem.ledger.core.data.local.prefs.UserPreferencesRepository
import io.github.visiongem.ledger.core.data.repo.BudgetRepository
import io.github.visiongem.ledger.core.data.repo.CategoryRepository
import java.math.BigDecimal
import java.time.YearMonth
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class BudgetsViewModel @Inject constructor(
    private val budgetRepository: BudgetRepository,
    categoryRepository: CategoryRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
) : BaseViewModel() {

    private val month: YearMonth = YearMonth.now()

    val state: StateFlow<BudgetsUiState> = combine(
        categoryRepository.observeByType(CategoryType.EXPENSE),
        budgetRepository.observeBudgetsWithUsage(month),
        userPreferencesRepository.flow,
    ) { categories, budgetsWithUsage, prefs ->
        val budgetByCategoryId = budgetsWithUsage.associateBy { it.budget.categoryId }
        BudgetsUiState(
            month = month,
            defaultCurrency = prefs.defaultCurrency,
            rows = categories.map { c ->
                BudgetRow(category = c, budgetWithUsage = budgetByCategoryId[c.id])
            },
            loading = false,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STATE_TIMEOUT_MILLIS),
        initialValue = BudgetsUiState(),
    )

    fun setBudget(categoryId: Long, amount: BigDecimal) {
        viewModelScope.launch {
            val currency = userPreferencesRepository.flow.first().defaultCurrency
            // Upsert: if a budget for (categoryId, month) already exists, Room's
            // @Upsert handles update; otherwise insert. Keep id=0 for the insert
            // path; UPDATE uses categoryId+month as the natural key via DAO.
            val existing = budgetRepository.observeByCategoryAndMonth(categoryId, month).first()
            budgetRepository.upsert(
                Budget(
                    id = existing?.id ?: 0L,
                    categoryId = categoryId,
                    month = month,
                    limit = amount,
                    currencyCode = currency,
                )
            )
        }
    }

    fun clearBudget(categoryId: Long) {
        viewModelScope.launch {
            val existing = budgetRepository.observeByCategoryAndMonth(categoryId, month).first()
            if (existing != null) {
                budgetRepository.deleteById(existing.id)
            }
        }
    }

    private companion object {
        const val STATE_TIMEOUT_MILLIS = 5_000L
    }
}
