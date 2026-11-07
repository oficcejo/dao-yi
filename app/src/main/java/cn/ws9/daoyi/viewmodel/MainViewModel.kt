package cn.ws9.daoyi.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import cn.ws9.daoyi.core.bayes.BayesResult
import cn.ws9.daoyi.core.bazi.BaziChart
import cn.ws9.daoyi.core.llm.DeepSeekClient
import cn.ws9.daoyi.core.llm.LlmConfig
import cn.ws9.daoyi.core.llm.LlmPrescription
import cn.ws9.daoyi.core.pipeline.DaoYiPipeline
import cn.ws9.daoyi.core.pipeline.PipelineRequest
import cn.ws9.daoyi.core.pipeline.PipelineSettings
import cn.ws9.daoyi.core.pipeline.RunStatus
import cn.ws9.daoyi.core.safety.SafetyReport
import cn.ws9.daoyi.core.tcm.Formula
import cn.ws9.daoyi.core.tcm.PatentMedicine
import cn.ws9.daoyi.core.tcm.PersonProfile
import cn.ws9.daoyi.data.AppSettings
import cn.ws9.daoyi.data.HistoryEntry
import cn.ws9.daoyi.data.HistoryStore
import cn.ws9.daoyi.data.KnowledgeLoader
import cn.ws9.daoyi.data.SettingsStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.LocalTime

data class RunRequest(
    val date: LocalDate,
    val time: LocalTime,
    val longitude: Double,
    val placeName: String,
    val male: Boolean,
    val symptomText: String,
    val selected: List<String>,
    val profile: PersonProfile,
    val referenceYear: Int = LocalDate.now().year
)

data class UiState(
    val settings: AppSettings = AppSettings(),
    val loading: Boolean = false,
    val chart: BaziChart? = null,
    val bayes: BayesResult? = null,
    val prescription: LlmPrescription? = null,
    val safety: SafetyReport? = null,
    val fallback: Formula? = null,
    val fallbackPatent: PatentMedicine? = null,
    val refusal: String? = null,
    val message: String = "",
    val history: List<HistoryEntry> = emptyList(),
    val settingsNotice: String = "",
    val status: RunStatus = RunStatus()
)

class MainViewModel(app: Application) : AndroidViewModel(app) {

    private val store = SettingsStore(app)
    private val history = HistoryStore(app)

    private val _ui = MutableStateFlow(
        UiState(
            settings = store.load(),
            history = history.all(),
            settingsNotice = store.fallbackNotice
        )
    )
    val ui: StateFlow<UiState> = _ui.asStateFlow()

    init {
        KnowledgeLoader.applyOverride(app)
    }

    /** 兼容旧调用；内部一律走基于最新状态的更新，避免两处界面互相覆盖设置。 */
    fun saveSettings(s: AppSettings) {
        updateSettings { s }
    }

    /** 保存到 IO 线程；任何密钥库/存储异常都在 SettingsStore 内兜住，不会让界面崩溃。 */
    fun updateSettings(change: (AppSettings) -> AppSettings) {
        viewModelScope.launch {
            val next = runCatching { change(_ui.value.settings) }
                .getOrElse { _ui.value.settings }
            val notice = withContext(Dispatchers.IO) { store.save(next) }
            _ui.value = _ui.value.copy(
                settings = next,
                settingsNotice = if (notice.isBlank()) "已保存到本机。" else notice
            )
        }
    }

    fun clearSettingsNotice() {
        if (_ui.value.settingsNotice.isNotEmpty()) {
            _ui.value = _ui.value.copy(settingsNotice = "")
        }
    }

    fun clearHistory() {
        history.clear()
        _ui.value = _ui.value.copy(history = emptyList())
    }

    fun run(req: RunRequest) {
        viewModelScope.launch {
            _ui.value = _ui.value.copy(
                loading = true, message = "", refusal = null, chart = null, bayes = null,
                prescription = null, safety = null, fallback = null, fallbackPatent = null,
                status = RunStatus()
            )
            val settings = _ui.value.settings
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    DaoYiPipeline.run(req.toPipeline(settings)) { system, user ->
                        DeepSeekClient(
                            LlmConfig(
                                apiKey = settings.apiKey,
                                baseUrl = settings.baseUrl,
                                model = settings.model
                            )
                        ).call(system, user)
                    }
                }.getOrElse { DaoYiPipeline.error(it) }
            }
            result.historyRecord?.let { h -> history.add(h.syndrome, h.formula, h.source) }
            _ui.value = _ui.value.copy(
                loading = false,
                chart = result.chart,
                bayes = result.bayes,
                prescription = result.prescription,
                safety = result.safety,
                fallback = result.fallback,
                fallbackPatent = result.fallbackPatent,
                refusal = result.refusal,
                message = result.message,
                status = result.status,
                history = history.all()
            )
        }
    }

    private fun RunRequest.toPipeline(s: AppSettings) = PipelineRequest(
        date = date,
        time = time,
        longitude = longitude,
        placeName = placeName,
        male = male,
        symptomText = symptomText,
        selected = selected,
        profile = profile,
        referenceYear = referenceYear,
        settings = PipelineSettings(
            apiKey = s.apiKey,
            baseUrl = s.baseUrl,
            model = s.model,
            lambda = s.lambda,
            applyEquationOfTime = s.applyEquationOfTime,
            ziHourShiftsDay = s.ziHourShiftsDay
        )
    )
}
