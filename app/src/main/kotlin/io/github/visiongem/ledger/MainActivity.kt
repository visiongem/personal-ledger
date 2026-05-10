package io.github.visiongem.ledger

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.ui.NavDisplay
import dagger.hilt.android.AndroidEntryPoint
import io.github.visiongem.ledger.core.ui.theme.LedgerTheme
import io.github.visiongem.ledger.feature.account.edit.AccountEditScreen
import io.github.visiongem.ledger.feature.account.list.AccountListScreen
import io.github.visiongem.ledger.feature.account.nav.AccountEditRoute
import io.github.visiongem.ledger.feature.account.nav.AccountListRoute

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // Must run BEFORE super.onCreate() to take effect before Window attach. See androidx.activity docs.
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            LedgerTheme {
                val backStack = remember { mutableStateListOf<Any>(AccountListRoute) }
                NavDisplay(
                    backStack = backStack,
                    onBack = { backStack.removeLastOrNull() },
                    entryProvider = { key ->
                        when (key) {
                            is AccountListRoute -> NavEntry(key) {
                                AccountListScreen(
                                    onAccountClick = { id ->
                                        backStack.add(AccountEditRoute(id))
                                    },
                                    onAddClick = {
                                        backStack.add(AccountEditRoute(null))
                                    },
                                )
                            }
                            is AccountEditRoute -> NavEntry(key) {
                                AccountEditScreen(
                                    accountId = key.accountId,
                                    onDone = { backStack.removeLastOrNull() },
                                )
                            }
                            else -> error("Unknown route: $key")
                        }
                    },
                )
            }
        }
    }
}
