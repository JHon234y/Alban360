package com.example.data

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

@JsonClass(generateAdapter = true)
data class NominatimResult(
    val lat: String,
    val lon: String,
    @Json(name = "display_name") val displayName: String,
)

interface NominatimApi {
    @GET("search")
    suspend fun search(
        @Query("q") query: String,
        @Query("format") format: String = "json",
        @Query("limit") limit: Int = 1,
        @Header("User-Agent") userAgent: String = "AlbanCafe360-Android/1.0",
    ): List<NominatimResult>
}

data class FncCheckStatus(
    val reachable: Boolean,
    val message: String,
    val checkedAt: String,
)

data class LiveMarketSnapshot(
    val trmCop: Double? = null,
    val trmPrevCop: Double? = null,
    val trmDate: String? = null,
    val trmSource: String = "Datos Abiertos · Superfinanciera",
    val trmError: String? = null,
    val nyCoffeeCentsLb: Double? = null,
    val nyPrevCloseCentsLb: Double? = null,
    val nySymbol: String = "KC=F",
    val nyExchange: String = "ICE / NYBOT (Futuros Café C)",
    val nyError: String? = null,
    val fncDetectedCargaCop: Double? = null,
    val fncStatus: FncCheckStatus = FncCheckStatus(
        reachable = false,
        message = "Pulsa actualizar para consultar mercados en vivo y acceso FNC.",
        checkedAt = "Sin consultar",
    ),
    val fetchedAt: String = "Sin consultar",
) {
    val trmDeltaCop: Double?
        get() = if (trmCop != null && trmPrevCop != null && trmPrevCop > 0.0) {
            trmCop - trmPrevCop
        } else null

    val trmDeltaPct: Double?
        get() = if (trmCop != null && trmPrevCop != null && trmPrevCop > 0.0) {
            ((trmCop - trmPrevCop) / trmPrevCop) * 100.0
        } else null

    val nyUsdPerLb: Double?
        get() = nyCoffeeCentsLb?.let { it / 100.0 }

    val nyDeltaCents: Double?
        get() = if (nyCoffeeCentsLb != null && nyPrevCloseCentsLb != null && nyPrevCloseCentsLb > 0.0) {
            nyCoffeeCentsLb - nyPrevCloseCentsLb
        } else null

    val nyDeltaPct: Double?
        get() = if (nyCoffeeCentsLb != null && nyPrevCloseCentsLb != null && nyPrevCloseCentsLb > 0.0) {
            ((nyCoffeeCentsLb - nyPrevCloseCentsLb) / nyPrevCloseCentsLb) * 100.0
        } else null

    /**
     * International theoretical parity of 125 kg green coffee based on live NY Contract C (c/lb) & live TRM.
     * 125 kg = 275.5778 lb.
     */
    val nyParityCopPerCarga125Kg: Double?
        get() = if (nyCoffeeCentsLb != null && trmCop != null && nyCoffeeCentsLb > 0.0 && trmCop > 0.0) {
            (nyCoffeeCentsLb / 100.0) * 275.577828 * trmCop
        } else null

    val nyParityCopPerKg: Double?
        get() = nyParityCopPerCarga125Kg?.let { it / 125.0 }
}

