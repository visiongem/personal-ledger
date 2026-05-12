package io.github.visiongem.ledger.feature.record.list

import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.visiongem.ledger.core.base.BaseViewModel
import io.github.visiongem.ledger.core.data.domain.Record
import io.github.visiongem.ledger.core.data.domain.RecordType
import io.github.visiongem.ledger.core.data.repo.AccountRepository
import io.github.visiongem.ledger.core.data.repo.CategoryRepository
import io.github.visiongem.ledger.core.data.repo.RecordRepository
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class RecordListViewModel @Inject constructor(
    private val recordRepository: RecordRepository,
    accountRepository: AccountRepository,
    categoryRepository: CategoryRepository,
) : BaseViewModel() {

    private val filter = MutableStateFlow(Filter())

    val state: StateFlow<RecordListUiState> = combine(
        recordRepository.observeInRange(WIDE_START, WIDE_END),
        accountRepository.observeAll(),
        categoryRepository.observeAll(),
        filter,
    ) { records, accounts, categories, currentFilter ->
        val accountById = accounts.associateBy { it.id }
        val categoryById = categories.associateBy { it.id }
        val filtered = records.asSequence()
            .filter { currentFilter.type == null || it.type == currentFilter.type }
            .filter { currentFilter.accountId == null || it.accountId == currentFilter.accountId }
            .toList()
        RecordListUiState(
            rows = filtered.map { record ->
                val account = accountById[record.accountId]
                val targetAccount = record.transferToAccountId?.let { accountById[it] }
                RecordRow(
                    record = record,
                    accountName = account?.name ?: "Unknown",
                    categoryName = record.categoryId?.let { categoryById[it]?.name },
                    displayCurrency = account?.currencyCode ?: "USD",
                    targetAccountName = targetAccount?.name,
                )
            },
            accounts = accounts,
            selectedType = currentFilter.type,
            selectedAccountId = currentFilter.accountId,
            loading = false,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STATE_TIMEOUT_MILLIS),
        initialValue = RecordListUiState(),
    )

    private val _deletionEvents = MutableSharedFlow<Record>(extraBufferCapacity = 1)
    val deletionEvents: SharedFlow<Record> = _deletionEvents.asSharedFlow()

    fun onLongPress(record: Record) {
        viewModelScope.launch {
            recordRepository.deleteById(record.id)
            _deletionEvents.tryEmit(record)
        }
    }

    fun undoDelete(record: Record) {
        viewModelScope.launch {
            recordRepository.upsert(record)
        }
    }

    fun onTypeFilterChange(type: RecordType?) {
        filter.update { it.copy(type = type) }
    }

    fun onAccountFilterChange(accountId: Long?) {
        filter.update { it.copy(accountId = accountId) }
    }

    private data class Filter(
        val type: RecordType? = null,
        val accountId: Long? = null,
    )

    private companion object {
        // Wide bounds bypass the BETWEEN clause; SQLite Long-stored epoch days handle this fine.
        val WIDE_START: LocalDate = LocalDate.of(1900, 1, 1)
        val WIDE_END: LocalDate = LocalDate.of(2200, 12, 31)
        const val STATE_TIMEOUT_MILLIS = 5_000L
    }
}
