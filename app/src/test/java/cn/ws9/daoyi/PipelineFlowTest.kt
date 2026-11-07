package cn.ws9.daoyi

import cn.ws9.daoyi.core.llm.DeepSeekClient
import cn.ws9.daoyi.core.llm.JsonLite
import cn.ws9.daoyi.core.llm.LlmConfig
import cn.ws9.daoyi.core.pipeline.DaoYiPipeline
import cn.ws9.daoyi.core.pipeline.PipelineRequest
import cn.ws9.daoyi.core.pipeline.PipelineResult
import cn.ws9.daoyi.core.pipeline.PipelineSettings
import cn.ws9.daoyi.core.pipeline.RunKind
import cn.ws9.daoyi.core.tcm.PersonProfile
import com.sun.net.httpserver.HttpServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test
import java.io.File
import java.net.InetSocketAddress
import java.time.LocalDate
import java.time.LocalTime
import java.util.concurrent.atomic.AtomicInteger

/**
 * 全流程回归：本地起一个 OpenAI 兼容假服务，真实走
 * 症候抽取 → 红旗拒答 → 排盘 → 贝叶斯 → HTTP 请求 → JSON 归一化 → 安全闸门 → 结果结构。
 * 每例的中间结果写入 build/reports/daoyi-flow.txt，便于逐环节核对。
 */
class PipelineFlowTest {

    private lateinit var server: HttpServer
    private val responseBody = java.util.concurrent.atomic.AtomicReference("")
    private val statusCode = AtomicInteger(200)
    private val requestCount = AtomicInteger(0)

    @Volatile
    private var lastRequestBody = ""

    @Before
    fun setUp() {
        requestCount.set(0)
        lastRequestBody = ""
        statusCode.set(200)
        server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server.createContext("/v1/chat/completions") { ex ->
            requestCount.incrementAndGet()
            lastRequestBody = ex.requestBody.readBytes().toString(Charsets.UTF_8)
            val bytes = responseBody.get().toByteArray(Charsets.UTF_8)
            ex.sendResponseHeaders(statusCode.get(), bytes.size.toLong())
            ex.responseBody.use { it.write(bytes) }
            ex.close()
        }
        server.start()
    }

    @After
    fun tearDown() {
        server.stop(0)
    }

    private fun url() = "http://127.0.0.1:${server.address.port}/v1"

    private fun chat(content: String): String = JsonLite.stringify(
        mapOf("choices" to listOf(mapOf("message" to mapOf("content" to content))))
    )

    private fun runCase(
        content: String,
        apiKey: String = "sk-test",
        code: Int = 200,
        symptoms: String = "恶寒 发热 无汗 头痛 脉浮",
        profile: PersonProfile = PersonProfile(age = 36)
    ): PipelineResult {
        responseBody.set(
            if (code == 200) chat(content)
            else """{"error":{"message":"invalid api key"}}"""
        )
        statusCode.set(code)
        val s = PipelineSettings(
            apiKey = apiKey, baseUrl = url(), model = "test-model",
            lambda = 0.15, applyEquationOfTime = true, ziHourShiftsDay = true
        )
        val req = PipelineRequest(
            date = LocalDate.of(1990, 5, 15),
            time = LocalTime.of(10, 30),
            longitude = 116.59,
            placeName = "济宁",
            male = true,
            symptomText = symptoms,
            selected = emptyList(),
            profile = profile,
            referenceYear = 2026,
            settings = s
        )
        return DaoYiPipeline.run(req) { system, user ->
            DeepSeekClient(
                LlmConfig(
                    apiKey = s.apiKey, baseUrl = s.baseUrl, model = s.model, timeoutSeconds = 5
                )
            ).call(system, user)
        }
    }

    private fun check(case: String, r: PipelineResult) {
        // 任何情况下主输出与降级参考卡都不能同时出现（这正是「输出串到参考卡下面」的根因）
        assertFalse("[$case] 主输出与降级卡互斥", r.prescription != null && r.fallback != null)
        log("【$case】")
        log("  状态   : ${r.kind} · ${r.status.headline}")
        log("  原因   : ${r.status.reason}")
        log("  排盘   : ${r.chart?.pillars?.joinToString(" ") { it.name } ?: "未执行"} / 日主 ${r.chart?.dayMaster ?: "-"}")
        log("  节气   : ${r.chart?.solarTermName ?: "-"}")
        log("  运气   : ${r.chart?.yunqi?.ganZhi ?: "-"} ${r.chart?.yunqi?.siTian ?: ""}")
        log("  证型   : ${r.bayes?.ranked?.firstOrNull()?.syndrome?.name ?: "-"} " +
                "后验 ${r.bayes?.ranked?.firstOrNull()?.probability?.let { (it * 100).toInt() } ?: "-"}% " +
                "/ 证据偏弱=${r.bayes?.lowConfidence ?: "-"}")
        log("  主输出 : " + (r.prescription?.let { "${it.formulaName} / ${it.herbs.size} 味 / 中成药 ${it.patentMedicines.size} 条" } ?: "无"))
        log("  降级卡 : ${r.fallback?.name ?: "无"}")
        log("  闸门   : ${r.safety?.warnings?.size ?: 0} 条警告 / blocked=${r.safety?.blocked ?: false}")
        log("  拒答   : ${r.refusal ?: "无"}")
        log("  详情   : ${r.status.technical.take(140).ifBlank { "-" }}")
        log("  请求数 : ${requestCount.get()}")
        log("")
    }

