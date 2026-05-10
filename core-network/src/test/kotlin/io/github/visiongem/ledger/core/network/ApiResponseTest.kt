package io.github.visiongem.ledger.core.network

import com.google.common.truth.Truth.assertThat
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import java.io.IOException
import org.junit.jupiter.api.Test

class ApiResponseTest {

    private val moshi = Moshi.Builder().build()
    private val stringResponseType = Types.newParameterizedType(
        ApiResponse::class.java,
        String::class.java,
    )
    private val adapter = moshi.adapter<ApiResponse<String>>(stringResponseType)

    // ===== ApiResponse Moshi codec =====

    @Test
    fun deserializeSuccess() {
        val json = """{"code":0,"msg":"ok","data":"hello"}"""
        val response = adapter.fromJson(json)!!
        assertThat(response.code).isEqualTo(0)
        assertThat(response.msg).isEqualTo("ok")
        assertThat(response.data).isEqualTo("hello")
    }

    @Test
    fun deserializeFailureWithNullData() {
        val json = """{"code":500,"msg":"server error","data":null}"""
        val response = adapter.fromJson(json)!!
        assertThat(response.code).isEqualTo(500)
        assertThat(response.msg).isEqualTo("server error")
        assertThat(response.data).isNull()
    }

    @Test
    fun deserializeWithMissingMsg() {
        val json = """{"code":0,"data":"x"}"""
        val response = adapter.fromJson(json)!!
        assertThat(response.code).isEqualTo(0)
        assertThat(response.msg).isNull()
        assertThat(response.data).isEqualTo("x")
    }

    @Test
    fun roundTripSerialization() {
        val original = ApiResponse(code = 42, msg = "answer", data = "value")
        val json = adapter.toJson(original)
        val parsed = adapter.fromJson(json)!!
        assertThat(parsed).isEqualTo(original)
    }

    @Test
    fun successConstantIsZero() {
        assertThat(ApiResponse.SUCCESS_CODE).isEqualTo(0)
    }

    // ===== NetworkException hierarchy =====

    @Test
    fun apiExceptionExposesCodeAndMessage() {
        val ex = ApiException(code = 1001, message = "Invalid token")
        assertThat(ex.code).isEqualTo(1001)
        assertThat(ex.message).isEqualTo("Invalid token")
        assertThat(ex).isInstanceOf(NetworkException::class.java)
    }

    @Test
    fun ioFailureWrapsCause() {
        val cause = IOException("timeout")
        val ex = IoFailure(cause)
        assertThat(ex.cause).isSameInstanceAs(cause)
        assertThat(ex).isInstanceOf(NetworkException::class.java)
    }

    @Test
    fun parseFailureWrapsCause() {
        val cause = IllegalStateException("bad json")
        val ex = ParseFailure(cause)
        assertThat(ex.cause).isSameInstanceAs(cause)
        assertThat(ex).isInstanceOf(NetworkException::class.java)
    }
}
