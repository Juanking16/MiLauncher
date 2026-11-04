package com.milauncher.data

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.net.Uri
import androidx.core.graphics.drawable.toBitmap

/**
 * Carga iconos desde paquetes de iconos instalados.
 * Los icon packs exponen sus iconos vía activities/appfilters según el estándar de la comunidad.
 */
class IconPackLoader(private val context: Context) {

    data class IconPack(val packageName: String, val nombre: String)

    /** Lista los icon packs instalados (apps que declaran category ICON_PACK). */
    fun listarPacks(): List<IconPack> {
        val pm = context.packageManager
        val it = Intent("org.adw.launcher.THEMES")
        val result = mutableListOf<IconPack>()
        for (info in pm.queryIntentActivities(it, 0) + pm.queryIntentActivities(Intent("com.novaline.launcher.icons.theme"), 0)) {
            val pkg = info.activityInfo.packageName
            if (pkg != context.packageName && result.none { r -> r.packageName == pkg }) {
                result += IconPack(pkg, info.loadLabel(pm).toString())
            }
        }
        return result.sortedBy { it.nombre.lowercase() }
    }

    /** Intenta cargar el icono de una app desde el pack. Devuelve null si el pack no lo trae. */
    fun cargarIcono(packPkg: String, appPkg: String, tamano: Int): Bitmap? {
        val pm = context.packageManager
        val packInfo = try { pm.getPackageInfo(packPkg, PackageManager.GET_META_DATA) } catch (_: Exception) { return null }
        val base = "com.milauncher.iconpack." // no estándar; los packs usan drawable por nombre
        // Estándar: res/drawable/<alias>.xml con <item drawable="..."/> y assets/appfilter.xml
        val componentKey = appPkg
        // 1) Probar alias por nombre de paquete en drawables
        val res = try { pm.getResourcesForApplication(packInfo) } catch (_: Exception) { return null }
        val appfilterId = res.getIdentifier("appfilter", "xml", packPkg)
        val alias = if (appfilterId != 0) {
            try {
                val xp = res.getXml(appfilterId)
                var encontrado: String? = null
                while (xp.next() != org.xmlpull.v1.XmlPullParser.END_DOCUMENT) {
                    if (xp.eventType == org.xmlpull.v1.XmlPullParser.START_TAG && xp.name == "item") {
                        if (xp.getAttributeValue(null, "component")?.contains(componentKey) == true) {
                            val d = xp.getAttributeValue(null, "drawable")
                            val im = xp.getAttributeValue(null, "img")
                            encontrado = d ?: im?.removeSuffix(".png")
                            break
                        }
                    }
                }
                xp.close()
                encontrado
            } catch (_: Exception) { null }
        } else null
        if (alias != null) {
            val id = res.getIdentifier(alias, "drawable", packPkg)
            if (id != 0) {
                return try { res.getDrawable(id, null).toBitmap(tamano, tamano) } catch (_: Exception) { null }
            }
            val mipmapId = res.getIdentifier(alias, "mipmap", packPkg)
            if (mipmapId != 0) {
                return try { res.getDrawable(mipmapId, null).toBitmap(tamano, tamano) } catch (_: Exception) { null }
            }
        }
        return null
    }

    companion object {
        /** Combina el icono del pack con fallback: si no hay, usa el original. */
        fun conFallback(pack: Bitmap?, original: Bitmap): Bitmap = pack ?: original

        /** Crea un icono de carpeta 2x2 con hasta 4 iconos de apps. */
        fun iconoCarpeta(iconos: List<Bitmap>, tamano: Int, colorFondo: Int): Bitmap {
            val b = Bitmap.createBitmap(tamano, tamano, Bitmap.Config.ARGB_8888)
            val c = Canvas(b)
            val fondo = android.graphics.Paint().apply { this.color = colorFondo }
            val radio = tamano / 4f
            val path = android.graphics.Path().apply {
                addRoundRect(0f, 0f, tamano.toFloat(), tamano.toFloat(), radio, radio, android.graphics.Path.Direction.CW)
            }
            c.drawPath(path, fondo)
            val cuarto = tamano / 2
            val mostrados = iconos.take(4)
            val off = (cuarto - tamano / 5) / 2
            mostrados.forEachIndexed { i, icono ->
                val x = (i % 2) * cuarto + off
                val y = (i / 2) * cuarto + off
                c.drawBitmap(icono, null, android.graphics.Rect(x, y, x + cuarto - off, y + cuarto - off), null)
            }
            return b
        }
    }
}
