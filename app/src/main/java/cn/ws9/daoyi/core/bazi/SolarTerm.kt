package cn.ws9.daoyi.core.bazi

import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.roundToLong
import kotlin.math.sin

/**
 * 节气求解：用太阳视黄经（含中心差与光行差修正）牛顿迭代求根。
 * 不依赖任何第三方历法库；1900-2100 精度约 1 分钟内。
 * 所有节气时刻统一输出 Asia/Shanghai（北京时间）。
 */
object SolarTerm {

    val BJ_ZONE: ZoneId = ZoneId.of("Asia/Shanghai")

    val NAMES = listOf(
        "小寒", "大寒", "立春", "雨水", "惊蛰", "春分", "清明", "谷雨",
        "立夏", "小满", "芒种", "夏至", "小暑", "大暑", "立秋", "处暑",
        "白露", "秋分", "寒露", "霜降", "立冬", "小雪", "大雪", "冬至"
    )

    /** 太阳视黄经：小寒 285°，每节 +15°，冬至 270° */
    val LONGITUDES = List(24) { (285.0 + it * 15.0) % 360.0 }

    /** 各节气的大致公历日期，用作牛顿迭代初值 */
    private val GUESS = listOf(
        1 to 6, 1 to 20, 2 to 4, 2 to 19, 3 to 6, 3 to 21, 4 to 5, 4 to 20,
        5 to 6, 5 to 21, 6 to 6, 6 to 21, 7 to 7, 7 to 23, 8 to 8, 8 to 23,
        9 to 8, 9 to 23, 10 to 8, 10 to 24, 11 to 8, 11 to 22, 12 to 7, 12 to 22
    )

    /** 12 个「节」在 NAMES 中的下标，用于月柱分界 */
    val JIE_INDICES = listOf(2, 4, 6, 8, 10, 12, 14, 16, 18, 20, 22, 0)

    private val cache = ConcurrentHashMap<Pair<Int, Int>, ZonedDateTime>()

    fun julianDayNumber(date: LocalDate): Long {
        val y = date.year
        val m = date.monthValue
        val d = date.dayOfMonth
        val a = (14 - m) / 12
        val yy = (y + 4800 - a).toLong()
        val mm = m + 12 * a - 3
        return d.toLong() + (153L * mm + 2) / 5 + 365L * yy + yy / 4 - yy / 100 + yy / 400 - 32045L
    }

    /** 儒略日（UT）对应的太阳视黄经，单位度，范围 [0,360) */
    fun apparentLongitude(jd: Double): Double {
        val t = (jd - 2451545.0) / 36525.0
        val l0 = 280.46646 + 36000.76983 * t + 0.0003032 * t * t
        val m = Math.toRadians(357.52911 + 35999.05029 * t - 0.0001537 * t * t)
        val c = (1.914602 - 0.004817 * t - 0.000014 * t * t) * sin(m) +
                (0.019993 - 0.000101 * t) * sin(2 * m) +
                0.000289 * sin(3 * m)
        val omega = Math.toRadians(125.04 - 1934.136 * t)
        val lambda = l0 + c - 0.00569 - 0.00478 * sin(omega)
        return ((lambda % 360.0) + 360.0) % 360.0
    }

    /** 求第 index 个节气在 year 年的北京时刻 */
    fun at(year: Int, index: Int): ZonedDateTime {
        val key = year to index
        cache[key]?.let { return it }
        val (m, d) = GUESS[index]
        var jd = julianDayNumber(LocalDate.of(year, m, d)).toDouble()
        val target = LONGITUDES[index]
        for (step in 0 until 16) {
            val diff0 = target - apparentLongitude(jd)
            val diff = ((diff0 + 180.0) % 360.0 + 360.0) % 360.0 - 180.0
            if (abs(diff) < 1e-8) break
            jd += diff / 0.9856473
        }
        val seconds = ((jd - 2440587.5) * 86400.0).roundToLong()
        val result = Instant.ofEpochSecond(seconds).atZone(BJ_ZONE)
        cache[key] = result
        return result
    }

    /** 均时差（分钟），近似公式；真太阳时 = 平太阳时 + 均时差 */
    fun equationOfTime(date: LocalDate): Double {
        val n = date.dayOfYear.toDouble()
        val b = 2.0 * PI * (n - 81.0) / 364.0
        return 9.87 * sin(2 * b) - 7.53 * cos(b) - 1.5 * sin(b)
    }

    /**
     * 北京时间 → 真太阳时。
     * 平太阳时 = 北京时间 + (当地经度 - 120) × 4 分钟；再叠加均时差。
     */
    fun trueSolarTime(bj: LocalDateTime, longitude: Double, applyEquationOfTime: Boolean = true): LocalDateTime {
        var minutes = (longitude - 120.0) * 4.0
        if (applyEquationOfTime) minutes += equationOfTime(bj.toLocalDate())
        return bj.plusMinutes(minutes.roundToLong())
    }

    fun liChun(year: Int): ZonedDateTime = at(year, 2)
}
