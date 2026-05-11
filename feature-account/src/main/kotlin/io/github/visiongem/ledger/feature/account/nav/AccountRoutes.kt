package io.github.visiongem.ledger.feature.account.nav

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data object AccountListRoute : Parcelable

@Parcelize
data class AccountEditRoute(val accountId: Long?) : Parcelable
