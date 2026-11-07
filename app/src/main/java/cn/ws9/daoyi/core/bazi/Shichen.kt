package cn.ws9.daoyi.core.bazi

import java.time.LocalTime

/** 十二时辰与公历时刻的映射。纯 Kotlin，便于 JVM 单元测试。 */
object Shichen {

    val ZHI = listOf("子", "丑", "寅", "卯", "辰", "巳", "午", "未", "申", "酉", "戌", "亥")
    val NAMES = ZHI.map { "${it}时" }
    val RANGES = listOf(
        "23:00–01:00", "01:00–03:00", "03:00–05:00", "05:00–07:00",
        "07:00–09:00", "09:00–11:00", "11:00–13:00", "13:00–15:00",
        "15:00–17:00", "17:00–19:00", "19:00–21:00", "21:00–23:00"
    )
    /** 每个时辰的默认代表时刻（该时辰前半段的中点）。子时取晚子时 23:30。 */
    val DEFAULTS = listOf(
        "23:30", "01:30", "03:30", "05:30", "07:30", "09:30",
        "11:30", "13:30", "15:30", "17:30", "19:30", "21:30"
    )

    /** 23:00–00:59 为子时；01:00–02:59 丑时；以此类推。 */
    fun indexOf(time: LocalTime): Int {
        val h = time.hour
        return if (h == 23 || h == 0) 0 else (h + 1) / 2
    }

    fun name(time: LocalTime): String = NAMES[indexOf(time)]

    fun defaultTime(index: Int): LocalTime =
        LocalTime.parse(DEFAULTS[index.coerceIn(0, 11)])
}
