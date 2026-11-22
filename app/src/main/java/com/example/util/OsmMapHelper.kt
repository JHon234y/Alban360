package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import android.graphics.Paint
import android.graphics.Path
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.OnlineTileSourceBase
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.MapTileIndex
import org.osmdroid.views.MapView
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean

private val osmConfigured = AtomicBoolean(false)

enum class OsmLayerMode(val label: String, val attribution: String) {
    SATELLITE("Satélite", "Esri World Imagery"),
    MAPNIK("Mapa OSM", "OpenStreetMap Mapnik"),
    TOPO("Relieve", "OpenTopoMap"),
}

val EsriSatelliteTileSource: OnlineTileSourceBase = object : OnlineTileSourceBase(
    "EsriWorldImagery",
    0,
    19,
    256,
    "",
    arrayOf("https://server.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer/tile/")
) {
    override fun getTileURLString(pMapTileIndex: Long): String {
        val z = MapTileIndex.getZoom(pMapTileIndex)
        val x = MapTileIndex.getX(pMapTileIndex)
        val y = MapTileIndex.getY(pMapTileIndex)
        return "${baseUrl}$z/$y/$x"
    }
}

fun configureOsmDroidCache(context: Context) {
    if (!osmConfigured.compareAndSet(false, true)) return
    val cfg = Configuration.getInstance()
    cfg.userAgentValue = "AlbanCafe360-Android/1.0"
    val cacheBase = File(context.applicationContext.cacheDir, "osmdroid")
    cfg.osmdroidBasePath = cacheBase
    cfg.osmdroidTileCache = File(cacheBase, "tiles")
}

fun applyOsmTileSource(mapView: MapView, mode: OsmLayerMode) {
    val target = when (mode) {
        OsmLayerMode.SATELLITE -> EsriSatelliteTileSource
        OsmLayerMode.MAPNIK -> TileSourceFactory.MAPNIK
        OsmLayerMode.TOPO -> TileSourceFactory.OpenTopo
    }
    if (mapView.tileProvider.tileSource.name() != target.name()) {
        mapView.setTileSource(target)
    }
}

fun createColoredMapPinDrawable(context: Context, fillColor: Int): Drawable {
    val density = context.resources.displayMetrics.density
    val width = (34f * density).toInt().coerceAtLeast(34)
    val height = (44f * density).toInt().coerceAtLeast(44)
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    val cx = width / 2f
    val radius = width * 0.38f
    val topCy = radius + (3f * density)
    val tipY = height - (3f * density)

    val pinPath = Path().apply {
        addCircle(cx, topCy, radius, Path.Direction.CW)
        moveTo(cx - radius * 0.92f, topCy + radius * 0.35f)
        lineTo(cx, tipY)
        lineTo(cx + radius * 0.92f, topCy + radius * 0.35f)
        close()
    }

    // Subtle ground shadow
    val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AndroidColor.argb(60, 0, 0, 0)
        style = Paint.Style.FILL
    }
    canvas.drawOval(
        cx - radius * 0.45f,
        tipY - 2f * density,
        cx + radius * 0.45f,
        tipY + 2f * density,
        shadowPaint,
    )

    // Crisp white border
    val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AndroidColor.WHITE
        style = Paint.Style.STROKE
        strokeWidth = 3f * density
        strokeJoin = Paint.Join.ROUND
    }
    canvas.drawPath(pinPath, borderPaint)

    // Pin color fill
    val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = fillColor
        style = Paint.Style.FILL
    }
    canvas.drawPath(pinPath, fillPaint)

    // Inner white dot
    val centerDotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AndroidColor.WHITE
        style = Paint.Style.FILL
    }
    canvas.drawCircle(cx, topCy, radius * 0.36f, centerDotPaint)

    return BitmapDrawable(context.resources, bitmap)
}

fun String.parseCoordinateOrNull(): Double? =
    trim().replace(',', '.').toDoubleOrNull()

fun fitMapToPoints(mapView: MapView, points: List<Pair<Double, Double>>) {
    if (points.isEmpty()) return
    if (points.size == 1) {
        val (lat, lon) = points.first()
        mapView.controller.animateTo(org.osmdroid.util.GeoPoint(lat, lon), 15.8, 650L)
        return
    }
    val lats = points.map { it.first }
    val lons = points.map { it.second }
    val north = lats.max()
    val south = lats.min()
    val east = lons.max()
    val west = lons.min()
    val latSpan = (north - south).coerceAtLeast(0.01)
    val lonSpan = (east - west).coerceAtLeast(0.01)
    val box = org.osmdroid.util.BoundingBox(
        (north + latSpan * 0.25).coerceAtMost(85.0),
        (east + lonSpan * 0.25).coerceAtMost(180.0),
        (south - latSpan * 0.25).coerceAtLeast(-85.0),
        (west - lonSpan * 0.25).coerceAtLeast(-180.0),
    )
    mapView.post {
        runCatching {
            mapView.zoomToBoundingBox(box, true, 72)
        }.onFailure {
            mapView.controller.animateTo(
                org.osmdroid.util.GeoPoint((north + south) / 2.0, (east + west) / 2.0),
                13.5,
                500L,
            )
        }
    }
}

