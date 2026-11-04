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
    val mostrarNombres: Boolean = true
)

class SettingsStore(private val context: Context) {
    private val kColumnas = intPreferencesKey("columnas")
    private val kTamano = intPreferencesKey("tamano_icono")
    private val kFondo = floatPreferencesKey("fondo_oscuro")
    private val kNombres = booleanPreferencesKey("mostrar_nombres")

    val flow: Flow<Ajustes> = context.dataStore.data.map { p ->
        Ajustes(
            columnas = p[kColumnas] ?: 4,
            tamanoIcono = p[kTamano] ?: 52,
            fondoOscuro = p[kFondo] ?: 0.6f,
            mostrarNombres = p[kNombres] ?: true
        )
    }

    suspend fun guardar(a: Ajustes) {
        context.dataStore.edit { p ->
            p[kColumnas] = a.columnas
            p[kTamano] = a.tamanoIcono
            p[kFondo] = a.fondoOscuro
            p[kNombres] = a.mostrarNombres
        }
    }
}