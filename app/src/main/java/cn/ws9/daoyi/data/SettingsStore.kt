package cn.ws9.daoyi.data

import android.content.Context

data class AppSettings(
    val apiKey: String = "",
    val baseUrl: String = "https://api.deepseek.com/v1",
    val model: String = "deepseek-chat",
    val lambda: Double = 0.15,
    val applyEquationOfTime: Boolean = true,
    val ziHourShiftsDay: Boolean = true,
    val longitude: Double = 116.59,
    val placeName: String = "山东济宁",
    val age: Int = 35,
    val male: Boolean = true
)

class SettingsStore(context: Context) {

    private val prefs = context.applicationContext.getSharedPreferences("daoyi_settings", Context.MODE_PRIVATE)
    private val crypto = CryptoStore(context.applicationContext)

    /** 密钥库降级状态需要长期可见，而不只在「保存」那一刻提示。 */
    val fallbackNotice: String get() = crypto.fallbackNotice

    fun load(): AppSettings = runCatching {
        AppSettings(
            apiKey = crypto.read(),
            baseUrl = prefs.getString("base_url", "https://api.deepseek.com/v1").orEmpty()
                .ifBlank { "https://api.deepseek.com/v1" },
            model = prefs.getString("model", "deepseek-chat").orEmpty().ifBlank { "deepseek-chat" },
            lambda = prefs.getFloat("lambda", 0.15f).toDouble(),
            applyEquationOfTime = prefs.getBoolean("eot", true),
            ziHourShiftsDay = prefs.getBoolean("zi_shift", true),
            longitude = prefs.getFloat("longitude", 116.59f).toDouble(),
            placeName = prefs.getString("place", "山东济宁").orEmpty().ifBlank { "山东济宁" },
            age = prefs.getInt("age", 35),
            male = prefs.getBoolean("male", true)
        )
    }.getOrDefault(AppSettings())

    /** @return 空串表示保存成功；非空为需要展示给用户的安全/错误提示。 */
    fun save(s: AppSettings): String = runCatching {
        val notice = crypto.save(s.apiKey)
        prefs.edit()
            .putString("base_url", s.baseUrl)
            .putString("model", s.model)
            .putFloat("lambda", s.lambda.toFloat())
            .putBoolean("eot", s.applyEquationOfTime)
            .putBoolean("zi_shift", s.ziHourShiftsDay)
            .putFloat("longitude", s.longitude.toFloat())
            .putString("place", s.placeName)
            .putInt("age", s.age)
            .putBoolean("male", s.male)
            .apply()
        notice.ifBlank { crypto.fallbackNotice }
    }.getOrElse { e -> "⚠ 设置写入失败：${e.message ?: e.javaClass.simpleName}" }
}
