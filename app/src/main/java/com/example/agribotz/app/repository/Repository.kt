package com.example.agribotz.app.repository

import com.example.agribotz.R
import com.example.agribotz.app.domain.ApiResult
import com.example.agribotz.app.domain.ErrorResponse
import com.example.agribotz.app.domain.GPS
import com.example.agribotz.app.domain.Variable
import com.example.agribotz.app.network.AddSiteRequest
import com.example.agribotz.app.network.AddSiteResponse
import com.example.agribotz.app.network.DeleteSiteRequest
import com.example.agribotz.app.network.DeleteSiteResponse
import com.example.agribotz.app.network.GadgetGpsRequest
import com.example.agribotz.app.network.GadgetGpsResponse
import com.example.agribotz.app.network.GadgetInfoRequest
import com.example.agribotz.app.network.GadgetInfoResponse
import com.example.agribotz.app.network.GetSitesRequest
import com.example.agribotz.app.network.GetSitesResponse
import com.example.agribotz.app.network.LoginRequest
import com.example.agribotz.app.network.LoginResponse
import com.example.agribotz.app.network.NetworkApi
import com.example.agribotz.app.network.RenameGadgetRequest
import com.example.agribotz.app.network.RenameGadgetResponse
import com.example.agribotz.app.network.RenameSiteRequest
import com.example.agribotz.app.network.RenameSiteResponse
import com.example.agribotz.app.network.SiteInfoRequest
import com.example.agribotz.app.network.SiteInfoResponse
import com.example.agribotz.app.network.UpdateVariableRequest
import com.example.agribotz.app.network.UpdateVariableResponse
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

class Repository {

    private val moshi: Moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val errorAdapter: JsonAdapter<ErrorResponse> = moshi.adapter(ErrorResponse::class.java)


    suspend fun login(mobileNumber: String, password: String): ApiResult<LoginResponse> {
        return try {
            val response = NetworkApi.services.loginAsync(LoginRequest(mobileNumber, password))

            if (response.isSuccessful) {
                response.body()?.let { body ->
                    ApiResult.Success(body)
                } ?: ApiResult.Error(
                    devMessage = "Empty response body",
                    userMessageKey = R.string.Error_Something_Wrong
                )
            }
            else {
                val errorJson = response.errorBody()?.string()
                val parsedError = errorJson?.let {
                    try {
                        errorAdapter.fromJson(it)?.error
                    }
                    catch (e: Exception) {
                        null
                    }
                }

                ApiResult.Error(
                    devMessage = parsedError ?: "HTTP ${response.code()} ${response.message()}",
                    userMessageKey = R.string.Error_Something_Wrong,
                    userMessageString = parsedError?.takeIf { it.isNotBlank() } // only use real backend messages
                )
            }
        }
        catch (e: java.io.IOException) {
            ApiResult.Error(
                devMessage = "Network error: ${e.localizedMessage}",
                userMessageKey = R.string.Error_Internet_Connection
            )
        }
        catch (e: Exception) {
            ApiResult.Error(
                devMessage = "Unexpected error: ${e.localizedMessage}",
                userMessageKey = R.string.Error_Something_Wrong
            )
        }
    }

    suspend fun getSites(token: String, userId: String? = null): ApiResult<GetSitesResponse> {
        return try {
            val response = NetworkApi.services.getSitesAsync(token, GetSitesRequest(userId))

            if (response.isSuccessful) {
                response.body()?.let { body ->
                    ApiResult.Success(body)
                } ?: ApiResult.Error(
                    devMessage = "Empty response body",
                    userMessageKey = R.string.Error_Something_Wrong
                )
            }
            else {
                val errorJson = response.errorBody()?.string()
                val parsedError = errorJson?.let {
                    try {
                        errorAdapter.fromJson(it)?.error
                    }
                    catch (e: Exception) {
                        null
                    }
                }

                ApiResult.Error(
                    devMessage = parsedError ?: "HTTP ${response.code()} ${response.message()}",
                    userMessageKey = R.string.Error_Something_Wrong,
                    userMessageString = parsedError?.takeIf { it.isNotBlank() } // only use real backend messages
                )
            }
        }
        catch (e: java.io.IOException) {
            ApiResult.Error(
                devMessage = "Network error: ${e.localizedMessage}",
                userMessageKey = R.string.Error_Internet_Connection
            )
        }
        catch (e: Exception) {
            ApiResult.Error(
                devMessage = "Unexpected error: ${e.localizedMessage}",
                userMessageKey = R.string.Error_Something_Wrong
            )
        }
    }

