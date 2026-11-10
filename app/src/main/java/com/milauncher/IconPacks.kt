package com.milauncher

import android.content.Context
import android.content.Intent
import android.content.res.Resources
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory

data class PaqueteIconos(val paquete: String, val nombre: String)

class MapaIconos(
    val porComponente: Map<String, String>,
    val porPaquete: Map<String, String>
)

class IconPacks(private val ctx: Context) {

    fun detectar(): List<PaqueteIconos> {
        val pm = ctx.packageManager
        val acciones = listOf("org.adw.launcher.THEMES", "com.novalauncher.THEME", "com.gau.go.launcherex.theme")
        val vistos = LinkedHashMap<String, PaqueteIconos>()
        for (accion in acciones) {
            try {
                for (ri in pm.queryIntentActivities(Intent(accion), 0)) {
                    val pkg = ri.activityInfo.packageName
                    if (!vistos.containsKey(pkg)) {
                        val nombre = pm.getApplicationLabel(ri.activityInfo.applicationInfo).toString()
                        vistos[pkg] = PaqueteIconos(pkg, nombre)
                    }
                }
            } catch (e: Exception) { }
        }
        return vistos.values.sortedBy { it.nombre.lowercase() }
    }

    // FUNCIÓN VITAL: Ahora es 'suspend' para que corra en segundo plano
    suspend fun aplicar(base: List<AppItem>, pack: String): List<AppItem> = withContext(Dispatchers.IO) {
        try {
            val res = ctx.packageManager.getResourcesForApplication(pack)
            val mapa = cargarMapa(res, pack)
            if (mapa == null) return@withContext base
            
            base.map { app ->
                val icono = iconoDe(res, pack, mapa, app)
                if (icono != null) app.copy(icon = icono) else app
            }
        } catch (e: Exception) {
            base
        }
    }

    private fun abrirParser(res: Resources, pkg: String): XmlPullParser? {
        try {
            val id = res.getIdentifier("appfilter", "xml", pkg)
            if (id != 0) return res.getXml(id)
            val parser = XmlPullParserFactory.newInstance().newPullParser()
            parser.setInput(res.assets.open("appfilter.xml"), "UTF-8")
            return parser
        } catch (e: Exception) { return null }
    }

    private fun cargarMapa(res: Resources, pkg: String): MapaIconos? {
        val p = abrirParser(res, pkg) ?: return null
        val porComp = HashMap<String, String>()
        val porPaq = HashMap<String, String>()
        try {
            var evento = p.eventType
            while (evento != XmlPullParser.END_DOCUMENT) {
                if (evento == XmlPullParser.START_TAG && p.name == "item") {
                    val comp = p.getAttributeValue(null, "component")
                    val dib = p.getAttributeValue(null, "drawable")
                    if (comp != null && dib != null) {
                        val limpio = comp.removePrefix("ComponentInfo{").removeSuffix("}")
                        porComp[limpio] = dib
                        val paquete = limpio.substringBefore("/")
                        if (!porPaq.containsKey(paquete)) porPaq[paquete] = dib
                    }
                }
                evento = p.next()
            }
        } catch (e: Exception) { }
        if (porComp.isEmpty()) return null
        return MapaIconos(porComp, porPaq)
    }

    private fun iconoDe(res: Resources, pack: String, mapa: MapaIconos, app: AppItem): ImageBitmap? {
        val nombre = mapa.porComponente[app.packageName + "/" + app.activityName]
            ?: mapa.porPaquete[app.packageName]
        if (nombre == null) return null
        try {
            var id = res.getIdentifier(nombre, "drawable", pack)
            if (id == 0) id = res.getIdentifier(nombre, "mipmap", pack)
            if (id == 0) return null
            // Tamaño dinámico para no saturar la memoria
            val size = (48 * res.displayMetrics.density).toInt()
            return res.getDrawable(id, null).toBitmap(size, size).asImageBitmap()
        } catch (e: Exception) {
            return null
        }
    }
}