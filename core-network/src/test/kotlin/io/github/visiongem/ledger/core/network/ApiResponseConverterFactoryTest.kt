package io.github.visiongem.ledger.core.network

import com.google.common.truth.Truth.assertThat
import com.squareup.moshi.Moshi
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import retrofit2.Converter
import retrofit2.Retrofit

class ApiResponseConverterFactoryTest {

    private val moshi: Moshi = Moshi.Builder().build()
    private val retrofit: Retrofit = Retrofit.Builder()
        .baseUrl("https://example.invalid/")
        .addConverterFactory(ApiResponseConverterFactory.create(moshi))
        .build()
    private val jsonMedia = "application/json".toMediaType()

    private fun decode(json: String): String? {
        val body = json.toResponseBody(jsonMedia)
        val converter: Converter<ResponseBody, String?> =
            retrofit.responseBodyConverter(String::class.java, emptyArray())
        return converter.convert(body)
    }

    @Test
    fun successReturnsData() {
        val result = decode("""{"code":0,"msg":"ok","data":"hello"}""")
        assertThat(result).isEqualTo("hello")
    }

    @Test
    fun successWithNullDataReturnsNull() {
        val result = decode("""{"code":0,"msg":"ok","data":null}""")
        assertThat(result).isNull()
    }

    @Test
    fun businessFailureThrowsApiException() {
        val ex = assertThrows<ApiException> {
            decode("""{"code":1001,"msg":"unauthorized","data":null}""")
        }
        assertThat(ex.code).isEqualTo(1001)
        assertThat(ex.message).isEqualTo("unauthorized")
    }

    @Test
    fun businessFailureWithMissingMsgUsesEmptyString() {
        val ex = assertThrows<ApiException> {
            decode("""{"code":500,"data":null}""")
        }
        assertThat(ex.code).isEqualTo(500)
        assertThat(ex.message).isEqualTo("")
    }

    @Test
    fun malformedJsonThrowsParseFailure() {
        assertThrows<ParseFailure> { decode("not valid json at all") }
    }

    @Test
    fun customSuccessCodeRecognized() {
        val customRetrofit = Retrofit.Builder()
            .baseUrl("https://example.invalid/")
            .addConverterFactory(ApiResponseConverterFactory.create(moshi, successCode = 200))
            .build()
        val converter: Converter<ResponseBody, String?> =
            customRetrofit.responseBodyConverter(String::class.java, emptyArray())
        val body = """{"code":200,"msg":"ok","data":"x"}""".toResponseBody(jsonMedia)
        assertThat(converter.convert(body)).isEqualTo("x")
    }
}
