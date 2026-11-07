package cn.ws9.daoyi.core.tcm

data class Herb(
    val name: String,
    val minGram: Int,
    val maxGram: Int,
    val nature: String = "",
    val toxic: Boolean = false,
    val pregnancyAvoid: Boolean = false,
    val decoctNote: String = ""
)

data class Formula(
    val name: String,
    val source: String,
    val herbs: List<Pair<String, Int>>,
    val method: String,
    val syndrome: String,
    val note: String = ""
)

data class Syndrome(
    val id: String,
    val name: String,
    val liuJing: String,
    val therapy: String,
    val main: List<String>,
    val optional: List<String>,
    val negative: List<String>,
    val bias: Map<String, Double>,
    val coldHeat: Double,
    val dryDamp: Double,
    val classic: String
)

data class PatentMedicine(
    val name: String,
    val syndrome: String,
    val caution: String
)

data class PersonProfile(
    val age: Int = 35,
    val male: Boolean = true,
    val pregnant: Boolean = false,
    val liverKidneyIssue: Boolean = false,
    val allergy: String = ""
)

data class SymptomHit(val text: String, val group: String, val weight: Double)

data class Extraction(
    val symptoms: List<SymptomHit>,
    val redFlags: List<String>,
    val rawText: String
)