    suspend fun addSite(token: String, siteName: String): ApiResult<AddSiteResponse> {
        return try {
            val response = NetworkApi.services.addSiteAsync(token, AddSiteRequest(siteName))

            if (response.isSuccessful) {
                response.body()?.let { body ->
                    ApiResult.Success(body)
                } ?: ApiResult.Error(
                    devMessage = "Empty response body",
                    userMessageKey = R.string.Error_Something_Wrong
                )
            }
            else {
                val errorJson = response.errorBody()?.string()
                val parsedError = errorJson?.let {
                    try {
                        errorAdapter.fromJson(it)?.error
                    }
                    catch (e: Exception) {
                        null
                    }
                }

                ApiResult.Error(
                    devMessage = parsedError ?: "HTTP ${response.code()} ${response.message()}",
                    userMessageKey = R.string.Error_Something_Wrong,
                    userMessageString = parsedError?.takeIf { it.isNotBlank() } // only use real backend messages
                )
            }
        }
        catch (e: java.io.IOException) {
            ApiResult.Error(
                devMessage = "Network error: ${e.localizedMessage}",
                userMessageKey = R.string.Error_Internet_Connection
            )
        }
        catch (e: Exception) {
            ApiResult.Error(
                devMessage = "Unexpected error: ${e.localizedMessage}",
                userMessageKey = R.string.Error_Something_Wrong
            )
        }
    }

    suspend fun deleteSite(token: String, siteId: String): ApiResult<DeleteSiteResponse> {
        return try {
            val response = NetworkApi.services.deleteSiteAsync(token, DeleteSiteRequest(siteId))

            if (response.isSuccessful) {
                response.body()?.let { body ->
                    ApiResult.Success(body)
                } ?: ApiResult.Error(
                    devMessage = "Empty response body",
                    userMessageKey = R.string.Error_Something_Wrong
                )
            }
            else {
                val errorJson = response.errorBody()?.string()
                val parsedError = errorJson?.let {
                    try {
                        errorAdapter.fromJson(it)?.error
                    }
                    catch (e: Exception) {
                        null
                    }
                }

                ApiResult.Error(
                    devMessage = parsedError ?: "HTTP ${response.code()} ${response.message()}",
                    userMessageKey = R.string.Error_Something_Wrong,
                    userMessageString = parsedError?.takeIf { it.isNotBlank() } // only use real backend messages
                )
            }
        }
        catch (e: java.io.IOException) {
            ApiResult.Error(
                devMessage = "Network error: ${e.localizedMessage}",
                userMessageKey = R.string.Error_Internet_Connection
            )
        }
        catch (e: Exception) {
            ApiResult.Error(
                devMessage = "Unexpected error: ${e.localizedMessage}",
                userMessageKey = R.string.Error_Something_Wrong
            )
        }
    }

