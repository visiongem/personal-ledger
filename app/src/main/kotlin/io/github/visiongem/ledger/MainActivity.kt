package io.github.visiongem.ledger

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.ui.NavDisplay
import dagger.hilt.android.AndroidEntryPoint
import io.github.visiongem.ledger.core.data.domain.ThemeMode
import io.github.visiongem.ledger.core.ui.theme.LedgerTheme
import io.github.visiongem.ledger.feature.account.edit.AccountEditScreen
import io.github.visiongem.ledger.feature.account.list.AccountListScreen
import io.github.visiongem.ledger.feature.account.nav.AccountEditRoute
import io.github.visiongem.ledger.feature.account.nav.AccountListRoute
import io.github.visiongem.ledger.feature.record.edit.RecordEditScreen
import io.github.visiongem.ledger.feature.record.list.RecordListScreen
import io.github.visiongem.ledger.feature.record.nav.RecordEditRoute
import io.github.visiongem.ledger.feature.record.nav.RecordListRoute
import io.github.visiongem.ledger.feature.settings.SettingsHomeScreen
import io.github.visiongem.ledger.feature.settings.nav.SettingsHomeRoute
import io.github.visiongem.ledger.feature.stats.StatsHomeScreen
import io.github.visiongem.ledger.feature.stats.nav.StatsHomeRoute

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // Must run BEFORE super.onCreate() to take effect before Window attach. See androidx.activity docs.
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            val mainViewModel: MainViewModel = hiltViewModel()
            val themeMode by mainViewModel.themeMode.collectAsStateWithLifecycle()

            val darkTheme = when (themeMode) {
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }

            LedgerTheme(darkTheme = darkTheme) {
                LedgerApp()
            }
        }
    }
}

private data class TopLevelDestination(
    val route: Any,
    val label: String,
    val icon: ImageVector,
)

private val topLevelDestinations = listOf(
    TopLevelDestination(RecordListRoute, "Records", Icons.Default.Receipt),
    TopLevelDestination(AccountListRoute, "Accounts", Icons.Default.AccountBalance),
    TopLevelDestination(StatsHomeRoute, "Stats", Icons.Default.BarChart),
    TopLevelDestination(SettingsHomeRoute, "Settings", Icons.Default.Settings),
)

@Composable
private fun LedgerApp() {
    // Per-tab back stacks so a sub-page (e.g. RecordEdit) stays alive when the user
    // switches tabs and returns. Stacks themselves use `remember` (not Saveable) —
    // config changes will drop sub-pages but the running session keeps them.
    val recordsStack = remember { mutableStateListOf<Any>(RecordListRoute) }
    val accountsStack = remember { mutableStateListOf<Any>(AccountListRoute) }
    val statsStack = remember { mutableStateListOf<Any>(StatsHomeRoute) }
    val settingsStack = remember { mutableStateListOf<Any>(SettingsHomeRoute) }

    // Saveable so the chosen tab survives rotation.
    var selectedTabIndex by rememberSaveable { mutableStateOf(0) }
    val tabStacks = listOf(recordsStack, accountsStack, statsStack, settingsStack)
    val currentStack = tabStacks[selectedTabIndex]

    Scaffold(
        bottomBar = {
            NavigationBar {
                topLevelDestinations.forEachIndexed { index, dest ->
                    NavigationBarItem(
                        selected = index == selectedTabIndex,
                        onClick = { selectedTabIndex = index },
                        icon = { Icon(dest.icon, contentDescription = dest.label) },
                        label = { Text(dest.label) },
                    )
                }
            }
        },
    ) { paddingValues ->
        NavDisplay(
            backStack = currentStack,
            modifier = Modifier.padding(paddingValues),
            onBack = { currentStack.removeLastOrNull() },
            entryProvider = { key ->
                when (key) {
                    is RecordListRoute -> NavEntry(key) {
                        RecordListScreen(
                            onRecordClick = { id -> recordsStack.add(RecordEditRoute(id)) },
                            onAddClick = { recordsStack.add(RecordEditRoute(null)) },
                        )
                    }
                    is RecordEditRoute -> NavEntry(key) {
                        RecordEditScreen(
                            recordId = key.recordId,
                            onDone = { recordsStack.removeLastOrNull() },
                        )
                    }
                    is AccountListRoute -> NavEntry(key) {
                        AccountListScreen(
                            onAccountClick = { id -> accountsStack.add(AccountEditRoute(id)) },
                            onAddClick = { accountsStack.add(AccountEditRoute(null)) },
                        )
                    }
                    is AccountEditRoute -> NavEntry(key) {
                        AccountEditScreen(
                            accountId = key.accountId,
                            onDone = { accountsStack.removeLastOrNull() },
                        )
                    }
                    is StatsHomeRoute -> NavEntry(key) { StatsHomeScreen() }
                    is SettingsHomeRoute -> NavEntry(key) { SettingsHomeScreen() }
                    else -> error("Unknown route: $key")
                }
            },
        )
    }
}
