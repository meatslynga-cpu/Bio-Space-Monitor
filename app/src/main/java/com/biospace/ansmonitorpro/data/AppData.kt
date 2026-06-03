package com.biospace.ansmonitorpro.data

// ── Space Weather ─────────────────────────────────────────────────────────────
data class SpaceWeatherData(
    val kp: Double = 0.0,
    val kpLabel: String = "QUIET",
    val kpHistory: List<Double> = emptyList(),
    val stormG: String = "G0",
    val stormS: String = "S0",
    val stormR: String = "R0",
    val solarWindSpeed: Double = 400.0,
    val solarWindDensity: Double = 5.0,
    val solarWindTemp: Double = 100.0,
    val bz: Double = 0.0,
    val bt: Double = 5.0,
    val bzLabel: String = "NEUTRAL",
    val bzHistory: List<Double> = emptyList(),
    val speedHistory: List<Double> = emptyList(),
    val densityHistory: List<Double> = emptyList(),
    // Extended from BioSpaceMonitor
    val gstActive: Boolean = false,
    val ipsCount: Int = 0,
    val hssActive: Boolean = false,
    val sepActive: Boolean = false,
    val hemisphericPower: Double = 20.0,
    val fountainDumping: String = "QUIET",
    val flares: List<FlareEntry> = emptyList(),
    val cmeSpeed: Double = 300.0,
    val cmeArrivalHrs: Int = 999,
    val cmeDirection: String = "Non-Halo",
    val timestamp: String = ""
)

data class FlareEntry(
    val flareClass: String = "B1.0",
    val startTime: String = "--:--",
    val direction: String = "Limb",
    val hasCme: Boolean = false
)

// ── Schumann (derived) ────────────────────────────────────────────────────────
data class SchumannData(
    val fundamentalHz: Double = 7.83,
    val freqDrift: Double = 0.0,
    val amplitudePt: Double = 1.0,
    val qFactor: Double = 4.0,
    val coherenceScore: Int = 84,
    val coherenceLabel: String = "HIGH COHERENCE",
    val intensityLabel: String = "NORMAL",
    val freqDriftLabel: String = "STABLE",
    val ampLabel: String = "NORMAL",
    val tecLocal: Double = 16.4,
    val tecDelta: Double = 0.1,
    val cavityHeight: String = "NOMINAL",
    val bzStability: String = "STABLE NORTH",
    val ampHistory: List<Double> = emptyList()
)

// ── Biometrics (watch + manual) ───────────────────────────────────────────────
data class Biometrics(
    val heartRate: Int = 0,
    val hrSource: String = "MANUAL",
    val spO2: Int = 0,
    val spO2Source: String = "MANUAL",
    val bpSys: Int = 0,
    val bpDia: Int = 0,
    val bpSource: String = "MANUAL",
    val rmssd: Float = 0f,
    val sdnn: Float = 0f,
    val pnn50: Float = 0f,
    val hrvSource: String = "MANUAL",
    val steps: Int = 0,
    val calories: Int = 0,
    val sleepHours: Float = 0f,
    val sleepQuality: Int = 0,
    val stressScore: Int = 0,
    val respirationRate: Int = 0,
    val isWatchConnected: Boolean = false
)

// ── Environment ───────────────────────────────────────────────────────────────
data class EnvData(
    val cityName: String = "Locating…",
    val lat: Double = 0.0,
    val lon: Double = 0.0,
    val tempF: Int = 0,
    val humidity: Int = 0,
    val pressureHpa: Double = 1013.0,
    val windMph: Int = 0,
    val uvIndex: Double = 3.0,
    val dewpointF: Int = 0,
    val tempLabel: String = "NORMAL",
    val humidLabel: String = "NORMAL",
    val pressureLabel: String = "STABLE",
    val windLabel: String = "LIGHT",
    val pressureDelta: Double = 0.0,
    val pressureHistory: List<Double> = emptyList(),
    val heatLoad: String = "LOW",
    val humidStress: String = "LOW",
    val heatIndex: Int = 0
)

// ── ANS / Burden Engine output ────────────────────────────────────────────────
data class AnsData(
    val loadIndex: Int = 0,
    val loadLabel: String = "LOW LOAD",
    val alertLevel: AlertLevel = AlertLevel.GREEN,
    val magnitude: Int = 0,
    val fluctuation: Int = 0,
    val coherencePct: Int = 74,
    val sympatheticBias: Int = 68,
    val fieldQuality: String = "ADEQUATE",
    val fieldQualityScore: Float = 0.65f,
    val ansBalance: Float = 0.65f,
    val hrvImpact: String = "NORMAL",
    val cortisol: String = "NORMAL",
    val melatonin: String = "VARIABLE",
    val symptoms: List<SymptomEntry> = emptyList(),
    val mitigationProtocol: List<String> = emptyList(),
    val breakdown: Map<String, BurdenComponent> = emptyMap(),
    val narrativeLine: String = ""
)

data class BurdenComponent(
    val name: String,
    val magnitude: Float,
    val fluctuation: Float,
    val combined: Float,
    val unit: String = "",
    val value: String = ""
)

