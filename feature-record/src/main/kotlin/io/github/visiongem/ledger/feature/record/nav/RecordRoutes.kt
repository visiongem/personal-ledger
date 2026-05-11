package io.github.visiongem.ledger.feature.record.nav

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data object RecordListRoute : Parcelable

@Parcelize
data class RecordEditRoute(val recordId: Long?) : Parcelable
