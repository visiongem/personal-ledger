package io.github.visiongem.ledger.core.network

import com.squareup.moshi.JsonDataException
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import java.io.IOException
import java.lang.reflect.Type
import okhttp3.ResponseBody
import retrofit2.Converter
import retrofit2.Retrofit

// Decodes every response body as ApiResponse<T>: returns the unwrapped data on success,
// throws ApiException on a non-success code, ParseFailure on malformed JSON.
// Pair this with retrofit-converter-moshi only if you need raw (non-envelope) endpoints,
// in which case prefer a separate Retrofit instance.
class ApiResponseConverterFactory private constructor(
    private val moshi: Moshi,
    private val successCode: Int,
) : Converter.Factory() {

    override fun responseBodyConverter(
        type: Type,
        annotations: Array<out Annotation>,
        retrofit: Retrofit,
    ): Converter<ResponseBody, *> {
        val envelopeType = Types.newParameterizedType(ApiResponse::class.java, type)
        val envelopeAdapter = moshi.adapter<ApiResponse<Any?>>(envelopeType)
        return Converter<ResponseBody, Any?> { body ->
            val envelope = body.use { source ->
                try {
                    envelopeAdapter.fromJson(source.source())
                } catch (e: JsonDataException) {
                    throw ParseFailure(e)
                } catch (e: IOException) {
                    throw ParseFailure(e)
                }
            } ?: throw ParseFailure(IllegalStateException("Empty response body"))
            if (envelope.code == successCode) {
                envelope.data
            } else {
                throw ApiException(code = envelope.code, message = envelope.msg.orEmpty())
            }
        }
    }

    companion object {
        fun create(
            moshi: Moshi,
            successCode: Int = ApiResponse.SUCCESS_CODE,
        ): ApiResponseConverterFactory = ApiResponseConverterFactory(moshi, successCode)
    }
}
