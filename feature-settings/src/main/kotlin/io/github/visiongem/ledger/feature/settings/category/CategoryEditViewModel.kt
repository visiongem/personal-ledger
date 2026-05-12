package io.github.visiongem.ledger.feature.settings.category

import android.content.Context
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.visiongem.ledger.core.base.BaseViewModel
import io.github.visiongem.ledger.core.data.domain.Category
import io.github.visiongem.ledger.core.data.domain.CategoryType
import io.github.visiongem.ledger.core.data.repo.CategoryRepository
import io.github.visiongem.ledger.feature.settings.R
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update

@HiltViewModel
class CategoryEditViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val categoryRepository: CategoryRepository,
) : BaseViewModel() {

    private val _state = MutableStateFlow(CategoryEditUiState())
    val state: StateFlow<CategoryEditUiState> = _state.asStateFlow()

    fun loadIfNeeded(id: Long?) {
        if (id == null) return
        if (_state.value.id == id) return
        launchCatching {
            val category = categoryRepository.observeById(id).first() ?: return@launchCatching
            _state.update {
                it.copy(
                    id = category.id,
                    name = category.name,
                    type = category.type,
                    isEditing = true,
                )
            }
        }
    }

    fun onNameChange(value: String) {
        _state.update { it.copy(name = value, errorMessage = null) }
    }

    fun onTypeChange(value: CategoryType) {
        // Type change on an existing category is allowed but kept off the screen
        // (the segmented buttons disable on edit) — record.type is stored
        // separately on each Record, so historical records keep their type.
        _state.update { it.copy(type = value, errorMessage = null) }
    }

    fun save() {
        val current = _state.value
        val trimmedName = current.name.trim()
        if (trimmedName.isEmpty()) {
            _state.update { it.copy(errorMessage = context.getString(R.string.category_err_name_required)) }
            return
        }

        launchCatching(
            onError = { error ->
                _state.update {
                    it.copy(
                        saving = false,
                        errorMessage = error.message ?: context.getString(R.string.category_err_save_failed),
                    )
                }
            }
        ) {
            _state.update { it.copy(saving = true, errorMessage = null) }
            val sortOrder = if (current.id != null) {
                // Editing: keep whatever the existing row has by reading it back.
                categoryRepository.observeById(current.id).first()?.sortOrder ?: 0
            } else {
                // Creating: append to the end of its type group.
                val maxInType = categoryRepository.observeByType(current.type)
                    .first()
                    .maxOfOrNull { it.sortOrder }
                    ?: 0
                maxInType + 1
            }
            categoryRepository.upsert(
                Category(
                    id = current.id ?: 0L,
                    name = trimmedName,
                    type = current.type,
                    iconKey = null,
                    sortOrder = sortOrder,
                )
            )
            _state.update { it.copy(saving = false, saved = true) }
        }
    }
}
