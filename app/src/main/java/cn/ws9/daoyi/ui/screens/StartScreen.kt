package cn.ws9.daoyi.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cn.ws9.daoyi.core.bazi.Shichen
import cn.ws9.daoyi.core.tcm.AgeCalculator
import cn.ws9.daoyi.core.tcm.PersonProfile
import cn.ws9.daoyi.ui.components.DaoCard
import cn.ws9.daoyi.ui.components.DatePickerDialog
import cn.ws9.daoyi.ui.components.Disclaimer
import cn.ws9.daoyi.ui.components.GoldDivider
import cn.ws9.daoyi.ui.components.KeyValue
import cn.ws9.daoyi.ui.components.PickerField
import cn.ws9.daoyi.ui.components.SectionHeader
import cn.ws9.daoyi.ui.components.SelectChip
import cn.ws9.daoyi.ui.components.ShichenPickerDialog
import cn.ws9.daoyi.ui.theme.CardBg
import cn.ws9.daoyi.ui.theme.Cinnabar
import cn.ws9.daoyi.ui.theme.Gold
import cn.ws9.daoyi.ui.theme.Ink
import cn.ws9.daoyi.ui.theme.InkMuted
import cn.ws9.daoyi.ui.theme.Paper
import cn.ws9.daoyi.viewmodel.MainViewModel
import cn.ws9.daoyi.viewmodel.RunRequest
import cn.ws9.daoyi.viewmodel.UiState
import java.time.LocalDate
import java.time.LocalTime

private val CITIES = listOf(
    "济宁" to 116.59, "北京" to 116.40, "上海" to 121.47,
    "广州" to 113.26, "成都" to 104.07, "乌鲁木齐" to 87.62
)

private val QUICK_SYMPTOMS = listOf(
    "恶寒", "发热", "无汗", "汗出", "头痛", "口苦",
    "胸胁苦满", "口渴", "咳嗽", "腹胀", "便溏", "便秘",
    "乏力", "畏寒", "失眠", "心烦", "眩晕", "水肿",
    "苔黄", "苔腻", "脉弦", "脉浮", "脉沉", "舌淡"
)

