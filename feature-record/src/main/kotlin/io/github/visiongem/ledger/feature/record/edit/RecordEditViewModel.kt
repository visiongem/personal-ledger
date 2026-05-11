package io.github.visiongem.ledger.feature.record.edit

import android.content.Context
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.visiongem.ledger.core.base.BaseViewModel
import io.github.visiongem.ledger.core.data.domain.Category
import io.github.visiongem.ledger.core.data.domain.CategoryType
import io.github.visiongem.ledger.core.data.domain.Record
import io.github.visiongem.ledger.core.data.domain.RecordType
import io.github.visiongem.ledger.core.data.repo.AccountRepository
import io.github.visiongem.ledger.core.data.repo.CategoryRepository
import io.github.visiongem.ledger.core.data.repo.RecordRepository
import io.github.visiongem.ledger.feature.record.R
import java.math.BigDecimal
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class RecordEditViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val recordRepository: RecordRepository,
    accountRepository: AccountRepository,
    categoryRepository: CategoryRepository,
) : BaseViewModel() {

    private val _state = MutableStateFlow(RecordEditUiState())
    val state: StateFlow<RecordEditUiState> = _state.asStateFlow()

    private var allCategories: List<Category> = emptyList()

    init {
        viewModelScope.launch {
            accountRepository.observeActive().collect { accounts ->
                _state.update { it.copy(accountOptions = accounts) }
            }
        }
        viewModelScope.launch {
            categoryRepository.observeAll().collect { categories ->
                allCategories = categories
                _state.update {
                    it.copy(categoryOptions = filterCategories(categories, it.type))
                }
            }
        }
    }

    private fun filterCategories(all: List<Category>, type: RecordType): List<Category> =
        all.filter { c ->
            when (type) {
                RecordType.INCOME -> c.type == CategoryType.INCOME
                RecordType.EXPENSE -> c.type == CategoryType.EXPENSE
                RecordType.TRANSFER -> false
            }
        }

    fun loadIfNeeded(id: Long?) {
        if (id == null) return
        if (_state.value.id == id) return
        launchCatching {
            val record = recordRepository.observeById(id).first() ?: return@launchCatching
            _state.update {
                it.copy(
                    id = record.id,
                    type = record.type,
                    accountId = record.accountId,
                    categoryId = record.categoryId,
                    targetAccountId = record.transferToAccountId,
                    transferAmount = record.transferAmount?.toPlainString().orEmpty(),
                    amount = record.amount.toPlainString(),
                    dateInput = record.occurredOn.toString(),
                    note = record.note.orEmpty(),
                    categoryOptions = filterCategories(allCategories, record.type),
                )
            }
        }
    }

    fun onTypeChange(type: RecordType) {
        _state.update { current ->
            val sameType = type == current.type
            current.copy(
                type = type,
                // INCOME / EXPENSE pools differ; TRANSFER has no category.
                categoryId = if (type == RecordType.TRANSFER) null
                else current.categoryId.takeIf { sameType },
                categoryOptions = filterCategories(allCategories, type),
                // Switching away from TRANSFER clears transfer-only fields.
                targetAccountId = if (type == RecordType.TRANSFER) current.targetAccountId else null,
                transferAmount = if (type == RecordType.TRANSFER) current.transferAmount else "",
                errorMessage = null,
            )
        }
    }

    fun onAccountChange(id: Long) {
        _state.update { it.copy(accountId = id, errorMessage = null) }
    }

    fun onCategoryChange(id: Long) {
        _state.update { it.copy(categoryId = id, errorMessage = null) }
    }

    fun onTargetAccountChange(id: Long) {
        _state.update { it.copy(targetAccountId = id, errorMessage = null) }
    }

    fun onTransferAmountChange(value: String) {
        _state.update { it.copy(transferAmount = value, errorMessage = null) }
    }

    fun onAmountChange(value: String) {
        _state.update { it.copy(amount = value, errorMessage = null) }
    }

    fun onDateChange(value: String) {
        _state.update { it.copy(dateInput = value, errorMessage = null) }
    }

    fun onNoteChange(value: String) {
        _state.update { it.copy(note = value, errorMessage = null) }
    }

    fun save() {
        val s = _state.value
        if (s.accountId == null) {
            _state.update { it.copy(errorMessage = context.getString(R.string.record_err_pick_account)) }
            return
        }
        val amount = runCatching { BigDecimal(s.amount) }.getOrNull()
        if (amount == null || amount.signum() <= 0) {
            _state.update { it.copy(errorMessage = context.getString(R.string.record_err_amount_positive)) }
            return
        }
        val date = runCatching { LocalDate.parse(s.dateInput) }.getOrNull()
        if (date == null) {
            _state.update { it.copy(errorMessage = context.getString(R.string.record_err_date_invalid)) }
            return
        }

        when (s.type) {
            RecordType.TRANSFER -> saveTransfer(s, amount, date)
            RecordType.INCOME, RecordType.EXPENSE -> saveIncomeOrExpense(s, amount, date)
        }
    }

    private fun saveIncomeOrExpense(s: RecordEditUiState, amount: BigDecimal, date: LocalDate) {
        if (s.categoryId == null) {
            _state.update { it.copy(errorMessage = context.getString(R.string.record_err_pick_category)) }
            return
        }
        persist(
            Record(
                id = s.id ?: 0L,
                accountId = s.accountId!!,
                categoryId = s.categoryId,
                type = s.type,
                amount = amount,
                occurredOn = date,
                note = s.note.takeIf { it.isNotBlank() },
            )
        )
    }

    private fun saveTransfer(s: RecordEditUiState, amount: BigDecimal, date: LocalDate) {
        if (s.targetAccountId == null) {
            _state.update { it.copy(errorMessage = context.getString(R.string.record_err_pick_target)) }
            return
        }
        if (s.targetAccountId == s.accountId) {
            _state.update { it.copy(errorMessage = context.getString(R.string.record_err_same_account)) }
            return
        }
        val source = s.accountOptions.firstOrNull { it.id == s.accountId }
        val target = s.accountOptions.firstOrNull { it.id == s.targetAccountId }
        val transferAmount: BigDecimal = if (source?.currencyCode == target?.currencyCode) {
            amount
        } else {
            val parsed = runCatching { BigDecimal(s.transferAmount) }.getOrNull()
            if (parsed == null || parsed.signum() <= 0) {
                _state.update {
                    it.copy(errorMessage = context.getString(R.string.record_err_dest_amount_positive))
                }
                return
            }
            parsed
        }
        persist(
            Record(
                id = s.id ?: 0L,
                accountId = s.accountId!!,
                categoryId = null,
                type = RecordType.TRANSFER,
                amount = amount,
                occurredOn = date,
                note = s.note.takeIf { it.isNotBlank() },
                transferToAccountId = s.targetAccountId,
                transferAmount = transferAmount,
            )
        )
    }

    private fun persist(record: Record) {
        launchCatching(
            onError = { error ->
                _state.update {
                    it.copy(
                        saving = false,
                        errorMessage = error.message ?: context.getString(R.string.record_err_save_failed),
                    )
                }
            }
        ) {
            _state.update { it.copy(saving = true, errorMessage = null) }
            recordRepository.upsert(record)
            _state.update { it.copy(saving = false, saved = true) }
        }
    }
}
