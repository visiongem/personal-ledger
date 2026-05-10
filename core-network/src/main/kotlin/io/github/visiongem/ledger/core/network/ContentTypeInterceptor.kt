package io.github.visiongem.ledger.core.network

import okhttp3.Interceptor
import okhttp3.Response

// Defensive header fallback: if a request carries a body whose RequestBody has no MediaType,
// stamp it as JSON. Retrofit's converter usually sets this; interceptor is the safety net for
// raw OkHttp callers (e.g. test fixtures, manual Request.Builder usage).
class ContentTypeInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val body = request.body
        val needsHeader = body != null &&
            body.contentType() == null &&
            request.header(HEADER_CONTENT_TYPE) == null
        val outbound = if (needsHeader) {
            request.newBuilder()
                .header(HEADER_CONTENT_TYPE, DEFAULT_JSON_CONTENT_TYPE)
                .build()
        } else {
            request
        }
        return chain.proceed(outbound)
    }

    private companion object {
        const val HEADER_CONTENT_TYPE = "Content-Type"
        const val DEFAULT_JSON_CONTENT_TYPE = "application/json; charset=utf-8"
    }
}
