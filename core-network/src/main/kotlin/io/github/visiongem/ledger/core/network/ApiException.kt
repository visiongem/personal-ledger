package io.github.visiongem.ledger.core.network

sealed class NetworkException(
    message: String? = null,
    cause: Throwable? = null,
) : RuntimeException(message, cause)

class ApiException(
    val code: Int,
    message: String,
) : NetworkException(message = message)

class IoFailure(cause: Throwable) : NetworkException(cause = cause)

class ParseFailure(cause: Throwable) : NetworkException(cause = cause)