    suspend fun renameSite(token: String, siteId: String, newName: String): ApiResult<RenameSiteResponse> {
        return try {
            val response = NetworkApi.services.renameSiteAsync(token, RenameSiteRequest(siteId, newName))

            if (response.isSuccessful) {
                response.body()?.let { body ->
                    ApiResult.Success(body)
                } ?: ApiResult.Error(
                    devMessage = "Empty response body",
                    userMessageKey = R.string.Error_Something_Wrong
                )
            }
            else {
                val errorJson = response.errorBody()?.string()
                val parsedError = errorJson?.let {
                    try {
                        errorAdapter.fromJson(it)?.error
                    }
                    catch (e: Exception) {
                        null
                    }
                }

                ApiResult.Error(
                    devMessage = parsedError ?: "HTTP ${response.code()} ${response.message()}",
                    userMessageKey = R.string.Error_Something_Wrong,
                    userMessageString = parsedError?.takeIf { it.isNotBlank() } // only use real backend messages
                )
            }
        }
        catch (e: java.io.IOException) {
            ApiResult.Error(
                devMessage = "Network error: ${e.localizedMessage}",
                userMessageKey = R.string.Error_Internet_Connection
            )
        }
        catch (e: Exception) {
            ApiResult.Error(
                devMessage = "Unexpected error: ${e.localizedMessage}",
                userMessageKey = R.string.Error_Something_Wrong
            )
        }
    }

    suspend fun siteInfo(token: String, siteId: String): ApiResult<SiteInfoResponse> {
        return try {
            val response = NetworkApi.services.siteInfoAsync(token, SiteInfoRequest(siteId))

            if (response.isSuccessful) {
                response.body()?.let { body ->
                    ApiResult.Success(body)
                } ?: ApiResult.Error(
                    devMessage = "Empty response body",
                    userMessageKey = R.string.Error_Something_Wrong
                )
            }
            else {
                val errorJson = response.errorBody()?.string()
                val parsedError = errorJson?.let {
                    try {
                        errorAdapter.fromJson(it)?.error
                    }
                    catch (e: Exception) {
                        null
                    }
                }

                ApiResult.Error(
                    devMessage = parsedError ?: "HTTP ${response.code()} ${response.message()}",
                    userMessageKey = R.string.Error_Something_Wrong,
                    userMessageString = parsedError?.takeIf { it.isNotBlank() } // only use real backend messages
                )
            }
        }
        catch (e: java.io.IOException) {
            ApiResult.Error(
                devMessage = "Network error: ${e.localizedMessage}",
                userMessageKey = R.string.Error_Internet_Connection
            )
        }
        catch (e: Exception) {
            ApiResult.Error(
                devMessage = "Unexpected error: ${e.localizedMessage}",
                userMessageKey = R.string.Error_Something_Wrong
            )
        }
    }

    suspend fun gadgetInfo(token: String, gadgetId: String): ApiResult<GadgetInfoResponse> {
        return try {
            val response = NetworkApi.services.gadgetInfoAsync(token, GadgetInfoRequest(gadgetId))

            if (response.isSuccessful) {
                response.body()?.let { body ->
                    ApiResult.Success(body)
                } ?: ApiResult.Error(
                    devMessage = "Empty response body",
                    userMessageKey = R.string.Error_Something_Wrong
                )
            }
            else {
                val errorJson = response.errorBody()?.string()
                val parsedError = errorJson?.let {
                    try {
                        errorAdapter.fromJson(it)?.error
                    }
                    catch (e: Exception) {
                        null
                    }
                }

                ApiResult.Error(
                    devMessage = parsedError ?: "HTTP ${response.code()} ${response.message()}",
                    userMessageKey = R.string.Error_Something_Wrong,
                    userMessageString = parsedError?.takeIf { it.isNotBlank() } // only use real backend messages
                )
            }
        }
        catch (e: java.io.IOException) {
            ApiResult.Error(
                devMessage = "Network error: ${e.localizedMessage}",
                userMessageKey = R.string.Error_Internet_Connection
            )
        }
        catch (e: Exception) {
            ApiResult.Error(
                devMessage = "Unexpected error: ${e.localizedMessage}",
                userMessageKey = R.string.Error_Something_Wrong
            )
        }
    }

