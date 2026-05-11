package io.github.visiongem.ledger.feature.record.list

import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.visiongem.ledger.core.base.BaseViewModel
import io.github.visiongem.ledger.core.data.domain.Record
import io.github.visiongem.ledger.core.data.repo.AccountRepository
import io.github.visiongem.ledger.core.data.repo.CategoryRepository
import io.github.visiongem.ledger.core.data.repo.RecordRepository
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class RecordListViewModel @Inject constructor(
    private val recordRepository: RecordRepository,
    accountRepository: AccountRepository,
    categoryRepository: CategoryRepository,
) : BaseViewModel() {

    val state: StateFlow<RecordListUiState> = combine(
        recordRepository.observeInRange(WIDE_START, WIDE_END),
        accountRepository.observeAll(),
        categoryRepository.observeAll(),
    ) { records, accounts, categories ->
        val accountById = accounts.associateBy { it.id }
        val categoryById = categories.associateBy { it.id }
        RecordListUiState(
            rows = records.map { record ->
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

    private companion object {
        // Wide bounds bypass the BETWEEN clause; SQLite Long-stored epoch days handle this fine.
        val WIDE_START: LocalDate = LocalDate.of(1900, 1, 1)
        val WIDE_END: LocalDate = LocalDate.of(2200, 12, 31)
        const val STATE_TIMEOUT_MILLIS = 5_000L
    }
}
