package cn.ws9.daoyi.core.llm

data class LlmConfig(
    val apiKey: String = "",
    val baseUrl: String = "https://api.deepseek.com/v1",
    val model: String = "deepseek-chat",
    val temperature: Double = 0.2,
    val timeoutSeconds: Int = 90
)

data class HerbDose(
    val name: String,
    val grams: Double,
    val processing: String = "",
    val note: String = ""
)

data class LlmPrescription(
    val syndrome: String = "",
    val therapy: String = "",
    val formulaName: String = "",
    val source: String = "",
    val herbs: List<HerbDose> = emptyList(),
    val decoction: String = "",
    val patentMedicines: List<String> = emptyList(),
    val diet: List<String> = emptyList(),
    val lifestyle: List<String> = emptyList(),
    val exercise: String = "",
    val emotional: String = "",
    val seasonal: String = "",
    val references: List<String> = emptyList(),
    val cautions: List<String> = emptyList(),
    val confidence: String = ""
)

sealed interface LlmOutcome {
    data class Success(val data: LlmPrescription, val raw: String) : LlmOutcome
    data class Failure(val message: String) : LlmOutcome
}
