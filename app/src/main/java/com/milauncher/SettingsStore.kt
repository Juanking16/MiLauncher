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
    val fondoOscuro: Float = 0.2f,
    val mostrarNombres: Boolean = true,
    val animacion: Int = 4,
    val animCajon: Int = 0,
    val estiloCajon: Int = 0,
    val forma: Int = 0,
    val espaciado: Int = 10,
    val transparenciaCajon: Float = 0.85f,
    val colorFondo: Int = 0,
    val tipoCajon: Int = 1,
    val mostrarReloj: Boolean = true,
    val relojFormato24: Boolean = true,
    val relojFecha: Boolean = true,
    val relojTamano: Int = 56,
    val relojGrosor: Int = 0,
    val relojFuente: Int = 0,
    val relojColor: Int = 0xFFFFFFFF.toInt(),
    val relojPosicion: Int = 0,
    val relojSombra: Boolean = true
)

class SettingsStore(private val context: Context) {
    private val kColumnas = intPreferencesKey("columnas")
    private val kTamano = intPreferencesKey("tamano_icono")
    private val kFondo = floatPreferencesKey("fondo_oscuro")
    private val kNombres = booleanPreferencesKey("mostrar_nombres")
    private val kAnim = intPreferencesKey("anim_apertura")
    private val kAnimCajon = intPreferencesKey("anim_cajon")
    private val kEstilo = intPreferencesKey("estilo_cajon")
    private val kForma = intPreferencesKey("forma")
    private val kEspaciado = intPreferencesKey("espaciado")
    private val kTransp = floatPreferencesKey("transparencia_cajon")
    private val kColor = intPreferencesKey("color_fondo")
    private val kTipo = intPreferencesKey("tipo_cajon")
    private val kReloj = booleanPreferencesKey("mostrar_reloj")
    private val kRelojFormato = booleanPreferencesKey("reloj_formato24")
    private val kRelojFecha = booleanPreferencesKey("reloj_fecha")
    private val kRelojTamano = intPreferencesKey("reloj_tamano")
    private val kRelojGrosor = intPreferencesKey("reloj_grosor")
    private val kRelojFuente = intPreferencesKey("reloj_fuente")
    private val kRelojColor = intPreferencesKey("reloj_color")
    private val kRelojPos = intPreferencesKey("reloj_posicion")
    private val kRelojSombra = booleanPreferencesKey("reloj_sombra")

    val flow: Flow<Ajustes> = context.dataStore.data.map { p ->
        Ajustes(
            columnas = p[kColumnas] ?: 4,
            tamanoIcono = p[kTamano] ?: 52,
            fondoOscuro = p[kFondo] ?: 0.2f,
            mostrarNombres = p[kNombres] ?: true,
            animacion = p[kAnim] ?: 4,
            animCajon = p[kAnimCajon] ?: 0,
            estiloCajon = p[kEstilo] ?: 0,
            forma = p[kForma] ?: 0,
            espaciado = p[kEspaciado] ?: 10,
            transparenciaCajon = p[kTransp] ?: 0.85f,
            colorFondo = p[kColor] ?: 0,
            tipoCajon = p[kTipo] ?: 1,
            mostrarReloj = p[kReloj] ?: true,
            relojFormato24 = p[kRelojFormato] ?: true,
            relojFecha = p[kRelojFecha] ?: true,
            relojTamano = p[kRelojTamano] ?: 56,
            relojGrosor = p[kRelojGrosor] ?: 0,
            relojFuente = p[kRelojFuente] ?: 0,
            relojColor = p[kRelojColor] ?: 0xFFFFFFFF.toInt(),
            relojPosicion = p[kRelojPos] ?: 0,
            relojSombra = p[kRelojSombra] ?: true
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
            p[kTipo] = a.tipoCajon
            p[kReloj] = a.mostrarReloj
            p[kRelojFormato] = a.relojFormato24
            p[kRelojFecha] = a.relojFecha
            p[kRelojTamano] = a.relojTamano
            p[kRelojGrosor] = a.relojGrosor
            p[kRelojFuente] = a.relojFuente
            p[kRelojColor] = a.relojColor
            p[kRelojPos] = a.relojPosicion
            p[kRelojSombra] = a.relojSombra
        }
    }
}