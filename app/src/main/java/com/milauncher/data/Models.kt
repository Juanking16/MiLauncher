package com.milauncher.data

import android.graphics.Bitmap

data class AppItem(
    val label: String,
    val packageName: String,
    val activityName: String,
    val icon: Bitmap
) {
    val key: String get() = "$packageName/$activityName"
}

enum class AccionGesto(val etiqueta: String) {
    NINGUNO("Ninguna"),
    ABRIR_CAJON("Abrir cajón de apps"),
    ABRIR_BUSCADOR("Abrir buscador"),
    ABRIR_AJUSTES("Abrir ajustes"),
    EXPANDIR_NOTIFICACIONES("Abrir notificaciones"),
    ABRIR_ULTIMA_APP("Abrir última app usada");

    companion object {
        fun desde(nombre: String?): AccionGesto =
            entries.firstOrNull { it.name == nombre } ?: NINGUNO
    }
}

data class Carpeta(
    val id: Long,
    val nombre: String,
    val apps: List<String>
)

data class WidgetPosicionado(
    val appWidgetId: Int,
    val spanX: Int = 2,
    val spanY: Int = 2
)