    private val STANDARD = """
        {"syndrome":"太阳伤寒 · 表实无汗","therapy":"发汗解表、宣肺平喘",
         "formulaName":"麻黄汤","source":"《伤寒论》35条",
         "herbs":[{"name":"麻黄","grams":9,"processing":"生","note":"药典常用量"},
                  {"name":"桂枝","grams":6},{"name":"苦杏仁","grams":9},{"name":"甘草","grams":3}],
         "decoction":"麻黄先煎去沫，温服，汗出即停后服",
         "patentMedicines":["感冒清热颗粒（风寒表证 / 风热不宜）"],
         "diet":["宜温热易消化"],"lifestyle":["避风保暖，微汗为度"],
         "exercise":"八段锦","emotional":"静养少怒","seasonal":"冬令避寒",
         "references":["《伤寒论》35条"],"cautions":["非处方，须线下就医"],"confidence":"中"}
    """.trimIndent()

    @Test
    fun `01 标准 JSON 全流程返回主输出`() {
        val r = runCase(STANDARD)
        check("01 标准 JSON", r)
        assertEquals(RunKind.MODEL_OK, r.kind)
        assertNotNull(r.chart)
        assertNotNull(r.bayes)
        assertNotNull(r.safety)
        assertNotNull(r.prescription)
        assertNull(r.fallback)
        assertTrue(r.status.modelUsed)
        assertEquals("麻黄汤", r.prescription!!.formulaName)
        assertEquals(4, r.prescription!!.herbs.size)
        // 「恶寒 发热 无汗 头痛」必须判为太阳伤寒，而非外寒内饮（旧版组内均值 bug 曾判错）
        assertEquals("S02", r.bayes!!.ranked.first().syndrome.id)
        assertNotNull(r.historyRecord)
        assertEquals(1, requestCount.get())
        // 请求体带证据链，但不带原始生辰
        assertTrue(lastRequestBody.contains("恶寒"))
        assertTrue(lastRequestBody.contains("response_format"))
        assertFalse("生辰原文不应上云", lastRequestBody.contains("1990-05-15"))
    }

    @Test
    fun `02 中文键 代码块 字符串药数组仍能解析`() {
        val content = "```json\n" + """
            {"证型":"太阳伤寒","治法":"发汗解表","方名":"麻黄汤",
             "药物":["麻黄 9g","桂枝 6g","苦杏仁 9g","炙甘草 3g"],
             "煎服法":"先煎麻黄","中成药":["感冒清热颗粒"],
             "饮食":["宜温热"],"条文依据":["《伤寒论》35条"],"置信度":"中"}
        """.trimIndent() + "\n```"
        val r = runCase(content)
        check("02 中文键 / 代码块 / 字符串药数组", r)
        assertEquals(RunKind.MODEL_OK, r.kind)
        assertEquals("麻黄汤", r.prescription!!.formulaName)
        assertEquals(4, r.prescription!!.herbs.size)
        assertEquals("麻黄", r.prescription!!.herbs.first().name)
        assertEquals(9.0, r.prescription!!.herbs.first().grams, 0.001)
    }

    @Test
    fun `03 对象数组中成药不再吐出 JSON 文本`() {
        val content = """
            {"方名":"桂枝汤","herbs":[{"name":"桂枝","grams":9}],
             "patentMedicines":[{"name":"桂枝颗粒"},{"name":"玉屏风颗粒"}],
             "references":[{"title":"《伤寒论》12条"}]}
        """.trimIndent()
        val r = runCase(content)
        check("03 对象数组中成药", r)
        assertEquals(RunKind.MODEL_OK, r.kind)
        assertEquals(2, r.prescription!!.patentMedicines.size)
        assertFalse(r.prescription!!.patentMedicines.joinToString().contains("{"))
        assertTrue(r.prescription!!.references.any { it.contains("伤寒论") })
    }

