package cn.ws9.daoyi.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import cn.ws9.daoyi.ui.components.DaoCard
import cn.ws9.daoyi.ui.components.Disclaimer
import cn.ws9.daoyi.ui.components.GoldDivider
import cn.ws9.daoyi.ui.components.KeyValue
import cn.ws9.daoyi.ui.components.SectionHeader
import cn.ws9.daoyi.ui.theme.Cinnabar
import cn.ws9.daoyi.ui.theme.Gold
import cn.ws9.daoyi.ui.theme.Ink
import cn.ws9.daoyi.ui.theme.InkMuted
import cn.ws9.daoyi.ui.theme.Paper
import cn.ws9.daoyi.viewmodel.MainViewModel
import cn.ws9.daoyi.viewmodel.UiState

@Composable
fun SettingsScreen(state: UiState, vm: MainViewModel) {
    val s = state.settings
    var key by remember(s.apiKey) { mutableStateOf(s.apiKey) }
    var base by remember(s.baseUrl) { mutableStateOf(s.baseUrl) }
    var model by remember(s.model) { mutableStateOf(s.model) }
    var lambda by remember(s.lambda) { mutableStateOf(s.lambda.toFloat()) }
    var eot by remember(s.applyEquationOfTime) { mutableStateOf(s.applyEquationOfTime) }
    var ziShift by remember(s.ziHourShiftsDay) { mutableStateOf(s.ziHourShiftsDay) }
    var longitude by remember(s.longitude) { mutableStateOf(s.longitude.toString()) }
    var place by remember(s.placeName) { mutableStateOf(s.placeName) }

    Column(
        Modifier.fillMaxSize().background(Paper)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(top = 26.dp, bottom = 40.dp)
    ) {
        Text("设 置", style = MaterialTheme.typography.displayMedium, color = Ink)
        Spacer(Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.width(26.dp).height(1.dp).background(Cinnabar))
            Spacer(Modifier.width(10.dp))
            Text("纯端侧 · 密钥不出本机", style = MaterialTheme.typography.bodySmall, color = Gold)
        }
        Spacer(Modifier.height(16.dp))
        Disclaimer()

        SectionHeader("壹", "大模型接入", "支持 DeepSeek 及任何 OpenAI 兼容服务")
        DaoCard {
            OutlinedTextField(
                value = key, onValueChange = { key = it; vm.clearSettingsNotice() },
                label = { Text("API Key（Android Keystore 加密存储）") },
                singleLine = true, visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(6.dp))
            Text(
                if (s.apiKey.isBlank()) "当前：未配置 Key（将使用本地典籍参考卡）"
                else "当前：已保存 · 尾号 ${s.apiKey.takeLast(4)}",
                style = MaterialTheme.typography.bodySmall,
                color = if (s.apiKey.isBlank()) InkMuted else Gold
            )
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = base, onValueChange = { base = it; vm.clearSettingsNotice() },
                label = { Text("Base URL") }, singleLine = true, modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = model, onValueChange = { model = it; vm.clearSettingsNotice() },
                label = { Text("模型名") }, singleLine = true, modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(10.dp))
            Text("常用：deepseek-chat / deepseek-reasoner；也可填自建服务地址。未配置 Key 时自动降级为本地典籍参考卡。",
                style = MaterialTheme.typography.bodySmall, color = InkMuted)
        }

        SectionHeader("贰", "推演参数", "八字偏性只调制证型先验，λ=0 即纯中医辨证基线")
        DaoCard {
            Text("本命偏性权重 λ = ${(lambda * 100).toInt() / 100.0}",
                style = MaterialTheme.typography.titleMedium, color = Ink)
            Slider(value = lambda, onValueChange = { lambda = it; vm.clearSettingsNotice() },
                valueRange = 0f..0.50f, steps = 10, modifier = Modifier.fillMaxWidth())
            Text("建议 0.15。八字与运气学说缺乏现代循证依据，调高仅用于研究对比，不改变四诊结论。",
                style = MaterialTheme.typography.bodySmall, color = InkMuted)
            Spacer(Modifier.height(14.dp))
            GoldDivider()
            Spacer(Modifier.height(8.dp))
            SwitchRow("真太阳时叠加均时差", eot) { eot = it; vm.clearSettingsNotice() }
            SwitchRow("23:00 起子时换日柱", ziShift) { ziShift = it; vm.clearSettingsNotice() }
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = place, onValueChange = { place = it; vm.clearSettingsNotice() },
                label = { Text("默认出生地") }, singleLine = true, modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = longitude, onValueChange = { longitude = it; vm.clearSettingsNotice() },
                label = { Text("默认东经（度）") }, singleLine = true, modifier = Modifier.fillMaxWidth()
            )
        }

        Spacer(Modifier.height(18.dp))
        Button(
            onClick = {
                vm.updateSettings { cur ->
                    cur.copy(
                        apiKey = key.trim(), baseUrl = base.trim(), model = model.trim(),
                        lambda = lambda.toDouble(), applyEquationOfTime = eot,
                        ziHourShiftsDay = ziShift, placeName = place,
                        longitude = longitude.toDoubleOrNull() ?: cur.longitude
                    )
                }
            },
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Ink, contentColor = Paper)
        ) { Text("保 存 设 置", style = MaterialTheme.typography.titleMedium) }

        if (state.settingsNotice.isNotBlank()) {
            Spacer(Modifier.height(10.dp))
            Text(
                state.settingsNotice,
                style = MaterialTheme.typography.bodySmall,
                color = if (state.settingsNotice.startsWith("⚠")) Cinnabar else Gold
            )
        }

        SectionHeader("叁", "本机数据", "推演记录仅存摘要，不含生辰原文与主述")
        DaoCard {
            KeyValue("缓存记录", "${state.history.size} 条")
            Spacer(Modifier.height(8.dp))
            Text("清除本机推演记录", style = MaterialTheme.typography.titleSmall, color = Cinnabar,
                modifier = Modifier
                    .background(Gold.copy(alpha = 0.08f), RoundedCornerShape(6.dp))
                    .clickable { vm.clearHistory() }
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            )
        }

        SectionHeader("肆", "工程与合规说明", "自用研究版")
        DaoCard {
            Text(
                "架构：端侧完成真太阳时、节气、四柱、旺衰、大运流年、五运六气、贝叶斯证型后验；" +
                        "仅把结构化证据链与典籍参考传给大模型；输出再经剂量/配伍/人群硬规则闸门。\n\n" +
                        "法规提示：《互联网诊疗监管细则（试行）》第二十一条禁止使用人工智能自动生成处方。" +
                        "本程序面向本人学习与典籍研讨，不对外提供诊疗服务、不生成处方、不替代医师。\n\n" +
                        "数据：药材剂量取自《中国药典》常用日用量区间，经方取自《伤寒论》《金匮要略》公开条文，" +
                        "均未经医师审校。健康信息属敏感个人信息，请自行评估向第三方模型传输的风险。",
                style = MaterialTheme.typography.bodyMedium, color = Ink
            )
        }
    }
}

@Composable
private fun SwitchRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = Ink, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onChange)
    }
}
