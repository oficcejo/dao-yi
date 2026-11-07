package cn.ws9.daoyi.core.llm

import java.net.HttpURLConnection
import java.net.InetAddress
import java.net.Socket
import java.net.URL
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.SSLSocket
import javax.net.ssl.SSLSocketFactory

/**
 * 纯端侧直连 OpenAI 兼容接口（DeepSeek / 自定义 BaseURL）。
 * 零第三方依赖：HttpURLConnection + 纯 Kotlin JsonLite；API Key 只在本机内存与 Keystore 中。
 */
class DeepSeekClient(private val config: LlmConfig) {

    companion object {
        const val NO_KEY = "尚未配置 API Key。请在「设置」中填入 DeepSeek 或其他兼容服务的 Key；未配置时本工具只展示本地典籍参考卡。"
    }

    fun call(system: String, user: String): LlmOutcome {
        if (config.apiKey.isBlank()) return LlmOutcome.Failure(NO_KEY)
        var lastError = "未知错误"
        // 第一次强制 JSON 模式；部分兼容服务不支持 response_format，第二次降级重试
        for (attempt in 0..1) {
            try {
                val body = buildBody(system, user, jsonMode = attempt == 0)
                val response = post(body)
                val content = extractContent(response)
                return LlmOutcome.Success(PrescriptionParser.parse(content), content)
            } catch (e: Exception) {
                lastError = cleanError(e)
                if (attempt == 0) continue
            }
        }
        return LlmOutcome.Failure("模型调用失败：$lastError")
    }

    private fun buildBody(system: String, user: String, jsonMode: Boolean): String {
        val messages = listOf(
            mapOf("role" to "system", "content" to system),
            mapOf("role" to "user", "content" to user)
        )
        val body = LinkedHashMap<String, Any?>()
        body["model"] = config.model
        body["messages"] = messages
        body["temperature"] = config.temperature
        body["max_tokens"] = 3200
        body["stream"] = false
        if (jsonMode) body["response_format"] = mapOf("type" to "json_object")
        return JsonLite.stringify(body)
    }

    private fun extractContent(response: String): String {
        val root = JsonLite.asMap(JsonLite.parse(response))
        val choices = JsonLite.asList(root["choices"])
        val message = JsonLite.asMap(JsonLite.asMap(choices.firstOrNull())["message"])
        val content = JsonLite.text(message["content"])
        if (content.isBlank()) {
            val err = JsonLite.text(root["error"]).take(160)
            throw IllegalStateException("接口未返回 content${if (err.isBlank()) "" else "：$err"}")
        }
        return content
    }

    private fun post(body: String): String {
        val base = config.baseUrl.trim().trimEnd('/')
        val endpoint = if (base.endsWith("/chat/completions")) base else "$base/chat/completions"
        val conn = URL(endpoint).openConnection() as HttpURLConnection
        // Android 6/7 的部分设备没有默认启用 TLSv1.2，访问现代 HTTPS 接口会 SSLHandshakeException。
        // 只在缺失时补齐协议，保留设备原有协议（不移除 TLSv1.3）；失败则静默沿用系统默认。
        if (conn is HttpsURLConnection) {
            runCatching { conn.sslSocketFactory = Tls12CompatFactory(conn.sslSocketFactory) }
        }
        try {
            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8")
            conn.setRequestProperty("Authorization", "Bearer ${config.apiKey.trim()}")
            conn.setRequestProperty("Accept", "application/json")
            conn.connectTimeout = 15_000
            conn.readTimeout = config.timeoutSeconds * 1000
            conn.doOutput = true
            conn.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
            val code = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            val text = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
            if (code !in 200..299) throw IllegalStateException("HTTP $code ${briefError(text)}")
            return text
        } finally {
            conn.disconnect()
        }
    }

    /** HTTP 错误只保留可读的一句，不把整段响应体/转义 JSON 抛到界面上。 */
    private fun briefError(raw: String): String {
        val msg = runCatching {
            val root = JsonLite.asMap(JsonLite.parse(raw))
            val err = root["error"]
            JsonLite.text(JsonLite.asMap(err)["message"]).ifBlank { JsonLite.text(err) }
        }.getOrDefault("")
        val plain = msg.ifBlank {
            if (raw.trimStart().startsWith("{")) "响应体无可读错误信息" else raw
        }
        return cleanErrorText(plain, 100)
    }

    private fun cleanError(e: Exception): String =
        cleanErrorText(e.message ?: e.javaClass.simpleName, 200)

    private fun cleanErrorText(raw: String, limit: Int): String =
        raw.replace(Regex("<[^>]+>"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
            .take(limit)
            .ifBlank { "无详细信息" }
}

/**
 * 老设备 TLS 兼容包装：仅在 socket 支持的协议里补齐 TLSv1.2，
 * 不改变现代设备已有的 TLSv1.3，也不改动密码套件。
 */
private class Tls12CompatFactory(private val delegate: SSLSocketFactory) : SSLSocketFactory() {

    override fun getDefaultCipherSuites(): Array<String> = delegate.defaultCipherSuites
    override fun getSupportedCipherSuites(): Array<String> = delegate.supportedCipherSuites

    override fun createSocket(): Socket = patch(delegate.createSocket())
    override fun createSocket(host: String?, port: Int): Socket = patch(delegate.createSocket(host, port))
    override fun createSocket(host: String?, port: Int, localHost: InetAddress?, localPort: Int): Socket =
        patch(delegate.createSocket(host, port, localHost, localPort))
    override fun createSocket(host: InetAddress?, port: Int): Socket = patch(delegate.createSocket(host, port))
    override fun createSocket(address: InetAddress?, port: Int, localAddress: InetAddress?, localPort: Int): Socket =
        patch(delegate.createSocket(address, port, localAddress, localPort))
    override fun createSocket(s: Socket?, host: String?, port: Int, autoClose: Boolean): Socket =
        patch(delegate.createSocket(s, host, port, autoClose))

    private fun patch(socket: Socket): Socket {
        if (socket is SSLSocket) {
            runCatching {
                val supported = socket.supportedProtocols.toSet()
                val enabled = socket.enabledProtocols.toMutableList()
                for (protocol in listOf("TLSv1.1", "TLSv1.2")) {
                    if (protocol in supported && protocol !in enabled) enabled.add(protocol)
                }
                socket.enabledProtocols = enabled.toTypedArray()
            }
        }
        return socket
    }
}
