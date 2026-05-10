package io.github.visiongem.ledger.core.network

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ApiResponse<T>(
    val code: Int,
    val msg: String? = null,
    val data: T? = null,
) {
    val isSuccess: Boolean
        get() = code == SUCCESS_CODE

    companion object {
        const val SUCCESS_CODE = 0
    }
}
