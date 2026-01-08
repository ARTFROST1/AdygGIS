package com.adygyes.app.data.remote

import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response
import timber.log.Timber
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.net.ssl.SSLException

/**
 * OkHttp interceptor that implements retry logic with exponential backoff
 * 
 * Retries network requests on transient failures:
 * - Connection timeouts
 * - DNS resolution failures
 * - SSL handshake errors
 * - Generic IOExceptions
 * 
 * Uses exponential backoff to avoid overwhelming the server/network.
 */
class RetryInterceptor(
    private val maxRetries: Int = 3,
    private val initialDelayMs: Long = 1000L,
    private val maxDelayMs: Long = 10000L,
    private val backoffMultiplier: Double = 2.0
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        var currentDelay = initialDelayMs
        var lastException: IOException? = null
        
        val startTime = System.currentTimeMillis()
        
        for (attempt in 0..maxRetries) {
            try {
                if (attempt > 0) {
                    Timber.d("🔄 Retry attempt $attempt/$maxRetries for ${request.url} (delay: ${currentDelay}ms)")
                    
                    // Exponential backoff delay
                    runBlocking {
                        delay(currentDelay)
                    }
                    
                    // Increase delay for next retry
                    currentDelay = (currentDelay * backoffMultiplier).toLong()
                        .coerceAtMost(maxDelayMs)
                } else {
                    Timber.d("📡 Initial request to ${request.url}")
                }
                
                val attemptStart = System.currentTimeMillis()
                val response = chain.proceed(request)
                val attemptDuration = System.currentTimeMillis() - attemptStart
                
                Timber.d("✅ Response received in ${attemptDuration}ms (code: ${response.code})")
                
                // If response is successful or client error (4xx), don't retry
                if (response.isSuccessful || response.code in 400..499) {
                    return response
                }
                
                // Server errors (5xx) - retry
                if (response.code >= 500 && attempt < maxRetries) {
                    Timber.w("⚠️ Server error ${response.code}, will retry...")
                    response.close()
                    continue
                }
                
                return response
                
            } catch (e: SocketTimeoutException) {
                lastException = e
                val elapsed = System.currentTimeMillis() - startTime
                Timber.w("⏱️ Request timeout after ${elapsed}ms (attempt ${attempt + 1}/${maxRetries + 1}): ${e.message}")
                if (attempt >= maxRetries) throw e
                
            } catch (e: UnknownHostException) {
                lastException = e
                Timber.w("🌐 DNS resolution failed (attempt ${attempt + 1}/${maxRetries + 1}): ${e.message}")
                if (attempt >= maxRetries) throw e
                
            } catch (e: SSLException) {
                lastException = e
                Timber.w("🔒 SSL error (attempt ${attempt + 1}/${maxRetries + 1}): ${e.message}")
                if (attempt >= maxRetries) throw e
                
            } catch (e: IOException) {
                lastException = e
                Timber.w("❌ Network error (attempt ${attempt + 1}/${maxRetries + 1}): ${e.message}")
                if (attempt >= maxRetries) throw e
            }
        }
        
        // All retries exhausted
        throw lastException ?: IOException("Request failed after $maxRetries retries")
    }
}
