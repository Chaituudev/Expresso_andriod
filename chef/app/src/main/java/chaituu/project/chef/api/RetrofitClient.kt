package chaituu.project.chef.api

import android.content.Context
import chaituu.project.chef.BuildConfig
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {
    private var token: String? = null

    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val httpClient: OkHttpClient
        get() {
            val logging = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            }

            val authInterceptor = Interceptor { chain ->
                var request = chain.request()
                token?.let {
                    request = request.newBuilder()
                        .addHeader("Authorization", "Bearer $it")
                        .build()
                }
                chain.proceed(request)
            }

            return OkHttpClient.Builder()
                .addInterceptor(logging)
                .addInterceptor(authInterceptor)
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build()
        }

    private val retrofit: Retrofit
        get() = Retrofit.Builder()
            .baseUrl(BuildConfig.BACKEND_BASE_URL)
            .client(httpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()

    val apiService: ApiService
        get() = retrofit.create(ApiService::class.java)

    fun setToken(newToken: String) {
        token = newToken
    }

    fun clearToken() {
        token = null
    }
}
