package com.example.agribotz.app.network

import android.util.Log
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.sse.EventSource
import okhttp3.sse.EventSourceListener
import okhttp3.sse.EventSources
import java.net.URLEncoder
import java.util.Locale
import java.util.concurrent.TimeUnit

class GadgetVariableEventsClient {

    private var eventSource: EventSource? = null

    private val moshi: Moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val eventAdapter = moshi.adapter(VariableUpdateEvent::class.java)

    private val sseClient: OkHttpClient = OkHttpClient.Builder()
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .addInterceptor { chain ->
            val locale = Locale.getDefault().language
            val original = chain.request()
            val request = original.newBuilder()
                .header("Accept-Language", locale)
                .method(original.method, original.body)
                .build()
            chain.proceed(request)
        }
        .build()

    fun connect(
        token: String,
        variableIds: List<String>,
        onVariableUpdate: (VariableUpdateEvent) -> Unit,
        onError: (Throwable?) -> Unit
    ) {
        disconnect()

        if (variableIds.isEmpty()) return

        val encodedIds = variableIds.joinToString(",") {
            URLEncoder.encode(it, "UTF-8")
        }

        val request = Request.Builder()
            .url("${BASE_URL}api/iotCloud/variable-events?variableIds=$encodedIds")
            .addHeader("Authorization", token)
            .addHeader("Accept", "text/event-stream")
            .build()

        eventSource = EventSources.createFactory(sseClient)
            .newEventSource(
                request,
                object : EventSourceListener() {

                    override fun onOpen(eventSource: EventSource, response: Response) {
                        Log.d("GadgetVariableEvents", "SSE connected")
                    }

                    override fun onEvent(
                        eventSource: EventSource,
                        id: String?,
                        type: String?,
                        data: String
                    ) {
                        if (type != "variable-update") return

                        try {
                            eventAdapter.fromJson(data)?.let { event ->
                                onVariableUpdate(event)
                            }
                        } catch (e: Exception) {
                            Log.e("GadgetVariableEvents", "Failed to parse SSE event", e)
                        }
                    }

                    override fun onClosed(eventSource: EventSource) {
                        Log.d("GadgetVariableEvents", "SSE closed")
                    }

                    override fun onFailure(
                        eventSource: EventSource,
                        t: Throwable?,
                        response: Response?
                    ) {
                        Log.e("GadgetVariableEvents", "SSE failed", t)
                        onError(t)
                    }
                }
            )
    }

    fun disconnect() {
        eventSource?.cancel()
        eventSource = null
    }
}