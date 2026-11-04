package com.milauncher

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
    val animacion: Int = 1,
    val animCajon: Int = 0,
    val estiloCajon: Int = 0,
    val forma: Int = 0,
    val espaciado: Int = 10,
    val transparenciaCajon: Float = 0.85f,
    val colorFondo: Int = 0
)

class SettingsStore(private val context: Context) {
    private val kColumnas = intPreferencesKey("columnas")
    private val kTamano = intPreferencesKey("tamano_icono")
    private val kFondo = floatPreferencesKey("fondo_oscuro")
    private val kNombres = booleanPreferencesKey("mostrar_nombres")
    private val kAnim = intPreferencesKey("animacion")
    private val kAnimCajon = intPreferencesKey("anim_cajon")
    private val kEstilo = intPreferencesKey("estilo_cajon")
    private val kForma = intPreferencesKey("forma")
    private val kEspaciado = intPreferencesKey("espaciado")
    private val kTransp = floatPreferencesKey("transparencia_cajon")
    private val kColor = intPreferencesKey("color_fondo")

    val flow: Flow<Ajustes> = context.dataStore.data.map { p ->
        Ajustes(
            columnas = p[kColumnas] ?: 4,
            tamanoIcono = p[kTamano] ?: 52,
            fondoOscuro = p[kFondo] ?: 0.6f,
            mostrarNombres = p[kNombres] ?: true,
            animacion = p[kAnim] ?: 1,
            animCajon = p[kAnimCajon] ?: 0,
            estiloCajon = p[kEstilo] ?: 0,
            forma = p[kForma] ?: 0,
            espaciado = p[kEspaciado] ?: 10,
            transparenciaCajon = p[kTransp] ?: 0.85f,
            colorFondo = p[kColor] ?: 0
        )
    }

    suspend fun guardar(a: Ajustes) {
        context.dataStore.edit { p ->
            p[kColumnas] = a.columnas
            p[kTamano] = a.tamanoIcono
            p[kFondo] = a.fondoOscuro
            p[kNombres] = a.mostrarNombres
            p[kAnim] = a.animacion
            p[kAnimCajon] = a.animCajon
            p[kEstilo] = a.estiloCajon
            p[kForma] = a.forma
            p[kEspaciado] = a.espaciado
            p[kTransp] = a.transparenciaCajon
            p[kColor] = a.colorFondo
        }
    }
}