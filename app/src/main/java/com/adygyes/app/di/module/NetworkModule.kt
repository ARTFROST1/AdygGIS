package com.adygyes.app.di.module

import com.adygyes.app.BuildConfig
import com.adygyes.app.data.remote.RetryInterceptor
import com.adygyes.app.data.remote.api.SupabaseApiService
import com.adygyes.app.data.remote.config.SupabaseConfig
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.Call
import okhttp3.Connection
import okhttp3.ConnectionPool
import okhttp3.Dns
import okhttp3.EventListener
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.dnsoverhttps.DnsOverHttps
import okhttp3.logging.HttpLoggingInterceptor
import java.io.IOException
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import timber.log.Timber
import java.net.Inet4Address
import java.net.InetAddress
import java.net.UnknownHostException
import java.util.concurrent.TimeUnit
import javax.inject.Qualifier
import javax.inject.Singleton

/**
 * Qualifier for Supabase-specific OkHttpClient
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class SupabaseClient

/**
 * Qualifier for Supabase auth interceptor
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class SupabaseInterceptor

/**
 * Hilt module for network-related dependencies (Retrofit, OkHttp, Supabase)
 */
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    
    /**
     * JSON parser configuration for kotlinx.serialization
     */
    @Provides
    @Singleton
    fun provideJson(): Json = Json {
        ignoreUnknownKeys = true  // Ignore unknown fields from API
        isLenient = true          // Allow lenient parsing
        coerceInputValues = true  // Coerce null to default values
        encodeDefaults = false    // Don't encode default values
    }
    
    /**
     * Supabase auth interceptor - adds required headers for API authentication
     */
    @Provides
    @Singleton
    @SupabaseInterceptor
    fun provideSupabaseInterceptor(): Interceptor = Interceptor { chain ->
        val originalRequest = chain.request()

        val builder = originalRequest.newBuilder()
            .header(SupabaseConfig.Headers.API_KEY, SupabaseConfig.ANON_KEY)
            .header(
                SupabaseConfig.Headers.CONTENT_TYPE,
                SupabaseConfig.HeaderValues.APPLICATION_JSON
            )

        val hasAuthorization = originalRequest.header(SupabaseConfig.Headers.AUTHORIZATION)
            ?.isNotBlank() == true
        if (!hasAuthorization) {
            builder.header(
                SupabaseConfig.Headers.AUTHORIZATION,
                "${SupabaseConfig.HeaderValues.BEARER_PREFIX}${SupabaseConfig.ANON_KEY}"
            )
        }

        val newRequest = builder.build()
        
        chain.proceed(newRequest)
    }
    
    /**
     * Logging interceptor for debug builds
     */
    @Provides
    @Singleton
    fun provideLoggingInterceptor(): HttpLoggingInterceptor {
        return HttpLoggingInterceptor().apply {
            redactHeader(SupabaseConfig.Headers.API_KEY)
            redactHeader(SupabaseConfig.Headers.AUTHORIZATION)
            level = if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.BODY
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
        }
    }
    
    /**
     * Custom DNS with fallback for better reliability on cellular networks
     */
    @Provides
    @Singleton
    fun provideDns(): Dns {
        val dohClient = OkHttpClient.Builder()
            // Important: keep DoH client on system DNS to avoid recursion.
            .dns(Dns.SYSTEM)
            .connectTimeout(5, TimeUnit.SECONDS)
            .readTimeout(5, TimeUnit.SECONDS)
            .writeTimeout(5, TimeUnit.SECONDS)
            .callTimeout(10, TimeUnit.SECONDS)
            .build()

        val doh = DnsOverHttps.Builder()
            .client(dohClient)
            .url("https://cloudflare-dns.com/dns-query".toHttpUrl())
            .post(true)
            .build()

        return object : Dns {
            override fun lookup(hostname: String): List<InetAddress> {
                fun sortForStability(addresses: List<InetAddress>): List<InetAddress> {
                    // Prefer IPv4 first to avoid stalls on some broken IPv6 cellular routes.
                    // Still returns IPv6 addresses as fallback.
                    return addresses.sortedWith(
                        compareBy<InetAddress> { it !is Inet4Address }
                            .thenBy { it.hostAddress ?: "" }
                    )
                }

                fun shouldUseDoh(host: String): Boolean {
                    // Supabase project URLs are typically behind Cloudflare.
                    // Some carrier DNS/APN setups return wrong/unreachable IPs for these hosts.
                    return host.endsWith(".supabase.co", ignoreCase = true)
                }

                return try {
                    // IMPORTANT: For Supabase, prefer DoH FIRST.
                    // This avoids carrier DNS returning bad IPs and causing first-attempt stalls/timeouts.
                    if (shouldUseDoh(hostname)) {
                        try {
                            val dohAddresses = sortForStability(doh.lookup(hostname))
                            if (BuildConfig.DEBUG) {
                                Timber.d(
                                    "🌐 DNS resolved (DoH) %s -> %s",
                                    hostname,
                                    dohAddresses.joinToString { it.hostAddress ?: it.toString() }
                                )
                            }
                            if (dohAddresses.isNotEmpty()) return dohAddresses
                        } catch (t: Throwable) {
                            if (BuildConfig.DEBUG) Timber.w(t, "🌐 DoH lookup failed for %s", hostname)
                        }
                    }

                    // Fallback to system DNS
                    val systemAddresses = sortForStability(Dns.SYSTEM.lookup(hostname))
                    if (BuildConfig.DEBUG) {
                        Timber.d(
                            "🌐 DNS resolved (system) %s -> %s",
                            hostname,
                            systemAddresses.joinToString { it.hostAddress ?: it.toString() }
                        )
                    }
                    systemAddresses
                } catch (e: UnknownHostException) {
                    // Fallback: try again after a short delay
                    Thread.sleep(500)
                    try {
                        if (shouldUseDoh(hostname)) {
                            try {
                                val dohAddresses = sortForStability(doh.lookup(hostname))
                                if (BuildConfig.DEBUG) {
                                    Timber.d(
                                        "🌐 DNS resolved (DoH retry) %s -> %s",
                                        hostname,
                                        dohAddresses.joinToString { it.hostAddress ?: it.toString() }
                                    )
                                }
                                if (dohAddresses.isNotEmpty()) return dohAddresses
                            } catch (t: Throwable) {
                                if (BuildConfig.DEBUG) Timber.w(t, "🌐 DoH lookup failed for %s", hostname)
                            }
                        }

                        val systemAddresses = sortForStability(Dns.SYSTEM.lookup(hostname))
                        if (BuildConfig.DEBUG) {
                            Timber.d(
                                "🌐 DNS resolved (system retry) %s -> %s",
                                hostname,
                                systemAddresses.joinToString { it.hostAddress ?: it.toString() }
                            )
                        }
                        systemAddresses
                    } catch (e2: UnknownHostException) {
                        throw e // Throw original exception
                    }
                }
            }
        }
    }
    
    /**
     * Event listener for detailed network diagnostics in debug builds
     */
    @Provides
    @Singleton
    fun provideEventListener(): EventListener {
        return if (BuildConfig.DEBUG) {
            object : EventListener() {
                private var callStartTime = 0L

                override fun callStart(call: Call) {
                    callStartTime = System.currentTimeMillis()
                    Timber.d("📞 Call started: ${call.request().url}")
                }

                override fun dnsStart(call: Call, domainName: String) {
                    Timber.d("🔍 DNS lookup started: $domainName")
                }

                override fun dnsEnd(call: Call, domainName: String, inetAddressList: List<java.net.InetAddress>) {
                    val ips = inetAddressList.take(3).joinToString { it.hostAddress ?: "?" }
                    Timber.d("✅ DNS resolved: $domainName -> $ips (${inetAddressList.size} total)")
                }

                override fun connectStart(call: Call, inetSocketAddress: java.net.InetSocketAddress, proxy: java.net.Proxy) {
                    Timber.d("🔌 Connecting to: ${inetSocketAddress.address.hostAddress}:${inetSocketAddress.port}")
                }

                override fun secureConnectStart(call: Call) {
                    Timber.d("🔒 TLS handshake started")
                }

                override fun secureConnectEnd(call: Call, handshake: okhttp3.Handshake?) {
                    Timber.d("✅ TLS handshake complete: ${handshake?.tlsVersion}")
                }

                override fun connectEnd(call: Call, inetSocketAddress: java.net.InetSocketAddress, proxy: java.net.Proxy, protocol: Protocol?) {
                    val elapsed = System.currentTimeMillis() - callStartTime
                    Timber.d("✅ Connected in ${elapsed}ms: $protocol")
                }

                override fun connectFailed(call: Call, inetSocketAddress: java.net.InetSocketAddress, proxy: java.net.Proxy, protocol: Protocol?, ioe: IOException) {
                    val elapsed = System.currentTimeMillis() - callStartTime
                    Timber.w("❌ Connect failed after ${elapsed}ms to ${inetSocketAddress.address.hostAddress}: ${ioe.message}")
                }

                override fun callFailed(call: Call, ioe: IOException) {
                    val elapsed = System.currentTimeMillis() - callStartTime
                    Timber.e("❌ Call failed after ${elapsed}ms: ${ioe.javaClass.simpleName} - ${ioe.message}")
                }

                override fun callEnd(call: Call) {
                    val elapsed = System.currentTimeMillis() - callStartTime
                    Timber.d("✅ Call completed in ${elapsed}ms")
                }
            }
        } else {
            EventListener.NONE
        }
    }

    /**
     * Retry interceptor for automatic retry with exponential backoff
     * Tuned for cellular: fewer retries, longer per-attempt timeouts
     */
    @Provides
    @Singleton
    fun provideRetryInterceptor(): RetryInterceptor {
        return RetryInterceptor(
            maxRetries = 1,              // Single retry - rely on OkHttp timeouts instead
            initialDelayMs = 3000L,      // 3s delay before retry
            maxDelayMs = 3000L,
            backoffMultiplier = 1.0
        )
    }
    
    /**
     * Connection pool for reusing connections
     */
    @Provides
    @Singleton
    fun provideConnectionPool(): ConnectionPool {
        return ConnectionPool(
            maxIdleConnections = 5,
            keepAliveDuration = 30,
            timeUnit = TimeUnit.SECONDS
        )
    }
    
    /**
     * OkHttp client configured for Supabase API with:
     * - Increased timeouts for cellular networks
     * - Automatic retry with exponential backoff
     * - DNS fallback for unreliable networks
     * - Connection pooling for performance
     */
    @Provides
    @Singleton
    @SupabaseClient
    fun provideSupabaseOkHttpClient(
        @SupabaseInterceptor supabaseInterceptor: Interceptor,
        loggingInterceptor: HttpLoggingInterceptor,
        retryInterceptor: RetryInterceptor,
        eventListener: EventListener,
        dns: Dns,
        connectionPool: ConnectionPool
    ): OkHttpClient {
        return OkHttpClient.Builder()
            // Interceptors order matters:
            // 1. Retry logic (outermost)
            .addInterceptor(retryInterceptor)
            // 2. Auth headers
            .addInterceptor(supabaseInterceptor)
            // 3. Logging (innermost, after all modifications)
            .addInterceptor(loggingInterceptor)
            
            // Event listener for detailed diagnostics
            .eventListener(eventListener)
            
            // Optimized timeouts for cellular networks
            // Connect timeout: Fast failure on dead TCP connections
            .connectTimeout(15, TimeUnit.SECONDS)
            // Read timeout: INCREASED for slow cellular data transfer (40KB JSON can take time)
            .readTimeout(60, TimeUnit.SECONDS)      // 60s to handle slow cellular data transfer
            // Write timeout: Requests are small, keep short
            .writeTimeout(15, TimeUnit.SECONDS)
            // Call timeout: Total time budget for request+response+retry
            .callTimeout(90, TimeUnit.SECONDS)      // 90s total (allows 1 full retry cycle)
            
            // DNS with fallback
            .dns(dns)
            
            // Connection pooling
            .connectionPool(connectionPool)
            
            // Retry on connection failure
            .retryOnConnectionFailure(true)
            
            .build()
    }
    
    /**
     * Retrofit instance for Supabase REST API
     */
    @Provides
    @Singleton
    fun provideRetrofit(
        @SupabaseClient okHttpClient: OkHttpClient,
        json: Json
    ): Retrofit {
        return Retrofit.Builder()
            .baseUrl(SupabaseConfig.BASE_URL + "/") // Ensure trailing slash
            .client(okHttpClient)
            .addConverterFactory(
                json.asConverterFactory("application/json".toMediaType())
            )
            .build()
    }
    
    /**
     * Supabase API service
     */
    @Provides
    @Singleton
    fun provideSupabaseApiService(retrofit: Retrofit): SupabaseApiService {
        return retrofit.create(SupabaseApiService::class.java)
    }
}
