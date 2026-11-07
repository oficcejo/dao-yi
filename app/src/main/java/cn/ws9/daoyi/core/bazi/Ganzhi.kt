package cn.ws9.daoyi.core.bazi

import kotlin.math.abs

data class HiddenStem(val gan: String, val weight: Double, val tier: String)

object Ganzhi {

    val GAN = listOf("甲", "乙", "丙", "丁", "戊", "己", "庚", "辛", "壬", "癸")
    val ZHI = listOf("子", "丑", "寅", "卯", "辰", "巳", "午", "未", "申", "酉", "戌", "亥")
    val WUXING = listOf("木", "火", "土", "金", "水")
    val GAN_WUXING = listOf("木", "木", "火", "火", "土", "土", "金", "金", "水", "水")
    val ZHI_WUXING = listOf("水", "土", "木", "木", "土", "火", "火", "土", "金", "金", "土", "水")
    val GAN_YANG = listOf(true, false, true, false, true, false, true, false, true, false)
    val ZHI_YANG = listOf(true, false, true, false, true, false, true, false, true, false, true, false)

    val JIAZI = List(60) { i -> GAN[i % 10] + ZHI[i % 12] }

    private val NAYIN_NAMES = listOf(
        "海中金", "炉中火", "大林木", "路旁土", "剑锋金",
        "山头火", "涧下水", "城头土", "白蜡金", "杨柳木",
        "泉中水", "屋上土", "霹雳火", "松柏木", "长流水",
        "沙中金", "山下火", "平地木", "壁上土", "金箔金",
        "覆灯火", "天河水", "大驿土", "钗钏金", "桑柘木",
        "大溪水", "沙中土", "天上火", "石榴木", "大海水"
    )

    private val CANG: Map<String, List<HiddenStem>> = mapOf(
        "子" to listOf(HiddenStem("癸", 1.0, "本气")),
        "丑" to listOf(HiddenStem("己", 1.0, "本气"), HiddenStem("癸", 0.6, "中气"), HiddenStem("辛", 0.3, "余气")),
        "寅" to listOf(HiddenStem("甲", 1.0, "本气"), HiddenStem("丙", 0.6, "中气"), HiddenStem("戊", 0.3, "余气")),
        "卯" to listOf(HiddenStem("乙", 1.0, "本气")),
        "辰" to listOf(HiddenStem("戊", 1.0, "本气"), HiddenStem("乙", 0.6, "中气"), HiddenStem("癸", 0.3, "余气")),
        "巳" to listOf(HiddenStem("丙", 1.0, "本气"), HiddenStem("戊", 0.6, "中气"), HiddenStem("庚", 0.3, "余气")),
        "午" to listOf(HiddenStem("丁", 1.0, "本气"), HiddenStem("己", 0.6, "中气")),
        "未" to listOf(HiddenStem("己", 1.0, "本气"), HiddenStem("丁", 0.6, "中气"), HiddenStem("乙", 0.3, "余气")),
        "申" to listOf(HiddenStem("庚", 1.0, "本气"), HiddenStem("壬", 0.6, "中气"), HiddenStem("戊", 0.3, "余气")),
        "酉" to listOf(HiddenStem("辛", 1.0, "本气")),
        "戌" to listOf(HiddenStem("戊", 1.0, "本气"), HiddenStem("辛", 0.6, "中气"), HiddenStem("丁", 0.3, "余气")),
        "亥" to listOf(HiddenStem("壬", 1.0, "本气"), HiddenStem("甲", 0.6, "中气"))
    )

    fun hidden(zhi: String): List<HiddenStem> = CANG[zhi] ?: emptyList()

    fun ganOfJiazi(index: Int): String = GAN[mod(index, 10)]
    fun zhiOfJiazi(index: Int): String = ZHI[mod(index, 12)]
    fun jiaziName(index: Int): String = GAN[mod(index, 10)] + ZHI[mod(index, 12)]
    fun nayin(index: Int): String = NAYIN_NAMES[mod(index, 60) / 2]

    fun mod(v: Int, m: Int): Int = ((v % m) + m) % m
    fun mod(v: Long, m: Long): Long = ((v % m) + m) % m

    fun ganIndex(gan: String): Int = GAN.indexOf(gan).coerceAtLeast(0)
    fun zhiIndex(zhi: String): Int = ZHI.indexOf(zhi).coerceAtLeast(0)
    fun jiaziIndexOf(gan: String, zhi: String): Int {
        for (i in 0 until 60) {
            if (GAN[i % 10] == gan && ZHI[i % 12] == zhi) return i
        }
        return 0
    }

    fun yearJiazi(year: Int): Int = mod(year - 4, 60)

    fun wx(value: String): String {
        val gi = GAN.indexOf(value)
        if (gi >= 0) return GAN_WUXING[gi]
        val zi = ZHI.indexOf(value)
        if (zi >= 0) return ZHI_WUXING[zi]
        return "土"
    }

    fun isYang(gan: String): Boolean {
        val i = GAN.indexOf(gan)
        return if (i >= 0) GAN_YANG[i] else true
    }

    private fun wxIndex(w: String): Int = WUXING.indexOf(w).coerceAtLeast(0)

    /** 五行关系：0 同我 1 我生 2 我克 3 克我 4 生我 */
    fun relation(dayGan: String, other: String): Int {
        val a = wxIndex(wx(dayGan))
        val b = wxIndex(wx(other))
        val d = mod(b - a, 5)
        return d
    }

    /** 十神：以日干为基准。异性为正、同性为偏（我生/同我另循其名）。 */
    fun tenGod(dayGan: String, other: String): String {
        val same = isYang(dayGan) == isYang(other)
        return when (relation(dayGan, other)) {
            0 -> if (same) "比肩" else "劫财"
            1 -> if (same) "食神" else "伤官"
            2 -> if (same) "偏财" else "正财"
            3 -> if (same) "七杀" else "正官"
            else -> if (same) "偏印" else "正印"
        }
    }

    /** 生：+1；克：+2（模 5）。仅供运气/脏腑映射使用。 */
    fun generates(a: String, b: String): Boolean = mod(wxIndex(b) - wxIndex(a), 5) == 1
    fun controls(a: String, b: String): Boolean = mod(wxIndex(b) - wxIndex(a), 5) == 2

    fun distance(a: Int, b: Int): Int = abs(a - b)
}
