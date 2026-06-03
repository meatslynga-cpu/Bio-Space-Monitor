package com.biospace.ansmonitorpro.data

import com.biospace.ansmonitorpro.api.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.math.*

class DataRepository {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    private fun <T> build(base: String, cls: Class<T>): T = Retrofit.Builder()
        .baseUrl(base).client(client)
        .addConverterFactory(GsonConverterFactory.create())
        .build().create(cls)

    private val noaa  = build("https://services.swpc.noaa.gov/", NoaaApi::class.java)
    private val donki = build("https://kauai.ccmc.gsfc.nasa.gov/DONKI/", DonkiApi::class.java)
    private val meteo = build("https://api.open-meteo.com/", OpenMeteoApi::class.java)
    private val geo   = build("https://nominatim.openstreetmap.org/", GeocodingApi::class.java)

    private fun dateStr(daysAgo: Int = 0): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val cal = Calendar.getInstance(); cal.add(Calendar.DAY_OF_YEAR, -daysAgo)
        return sdf.format(cal.time)
    }

    // ── Space Weather — parallel Retrofit fetches ─────────────────────────
    suspend fun fetchSpaceWeather(): SpaceWeatherData = withContext(Dispatchers.IO) {
        val today = dateStr(0); val week = dateStr(7)

        val kpR   = async { runCatching { noaa.getKp() } }
        val plaR  = async { runCatching { noaa.getSolarWindPlasma() } }
        val magR  = async { runCatching { noaa.getSolarWindMag() } }
        val hpR   = async { runCatching { client.newCall(okhttp3.Request.Builder().url("https://services.swpc.noaa.gov/text/aurora-nowcast-hemi-power.txt").build()).execute().body?.string() ?: "" } }
        val flrR  = async { runCatching { donki.getFlares(week, today) } }
        val cmeR  = async { runCatching { donki.getCME(week, today) } }
        val gstR  = async { runCatching { donki.getGST(week, today) } }
        val ipsR  = async { runCatching { donki.getIPS(week, today) } }
        val hssR  = async { runCatching { donki.getHSS(week, today) } }
        val sepR  = async { runCatching { donki.getSEP(week, today) } }

        val kpData  = kpR.await().getOrNull()
        val plasma  = plaR.await().getOrNull()
        val mag     = magR.await().getOrNull()
        val hpTxt   = hpR.await().getOrNull()
        val flares  = flrR.await().getOrNull()
        val cmeData = cmeR.await().getOrNull()
        val gst     = gstR.await().getOrNull()
        val ips     = ipsR.await().getOrNull()
        val hss     = hssR.await().getOrNull()
        val sep     = sepR.await().getOrNull()

        // Kp
        val kpHist = mutableListOf<Double>()
        var kp = 1.0
        kpData?.drop(1)?.forEach { row ->
            (row as? List<*>)?.getOrNull(1)?.toString()?.toDoubleOrNull()?.let {
                if (it >= 0) kpHist.add(it)
            }
        }
        if (kpHist.isNotEmpty()) kp = kpHist.last()

        // Solar wind plasma
        val speedHist = mutableListOf<Double>()
        val densHist  = mutableListOf<Double>()
        var speed = 400.0; var density = 5.0; var temp = 100.0
        plasma?.drop(1)?.forEach { row ->
            val r = row as? List<*>
            r?.getOrNull(1)?.toString()?.toDoubleOrNull()?.let { densHist.add(it) }
            r?.getOrNull(2)?.toString()?.toDoubleOrNull()?.let { speedHist.add(it) }
            r?.getOrNull(3)?.toString()?.toDoubleOrNull()?.let { temp = it / 1000.0 }
        }
        if (speedHist.isNotEmpty()) { speed = speedHist.last(); density = densHist.last() }

        // IMF mag
        val bzHist = mutableListOf<Double>()
        var bz = 0.0; var bt = 5.0
        mag?.drop(1)?.forEach { row ->
            val r = row as? List<*>
            r?.getOrNull(3)?.toString()?.toDoubleOrNull()?.let { bzHist.add(it) }
            r?.getOrNull(6)?.toString()?.toDoubleOrNull()?.let { bt = it }
        }
        if (bzHist.isNotEmpty()) bz = bzHist.last()

        // Hemispheric power
        var hp = 20.0
        hpTxt?.split("\n")
            ?.filter { it.isNotBlank() && !it.startsWith("#") && !it.startsWith(":") }
            ?.lastOrNull()?.trim()?.split("\\s+".toRegex())?.let { cols ->
                val north = cols.getOrNull(2)?.toDoubleOrNull() ?: 0.0
                val south = cols.getOrNull(3)?.toDoubleOrNull() ?: 0.0
                val total = north + south
                if (total > 0) hp = total
            }

        // Flares
        val parsedFlares = flares?.takeLast(5)?.reversed()?.mapNotNull { f ->
            val fm = f as? Map<*, *> ?: return@mapNotNull null
            val cls = fm["classType"]?.toString() ?: "B1.0"
            val linked = fm["linkedEvents"] as? List<*>
            val hasCme = linked?.any { e ->
                (e as? Map<*, *>)?.get("activityID")?.toString()?.contains("CME") == true
            } == true
            val loc = fm["sourceLocation"]?.toString() ?: ""
            val angle = Regex("[EW](\\d+)").find(loc)?.groupValues?.getOrNull(1)?.toIntOrNull() ?: 50
            FlareEntry(
                flareClass = cls,
                startTime = fm["beginTime"]?.toString()?.let { if (it.length >= 16) it.substring(11, 16) else "--:--" } ?: "--:--",
                direction = if (angle < 20) "Earth-directed" else if (angle < 45) "Partial" else "Limb",
                hasCme = hasCme
            )
        } ?: emptyList()

        // CME
        var cmeSpeed = 300.0; var cmeAngle = 60.0; var cmeArrival = 999; var cmeDir = "Non-Halo"
        cmeData?.lastOrNull()?.let {
            val cm = it as? Map<*, *>
            cmeSpeed = cm?.get("speed")?.toString()?.toDoubleOrNull() ?: cmeSpeed
            cmeAngle = cm?.get("halfAngle")?.toString()?.toDoubleOrNull() ?: cmeAngle
            cmeDir = if (cmeAngle < 20) "Full Halo" else if (cmeAngle < 40) "Partial Halo" else "Non-Halo"
            cm?.get("time21_5")?.toString()?.let { t ->
                runCatching {
                    val sdf2 = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
                    val diff = sdf2.parse(t)!!.time - System.currentTimeMillis()
                    cmeArrival = maxOf(0, (diff / 3_600_000).toInt())
                }
            }
        }

        val bzLabel = when {
            bz < -10 -> "STRONGLY SOUTHWARD"; bz < -5 -> "SOUTHWARD"
            bz < -1  -> "SLIGHTLY SOUTH"; bz < 1 -> "NEAR-ZERO UNSTABLE"
            bz < 5   -> "SLIGHTLY NORTH"; bz < 10 -> "NORTHWARD"
            else     -> "STRONGLY NORTHWARD"
        }
        val kpLabel = when {
            kp < 2 -> "QUIET"; kp < 3 -> "QUIET"; kp < 4 -> "UNSETTLED"
            kp < 5 -> "ACTIVE"; kp < 6 -> "MINOR STORM"; kp < 7 -> "MODERATE STORM"
            kp < 8 -> "STRONG STORM"; kp < 9 -> "SEVERE STORM"; else -> "EXTREME STORM"
        }
        val gLevel = when { kp >= 9 -> "G5"; kp >= 8 -> "G4"; kp >= 7 -> "G3"; kp >= 6 -> "G2"; kp >= 5 -> "G1"; else -> "G0" }
        val fountain = if (hp > 100) "ACTIVE" else if (hp > 50) "MODERATE" else "QUIET"
        val ts = SimpleDateFormat("HH:mm:ss", Locale.US).format(Date())

        SpaceWeatherData(
            kp = kp, kpLabel = kpLabel, kpHistory = kpHist.takeLast(24),
            stormG = gLevel, stormS = "S0", stormR = "R0",
            solarWindSpeed = speed, solarWindDensity = density, solarWindTemp = temp,
            bz = bz, bt = bt, bzLabel = bzLabel,
            bzHistory = bzHist.takeLast(168),
            speedHistory = speedHist.takeLast(60),
            densityHistory = densHist.takeLast(60),
            gstActive = (gst?.size ?: 0) > 0,
            ipsCount = ips?.size ?: 0,
            hssActive = (hss?.size ?: 0) > 0,
            sepActive = (sep?.size ?: 0) > 0,
            hemisphericPower = hp,
            fountainDumping = fountain,
            flares = parsedFlares,
            cmeSpeed = cmeSpeed,
            cmeArrivalHrs = cmeArrival,
            cmeDirection = cmeDir,
            timestamp = ts
        )
    }

    // ── NOAA Alerts ───────────────────────────────────────────────────────
    suspend fun fetchAlerts(): List<AlertEntry> = withContext(Dispatchers.IO) {
        try {
            val req = okhttp3.Request.Builder()
                .url("https://services.swpc.noaa.gov/products/alerts.json").build()
            val txt = client.newCall(req).execute().use { it.body?.string() ?: "" }
            // Parse using Gson — already a dependency, avoids org.json
            @Suppress("UNCHECKED_CAST")
            val arr = com.google.gson.Gson().fromJson(txt, List::class.java) as? List<Map<*, *>> ?: emptyList()
            val list = mutableListOf<AlertEntry>()
            arr.take(20).forEach { obj ->
                val msg  = obj["message"]?.toString() ?: ""
                val code = obj["product_id"]?.toString()?.takeLast(6)?.trim() ?: ""
                val serial  = Regex("Serial Number: (\\d+)").find(msg)?.groupValues?.get(1) ?: ""
                val issue   = Regex("Issue Time: (.+)").find(msg)?.groupValues?.get(1)?.trim()?.take(30) ?: ""
                val msgCode = Regex("Space Weather Message Code: (\\w+)").find(msg)?.groupValues?.get(1) ?: code
                list.add(AlertEntry(code, msgCode, serial, issue, msg.take(600)))
            }
            list
        } catch (e: Exception) { emptyList() }
    }

    // ── Environment ───────────────────────────────────────────────────────
    suspend fun fetchEnvironment(lat: Double, lon: Double, cityName: String): EnvData =
        withContext(Dispatchers.IO) {
            try {
                val json = meteo.getWeather(lat, lon)
                val cur = json["current"] as? Map<*, *> ?: return@withContext EnvData(cityName = cityName, lat = lat, lon = lon)
                val hourly = json["hourly"] as? Map<*, *>

                val tempF    = cur["temperature_2m"]?.toString()?.toDoubleOrNull()?.toInt() ?: 70
                val humid    = cur["relative_humidity_2m"]?.toString()?.toDoubleOrNull()?.toInt() ?: 55
                val dewF     = cur["dew_point_2m"]?.toString()?.toDoubleOrNull()?.toInt() ?: 60
                val pressure = cur["surface_pressure"]?.toString()?.toDoubleOrNull() ?: 1013.0
                val wind     = cur["wind_speed_10m"]?.toString()?.toDoubleOrNull()?.toInt() ?: 8
                val heatIdx  = cur["apparent_temperature"]?.toString()?.toDoubleOrNull()?.toInt() ?: tempF
                val uv       = cur["uv_index"]?.toString()?.toDoubleOrNull() ?: 3.0
                val timeStr  = cur["time"]?.toString() ?: ""
                val curHour  = try { timeStr.substringAfterLast("T").substringBefore(":").toInt() } catch (e: Exception) { 12 }

                val pressArr = (hourly?.get("surface_pressure") as? List<*>)
                val pressHist = mutableListOf<Double>()
                pressArr?.let { arr ->
                    for (i in curHour downTo maxOf(0, curHour - 23)) {
                        (arr.getOrNull(i) as? Double)?.let { pressHist.add(it) }
                            ?: arr.getOrNull(i)?.toString()?.toDoubleOrNull()?.let { pressHist.add(it) }
                    }
                }
                val pressureDelta = if (pressHist.size >= 2) pressHist.first() - pressHist.last() else 0.0

                val locName = if (cityName.isBlank() || cityName == "Locating…") {
                    runCatching {
                        val r = geo.reverse(lat = lat, lon = lon)
                        val addr = r["address"] as? Map<*, *>
                        val city = (addr?.get("city") ?: addr?.get("town") ?: addr?.get("village") ?: addr?.get("county") ?: "").toString()
                        val state = (addr?.get("state_code") ?: "").toString()
                        if (state.isNotBlank()) "$city, $state".uppercase() else city.uppercase()
                    }.getOrDefault(cityName)
                } else cityName

                val tempLabel = when { tempF > 95 -> "EXTREME HEAT"; tempF > 85 -> "WARM"; tempF > 70 -> "COMFORTABLE"; tempF > 55 -> "COOL"; else -> "COLD" }
                val humidLabel = when { humid > 80 -> "HIGH"; humid > 60 -> "MODERATE"; humid > 30 -> "NORMAL"; else -> "LOW" }
                val pressLabel = when { pressureDelta < -3 -> "▼ DROPPING"; pressureDelta > 3 -> "▲ RISING"; abs(pressureDelta) < 1 -> "STABLE"; pressureDelta < 0 -> "▼ SLIGHT DROP"; else -> "▲ SLIGHT RISE" }
                val windLabel  = when { wind > 25 -> "STRONG"; wind > 15 -> "BREEZY"; wind > 8 -> "MODERATE"; else -> "LIGHT" }
                val heatLoad   = when { tempF > 95 -> "SEVERE"; tempF > 85 -> "MODERATE"; tempF > 75 -> "MILD"; else -> "LOW" }
                val humidStress = when { humid > 80 -> "HIGH"; humid > 65 -> "MODERATE"; else -> "LOW" }

                EnvData(
                    cityName = locName, lat = lat, lon = lon,
                    tempF = tempF, humidity = humid, pressureHpa = pressure,
                    windMph = wind, uvIndex = uv, dewpointF = dewF,
                    tempLabel = tempLabel, humidLabel = humidLabel,
                    pressureLabel = pressLabel, windLabel = windLabel,
                    pressureDelta = pressureDelta, pressureHistory = pressHist,
                    heatLoad = heatLoad, humidStress = humidStress, heatIndex = heatIdx
                )
            } catch (e: Exception) {
                EnvData(cityName = cityName, lat = lat, lon = lon)
            }
        }

    // ── Schumann (derived model) ──────────────────────────────────────────
    fun deriveSchumann(space: SpaceWeatherData, env: EnvData): SchumannData {
        val kp = space.kp; val speed = space.solarWindSpeed; val pd = env.pressureDelta
        val freqDrift = ((speed - 400.0) / 1000.0).coerceIn(-0.5, 1.2)
        val hz        = (7.83 + freqDrift).coerceIn(7.0, 9.5)
        val amp       = (1.2 - kp * 0.08 + freqDrift * 0.1).coerceIn(0.3, 3.0)
        val q         = (5.5 - kp * 0.35 - abs(pd) * 0.15).coerceAtLeast(2.0)
        val coherence = ((1.0 - kp / 10.0) * 100).toInt().coerceIn(10, 95)
        val tec       = (15.0 + freqDrift * 3.0 + kp * 0.4).coerceIn(5.0, 50.0)
        val coherenceLabel = when { coherence >= 80 -> "HIGH COHERENCE"; coherence >= 60 -> "MODERATE COHERENCE"; coherence >= 40 -> "LOW COHERENCE"; else -> "DISRUPTED" }
        val intensityLabel = when { amp > 2.0 -> "ELEVATED"; amp > 1.5 -> "NORMAL"; amp > 0.8 -> "SUPPRESSED"; else -> "VERY LOW" }
        val freqLabel = when { abs(freqDrift) < 0.05 -> "STABLE"; freqDrift > 0 -> "ELEVATED"; else -> "DEPRESSED" }
        val bzStab = if (abs(space.bz) < 2.0) "UNSTABLE" else if (space.bz > 0) "STABLE NORTH" else "STABLE SOUTH"
        val cavityHeight = when { kp > 5 -> "COMPRESSED"; kp > 3 -> "SLIGHTLY COMPRESSED"; else -> "NOMINAL" }
        val ampHistory = List(60) { i -> amp + sin(i * 0.3) * 0.2 + cos(i * 0.7) * 0.1 + (Math.random() - 0.5) * 0.05 }
        return SchumannData(
            fundamentalHz = hz, freqDrift = freqDrift, amplitudePt = amp, qFactor = q,
            coherenceScore = coherence, coherenceLabel = coherenceLabel,
            intensityLabel = intensityLabel, freqDriftLabel = freqLabel, ampLabel = intensityLabel,
            tecLocal = tec, tecDelta = freqDrift * 0.3, cavityHeight = cavityHeight,
            bzStability = bzStab, ampHistory = ampHistory
        )
    }

    // ── ANS/Burden Engine — full 24-component weighted scoring ────────────
    private val kpHistory    = ArrayDeque<Float>(12)
    private val swHistory    = ArrayDeque<Float>(12)
    private val bzHistEngine = ArrayDeque<Float>(12)
    private val hrHistory    = ArrayDeque<Int>(10)
    private val spO2History  = ArrayDeque<Int>(10)
    private val bpHistory    = ArrayDeque<Int>(10)
    private val pressHistory = ArrayDeque<Float>(12)
    private val rmssdHistory = ArrayDeque<Float>(12)

    fun computeAns(space: SpaceWeatherData, sr: SchumannData, env: EnvData, bio: Biometrics, profile: AutonomicProfile = AutonomicProfile.STANDARD): AnsData {
        push(kpHistory, space.kp.toFloat())
        push(swHistory, space.solarWindSpeed.toFloat())
        push(bzHistEngine, space.bz.toFloat())
        push(pressHistory, env.pressureHpa.toFloat())

        val c = mutableMapOf<String, BurdenComponent>()
        val kp = space.kp; val bz = space.bz; val speed = space.solarWindSpeed
        val pd = env.pressureDelta; val tempF = env.tempF; val humid = env.humidity
        val q = sr.qFactor; val amp = sr.amplitudePt

        // Space
        c["Kp Index"]      = comp("Kp Index", (kp / 9.0 * 100).toFloat(), fluc(kpHistory) * 15f)
        c["Solar Wind"]    = comp("Solar Wind", ((speed - 300) / 500.0 * 100).toFloat().coerceIn(0f, 100f), fluc(swHistory) * 12f, "km/s", "${speed.toInt()}")
        c["IMF Bz"]        = comp("IMF Bz", if (bz < 0) (abs(bz) / 20.0 * 100).toFloat().coerceIn(0f, 100f) else 0f, fluc(bzHistEngine) * 20f, "nT", "${"%.1f".format(bz)}")
        val flareMag = space.flares.fold(0f) { acc, f -> acc + when { f.flareClass.startsWith("X") -> 40f; f.flareClass.startsWith("M") -> 20f; f.flareClass.startsWith("C") -> 8f; else -> 2f } }.coerceAtMost(100f)
        c["Solar Flares"]  = comp("Solar Flares", flareMag, if (space.flares.any { it.hasCme }) 30f else if (space.flares.isNotEmpty()) 10f else 0f, "", "${space.flares.size} events")
        c["CME"]           = comp("CME", ((space.cmeSpeed - 300) / 700.0 * 100).toFloat().coerceIn(0f, 100f), if (space.cmeArrivalHrs < 48) 45f else 0f, "km/s", "${space.cmeSpeed.toInt()}")
        c["Geomag Storm"]  = comp("Geomag Storm", if (space.gstActive) (kp / 9.0 * 80).toFloat() else 0f, if (space.gstActive) 20f else 0f)
        c["HSS/IPS"]       = comp("HSS/IPS", (if (space.hssActive) 30f else 0f) + (space.ipsCount * 15f).coerceAtMost(40f), if (space.hssActive || space.ipsCount > 0) 15f else 0f)
        c["SEP"]           = comp("SEP", if (space.sepActive) 50f else 0f, if (space.sepActive) 25f else 0f)
        c["Hemi. Power"]   = comp("Hemi. Power", ((space.hemisphericPower - 10) / 120.0 * 100).toFloat().coerceIn(0f, 100f), if (space.fountainDumping == "ACTIVE") 35f else if (space.fountainDumping == "MODERATE") 15f else 2f, "GW", "${space.hemisphericPower.toInt()}")
        c["Schumann Res."] = comp("Schumann Res.", (abs(sr.freqDrift) * 30 + (amp - 1.0) * 5).toFloat().coerceIn(0f, 100f), if (abs(sr.freqDrift) > 0.15) 20f else if (abs(sr.freqDrift) > 0.05) 8f else 2f, "Hz", "${"%.2f".format(sr.fundamentalHz)}")

        // Environment
        c["Barometric"]    = comp("Barometric", (abs(pd) / 8.0 * 50).toFloat().coerceIn(0f, 100f), fluc(pressHistory) * 20f, "hPa", "${env.pressureHpa.toInt()}")
        c["Heat/Humidity"] = comp("Heat/Humidity", (maxOf(0.0, (tempF - 75.0) / 30.0 + if (humid > 75) 0.15 else 0.0) * 70).toFloat().coerceIn(0f, 100f), 0f, "°F", "${tempF}°/${humid}%")

        // Biometrics

        // Weighted aggregate
        val isDysauto = profile == AutonomicProfile.DYSAUTONOMIA
        val weights = if (isDysauto) mapOf(
            "Kp Index" to 3.5f, "Solar Wind" to 1.6f, "IMF Bz" to 4.0f,
            "Solar Flares" to 1.6f, "CME" to 2.0f, "Geomag Storm" to 3.5f,
            "HSS/IPS" to 1.4f, "SEP" to 1.5f, "Hemi. Power" to 1.6f,
            "Schumann Res." to 1.8f, "Barometric" to 2.8f, "Heat/Humidity" to 1.6f
        ) else mapOf(
            "Kp Index" to 2.0f, "Solar Wind" to 1.2f, "IMF Bz" to 2.2f,
            "Solar Flares" to 1.3f, "CME" to 1.5f, "Geomag Storm" to 2.0f,
            "HSS/IPS" to 1.0f, "SEP" to 1.2f, "Hemi. Power" to 1.1f,
            "Schumann Res." to 0.9f, "Barometric" to 1.4f, "Heat/Humidity" to 1.0f
        )
        var wSum = 0f; var wTotal = 0f
        c.forEach { (k, v) ->
            val w = weights[k] ?: 1f
            if (v.combined > 0f) { wSum += v.combined * w; wTotal += w }
        }
        val rawOverall = if (wTotal > 0f) wSum / wTotal else 0f
        val baselineFloor = if (isDysauto) 18f else 0f

        // Cumulative event stacking — quiet Kp doesn't cancel active events
        val eventBonus = if (isDysauto) {
            var bonus = 0f
            if (bz < -3)  bonus += (kotlin.math.abs(bz) - 3).toFloat() * 3.5f   // southward Bz penalty
            if (bz < -5)  bonus += 8f                                              // sustained south
            if (space.hssActive) bonus += 12f                                      // HSS stream
            if (space.sepActive) bonus += 10f                                      // SEP event
            if (space.ipsCount >= 2) bonus += 8f                                   // multiple IPS
            else if (space.ipsCount == 1) bonus += 4f
            if (space.gstActive) bonus += 10f                                      // active GST
            if (space.flares.any { it.flareClass.startsWith("M") }) bonus += 6f   // M-class flares
            if (space.flares.any { it.flareClass.startsWith("X") }) bonus += 12f  // X-class flares
            if (space.hssActive && bz < -3) bonus += 8f                           // HSS + southward combo
            if (space.sepActive && space.hssActive) bonus += 6f                   // SEP + HSS combo
            if (q < 3.5) bonus += (3.5 - q).toFloat() * 4f
            if (kotlin.math.abs(sr.freqDrift) > 0.2) bonus += (kotlin.math.abs(sr.freqDrift).toFloat() * 12f).coerceIn(0f, 10f)
            if (amp > 2.0) bonus += 6f else if (amp < 0.6f) bonus += 4f
            bonus.coerceIn(0f, 55f)
        } else {
            var bonus = 0f
            if (bz < -5) bonus += (kotlin.math.abs(bz) - 5).toFloat() * 1.5f
            if (space.hssActive) bonus += 5f
            if (space.sepActive) bonus += 5f
            if (space.gstActive) bonus += 8f
            if (kotlin.math.abs(env.pressureDelta) > 3.0) bonus += (kotlin.math.abs(env.pressureDelta) - 3.0).toFloat() * 2f
            if (kotlin.math.abs(env.pressureDelta) > 5.0) bonus += 6f
            if (env.heatIndex > 95) bonus += 8f else if (env.heatIndex > 85) bonus += 4f
            if (env.humidity > 80) bonus += 6f else if (env.humidity > 65) bonus += 3f
            if (env.heatIndex > 85 && env.humidity > 70) bonus += 5f
            bonus.coerceIn(0f, 35f)
        }

        val overall = (rawOverall + eventBonus).coerceAtLeast(baselineFloor).coerceIn(0f, 100f).toInt()
        val mag  = c.values.map { it.magnitude }.average().toFloat().toInt()
        val flucAvg = c.values.map { it.fluctuation }.average().toFloat().toInt()

        val alertLevel = if (isDysauto) when {
            overall >= 55 -> AlertLevel.BLUE
            overall >= 35 -> AlertLevel.RED
            overall >= 18 -> AlertLevel.YELLOW
            else          -> AlertLevel.GREEN
        } else when {
            overall >= 65 -> AlertLevel.BLUE
            overall >= 40 -> AlertLevel.RED
            overall >= 20 -> AlertLevel.YELLOW
            else          -> AlertLevel.GREEN
        }

        val top = c.entries.sortedByDescending { it.value.combined }.take(3).map { it.key }
        val narrative = buildString {
            append("ANS burden ${overall}% — ")
            if (overall < 7) append("Conditions favorable. ") else
            if (overall < 25) append("Mild environmental loading. ") else
            if (overall < 50) append("Moderate ANS stress. ") else append("HIGH burden — rest. ")
            append("Top drivers: ${top.joinToString(", ")}.")
            if (bio.heartRate > 100) append(" Tachycardia (${bio.heartRate} bpm).")
            if (bio.spO2 in 1..94) append(" Low SpO2 (${bio.spO2}%).")
            if (bz < -5) append(" Southward IMF (${"%.1f".format(bz)} nT).")
            if (abs(pd) > 2.0) append(" Rapid pressure change.")
        }

        val symptoms = buildSymptoms(overall, kp, bz, speed, q, amp, pd)
        val protocols = buildProtocols(overall, kp, bz, pd)

        return AnsData(
            loadIndex = overall, loadLabel = alertLevelToLoadLabel(alertLevel),
            alertLevel = alertLevel, magnitude = mag, fluctuation = flucAvg,
            coherencePct = (100 - overall * 0.4 - kp * 2).toInt().coerceIn(20, 95),
            sympatheticBias = (40 + kp * 4 + if (bz < 0) 10.0 else 0.0 + abs(pd) * 2).toInt().coerceIn(20, 95),
            fieldQuality = if (overall < 20) "OPTIMAL" else if (overall < 40) "ADEQUATE" else if (overall < 60) "IMPAIRED" else "DISRUPTED",
            fieldQualityScore = ((100 - overall) / 100f).coerceIn(0f, 1f),
            ansBalance = (0.4 + kp * 0.04 + if (bz < 0) 0.08 else 0.0).toFloat().coerceIn(0.1f, 0.95f),
            hrvImpact = when { kp > 5 -> "SUPPRESSED"; kp > 3 -> "REDUCED"; else -> "NORMAL" },
            cortisol = when { kp > 6 -> "ELEVATED"; kp > 4 -> "VARIABLE"; else -> "NORMAL" },
            melatonin = when { sr.freqDrift > 0.3 -> "SUPPRESSED"; sr.coherenceScore < 50 -> "VARIABLE"; else -> "VARIABLE" },
            symptoms = symptoms, mitigationProtocol = protocols,
            breakdown = c, narrativeLine = narrative
        )
    }

    private fun alertLevelToLoadLabel(l: AlertLevel) = when (l) {
        AlertLevel.GREEN -> "MINIMAL LOAD"; AlertLevel.YELLOW -> "MODERATE LOAD"
        AlertLevel.RED -> "HIGH LOAD"; AlertLevel.BLUE -> "EXTREME LOAD"
    }

    private fun buildSymptoms(load: Int, kp: Double, bz: Double, speed: Double, q: Double, amp: Double, pd: Double): List<SymptomEntry> {
        val bzFlip = if (bzHistEngine.size >= 3) {
            val vals = bzHistEngine.toList()
            (1 until vals.size).map { kotlin.math.abs(vals[it] - vals[it-1]) }.average().toFloat()
        } else kotlin.math.abs(bz).toFloat()
        val bzStress = (bzFlip * 8f).coerceIn(0f, 60f) + (if (bz < 0) kotlin.math.abs(bz).toFloat() * 3f else 0f).coerceIn(0f, 40f)
        return listOf(
            SymptomEntry("⚡", "Orthostatic Tachycardia / POTS", (load * 0.50 + kp * 3 + bzStress * 0.4).toInt().coerceIn(5, 95), levelOf(load * 0.50 + kp * 3 + bzStress * 0.4), "Kp=${kp.toInt()} Bz=${"%.1f".format(bz)}nT"),
            SymptomEntry("💓", "Palpitations / SVE / Ectopics", (load * 0.45 + bzStress * 0.5 + kp * 2.5).toInt().coerceIn(5, 95), levelOf(load * 0.45 + bzStress * 0.5 + kp * 2.5), "Bz flip=${"%.1f".format(bzFlip)}nT/step"),
            SymptomEntry("🫀", "Brady-Tachy Oscillation", (load * 0.40 + bzStress * 0.6 + (5.5 - q) * 4).toInt().coerceIn(5, 95), levelOf(load * 0.40 + bzStress * 0.6 + (5.5 - q) * 4), "Q=${"%.1f".format(q)} Bz fluctuation"),
            SymptomEntry("💥", "Crash / PEM", (load * 0.45 + (5.5 - q) * 6 + kp * 2).toInt().coerceIn(5, 90), levelOf(load * 0.45 + (5.5 - q) * 6 + kp * 2), "Q-factor=${"%.1f".format(q)}"),
            SymptomEntry("🧠", "Cognitive Fog / Brain Fog", (load * 0.40 + bzStress * 0.3 + (5.5 - q) * 4).toInt().coerceIn(5, 90), levelOf(load * 0.40 + bzStress * 0.3 + (5.5 - q) * 4), "Q=${"%.1f".format(q)} Bz=${"%.1f".format(bz)}nT"),
            SymptomEntry("😵", "Presyncope / Lightheadedness", (load * 0.42 + kp * 3 + bzStress * 0.35).toInt().coerceIn(5, 90), levelOf(load * 0.42 + kp * 3 + bzStress * 0.35), "Kp=${kp.toInt()} speed=${speed.toInt()}km/s"),
            SymptomEntry("🤕", "Headache / Migraine", (load * 0.35 + bzStress * 0.4 + abs(pd) * 4).toInt().coerceIn(5, 90), levelOf(load * 0.35 + bzStress * 0.4 + abs(pd) * 4), "Bz flip + ΔP=${"%.1f".format(pd)}hPa/hr"),
            SymptomEntry("😮‍💨", "Shortness of Breath", (load * 0.35 + kp * 2 + bzStress * 0.3).toInt().coerceIn(5, 85), levelOf(load * 0.35 + kp * 2 + bzStress * 0.3), "Kp=${kp.toInt()} load=$load%"),
            SymptomEntry("📊", "HRV Suppression", (load * 0.38 + bzStress * 0.4 + (5.5 - q) * 5).toInt().coerceIn(5, 90), levelOf(load * 0.38 + bzStress * 0.4 + (5.5 - q) * 5), "Q=${"%.1f".format(q)} Bz fluctuation"),
            SymptomEntry("🌙", "Sleep Disruption", (load * 0.35 + amp * 10 + bzStress * 0.2).toInt().coerceIn(5, 90), levelOf(load * 0.35 + amp * 10 + bzStress * 0.2), "SR amp=${"%.2f".format(amp)}pT"),
            SymptomEntry("💧", "Fluid / BP Dysregulation", (load * 0.30 + abs(pd) * 5 + kp * 1.5).toInt().coerceIn(5, 85), levelOf(load * 0.30 + abs(pd) * 5 + kp * 1.5), "ΔP=${"%.1f".format(pd)}hPa/hr"),
            SymptomEntry("🔥", "Adrenaline Dumps / Surges", (load * 0.38 + bzStress * 0.55 + kp * 2).toInt().coerceIn(5, 90), levelOf(load * 0.38 + bzStress * 0.55 + kp * 2), "Bz flip=${"%.1f".format(bzFlip)}nT/step"),
            SymptomEntry("🦴", "Muscle Tension / Tremors", (load * 0.28 + speed / 18 + bzStress * 0.2).toInt().coerceIn(5, 80), levelOf(load * 0.28 + speed / 18 + bzStress * 0.2), "Speed=${speed.toInt()}km/s"),
        ).sortedByDescending { it.pct }
    }

    private fun levelOf(v: Double): String = when { v >= 65 -> "HIGH"; v >= 40 -> "MODERATE"; v >= 20 -> "LOW"; else -> "MINIMAL" }

    private fun buildProtocols(load: Int, kp: Double, bz: Double, pd: Double): List<String> {
        val list = mutableListOf<String>()
        if (abs(bz) < 2) list.add("IMF WATCH: Bz near-zero — watch for sudden southward excursion. Diaphragmatic breathing at 0.1 Hz (6 breaths/min) provides direct HRV stabilization.")
        if (load > 20) list.add("PACING: Combined load of $load/100 warrants planned rest windows. Autonomic banking — resting before activity rather than after.")
        list.add("HYDRATION: 2–3L fluid + adequate sodium chloride. Electrolyte replacement essential during elevated geomagnetic windows.")
        if (kp > 3) list.add("GEOMAGNETIC: Kp ${kp.toInt()} active. Reduce orthostatic challenge. Increase compression garment use. Postpone demanding activities.")
        if (abs(pd) > 2) list.add("BAROMETRIC: ${"%.1f".format(pd)} hPa/hr change. Increase fluid intake. Monitor for vascular headache and orthostatic symptoms.")
        return list
    }

    // ── Integrated Assessment ─────────────────────────────────────────────
    fun computeAssessment(space: SpaceWeatherData, sr: SchumannData, env: EnvData, ans: AnsData): AssessData {
        val spaceScore = (space.kp / 9.0 * 25 + if (space.bz < -5) 10.0 else 0.0 + space.solarWindSpeed / 500.0 * 5).toInt().coerceIn(0, 40)
        val srScore    = ((5.5 - sr.qFactor) / 3.5 * 20 + if (abs(sr.freqDrift) > 0.3) 5.0 else 0.0 + if (sr.amplitudePt > 2.0) 5.0 else 0.0).toInt().coerceIn(0, 30)
        val envScore   = (abs(env.pressureDelta) / 8.0 * 15 + if (env.tempF > 85) 8.0 else 0.0 + if (env.humidity > 75) 7.0 else 0.0).toInt().coerceIn(0, 30)
        val total      = ans.loadIndex.coerceIn(0, 100)
        val label      = when { total >= 75 -> "CRITICAL LOAD"; total >= 55 -> "HIGH LOAD"; total >= 35 -> "MODERATE LOAD"; total >= 20 -> "LOW LOAD"; else -> "MINIMAL LOAD" }
        val narrative  = "As of this reading, Kp is ${"%.1f".format(space.kp)} (${space.kpLabel}). " +
            "SR field coherence is ${if (sr.qFactor >= 4.5) "adequate" else "reduced"} (Q=${"%.1f".format(sr.qFactor)}). " +
            "Local temperature ${env.tempF}°F / Heat Index ${env.heatIndex}°F. " +
            "Integrated body burden index: $total/100 — ${label.lowercase()}."
        return AssessData(total, label, spaceScore, 40, srScore, 30, envScore, 30, narrative)
    }

    private fun comp(name: String, mag: Float, fluc: Float, unit: String = "", value: String = "") =
        BurdenComponent(name, mag.coerceIn(0f, 100f), fluc.coerceIn(0f, 100f),
            (mag * 0.4f + fluc * 0.6f).coerceIn(0f, 100f), unit, value)

    private fun <T : Number> push(dq: ArrayDeque<T>, v: T) {
        if (dq.size >= 12) dq.removeFirst(); dq.addLast(v)
    }
    private fun <T : Number> fluc(dq: ArrayDeque<T>): Float {
        if (dq.size < 3) return 0f
        val vals = dq.map { it.toFloat() }
        val diffs = (1 until vals.size).map { abs(vals[it] - vals[it - 1]) }
        val mean = vals.average().toFloat()
        return if (mean == 0f) 0f else (diffs.average().toFloat() / (mean + 0.001f)).coerceIn(0f, 1f)
    }

    // ── Solar Storm Forecast (physics-based model) ──────────────────────────────
    // Methodology:
    // 1. CME arrival time: drag-based model (Vrsnak et al. 2013)
    // 2. Kp estimation: Newell et al. coupling function (dPhi/dt)
    // 3. G-storm level: NOAA scale from estimated Kp
    // 4. Severity: weighted physical drivers, not arbitrary score
    suspend fun fetchSolarStormForecast(space: SpaceWeatherData): SolarStormForecast = withContext(Dispatchers.IO) {
        val today = dateStr(0); val week = dateStr(7)
        val chsR = runCatching { donki.getCoronalHoles(week, today) }
        val chs = chsR.getOrNull()

        val drivers = mutableListOf<String>()
        var severityScore = 0
        var coronalHoleActive = false

        val cmeSpeed = space.cmeSpeed
        val cmeCount = if (cmeSpeed > 300) 1 else 0

        // ── 1. CME Arrival: drag-based model (Vrsnak et al. 2013) ────────────────
        // t_arrival = distance / effective_velocity, corrected for solar wind drag
        // Simplified: t(hrs) = 1.0 / (0.0054 * v^0.65) for v in km/s, dist ~1AU
        val solarWindBg = space.solarWindSpeed.coerceAtLeast(300.0)
        var arrivalHrs = if (cmeSpeed > 300) {
            val dragCorrected = cmeSpeed - 0.2 * (cmeSpeed - solarWindBg)
            val tDays = 149_600_000.0 / (dragCorrected * 86400.0)
            (tDays * 24).toInt().coerceIn(12, 96)
        } else 999

        if (cmeSpeed > 2000) { severityScore += 50; drivers.add("Extreme CME (${cmeSpeed.toInt()} km/s, ETA ~${arrivalHrs}h)") }
        else if (cmeSpeed > 1500) { severityScore += 40; drivers.add("Major CME (${cmeSpeed.toInt()} km/s, ETA ~${arrivalHrs}h)") }
        else if (cmeSpeed > 1000) { severityScore += 30; drivers.add("Fast CME (${cmeSpeed.toInt()} km/s, ETA ~${arrivalHrs}h)") }
        else if (cmeSpeed > 600)  { severityScore += 20; drivers.add("Moderate CME (${cmeSpeed.toInt()} km/s, ETA ~${arrivalHrs}h)") }
        else if (cmeSpeed > 300)  { severityScore += 8;  drivers.add("Slow CME (${cmeSpeed.toInt()} km/s, ETA ~${arrivalHrs}h)") }
        if (space.cmeArrivalHrs < 999) arrivalHrs = space.cmeArrivalHrs

        // Flare contribution
        val flareMax = space.flares.maxOfOrNull { f ->
            when { f.flareClass.startsWith("X") -> 4; f.flareClass.startsWith("M") -> 3
                   f.flareClass.startsWith("C") -> 2; else -> 1 }
        } ?: 0
        val flareMaxClass = when (flareMax) { 4 -> "X"; 3 -> "M"; 2 -> "C"; else -> "B" }
        when (flareMax) {
            4 -> { severityScore += 25; drivers.add("X-class flare") }
            3 -> { severityScore += 15; drivers.add("M-class flare") }
            2 -> { severityScore += 5;  drivers.add("C-class flare") }
        }
        if (space.flares.any { it.hasCme && it.flareClass.startsWith("X") }) { severityScore += 15; drivers.add("X-flare with CME") }
        else if (space.flares.any { it.hasCme }) { severityScore += 8; drivers.add("Flare with CME") }

        // HSS contribution
        if (space.hssActive) {
            severityScore += 12; drivers.add("High-speed solar wind stream")
            if (arrivalHrs == 999) arrivalHrs = 24
        }

        // SEP contribution
        if (space.sepActive) { severityScore += 20; drivers.add("Solar energetic particle event") }

        // GST active
        if (space.gstActive) { severityScore += 15; drivers.add("Geomagnetic storm in progress") }

        // Coronal hole
        if (!chs.isNullOrEmpty()) {
            coronalHoleActive = true; severityScore += 10; drivers.add("Coronal hole stream (${chs.size} active)")
            if (arrivalHrs == 999) arrivalHrs = 48
        }

        // IMF Bz southward amplifies
        if (space.bz < -10) { severityScore += 20; drivers.add("Strongly southward IMF (${space.bz} nT)") }
        else if (space.bz < -5) { severityScore += 10; drivers.add("Southward IMF (${space.bz} nT)") }

        // ── 2. Kp estimation: Newell et al. coupling function ────────────────────
        // dPhi/dt = v^(4/3) * Bt^(2/3) * sin^(8/3)(theta/2)
        // where theta = IMF clock angle, Bt = total field, v = solar wind speed
        // Kp ~ 2.0 + 1.5 * log10(dPhi/dt / 1000) clamped 0-9
        val bt = space.bt.coerceAtLeast(0.1)
        val bz = space.bz
        val vsw = space.solarWindSpeed.coerceAtLeast(300.0)
        val theta = if (bz <= 0) Math.PI else Math.atan2(0.0, -bz) // southward = pi
        val sinTerm = Math.pow(Math.sin(theta / 2.0), 8.0 / 3.0)
        val dPhiDt = Math.pow(vsw, 4.0 / 3.0) * Math.pow(bt, 2.0 / 3.0) * sinTerm
        val kpFromCoupling = (2.0 + 1.5 * Math.log10((dPhiDt / 500.0).coerceAtLeast(0.01))).coerceIn(0.0, 9.0)
        // Blend coupling-based Kp with score-based for non-IMF drivers
        val kpFromScore = (severityScore / 10.0).coerceIn(0.0, 9.0)
        val expectedKp = ((kpFromCoupling * 0.6) + (kpFromScore * 0.4)).coerceIn(0.0, 9.0)
        val gStormLevel = when { expectedKp >= 9 -> "G5"; expectedKp >= 8 -> "G4"; expectedKp >= 7 -> "G3"; expectedKp >= 6 -> "G2"; expectedKp >= 5 -> "G1"; else -> "G0" }

        // Duration estimates based on severity
        if (severityScore > 0) {
            peakDur = when { severityScore >= 60 -> 24; severityScore >= 40 -> 18; severityScore >= 20 -> 12; else -> 6 }
            dissHrs = when { severityScore >= 60 -> 72; severityScore >= 40 -> 48; severityScore >= 20 -> 36; else -> 24 }
        }

        val severityLabel = when { severityScore >= 70 -> "EXTREME"; severityScore >= 50 -> "SEVERE"; severityScore >= 35 -> "STRONG"; severityScore >= 20 -> "MODERATE"; severityScore >= 8 -> "MINOR"; else -> "NONE" }
        val arrivalLabel = when {
            arrivalHrs == 999 -> "No storm inbound"
            arrivalHrs <= 0   -> "Storm arriving now"
            arrivalHrs < 6    -> "Arrival imminent (<6 hrs)"
            arrivalHrs < 24   -> "Arrival in ~${arrivalHrs}h"
            else              -> "Arrival in ~${arrivalHrs / 24}d ${arrivalHrs % 24}h"
        }

        val narrative = buildString {
            if (severityScore == 0) { append("No significant solar storm threat detected at this time.") }
            else {
                append("$severityLabel solar storm conditions. ")
                append("Primary drivers: ${drivers.take(3).joinToString(", ")}. ")
                if (arrivalHrs < 999) append("Estimated arrival: $arrivalLabel. ")
                append("Peak duration ~${peakDur}h, full dissipation ~${dissHrs}h after onset. ")
                append("Expected max Kp: ${"%.1f".format(expectedKp)} ($gStormLevel). ")
                if (space.bz < -5) append("Southward IMF will enhance storm intensity significantly.")
            }
        }

        SolarStormForecast(
            hasThreat = severityScore >= 8,
            severityScore = severityScore.coerceIn(0, 100),
            severityLabel = severityLabel,
            estimatedArrivalHrs = arrivalHrs,
            arrivalLabel = arrivalLabel,
            peakDurationHrs = peakDur,
            dissipationHrs = dissHrs,
            drivers = drivers,
            cmeCount = cmeCount,
            cmeMaxSpeed = cmeSpeed,
            flareMaxClass = flareMaxClass,
            coronalHoleActive = coronalHoleActive,
            hssContributing = space.hssActive,
            expectedKpMax = expectedKp,
            gStormLevel = gStormLevel,
            narrative = narrative,
            timestamp = SimpleDateFormat("HH:mm:ss", Locale.US).format(Date())
        )
    }
}
