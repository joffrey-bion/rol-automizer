package org.hildan.bots.riseoflords.client

import java.net.URI
import java.net.URLEncoder
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.charset.StandardCharsets

private const val FAKE_USER_AGENT =
    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/89.0.4389.90 Safari/537.36"

fun HttpClient.get(baseUrl: String, page: String, configureUri: UriBuilder.() -> Unit = {}): String {
    val request = HttpRequest.newBuilder(uri(baseUrl, page, configureUri)).GET()
    return executeForText(request)
}

fun HttpClient.post(
    baseUrl: String,
    page: String,
    configureUri: UriBuilder.() -> Unit = {},
    configureBody: PostDataBuilder.() -> Unit,
): String {
    val request = HttpRequest.newBuilder(uri(baseUrl, page, configureUri))
        .POST(HttpRequest.BodyPublishers.ofString(PostDataBuilder().apply(configureBody).build()))
        .header("Content-Type", "application/x-www-form-urlencoded")
    return executeForText(request)
}

class PostDataBuilder {
    private val formParams: MutableList<Pair<String, String>> = ArrayList()

    fun formParam(key: String, value: String) {
        formParams.add(key to value)
    }

    fun build() = formParams.urlEncodedString()
}

private fun List<Pair<String, String>>.urlEncodedString() = joinToString("&") { (key, value) ->
    buildString {
        append(URLEncoder.encode(key, StandardCharsets.UTF_8))
        append("=")
        append(URLEncoder.encode(value, StandardCharsets.UTF_8))
    }
}

private fun HttpClient.executeForText(requestBuilder: HttpRequest.Builder): String {
    val request = requestBuilder.header("User-Agent", FAKE_USER_AGENT).build()
    val response = send(request, HttpResponse.BodyHandlers.ofString())
    return when (response.statusCode()) {
        in 200..299 -> response.body() ?: error("No response body for $request")
        else -> error("Non-OK response status (${response.statusCode()}) for $request")
    }
}

private fun uri(baseUrl: String, page: String, configure: UriBuilder.() -> Unit) =
    UriBuilder(baseUrl).queryParam("p", page).apply(configure).build()

class UriBuilder(private val baseUrl: String) {
    private val queryParams: MutableList<Pair<String, String>> = ArrayList()

    fun queryParam(key: String, value: String): UriBuilder {
        queryParams.add(key to value)
        return this
    }

    fun build(): URI = URI("$baseUrl?${queryParams.urlEncodedString()}")
}
