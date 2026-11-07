package cn.ws9.daoyi.core.pipeline

import cn.ws9.daoyi.core.bayes.BayesEngine
import cn.ws9.daoyi.core.bayes.BayesResult
import cn.ws9.daoyi.core.bazi.BaziChart
import cn.ws9.daoyi.core.bazi.BaziEngine
import cn.ws9.daoyi.core.bazi.BirthInput
import cn.ws9.daoyi.core.llm.LlmOutcome
import cn.ws9.daoyi.core.llm.LlmPrescription
import cn.ws9.daoyi.core.llm.PromptBuilder
import cn.ws9.daoyi.core.safety.SafetyGate
import cn.ws9.daoyi.core.safety.SafetyReport
import cn.ws9.daoyi.core.tcm.Formula
import cn.ws9.daoyi.core.tcm.LocalFallback
import cn.ws9.daoyi.core.tcm.PatentMedicine
import cn.ws9.daoyi.core.tcm.PersonProfile
import cn.ws9.daoyi.core.tcm.SymptomExtractor
import java.time.LocalDate
import java.time.LocalTime

data class PipelineSettings(
    val apiKey: String = "",
    val baseUrl: String = "https://api.deepseek.com/v1",
    val model: String = "deepseek-chat",
    val lambda: Double = 0.15,
    val applyEquationOfTime: Boolean = true,
    val ziHourShiftsDay: Boolean = true
)

data class PipelineRequest(
    val date: LocalDate,
    val time: LocalTime,
    val longitude: Double,
    val placeName: String,
    val male: Boolean,
    val symptomText: String,
    val selected: List<String>,
    val profile: PersonProfile,
    val referenceYear: Int = LocalDate.now().year,
    val settings: PipelineSettings
)

/** 结果页顶部「运行状态」卡片的数据来源：说清楚本次到底有没有调用大模型、为什么降级。 */
data class RunStatus(
    val modelUsed: Boolean = false,
    val headline: String = "",
    val reason: String = "",
    val technical: String = ""
)

enum class RunKind { REFUSED, NO_EVIDENCE, MODEL_OK, MODEL_BLOCKED, FALLBACK, ERROR }

data class HistoryRecord(val syndrome: String, val formula: String, val source: String)

/** 一次推演的完整结果。prescription 与 fallback 互斥，避免结果页出现「主输出串到参考卡下面」。 */
data class PipelineResult(
    val kind: RunKind,
    val chart: BaziChart? = null,
    val bayes: BayesResult? = null,
    val prescription: LlmPrescription? = null,
    val safety: SafetyReport? = null,
    val fallback: Formula? = null,
    val fallbackPatent: PatentMedicine? = null,
    val refusal: String? = null,
    val message: String = "",
    val status: RunStatus = RunStatus(),
    val historyRecord: HistoryRecord? = null
)

/**
 * 全流程编排（纯 Kotlin，不依赖 Android，可在 JVM 里端到端回归）：
 * 症候抽取 → 红旗拒答 → 排盘 → 贝叶斯 → 大模型 → 安全闸门 → 结果结构。
 * 大模型调用以 lambda 注入，便于测试时接本地假服务或注入固定响应。
 */
object DaoYiPipeline {

