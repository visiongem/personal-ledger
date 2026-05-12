package io.github.visiongem.ledger.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.visiongem.ledger.core.data.domain.ThemeMode
import io.github.visiongem.ledger.core.ui.component.ViaTopBar
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

@Composable
fun SettingsHomeScreen(
    onManageCategoriesClick: () -> Unit = {},
    onManageBudgetsClick: () -> Unit = {},
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
            CategoriesSection(onManageCategoriesClick)
            HorizontalDivider()
            BudgetsSection(onManageBudgetsClick)
            HorizontalDivider()
            RatesSection(
                refreshing = state.refreshingRates,
                message = state.ratesMessage,
                lastRefreshedAt = state.lastRateRefreshAt,
                onRefresh = viewModel::refreshRates,
            )
            HorizontalDivider()
            BackupSection(
                busy = state.backupBusy,
                message = state.backupMessage,
                onExport = viewModel::exportBackupToUri,
                onImport = viewModel::importBackupFromUri,
            )
            HorizontalDivider()
            AboutSection(state.versionName)
        }
    }
}

@Composable
private fun CategoriesSection(onManageClick: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(text = stringResource(R.string.settings_categories_title), style = MaterialTheme.typography.titleMedium)
        io.github.visiongem.ledger.core.ui.component.ViaOutlineButton(
            text = stringResource(R.string.settings_categories_manage),
            onClick = onManageClick,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun BudgetsSection(onManageClick: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(text = stringResource(R.string.settings_budgets_title), style = MaterialTheme.typography.titleMedium)
        io.github.visiongem.ledger.core.ui.component.ViaOutlineButton(
            text = stringResource(R.string.settings_budgets_manage),
            onClick = onManageClick,
            modifier = Modifier.fillMaxWidth(),
        )
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
        contract = androidx.activity.result.contract.ActivityResultContracts.CreateDocument("application/zip"),
    ) { uri -> if (uri != null) onExport(uri) }

    val importLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.OpenDocument(),
    ) { uri -> if (uri != null) onImport(uri) }

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(text = stringResource(R.string.settings_backup_title), style = MaterialTheme.typography.titleMedium)
        io.github.visiongem.ledger.core.ui.component.ViaOutlineButton(
            text = stringResource(R.string.settings_backup_export),
            onClick = { exportLauncher.launch("ledger-backup-${java.time.LocalDate.now()}.zip") },
            enabled = !busy,
            modifier = Modifier.fillMaxWidth(),
        )
        io.github.visiongem.ledger.core.ui.component.ViaOutlineButton(
            text = stringResource(R.string.settings_backup_import),
            onClick = { importLauncher.launch(arrayOf("application/zip", "application/x-zip-compressed", "*/*")) },
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
    lastRefreshedAt: Instant?,
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
        Text(
            text = stringResource(
                R.string.settings_rates_last_updated_fmt,
                lastRefreshedAt?.let(::formatRefreshTimestamp)
                    ?: stringResource(R.string.settings_rates_never),
            ),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
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

private fun formatRefreshTimestamp(instant: Instant): String {
    val formatter = DateTimeFormatter
        .ofLocalizedDateTime(FormatStyle.MEDIUM)
        .withLocale(Locale.getDefault())
        .withZone(ZoneId.systemDefault())
    return formatter.format(instant)
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
    // Local draft state so typing renders immediately — committing to DataStore on every
    // keystroke (the previous design) made the field lag a round-trip behind the user.
    // Draft initializes from current and stays user-driven afterwards; the next time the
    // composable enters composition it picks up whatever DataStore now holds.
    var draft by remember { mutableStateOf(current) }

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(text = stringResource(R.string.settings_currency_title), style = MaterialTheme.typography.titleMedium)
        OutlinedTextField(
            value = draft,
            onValueChange = { input ->
                // Drop non-letters (digits, spaces, punctuation) before uppercasing + truncating.
                val normalized = input.filter { it.isLetter() }.uppercase().take(ISO_LENGTH)
                draft = normalized
                // Only commit ISO 4217-shaped (3 letters) values so partial / empty input
                // doesn't overwrite the previously-saved currency with garbage.
                if (normalized.length == ISO_LENGTH) onChange(normalized)
            },
            label = { Text(stringResource(R.string.settings_currency_hint)) },
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )
    }
}

private const val ISO_LENGTH = 3

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