@Composable
fun StartScreen(state: UiState, vm: MainViewModel, onGo: () -> Unit) {
    var dateText by remember { mutableStateOf("1990-05-15") }
    var timeText by remember { mutableStateOf("10:30") }
    var showDate by remember { mutableStateOf(false) }
    var showShichen by remember { mutableStateOf(false) }
    var city by remember { mutableStateOf(CITIES[0].first) }
    var longitudeText by remember { mutableStateOf(CITIES[0].second.toString()) }
    var male by remember { mutableStateOf(true) }
    var symptomText by remember { mutableStateOf("") }
    var selected by remember { mutableStateOf(setOf<String>()) }
    var pregnant by remember { mutableStateOf(false) }
    var liverKidney by remember { mutableStateOf(false) }
    var allergy by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") }

    val birthDate = runCatching { LocalDate.parse(dateText.trim()) }.getOrNull()
    val birthTime = runCatching { LocalTime.parse(timeText.trim()) }.getOrNull()
    val shichenLabel = birthTime?.let { Shichen.name(it) } ?: "未识别"
    val ageText = birthDate?.let { AgeCalculator.label(it) } ?: "待选有效日期"

    Column(
        Modifier.fillMaxSize().background(Paper)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(top = 26.dp, bottom = 32.dp)
    ) {
        Text("道医 · 参同契", style = MaterialTheme.typography.displayMedium, color = Ink)
        Spacer(Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.width(26.dp).height(1.dp).background(Cinnabar))
            Spacer(Modifier.width(10.dp))
            Text("八字 · 六经辨证 · 经方推演", style = MaterialTheme.typography.bodySmall, color = Gold)
        }
        Spacer(Modifier.height(18.dp))
        Disclaimer()
        Spacer(Modifier.height(6.dp))

        SectionHeader("壹", "生辰与时空", "点选公历日期与时辰 · 真太阳时校正 · 立春分年")
        DaoCard {
            Row(Modifier.fillMaxWidth()) {
                Box(Modifier.weight(1.15f)) {
                    PickerField("公历出生日期", dateText, "点选年月日") { showDate = true }
                }
                Spacer(Modifier.width(10.dp))
                Box(Modifier.weight(0.85f)) {
                    PickerField("出生时辰", shichenLabel, "点选十二时辰") { showShichen = true }
                }
            }
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = timeText, onValueChange = { timeText = it },
                label = { Text("精确时刻 HH:mm（选时辰后自动填入，可微调）") },
                singleLine = true, modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(12.dp))
            Text("出生地（决定真太阳时经度差）", style = MaterialTheme.typography.bodySmall, color = InkMuted)
            Spacer(Modifier.height(8.dp))
            CITIES.chunked(3).forEach { row ->
                Row(Modifier.fillMaxWidth()) {
                    row.forEach { (name, lon) ->
                        Box(Modifier.weight(1f)) {
                            SelectChip(name, city == name) {
                                city = name
                                longitudeText = lon.toString()
                            }
                        }
                    }
                    repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                }
            }
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = longitudeText, onValueChange = { longitudeText = it; city = "自定义" },
                label = { Text("东经（度）") }, singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth()) {
                Box(Modifier.weight(1f)) { SelectChip("男", male) { male = true } }
                Box(Modifier.weight(1f)) { SelectChip("女", !male) { male = false } }
                Box(Modifier.weight(1f)) {
                    SelectChip("均时差", state.settings.applyEquationOfTime) {
                        vm.updateSettings { it.copy(applyEquationOfTime = !it.applyEquationOfTime) }
                    }
                }
            }
        }

        SectionHeader("贰", "症候与主述", "结构化证据进入贝叶斯汇总")
        DaoCard {
            OutlinedTextField(
                value = symptomText, onValueChange = { symptomText = it },
                label = { Text("请描述症状、舌象、脉象…") },
                minLines = 3, maxLines = 6, modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(12.dp))
            Text("常用症候（可多选）", style = MaterialTheme.typography.bodySmall, color = InkMuted)
            Spacer(Modifier.height(6.dp))
            QUICK_SYMPTOMS.chunked(3).forEach { row ->
                Row(Modifier.fillMaxWidth()) {
                    row.forEach { s ->
                        Box(Modifier.weight(1f)) {
                            SelectChip(s, s in selected) {
                                selected = if (s in selected) selected - s else selected + s
                            }
                        }
                    }
                }
            }
        }

        SectionHeader("叁", "体质与禁忌", "年龄按出生日期自动推算 · 用于安全闸门校验")
        DaoCard {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text("年龄（自动）", style = MaterialTheme.typography.bodyMedium,
                    color = InkMuted, modifier = Modifier.width(96.dp))
                Text(
                    if (birthDate == null) "请先在上方选择有效出生日期" else "$ageText · 由出生日期自动推算",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (birthDate == null) Cinnabar else Ink,
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = allergy, onValueChange = { allergy = it },
                label = { Text("过敏史（选填）") }, singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth()) {
                Box(Modifier.weight(1f)) { SelectChip("妊娠", pregnant) { pregnant = !pregnant } }
                Box(Modifier.weight(1f)) { SelectChip("肝肾疾患", liverKidney) { liverKidney = !liverKidney } }
                Box(Modifier.weight(1f)) {
                    SelectChip("子时换日", state.settings.ziHourShiftsDay) {
                        vm.updateSettings { it.copy(ziHourShiftsDay = !it.ziHourShiftsDay) }
                    }
                }
            }
        }

        if (error.isNotBlank()) {
            Spacer(Modifier.height(10.dp))
            Text(error, style = MaterialTheme.typography.bodySmall, color = Cinnabar)
        }

        Spacer(Modifier.height(20.dp))
        Button(
            onClick = {
                val date = runCatching { LocalDate.parse(dateText.trim()) }.getOrNull()
                val time = runCatching { LocalTime.parse(timeText.trim()) }.getOrNull()
                val lon = longitudeText.toDoubleOrNull()
                if (date == null || time == null || lon == null || lon !in 70.0..140.0) {
                    error = "请点选公历出生日期与出生时辰；精确时刻须为 HH:mm，经度须在 70–140 之间。"
                    return@Button
                }
                if (symptomText.isBlank() && selected.isEmpty()) {
                    error = "请填写主述或至少选择一个症候。"
                    return@Button
                }
                error = ""
                vm.run(
                    RunRequest(
                        date = date, time = time, longitude = lon, placeName = city, male = male,
                        symptomText = symptomText, selected = selected.toList(),
                        profile = PersonProfile(
                            age = AgeCalculator.years(date),
                            male = male, pregnant = pregnant,
                            liverKidneyIssue = liverKidney, allergy = allergy
                        )
                    )
                )
                onGo()
            },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Ink, contentColor = Paper)
        ) {
            Text("起 局 推 演", fontFamily = FontFamily.Serif, fontSize = 17.sp, letterSpacing = 3.sp)
        }

        if (state.history.isNotEmpty()) {
            SectionHeader("肆", "近期推演", "仅存本机 · 不含生辰原文")
            DaoCard {
                state.history.take(4).forEach { h ->
                    KeyValue(h.time, "${h.syndrome} · ${h.formula}（${h.source}）")
                }
            }
        }
        Spacer(Modifier.height(14.dp))
        GoldDivider()
        Spacer(Modifier.height(10.dp))
        Text(
            "四柱与运气为传统文化参考维度，权重低于四诊；结果不构成诊疗意见。",
            style = MaterialTheme.typography.bodySmall, color = InkMuted
        )

        if (showDate) {
            DatePickerDialog(
                initial = birthDate ?: LocalDate.of(1990, 5, 15),
                onDismiss = { showDate = false },
                onPick = { dateText = it.toString(); showDate = false }
            )
        }
        if (showShichen) {
            ShichenPickerDialog(
                current = birthTime ?: LocalTime.of(10, 30),
                onDismiss = { showShichen = false },
                onPick = { t ->
                    timeText = "%02d:%02d".format(t.hour, t.minute)
                    showShichen = false
                }
            )
        }
    }
}
