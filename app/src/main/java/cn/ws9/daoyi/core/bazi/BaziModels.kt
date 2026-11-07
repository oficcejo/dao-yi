package cn.ws9.daoyi.core.bazi

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

data class Pillar(val gan: String, val zhi: String) {
    val name: String get() = gan + zhi
}

data class BirthInput(
    val date: LocalDate,
    val time: LocalTime,
    val longitude: Double,
    val placeName: String,
    val male: Boolean,
    val applyEquationOfTime: Boolean = true,
    val ziHourShiftsDay: Boolean = true,
    val referenceYear: Int = LocalDate.now().year
)

data class StrengthItem(val label: String, val delta: Double, val note: String = "")

data class StrengthResult(
    val score: Double,
    val level: String,
    val items: List<StrengthItem>,
    val power: Map<String, Double>,
    val bias: Map<String, Double>,
    val advice: String
)

data class DayunStep(val order: Int, val startAge: Int, val startYear: Int, val ganZhi: String)

data class DayunInfo(
    val forward: Boolean,
    val startAge: Double,
    val steps: List<DayunStep>,
    val liuNianYear: Int,
    val liuNian: Pillar,
    val current: String,
    val note: String
)

data class YunqiInfo(
    val year: Int,
    val ganZhi: String,
    val zhongYun: String,
    val taiGuoBuJi: String,
    val siTian: String,
    val zaiQuan: String,
    val zhuQi: String,
    val keQi: String,
    val bias: Map<String, Double>,
    val note: String
)

data class BaziChart(
    val pillars: List<Pillar>,
    val tenGods: List<String>,
    val hiddenStems: List<List<HiddenStem>>,
    val nayin: List<String>,
    val dayMaster: String,
    val solarTermName: String,
    val trueSolarTime: LocalDateTime,
    val beijingTime: LocalDateTime,
    val strength: StrengthResult,
    val dayun: DayunInfo,
    val yunqi: YunqiInfo,
    val wuxingBias: Map<String, Double>
) {
    val dayPillar: Pillar get() = pillars[2]
    val labels: List<String> get() = listOf("年柱", "月柱", "日柱", "时柱")
}
