package io.github.zdyoyoyo.backendquiz.data.remote

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

sealed interface FetchResult {
    data class Fetched(val body: String, val etag: String?) : FetchResult
    /** HTTP 304：伺服器說「跟你上次拿的一樣」 */
    data object NotModified : FetchResult
    data class Failed(val reason: String) : FetchResult
}

/** 抽成介面，測試時可以換成假的實作，不必真的連網路。 */
interface BankRemoteSource {
    suspend fun fetch(etag: String?): FetchResult
}

class OkHttpBankRemoteSource(
    private val client: OkHttpClient,
    private val url: String,
) : BankRemoteSource {

    // suspend 函式 ≈ 可以暫停、不會卡住執行緒的方法。
    // withContext(Dispatchers.IO) 把阻塞式網路呼叫丟到 IO 執行緒池，避免卡住主執行緒（UI）。
    override suspend fun fetch(etag: String?): FetchResult = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url(url)
            .apply { if (etag != null) header("If-None-Match", etag) }
            .build()
        try {
            client.newCall(request).execute().use { response -> // use {} ≈ try-with-resources
                when {
                    response.code == 304 -> FetchResult.NotModified
                    response.isSuccessful -> FetchResult.Fetched(response.body!!.string(), response.header("ETag"))
                    else -> FetchResult.Failed("HTTP ${response.code}")
                }
            }
        } catch (e: Exception) {
            FetchResult.Failed(e.message ?: e.javaClass.simpleName)
        }
    }
}