    suspend fun renameGadget(token: String, gadgetId: String, newName: String): ApiResult<RenameGadgetResponse> {
        return try {
            val response = NetworkApi.services.renameGadgetAsync(token, RenameGadgetRequest(gadgetId, newName))

            if (response.isSuccessful) {
                response.body()?.let { body ->
                    ApiResult.Success(body)
                } ?: ApiResult.Error(
                    devMessage = "Empty response body",
                    userMessageKey = R.string.Error_Something_Wrong
                )
            }
            else {
                val errorJson = response.errorBody()?.string()
                val parsedError = errorJson?.let {
                    try {
                        errorAdapter.fromJson(it)?.error
                    }
                    catch (e: Exception) {
                        null
                    }
                }

                ApiResult.Error(
                    devMessage = parsedError ?: "HTTP ${response.code()} ${response.message()}",
                    userMessageKey = R.string.Error_Something_Wrong,
                    userMessageString = parsedError?.takeIf { it.isNotBlank() } // only use real backend messages
                )
            }
        }
        catch (e: java.io.IOException) {
            ApiResult.Error(
                devMessage = "Network error: ${e.localizedMessage}",
                userMessageKey = R.string.Error_Internet_Connection
            )
        }
        catch (e: Exception) {
            ApiResult.Error(
                devMessage = "Unexpected error: ${e.localizedMessage}",
                userMessageKey = R.string.Error_Something_Wrong
            )
        }
    }

    suspend fun updateGadgetGps(token: String, gadgetId: String, gps: GPS): ApiResult<GadgetGpsResponse> {
        return try {
            val response = NetworkApi.services.updateGadgetGpsAsync(token, GadgetGpsRequest(gadgetId, gps))

            if (response.isSuccessful) {
                response.body()?.let { body ->
                    ApiResult.Success(body)
                } ?: ApiResult.Error(
                    devMessage = "Empty response body",
                    userMessageKey = R.string.Error_Something_Wrong
                )
            }
            else {
                val errorJson = response.errorBody()?.string()
                val parsedError = errorJson?.let {
                    try {
                        errorAdapter.fromJson(it)?.error
                    }
                    catch (e: Exception) {
                        null
                    }
                }

                ApiResult.Error(
                    devMessage = parsedError ?: "HTTP ${response.code()} ${response.message()}",
                    userMessageKey = R.string.Error_Something_Wrong,
                    userMessageString = parsedError?.takeIf { it.isNotBlank() } // only use real backend messages
                )
            }
        }
        catch (e: java.io.IOException) {
            ApiResult.Error(
                devMessage = "Network error: ${e.localizedMessage}",
                userMessageKey = R.string.Error_Internet_Connection
            )
        }
        catch (e: Exception) {
            ApiResult.Error(
                devMessage = "Unexpected error: ${e.localizedMessage}",
                userMessageKey = R.string.Error_Something_Wrong
            )
        }
    }

    suspend fun updateVariable(token: String, variableId: String, value: Variable): ApiResult<UpdateVariableResponse> {
        return try {
            val response = NetworkApi.services.updateVariableAsync(token, UpdateVariableRequest(variableId, value))

            if (response.isSuccessful) {
                response.body()?.let { body ->
                    ApiResult.Success(body)
                } ?: ApiResult.Error(
                    devMessage = "Empty response body",
                    userMessageKey = R.string.Error_Something_Wrong
                )
            }
            else {
                val errorJson = response.errorBody()?.string()
                val parsedError = errorJson?.let {
                    try {
                        errorAdapter.fromJson(it)?.error
                    }
                    catch (e: Exception) {
                        null
                    }
                }

                ApiResult.Error(
                    devMessage = parsedError ?: "HTTP ${response.code()} ${response.message()}",
                    userMessageKey = R.string.Error_Something_Wrong,
                    userMessageString = parsedError?.takeIf { it.isNotBlank() } // only use real backend messages
                )
            }
        }
        catch (e: java.io.IOException) {
            ApiResult.Error(
                devMessage = "Network error: ${e.localizedMessage}",
                userMessageKey = R.string.Error_Internet_Connection
            )
        }
        catch (e: Exception) {
            ApiResult.Error(
                devMessage = "Unexpected error: ${e.localizedMessage}",
                userMessageKey = R.string.Error_Something_Wrong
            )
        }
    }
}