class TerritoryNetworkService {
    private val okHttp by lazy {
        OkHttpClient.Builder()
            .connectTimeout(12, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()
    }

    private val moshi by lazy {
        Moshi.Builder()
            .addLast(KotlinJsonAdapterFactory())
            .build()
    }

    private val nominatimApi by lazy {
        Retrofit.Builder()
            .baseUrl("https://nominatim.openstreetmap.org/")
            .client(okHttp)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(NominatimApi::class.java)
    }

    suspend fun geocodeInNarino(address: String): Result<NominatimResult?> = withContext(Dispatchers.IO) {
        try {
            val clean = address.trim()
            if (clean.isEmpty()) return@withContext Result.success(null)
            val results = nominatimApi.search("$clean, San José de Albán, Nariño, Colombia")
            if (results.isNotEmpty()) {
                Result.success(results.first())
            } else {
                val broader = nominatimApi.search("$clean, Nariño, Colombia")
                Result.success(broader.firstOrNull())
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun checkFncPublications(checkedAtLabel: String): FncCheckStatus = withContext(Dispatchers.IO) {
        checkFncInternal(checkedAtLabel).first
    }

    suspend fun fetchLiveMarketSnapshot(checkedAtLabel: String): LiveMarketSnapshot = withContext(Dispatchers.IO) {
        coroutineScope {
            val trmDeferred = async { fetchLiveTrm() }
            val nyDeferred = async { fetchLiveNyCoffee() }
            val fncDeferred = async { checkFncInternal(checkedAtLabel) }

            val trm = trmDeferred.await()
            val ny = nyDeferred.await()
            val (fncStatus, detectedFncPrice) = fncDeferred.await()

            LiveMarketSnapshot(
                trmCop = trm.current,
                trmPrevCop = trm.previous,
                trmDate = trm.date,
                trmSource = trm.source,
                trmError = trm.error,
                nyCoffeeCentsLb = ny.price,
                nyPrevCloseCentsLb = ny.prevClose,
                nySymbol = ny.symbol,
                nyExchange = ny.exchange,
                nyError = ny.error,
                fncDetectedCargaCop = detectedFncPrice,
                fncStatus = fncStatus,
                fetchedAt = checkedAtLabel,
            )
        }
    }

    private data class TrmResult(
        val current: Double? = null,
        val previous: Double? = null,
        val date: String? = null,
        val source: String = "Datos Abiertos · Superfinanciera",
        val error: String? = null,
    )

    private fun fetchLiveTrm(): TrmResult {
        // 1) Official Colombian Superfinanciera TRM via Datos Abiertos Socrata API
        try {
            val url = "https://www.datos.gov.co/resource/32sa-8pi3.json?\$order=vigenciadesde%20DESC&\$limit=2"
            val req = Request.Builder()
                .url(url)
                .header("Accept", "application/json")
                .header("User-Agent", "AlbanCafe360-Android/1.0")
                .build()
            okHttp.newCall(req).execute().use { response ->
                val body = response.body?.string()
                if (response.isSuccessful && !body.isNullOrBlank()) {
                    val arr = JSONArray(body)
                    if (arr.length() > 0) {
                        val first = arr.getJSONObject(0)
                        val valor = first.optString("valor").toDoubleOrNull()
                        val date = first.optString("vigenciadesde").take(10).ifBlank { null }
                        val prevValor = if (arr.length() > 1) {
                            arr.getJSONObject(1).optString("valor").toDoubleOrNull()
                        } else null
                        if (valor != null && valor > 0.0) {
                            return TrmResult(
                                current = valor,
                                previous = prevValor,
                                date = date,
                                source = "TRM Oficial · Datos Abiertos",
                            )
                        }
                    }
                }
            }
        } catch (_: Exception) {
            // Fall through to secondary FX endpoint
        }

        // 2) Fallback: Yahoo Finance spot USD/COP
        try {
            val yahooFx = fetchYahooQuote("USDCOP=X")
            if (yahooFx.price != null && yahooFx.price > 0.0) {
                return TrmResult(
                    current = yahooFx.price,
                    previous = yahooFx.prevClose,
                    source = "Spot USD/COP · Yahoo Finance",
                )
            }
        } catch (_: Exception) {
            // Fall through
        }

        // 3) Fallback: open.er-api.com
        try {
            val req = Request.Builder()
                .url("https://open.er-api.com/v6/latest/USD")
                .header("Accept", "application/json")
                .header("User-Agent", "AlbanCafe360-Android/1.0")
                .build()
            okHttp.newCall(req).execute().use { response ->
                val body = response.body?.string()
                if (response.isSuccessful && !body.isNullOrBlank()) {
                    val json = JSONObject(body)
                    val rates = json.optJSONObject("rates")
                    val cop = rates?.optDouble("COP", -1.0)?.takeIf { it > 0.0 }
                    if (cop != null) {
                        return TrmResult(
                            current = cop,
                            date = json.optString("time_last_update_utc").take(16).ifBlank { null },
                            source = "Referencia USD/COP · ExchangeRate API",
                        )
                    }
                }
            }
        } catch (_: Exception) {
            // Return error below
        }

        return TrmResult(
            error = "No fue posible obtener la TRM del dólar en este momento. Verifica tu conexión e intenta de nuevo.",
        )
    }

    private data class YahooQuoteResult(
        val price: Double? = null,
        val prevClose: Double? = null,
        val symbol: String = "KC=F",
        val exchange: String = "ICE / NYBOT",
        val error: String? = null,
    )

    private fun fetchLiveNyCoffee(): YahooQuoteResult {
        val primary = fetchYahooQuote("KC=F", host = "query1.finance.yahoo.com")
        if (primary.price != null) return primary
        val secondary = fetchYahooQuote("KC=F", host = "query2.finance.yahoo.com")
        if (secondary.price != null) return secondary
        return YahooQuoteResult(
            error = primary.error ?: secondary.error ?: "No fue posible consultar futuros de Café C (KC=F) en Nueva York.",
        )
    }

    private fun fetchYahooQuote(symbol: String, host: String = "query1.finance.yahoo.com"): YahooQuoteResult {
        return try {
            val url = "https://$host/v8/finance/chart/$symbol?interval=1d&range=5d"
            val req = Request.Builder()
                .url(url)
                .header("Accept", "application/json")
                .header("User-Agent", "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0 Mobile Safari/537.36")
                .build()
            okHttp.newCall(req).execute().use { response ->
                val body = response.body?.string()
                if (!response.isSuccessful || body.isNullOrBlank()) {
                    return YahooQuoteResult(error = "Bolsa NY no disponible (HTTP ${response.code}).")
                }
                val root = JSONObject(body)
                val chart = root.optJSONObject("chart")
                val resultArr = chart?.optJSONArray("result")
                if (resultArr == null || resultArr.length() == 0) {
                    return YahooQuoteResult(error = "Sin cotizaciones devueltas para $symbol.")
                }
                val first = resultArr.getJSONObject(0)
                val meta = first.optJSONObject("meta")
                var marketPrice = meta?.optDouble("regularMarketPrice", Double.NaN)?.takeIf { !it.isNaN() && it > 0.0 }
                val prevClose = meta?.optDouble("chartPreviousClose", Double.NaN)?.takeIf { !it.isNaN() && it > 0.0 }
                    ?: meta?.optDouble("previousClose", Double.NaN)?.takeIf { !it.isNaN() && it > 0.0 }

                if (marketPrice == null) {
                    val closes = first.optJSONObject("indicators")
                        ?.optJSONArray("quote")
                        ?.optJSONObject(0)
                        ?.optJSONArray("close")
                    if (closes != null) {
                        for (i in closes.length() - 1 downTo 0) {
                            val v = closes.optDouble(i, Double.NaN)
                            if (!v.isNaN() && v > 0.0) {
                                marketPrice = v
                                break
                            }
                        }
                    }
                }

                if (marketPrice == null) {
                    YahooQuoteResult(error = "Cotización $symbol vacía en respuesta de mercado.")
                } else {
                    val exchangeName = meta?.optString("exchangeName")?.ifBlank { null } ?: "NYBOT / ICE"
                    YahooQuoteResult(
                        price = marketPrice,
                        prevClose = prevClose,
                        symbol = symbol,
                        exchange = exchangeName,
                    )
                }
            }
        } catch (e: Exception) {
            YahooQuoteResult(error = "Error consultando $symbol: ${e.localizedMessage ?: "sin conexión"}")
        }
    }

    private fun checkFncInternal(checkedAtLabel: String): Pair<FncCheckStatus, Double?> {
        return try {
            val req = Request.Builder()
                .url("https://federaciondecafeteros.org/publicaciones/")
                .header("User-Agent", "AlbanCafe360-Android/1.0")
                .build()
            okHttp.newCall(req).execute().use { response ->
                val html = if (response.isSuccessful) response.body?.string().orEmpty() else ""
                if (response.isSuccessful) {
                    val detected = extractFncCargaPriceFromHtml(html)
                    val msg = if (detected != null) {
                        "Portal FNC en línea. Se detectó referencia cercana a COP ${detected.toInt()} / carga de 125 kg; confirma el boletín diario oficial."
                    } else {
                        "Portal de publicaciones FNC accesible. Abre el boletín diario oficial (PDF) para consultar el precio interno exacto del día y registrarlo en tu historial."
                    }
                    FncCheckStatus(
                        reachable = true,
                        message = msg,
                        checkedAt = checkedAtLabel,
                    ) to detected
                } else {
                    FncCheckStatus(
                        reachable = false,
                        message = "No fue posible consultar el portal FNC (HTTP ${response.code}). Revisa el boletín directamente en el enlace oficial.",
                        checkedAt = checkedAtLabel,
                    ) to null
                }
            }
        } catch (_: Exception) {
            FncCheckStatus(
                reachable = false,
                message = "Sin conexión o acceso limitado al portal FNC. Verifica manualmente en el enlace oficial.",
                checkedAt = checkedAtLabel,
            ) to null
        }
    }

    private fun extractFncCargaPriceFromHtml(html: String): Double? {
        if (html.isBlank()) return null
        val regex = Regex("""Precio\s+interno[^$]{0,120}\$\s*([\d.,]{6,12})""", RegexOption.IGNORE_CASE)
        val match = regex.find(html) ?: return null
        val digits = match.groupValues.getOrNull(1)?.replace(".", "")?.replace(",", "")?.trim() ?: return null
        return digits.toDoubleOrNull()?.takeIf { it in 500_000.0..10_000_000.0 }
    }
}