enum class AlertLevel(val label: String, val instruction: String, val colorHex: String) {
    GREEN("FREE TO GO",
        "Space weather and biometrics are within normal range. You are free to go about your day.",
        "#00E87A"),
    YELLOW("PROCEED WITH CAUTION",
        "Elevated environmental or biometric stress detected. Go about your day but pay attention to how you feel.",
        "#F5C842"),
    RED("STOP AND ASSESS",
        "High ANS burden detected. Stop what you are doing. Sit down, assess how you feel, and do what is appropriate for your body.",
        "#FF3D5A"),
    BLUE("LAY DOWN IMMEDIATELY",
        "Severe ANS burden with rapid fluctuation detected. Stop all activity immediately. Lie down, breathe slowly, stay calm and relaxed.",
        "#1A8FFF")
}

data class SymptomEntry(
    val emoji: String,
    val name: String,
    val pct: Int,
    val level: String,
    val driver: String
)

// ── Symptom log (user-entered) ────────────────────────────────────────────────
data class SymptomLog(
    val id: Long = System.currentTimeMillis(),
    val timestamp: Long = System.currentTimeMillis(),
    val lightheadedness: Int = 0,
    val heartPounding: Int = 0,
    val fatigue: Int = 0,
    val brainFog: Int = 0,
    val chestPain: Int = 0,
    val nausea: Int = 0,
    val shortBreath: Int = 0,
    val tremors: Int = 0,
    val blurredVision: Int = 0,
    val headache: Int = 0,
    val notes: String = "",
    val kpAtLog: Double = 0.0,
    val bzAtLog: Double = 0.0,
    val solarWindAtLog: Double = 0.0,
    val hssAtLog: Boolean = false,
    val sepAtLog: Boolean = false,
    val gstAtLog: Boolean = false,
    val stormLevelAtLog: String = "G0",
    val schumannHzAtLog: Double = 7.83,
    val schumannQAtLog: Double = 4.0,
    val schumannAmpAtLog: Double = 1.0,
    val tempAtLog: Int = 0,
    val humidAtLog: Int = 0,
    val pressureAtLog: Double = 1013.0,
    val pressureDeltaAtLog: Double = 0.0,
    val heatIndexAtLog: Int = 0,
    val burdenAtLog: Int = 0,
    val alertLevelAtLog: String = "GREEN"
)
// ── Alert entries (NOAA) ──────────────────────────────────────────────────────
data class AlertEntry(
    val code: String,
    val messageCode: String,
    val serial: String,
    val issueTime: String,
    val body: String
)

// ── Assessment ────────────────────────────────────────────────────────────────
data class AssessData(
    val totalScore: Int = 0,
    val totalLabel: String = "LOW LOAD",
    val spaceScore: Int = 0,
    val spaceMax: Int = 40,
    val srScore: Int = 0,
    val srMax: Int = 30,
    val envScore: Int = 0,
    val envMax: Int = 30,
    val clinicalNarrative: String = ""
)

// ── Settings ──────────────────────────────────────────────────────────────────
enum class AutonomicProfile {
    STANDARD,       // Healthy autonomic function
    DYSAUTONOMIA    // POTS, IST, NCS, hyperadrenergic, vagal, etc.
}

data class AppSettings(
    val lat: Double = 40.71,
    val lon: Double = -74.01,
    val locationName: String = "",
    val useGps: Boolean = true,
    val watchMac: String = "",
    val geminiKey: String = "",
    val username: String = "",
    val autonomicProfile: AutonomicProfile = AutonomicProfile.STANDARD
)

// ── Chat ──────────────────────────────────────────────────────────────────────
data class ChatMessage(
    val callsign: String = "ANON",
    val message: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val kp: String = "0.00"
)

// ── Root app state ────────────────────────────────────────────────────────────
data class AppState(
    val space: SpaceWeatherData = SpaceWeatherData(),
    val schumann: SchumannData = SchumannData(),
    val ans: AnsData = AnsData(),
    val env: EnvData = EnvData(),
    val bio: Biometrics = Biometrics(),
    val alerts: List<AlertEntry> = emptyList(),
    val assess: AssessData = AssessData(),
    val symptomLogs: List<SymptomLog> = emptyList(),
    val settings: AppSettings = AppSettings(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val lastUpdated: String = "",
    val reportOutput: String = "",
    val reportLoading: Boolean = false,
    val stormForecast: SolarStormForecast = SolarStormForecast()
)

// ── Solar Storm Forecast ──────────────────────────────────────────────────────
data class SolarStormForecast(
    val hasThreat: Boolean = false,
    val severityScore: Int = 0,
    val severityLabel: String = "NONE",
    val estimatedArrivalHrs: Int = 999,
    val arrivalLabel: String = "No storm inbound",
    val peakDurationHrs: Int = 0,
    val dissipationHrs: Int = 0,
    val drivers: List<String> = emptyList(),
    val cmeCount: Int = 0,
    val cmeMaxSpeed: Double = 0.0,
    val flareMaxClass: String = "B",
    val coronalHoleActive: Boolean = false,
    val hssContributing: Boolean = false,
    val expectedKpMax: Double = 0.0,
    val gStormLevel: String = "G0",
    val narrative: String = "",
    val timestamp: String = ""
)
