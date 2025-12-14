package com.example.agribotz.app.network

import android.annotation.SuppressLint
import com.example.agribotz.app.domain.Variable
import com.squareup.moshi.FromJson
import com.squareup.moshi.Moshi
import com.squareup.moshi.ToJson
import com.squareup.moshi.adapters.PolymorphicJsonAdapterFactory
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

private const val BASE_URL = "https://dev.agribotz.com:3001/"

private val moshi = Moshi.Builder()
    .add(CustomDateAdapter)
    .add(VariablePolymorphicAdapter.factory)
    .addLast(KotlinJsonAdapterFactory())
    .build()

val logging =  HttpLoggingInterceptor()
    .setLevel(HttpLoggingInterceptor.Level.BODY)

val httpClient = OkHttpClient.Builder()
    .addInterceptor(logging)
    .addInterceptor { chain ->
        val locale = Locale.getDefault().language
        val original = chain.request()
        val request = original.newBuilder()
            .header("Accept-Language", locale)
            .method(original.method, original.body)
            .build()
        chain.proceed(request)
    }

private val retrofit = Retrofit.Builder()
    .addConverterFactory(MoshiConverterFactory.create(moshi))
    //Enabling Retrofit to produce a Coroutines-based API that's used instead of Callback in the ViewModel
    .baseUrl(BASE_URL)
    .client(httpClient.build())
    .build()

object NetworkApi {
    val services by lazy<Services> {
        retrofit.create(Services::class.java)
    }
}

interface Services {
    @POST("users/login")
    suspend fun loginAsync(@Body loginRequest: LoginRequest): Response<LoginResponse>

    @POST("api/iotCloud/get-user-sites")
    suspend fun getSitesAsync(
        @Header("Authorization") token: String,
        @Body getSitesRequest: GetSitesRequest
    ): Response<GetSitesResponse>

    @POST("api/iotCloud/add-site")
    suspend fun addSiteAsync(
        @Header("Authorization") token: String,
        @Body addSiteRequest: AddSiteRequest
    ): Response<AddSiteResponse>

    @POST("api/iotCloud/delete-site")
    suspend fun deleteSiteAsync(
        @Header("Authorization") token: String,
        @Body deleteSiteRequest: DeleteSiteRequest
    ): Response<DeleteSiteResponse>

    @POST("api/iotCloud/rename-site")
    suspend fun renameSiteAsync(
        @Header("Authorization") token: String,
        @Body renameSiteRequest: RenameSiteRequest
    ): Response<RenameSiteResponse>

    @POST("api/iotCloud/get-site-info")
    suspend fun siteInfoAsync(
        @Header("Authorization") token: String,
        @Body siteInfoRequest: SiteInfoRequest
    ): Response<SiteInfoResponse>

    @POST("api/iotCloud/get-gadget-info")
    suspend fun gadgetInfoAsync(
        @Header("Authorization") token: String,
        @Body gadgetInfoRequest: GadgetInfoRequest
    ): Response<GadgetInfoResponse>

    @POST("api/iotCloud/rename-gadget")
    suspend fun renameGadgetAsync(
        @Header("Authorization") token: String,
        @Body renameGadgetRequest: RenameGadgetRequest
    ): Response<RenameGadgetResponse>

    @POST("api/iotCloud/update-gadget-gps")
    suspend fun gadgetGpsAsync(
        @Header("Authorization") token: String,
        @Body gadgetGpsRequest: GadgetGpsRequest
    ): Response<GadgetGpsResponse>

    @POST("api/iotCloud/update-variable")
    suspend fun updateVariableAsync(
        @Header("Authorization") token: String,
        @Body updateVariableRequest: UpdateVariableRequest
    ): Response<UpdateVariableResponse>
}

object CustomDateAdapter {
    @SuppressLint("SimpleDateFormat")
    var dateFormat: SimpleDateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")

    @ToJson
    @Synchronized
    fun dateToJson(d: Date): String? {
        return dateFormat.format(d)
    }

    @FromJson
    @Synchronized
    @Throws(ParseException::class)
    fun dateFromJson(s: String): Date? {
        return dateFormat.parse(s)
    }

    init {
        dateFormat.timeZone = TimeZone.getTimeZone("GMT")
    }
}

object VariablePolymorphicAdapter {
    val factory: PolymorphicJsonAdapterFactory<Variable> =
        PolymorphicJsonAdapterFactory.of(Variable::class.java, "type")
            .withSubtype(Variable.IntegerVar::class.java, "integer")
            .withSubtype(Variable.FloatVar::class.java, "float")
            .withSubtype(Variable.StringVar::class.java, "string")
            .withSubtype(Variable.BooleanVar::class.java, "boolean")
            .withSubtype(Variable.ScheduleVar::class.java, "schedule")
}