    fun run(req: PipelineRequest, llm: (String, String) -> LlmOutcome): PipelineResult {
        val extraction = SymptomExtractor.extract(req.symptomText, req.selected)

        SafetyGate.checkBefore(extraction, req.profile)?.let {
            return PipelineResult(
                kind = RunKind.REFUSED,
                refusal = it,
                message = "本次未生成任何方剂。",
                status = RunStatus(
                    modelUsed = false,
                    headline = "已停止推演 · 安全保护",
                    reason = "检出红旗征象或特殊人群保护条件，未进入辨证，也未调用大模型",
                    technical = it
                )
            )
        }
        if (extraction.symptoms.isEmpty()) {
            return PipelineResult(
                kind = RunKind.NO_EVIDENCE,
                message = "未识别到有效症候，请在主述中补充症状（如恶寒、汗出、口苦、脉弦）。",
                status = RunStatus(
                    modelUsed = false,
                    headline = "证据不足 · 未进入推演",
                    reason = "未识别到词表症候，本次不排盘、不调用大模型",
                    technical = ""
                )
            )
        }

        val s = req.settings
        val chart = BaziEngine.compute(
            BirthInput(
                date = req.date, time = req.time, longitude = req.longitude,
                placeName = req.placeName, male = req.male,
                applyEquationOfTime = s.applyEquationOfTime,
                ziHourShiftsDay = s.ziHourShiftsDay,
                referenceYear = req.referenceYear
            )
        )
        val bayes = BayesEngine.infer(
            symptoms = extraction.symptoms,
            baziBias = chart.wuxingBias,
            yunqiBias = chart.yunqi.bias,
            lambda = s.lambda
        )

        val outcome = runCatching {
            llm(PromptBuilder.SYSTEM, PromptBuilder.user(chart, bayes, extraction, req.profile))
        }.getOrElse { e ->
            LlmOutcome.Failure(
                "模型调用异常：${e.javaClass.simpleName} · ${e.message?.take(120).orEmpty()}"
            )
        }

        return when (outcome) {
            is LlmOutcome.Success -> {
                val report = SafetyGate.verify(outcome.data, req.profile)
                val top = bayes.ranked.first().syndrome.name
                if (report.blocked) {
                    PipelineResult(
                        kind = RunKind.MODEL_BLOCKED,
                        chart = chart, bayes = bayes, safety = report,
                        refusal = report.refusal,
                        message = "模型已返回结果，但被安全闸门拦截，未展示方剂。",
                        status = RunStatus(
                            modelUsed = true,
                            headline = "已调用模型 · 输出被安全闸门拦截",
                            reason = "检出配伍禁忌或妊娠禁用药，方剂不予展示",
                            technical = report.warnings.joinToString("；").take(300)
                        )
                    )
                } else {
                    PipelineResult(
                        kind = RunKind.MODEL_OK,
                        chart = chart, bayes = bayes, safety = report,
                        // 不再把 safety.warnings 重复并入 cautions，避免两处堆同一批文字
                        prescription = outcome.data.copy(herbs = report.adjusted),
                        message = if (bayes.lowConfidence) "证型证据偏弱，结论仅供典籍研讨。" else "推演完成。",
                        status = RunStatus(
                            modelUsed = true,
                            headline = "模型已返回 · 经方推演",
                            reason = "首位证型「$top」后验 ${(bayes.ranked.first().probability * 100).toInt()}%；" +
                                    "输出已过剂量、配伍与人群闸门",
                            technical = ""
                        ),
                        historyRecord = HistoryRecord(
                            syndrome = top,
                            formula = outcome.data.formulaName,
                            source = if (s.apiKey.isBlank()) "本地典籍" else s.model
                        )
                    )
                }
            }

            is LlmOutcome.Failure -> {
                val top = bayes.ranked.first().syndrome
                PipelineResult(
                    kind = RunKind.FALLBACK,
                    chart = chart, bayes = bayes,
                    fallback = LocalFallback.pick(top),
                    fallbackPatent = LocalFallback.patentFor(top),
                    message = outcome.message,
                    status = RunStatus(
                        modelUsed = false,
                        headline = "未调用大模型 · 本地典籍参考卡",
                        reason = shortReason(outcome.message),
                        technical = outcome.message
                    )
                )
            }
        }
    }

    fun error(e: Throwable): PipelineResult = PipelineResult(
        kind = RunKind.ERROR,
        message = "推演已停止，本次未生成任何结论。",
        status = RunStatus(
            modelUsed = false,
            headline = "推演已停止 · 内部异常",
            reason = "请检查出生日期、时刻与症候输入后重试",
            technical = "${e.javaClass.simpleName}: ${e.message.orEmpty()}".take(300)
        )
    )

    private fun shortReason(msg: String): String {
        if (msg.contains("API Key")) return "未配置 API Key，未调用模型"
        return msg.removePrefix("模型调用失败：").replace(Regex("\\s+"), " ").trim().take(70)
    }
}
