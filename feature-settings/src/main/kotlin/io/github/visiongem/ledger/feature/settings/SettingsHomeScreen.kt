package io.github.visiongem.ledger.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.visiongem.ledger.core.data.domain.ThemeMode
import io.github.visiongem.ledger.core.ui.component.ViaTopBar

@Composable
fun SettingsHomeScreen(
    viewModel: SettingsHomeViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Scaffold(
        topBar = { ViaTopBar(title = "Settings") },
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            ThemeSection(state.themeMode, viewModel::onThemeModeChange)
            HorizontalDivider()
            CurrencySection(state.defaultCurrency, viewModel::onDefaultCurrencyChange)
            HorizontalDivider()
            RatesSection(
                refreshing = state.refreshingRates,
                message = state.ratesMessage,
                onRefresh = viewModel::refreshRates,
            )
            HorizontalDivider()
            AboutSection(state.versionName)
        }
    }
}

@Composable
private fun RatesSection(
    refreshing: Boolean,
    message: String?,
    onRefresh: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(text = "Exchange rates", style = MaterialTheme.typography.titleMedium)
        io.github.visiongem.ledger.core.ui.component.ViaOutlineButton(
            text = if (refreshing) "Refreshing…" else "Refresh exchange rates",
            onClick = onRefresh,
            enabled = !refreshing,
            modifier = Modifier.fillMaxWidth(),
        )
        message?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ThemeSection(selected: ThemeMode, onChange: (ThemeMode) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(text = "Theme", style = MaterialTheme.typography.titleMedium)
        ThemeMode.entries.forEach { mode ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .selectable(
                        selected = mode == selected,
                        onClick = { onChange(mode) },
                        role = Role.RadioButton,
                    )
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RadioButton(selected = mode == selected, onClick = { onChange(mode) })
                Text(
                    text = mode.label(),
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
        }
    }
}

private fun ThemeMode.label(): String = when (this) {
    ThemeMode.SYSTEM -> "Follow system"
    ThemeMode.LIGHT -> "Light"
    ThemeMode.DARK -> "Dark"
}

@Composable
private fun CurrencySection(current: String, onChange: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(text = "Default currency", style = MaterialTheme.typography.titleMedium)
        OutlinedTextField(
            value = current,
            onValueChange = onChange,
            label = { Text("ISO 4217 (e.g. USD, CNY, EUR)") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )
    }
}

@Composable
private fun AboutSection(version: String) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(text = "About", style = MaterialTheme.typography.titleMedium)
        Text(
            text = "Personal Ledger · v$version",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
