package com.biospace.ansmonitorpro.viewmodel

import android.app.Application
import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.biospace.ansmonitorpro.ble.WatchRepository
import com.biospace.ansmonitorpro.data.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import com.google.gson.Gson
import java.util.concurrent.TimeUnit

private val Context.store by preferencesDataStore("ans_settings")
private val K_LAT  = doublePreferencesKey("lat")
private val K_LON  = doublePreferencesKey("lon")
private val K_GPS  = booleanPreferencesKey("gps")
private val K_NAME = stringPreferencesKey("loc_name")
private val K_GEM  = stringPreferencesKey("gemini_key")
private val K_USER = stringPreferencesKey("username")
private val K_MAC  = stringPreferencesKey("watch_mac")
private val K_PROFILE = stringPreferencesKey("autonomic_profile")

class MainViewModel(app: Application) : AndroidViewModel(app) {

    private val repo      = DataRepository()
    val watchRepo         = WatchRepository(app)

    private val _uiState  = MutableStateFlow(AppState(isLoading = true))
    val uiState: StateFlow<AppState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch { loadSettings() }
        // Re-compute burden whenever biometrics update
        viewModelScope.launch {
            watchRepo.bio.collect { bio ->
                _uiState.update { s ->
                    val ans = repo.computeAns(s.space, s.schumann, s.env, bio, s.settings.autonomicProfile)
                    s.copy(bio = bio, ans = ans)
                }
            }
        }
        viewModelScope.launch { refresh() }
        // Auto-refresh every 5 minutes
        viewModelScope.launch {
            while (true) { delay(5 * 60 * 1000L); refresh() }
        }
    }

    fun setLocation(lat: Double, lon: Double, city: String) {
        _uiState.update { it.copy(settings = it.settings.copy(lat = lat, lon = lon, locationName = city)) }
        viewModelScope.launch { saveSettings(); refresh() }
    }

    fun setUseGps(v: Boolean) {
        _uiState.update { it.copy(settings = it.settings.copy(useGps = v)) }
        viewModelScope.launch { saveSettings() }
    }

    fun setWatchMac(mac: String) {
        _uiState.update { it.copy(settings = it.settings.copy(watchMac = mac)) }
        viewModelScope.launch { saveSettings() }
    }

    fun setGeminiKey(key: String) {
        _uiState.update { it.copy(settings = it.settings.copy(geminiKey = key)) }
        viewModelScope.launch { saveSettings() }
    }

    fun setUsername(name: String) {
        _uiState.update { it.copy(settings = it.settings.copy(username = name)) }
        viewModelScope.launch { saveSettings() }
    }

    fun onProfileChange(profile: AutonomicProfile) {
        _uiState.update { it.copy(settings = it.settings.copy(autonomicProfile = profile)) }
        val current = _uiState.value
        val ans = repo.computeAns(current.space, current.schumann, current.env, current.bio, profile)
        _uiState.update { it.copy(ans = ans) }
    }

    fun logSymptom(s: SymptomLog) {
        _uiState.update { it.copy(symptomLogs = (listOf(s) + it.symptomLogs).take(100)) }
    }

    fun refresh() {
        val s = _uiState.value.settings
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val space  = repo.fetchSpaceWeather()
                val env    = repo.fetchEnvironment(s.lat, s.lon, s.locationName)
                val sr     = repo.deriveSchumann(space, env)
                val bio    = watchRepo.bio.value
                val ans    = repo.computeAns(space, sr, env, bio, _uiState.value.settings.autonomicProfile)
                val assess = repo.computeAssessment(space, sr, env, ans)
                val alerts = repo.fetchAlerts()
                val storm  = repo.fetchSolarStormForecast(space)
                _uiState.update {
                    it.copy(
                        space = space, env = env, schumann = sr,
                        ans = ans, assess = assess, alerts = alerts,
                        stormForecast = storm,
                        isLoading = false, lastUpdated = space.timestamp,
                        settings = it.settings.copy(locationName = env.cityName)
                    )
                }
                saveSettings()
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = "Fetch error: ${e.message?.take(80)}") }
            }
        }
    }

    fun generateReport(clinical: Boolean) {
        val key = _uiState.value.settings.geminiKey.trim()
        if (key.isBlank()) {
            _uiState.update { it.copy(reportOutput = "⚠ Enter your Gemini API key in Settings.") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(reportLoading = true, reportOutput = "") }
            try {
                val s = _uiState.value
                val style = if (clinical) "clinical with physiological mechanisms and research citations" else "plain language, easy for anyone to understand"
                val prompt = buildReportPrompt(s, style)
                val result = callGemini(key, prompt)
                _uiState.update { it.copy(reportOutput = result, reportLoading = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(reportOutput = "⚠ Report failed: ${e.message}", reportLoading = false) }
            }
        }
    }

    private fun buildReportPrompt(s: AppState, style: String): String {
        val sw = s.space; val bio = s.bio; val b = s.ans
        return """You are a heliobiological ANS health analyst. Generate a $style report.

⚠ DISCLAIMER: Begin with this exact text: "⚠ DISCLAIMER: This is an AI-generated informational report. It is NOT medical advice. Always consult your physician."

CURRENT DATA:
ANS Burden: ${b.loadIndex}% | Alert: ${b.alertLevel.name} | Magnitude: ${b.magnitude}% | Fluctuation: ${b.fluctuation}%
Kp: ${"%.1f".format(sw.kp)} | Solar Wind: ${sw.solarWindSpeed.toInt()} km/s | IMF Bz: ${"%.1f".format(sw.bz)} nT (${sw.bzLabel})
Flares: ${sw.flares.size} | CME: ${sw.cmeSpeed.toInt()} km/s arrival ${sw.cmeArrivalHrs}hrs | GST: ${sw.gstActive} | HSS: ${sw.hssActive}
Hemispheric Power: ${sw.hemisphericPower.toInt()} GW (${sw.fountainDumping})
SR Fundamental: ${s.schumann.fundamentalHz} Hz (drift ${s.schumann.freqDrift > 0? "+" : ""}${"%.3f".format(s.schumann.freqDrift)} Hz from 7.83) | Amplitude: ${"%.2f".format(s.schumann.amplitudePt)} pT | Q-Factor: ${"%.1f".format(s.schumann.qFactor)} | Coherence: ${s.schumann.coherenceScore}% (${s.schumann.coherenceLabel}) | Cavity: ${s.schumann.cavityHeight} | TEC: ${"%.1f".format(s.schumann.tecLocal)} TECU (${"%.1f".format(s.schumann.tecDelta)} from median)
Weather: ${s.env.tempF}°F | Humidity: ${s.env.humidity}% | Pressure: ${s.env.pressureHpa.toInt()} hPa | ΔP: ${"%.1f".format(s.env.pressureDelta)} hPa/hr
Heart Rate: ${if (bio.heartRate > 0) "${bio.heartRate} bpm (${bio.hrSource})" else "not recorded"}
BP: ${if (bio.bpSys > 0) "${bio.bpSys}/${bio.bpDia} mmHg" else "not recorded"}
SpO2: ${if (bio.spO2 > 0) "${bio.spO2}%" else "not recorded"}
HRV RMSSD: ${if (bio.rmssd > 0f) "${bio.rmssd.toInt()} ms" else "not recorded"}
Sleep: ${if (bio.sleepHours > 0f) "${bio.sleepHours} hrs" else "not recorded"}
Top burden drivers: ${b.breakdown.entries.sortedByDescending { it.value.combined }.take(5).joinToString(", ") { "${it.key} (${it.value.combined.toInt()}%)" }}

Sections: 1) Overall ANS Assessment  2) Space & Environmental Drivers  3) Schumann Resonance Impact (frequency drift effect on HRV coherence, sleep, and vagal tone — note active harmonic band and ANS pathway)  4) Biometric Findings  5) Fluctuation Analysis (critical for dysautonomia — oscillation more harmful than stable levels)  6) Expected Symptoms Next 12-24hrs  7) Mitigation Strategies  8) 48hr Outlook"""
    }

    private suspend fun callGemini(key: String, prompt: String): String = withContext(Dispatchers.IO) {
        val client = OkHttpClient.Builder().readTimeout(60, TimeUnit.SECONDS).build()
        val gson = Gson()
        val bodyJson = gson.toJson(mapOf(
            "contents" to listOf(mapOf("parts" to listOf(mapOf("text" to prompt)))),
            "generationConfig" to mapOf("temperature" to 0.3, "maxOutputTokens" to 2000)
        ))
        val body = bodyJson.toRequestBody("application/json".toMediaType())
        val req = Request.Builder()
            .url("https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=$key")
            .post(body).build()
        val resp = client.newCall(req).execute()
        val json = gson.fromJson(resp.body?.string(), Map::class.java)
        val candidates = json["candidates"] as? List<*>
        val content = (candidates?.firstOrNull() as? Map<*, *>)?.get("content") as? Map<*, *>
        val parts = content?.get("parts") as? List<*>
        (parts?.firstOrNull() as? Map<*, *>)?.get("text")?.toString() ?: "No response from Gemini."
    }

    private suspend fun loadSettings() {
        getApplication<Application>().store.data.first().let {
            val s = AppSettings(
                lat = it[K_LAT] ?: 40.71, lon = it[K_LON] ?: -74.01,
                useGps = it[K_GPS] ?: true, locationName = it[K_NAME] ?: "",
                geminiKey = it[K_GEM] ?: "", username = it[K_USER] ?: "",
                watchMac = it[K_MAC] ?: "",
                autonomicProfile = try { AutonomicProfile.valueOf(it[K_PROFILE] ?: "STANDARD") } catch (e: Exception) { AutonomicProfile.STANDARD }
            )
            _uiState.update { st -> st.copy(settings = s) }
        }
    }

    private suspend fun saveSettings() {
        val s = _uiState.value.settings
        getApplication<Application>().store.edit {
            it[K_LAT] = s.lat; it[K_LON] = s.lon; it[K_GPS] = s.useGps
            it[K_NAME] = s.locationName; it[K_GEM] = s.geminiKey
            it[K_USER] = s.username; it[K_MAC] = s.watchMac
            it[K_PROFILE] = s.autonomicProfile.name
        }
    }
}
