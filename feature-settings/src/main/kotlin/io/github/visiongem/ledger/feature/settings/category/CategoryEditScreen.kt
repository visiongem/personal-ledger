package io.github.visiongem.ledger.feature.settings.category

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.visiongem.ledger.core.data.domain.CategoryType
import io.github.visiongem.ledger.core.ui.component.ViaFillButton
import io.github.visiongem.ledger.core.ui.component.ViaTopBar
import io.github.visiongem.ledger.feature.settings.R

@Composable
fun CategoryEditScreen(
    categoryId: Long?,
    onDone: () -> Unit,
    viewModel: CategoryEditViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(categoryId) { viewModel.loadIfNeeded(categoryId) }
    LaunchedEffect(state.saved) { if (state.saved) onDone() }

    Scaffold(
        topBar = {
            ViaTopBar(
                title = stringResource(
                    if (categoryId == null) R.string.category_edit_title_add
                    else R.string.category_edit_title_edit
                ),
                onBack = onDone,
            )
        },
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedTextField(
                value = state.name,
                onValueChange = viewModel::onNameChange,
                label = { Text(stringResource(R.string.category_edit_name_label)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
            TypeSegmentedButtons(
                selected = state.type,
                enabled = !state.isEditing,
                onChange = viewModel::onTypeChange,
            )
            state.errorMessage?.let { message ->
                Text(message, color = MaterialTheme.colorScheme.error)
            }
            ViaFillButton(
                text = stringResource(R.string.category_edit_save),
                onClick = viewModel::save,
                modifier = Modifier.fillMaxWidth(),
                loading = state.saving,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TypeSegmentedButtons(
    selected: CategoryType,
    enabled: Boolean,
    onChange: (CategoryType) -> Unit,
) {
    val visibleTypes = listOf(CategoryType.EXPENSE, CategoryType.INCOME)
    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
        visibleTypes.forEachIndexed { index, type ->
            SegmentedButton(
                selected = type == selected,
                onClick = { if (enabled) onChange(type) },
                shape = SegmentedButtonDefaults.itemShape(index, visibleTypes.size),
                enabled = enabled,
            ) {
                Text(
                    stringResource(
                        when (type) {
                            CategoryType.EXPENSE -> R.string.category_type_expense
                            CategoryType.INCOME -> R.string.category_type_income
                        }
                    )
                )
            }
        }
    }
}
