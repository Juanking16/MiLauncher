package com.milauncher.data

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "ajustes")

data class Ajustes(
    val columnas: Int = 4,
    val tamanoIcono: Int = 52,
    val fondoOscuro: Float = 0.6f,
    val mostrarNombres: Boolean = true,
    val transparenciaCajon: Float = 0.85f,
    val espaciado: Int = 10,
    val tema: String = "morado",
    val animaciones: Boolean = true,
    val mostrarDock: Boolean = true,
    val gestoArriba: AccionGesto = AccionGesto.ABRIR_CAJON,
    val gestoAbajo: AccionGesto = AccionGesto.ABRIR_BUSCADOR,
    val gestoDobleToque: AccionGesto = AccionGesto.EXPANDIR_NOTIFICACIONES
)

class SettingsStore(private val context: Context) {
    private val kColumnas = intPreferencesKey("columnas")
    private val kTamano = intPreferencesKey("tamano_icono")
    private val kFondo = floatPreferencesKey("fondo_oscuro")
    private val kNombres = booleanPreferencesKey("mostrar_nombres")
    private val kTransparenciaCajon = floatPreferencesKey("transparencia_cajon")
    private val kEspaciado = intPreferencesKey("espaciado")
    private val kTema = stringPreferencesKey("tema")
    private val kAnimaciones = booleanPreferencesKey("animaciones")
    private val kMostrarDock = booleanPreferencesKey("mostrar_dock")
    private val kGestoArriba = stringPreferencesKey("gesto_arriba")
    private val kGestoAbajo = stringPreferencesKey("gesto_abajo")
    private val kGestoDoble = stringPreferencesKey("gesto_doble")
    private val kOcultas = stringSetPreferencesKey("apps_ocultas")
    private val kCarpetas = stringPreferencesKey("carpetas_json")
    private val kIconPack = stringPreferencesKey("icon_pack")
    private val kWidgets = stringPreferencesKey("widgets_json")
    private val kUltimaApp = stringPreferencesKey("ultima_app")

    val flow: Flow<Ajustes> = context.dataStore.data.map { p ->
        Ajustes(
            columnas = p[kColumnas] ?: 4,
            tamanoIcono = p[kTamano] ?: 52,
            fondoOscuro = p[kFondo] ?: 0.6f,
            mostrarNombres = p[kNombres] ?: true,
            transparenciaCajon = p[kTransparenciaCajon] ?: 0.85f,
            espaciado = p[kEspaciado] ?: 10,
            tema = p[kTema] ?: "morado",
            animaciones = p[kAnimaciones] ?: true,
            mostrarDock = p[kMostrarDock] ?: true,
            gestoArriba = AccionGesto.desde(p[kGestoArriba]),
            gestoAbajo = AccionGesto.desde(p[kGestoAbajo]),
            gestoDobleToque = AccionGesto.desde(p[kGestoDoble])
        )
    }

    val ocultasFlow: Flow<Set<String>> = context.dataStore.data.map { p -> p[kOcultas] ?: emptySet() }
    val carpetasFlow: Flow<List<Carpeta>> = context.dataStore.data.map { p -> jsonACarpetas(p[kCarpetas]) }
    val iconPackFlow: Flow<String> = context.dataStore.data.map { p -> p[kIconPack] ?: "" }
    val widgetsFlow: Flow<List<WidgetPosicionado>> = context.dataStore.data.map { p -> jsonAWidgets(p[kWidgets]) }
    val ultimaAppFlow: Flow<String> = context.dataStore.data.map { p -> p[kUltimaApp] ?: "" }

    suspend fun guardar(a: Ajustes) {
        context.dataStore.edit { p ->
            p[kColumnas] = a.columnas
            p[kTamano] = a.tamanoIcono
            p[kFondo] = a.fondoOscuro
            p[kNombres] = a.mostrarNombres
            p[kTransparenciaCajon] = a.transparenciaCajon
            p[kEspaciado] = a.espaciado
            p[kTema] = a.tema
            p[kAnimaciones] = a.animaciones
            p[kMostrarDock] = a.mostrarDock
            p[kGestoArriba] = a.gestoArriba.name
            p[kGestoAbajo] = a.gestoAbajo.name
            p[kGestoDoble] = a.gestoDobleToque.name
        }
    }

    suspend fun toggleOculta(key: String) = context.dataStore.edit { p ->
        val actuales = p[kOcultas] ?: emptySet()
        p[kOcultas] = if (key in actuales) actuales - key else actuales + key
    }

    suspend fun setOcultas(nuevas: Set<String>) = context.dataStore.edit { p -> p[kOcultas] = nuevas }

    suspend fun guardarCarpetas(list: List<Carpeta>) = context.dataStore.edit { p ->
        p[kCarpetas] = carpetasAJson(list)
    }

    suspend fun setIconPack(pkg: String) = context.dataStore.edit { p -> p[kIconPack] = pkg }

    suspend fun guardarWidgets(list: List<WidgetPosicionado>) = context.dataStore.edit { p ->
        p[kWidgets] = widgetsAJson(list)
    }

    suspend fun setUltimaApp(key: String) = context.dataStore.edit { p -> p[kUltimaApp] = key }

    // ---- Serialización manual de carpetas (evita dependencia de kotlinx.serialization) ----
    private fun carpetasAJson(list: List<Carpeta>): String = list.joinToString("|") { c ->
        c.id.toString() + ";" + c.nombre.replace(";", "%3B").replace("|", "%7C") + ";" + c.apps.joinToString(",") {
            it.replace(";", "%3B").replace("|", "%7C").replace(",", "%2C")
        }
    }

    private fun jsonACarpetas(s: String?): List<Carpeta> {
        if (s.isNullOrBlank()) return emptyList()
        return s.split("|").mapNotNull { entry ->
            val partes = entry.split(";", limit = 3)
            if (partes.size < 2) return@mapNotNull null
            val id = partes[0].toLongOrNull() ?: return@mapNotNull null
            val nombre = partes[1].replace("%3B", ";").replace("%7C", "|")
            val apps = if (partes.size > 2 && partes[2].isNotBlank())
                partes[2].split(",").map { it.replace("%3B", ";").replace("%7C", "|").replace("%2C", ",") }
            else emptyList()
            Carpeta(id, nombre, apps)
        }
    }

    private fun widgetsAJson(list: List<WidgetPosicionado>): String =
        list.joinToString("|") { "${it.appWidgetId};${it.spanX};${it.spanY}" }

    private fun jsonAWidgets(s: String?): List<WidgetPosicionado> {
        if (s.isNullOrBlank()) return emptyList()
        return s.split("|").mapNotNull { entry ->
            val p = entry.split(";")
            val id = p.getOrNull(0)?.toIntOrNull() ?: return@mapNotNull null
            WidgetPosicionado(id, p.getOrNull(1)?.toIntOrNull() ?: 2, p.getOrNull(2)?.toIntOrNull() ?: 2)
        }
    }
}
