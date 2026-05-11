package io.github.visiongem.ledger.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.res.stringResource
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
        topBar = { ViaTopBar(title = stringResource(R.string.settings_title)) },
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
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
            BackupSection(
                busy = state.backupBusy,
                message = state.backupMessage,
                onExport = viewModel::exportRecordsToUri,
                onImport = viewModel::importRecordsFromUri,
            )
            HorizontalDivider()
            AboutSection(state.versionName)
        }
    }
}

@Composable
private fun BackupSection(
    busy: Boolean,
    message: String?,
    onExport: (android.net.Uri) -> Unit,
    onImport: (android.net.Uri) -> Unit,
) {
    val exportLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.CreateDocument("text/csv"),
    ) { uri -> if (uri != null) onExport(uri) }

    val importLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.OpenDocument(),
    ) { uri -> if (uri != null) onImport(uri) }

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(text = stringResource(R.string.settings_backup_title), style = MaterialTheme.typography.titleMedium)
        io.github.visiongem.ledger.core.ui.component.ViaOutlineButton(
            text = stringResource(R.string.settings_backup_export),
            onClick = { exportLauncher.launch("ledger-records-${java.time.LocalDate.now()}.csv") },
            enabled = !busy,
            modifier = Modifier.fillMaxWidth(),
        )
        io.github.visiongem.ledger.core.ui.component.ViaOutlineButton(
            text = stringResource(R.string.settings_backup_import),
            onClick = { importLauncher.launch(arrayOf("text/csv", "text/comma-separated-values", "text/*")) },
            enabled = !busy,
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
private fun RatesSection(
    refreshing: Boolean,
    message: String?,
    onRefresh: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(text = stringResource(R.string.settings_rates_title), style = MaterialTheme.typography.titleMedium)
        io.github.visiongem.ledger.core.ui.component.ViaOutlineButton(
            text = stringResource(
                if (refreshing) R.string.settings_rates_refreshing
                else R.string.settings_rates_refresh
            ),
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
        Text(text = stringResource(R.string.settings_theme_title), style = MaterialTheme.typography.titleMedium)
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
                    text = stringResource(mode.labelRes()),
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
        }
    }
}

private fun ThemeMode.labelRes(): Int = when (this) {
    ThemeMode.SYSTEM -> R.string.settings_theme_system
    ThemeMode.LIGHT -> R.string.settings_theme_light
    ThemeMode.DARK -> R.string.settings_theme_dark
}

@Composable
private fun CurrencySection(current: String, onChange: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(text = stringResource(R.string.settings_currency_title), style = MaterialTheme.typography.titleMedium)
        OutlinedTextField(
            value = current,
            onValueChange = onChange,
            label = { Text(stringResource(R.string.settings_currency_hint)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )
    }
}

@Composable
private fun AboutSection(version: String) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(text = stringResource(R.string.settings_about_title), style = MaterialTheme.typography.titleMedium)
        Text(
            text = stringResource(R.string.settings_version_fmt, version),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