    @Test
    fun `04 超药典剂量自动校正`() {
        val content = """
            {"formulaName":"麻黄汤","herbs":[{"name":"麻黄","grams":46.8},{"name":"桂枝","grams":6}]}
        """.trimIndent()
        val r = runCase(content)
        check("04 超剂量校正", r)
        assertEquals(RunKind.MODEL_OK, r.kind)
        val mahuang = r.prescription!!.herbs.first { it.name == "麻黄" }
        assertEquals(10.0, mahuang.grams, 0.001)
        assertTrue(r.safety!!.warnings.any { it.contains("超过药典") })
    }

    @Test
    fun `05 十八反配伍被拦截`() {
        val content = """
            {"formulaName":"测试方","herbs":[{"name":"甘草","grams":6},{"name":"甘遂","grams":2}]}
        """.trimIndent()
        val r = runCase(content)
        check("05 十八反拦截", r)
        assertEquals(RunKind.MODEL_BLOCKED, r.kind)
        assertNull(r.prescription)
        assertNull(r.fallback)
        assertNotNull(r.refusal)
        assertTrue(r.safety!!.blocked)
    }

    @Test
    fun `06 未配置 Key 明确降级且不发请求`() {
        val r = runCase(STANDARD, apiKey = "")
        check("06 未配置 Key", r)
        assertEquals(RunKind.FALLBACK, r.kind)
        assertNull(r.prescription)
        assertNotNull(r.fallback)
        assertFalse(r.status.modelUsed)
        assertTrue(r.status.headline.contains("未调用大模型"))
        assertTrue(r.status.technical.contains("API Key"))
        assertEquals(0, requestCount.get())
    }

    @Test
    fun `07 HTTP 401 明确降级且原因可读`() {
        val r = runCase(STANDARD, code = 401)
        check("07 HTTP 401", r)
        assertEquals(RunKind.FALLBACK, r.kind)
        assertNotNull(r.fallback)
        assertTrue(r.status.technical.contains("401"))
        assertFalse(r.status.technical.contains("\n"))
    }

    @Test
    fun `08 非 JSON 文本明确降级`() {
        val r = runCase("抱歉，作为 AI 我不能提供具体的用药建议。")
        check("08 非 JSON 响应", r)
        assertEquals(RunKind.FALLBACK, r.kind)
        assertTrue(r.status.technical.contains("未返回 JSON"))
        assertNotNull(r.fallback)
    }

    @Test
    fun `09 红旗症状在进入推理前拒答`() {
        val r = runCase(STANDARD, symptoms = "胸痛 呼吸困难 大汗")
        check("09 红旗拒答", r)
        assertEquals(RunKind.REFUSED, r.kind)
        assertNull(r.chart)
        assertNull(r.prescription)
        assertNull(r.fallback)
        assertNotNull(r.refusal)
        assertEquals(0, requestCount.get())
    }

    @Test
    fun `10 无有效症候不排盘不调模型`() {
        val r = runCase(STANDARD, symptoms = "")
        check("10 证据不足", r)
        assertEquals(RunKind.NO_EVIDENCE, r.kind)
        assertNull(r.chart)
        assertNull(r.fallback)
        assertEquals(0, requestCount.get())
    }

    @Test
    fun `11 JsonLite 基础正确性`() {
        val v = JsonLite.parse("""{"a":"含\"引号\n换行","n":-3.5,"b":true,"z":null,"arr":[1,"x",{"k":"v"}]}""")
        val m = JsonLite.asMap(v)
        assertEquals("含\"引号\n换行", JsonLite.text(m["a"]))
        assertEquals(-3.5, JsonLite.number(m["n"])!!, 0.001)
        assertEquals("true", JsonLite.text(m["b"]))
        assertEquals("", JsonLite.text(m["z"]))
        assertEquals(3, JsonLite.asList(m["arr"]).size)
        assertEquals("v", JsonLite.text(JsonLite.asMap(JsonLite.asList(m["arr"])[2])["k"]))
        log("【11 JsonLite】转义 / 负数 / 布尔 / null / 嵌套数组全部通过\n")
    }

    companion object {
        private val report = File("build/reports/daoyi-flow.txt")

        @BeforeClass
        @JvmStatic
        fun init() {
            report.parentFile?.mkdirs()
            report.writeText("道医 · 参同契｜全流程回归报告（本地假服务，真实 HTTP + 解析 + 推理）\n\n")
        }

        private fun log(line: String) {
            report.appendText(line + "\n")
            println(line)
        }
    }
}
