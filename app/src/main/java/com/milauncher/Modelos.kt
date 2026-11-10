package com.milauncher

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

data class AppItem(
    val label: String,
    val packageName: String,
    val activityName: String,
    val icon: ImageBitmap,
    val sistema: Boolean = false
)

data class Atajo(val etiqueta: String, val icono: ImageBitmap, val accion: (Rect) -> Unit)

data class Elemento(val atajo: Atajo, val app: AppItem?)

data class Opcion(val texto: String, val peligro: Boolean = false, val accion: () -> Unit)

// Guarda la posición de un icono en pantalla sin provocar recomposiciones
class Caja { var r: Rect = Rect.Zero }

val coloresFondo = listOf(
    Color(0xFF000000),
    Color(0xFF0B1B3A),
    Color(0xFF2A0B3A),
    Color(0xFF0B2A1B),
    Color(0xFF2B2B2B),
    Color(0xFF3A0B0B),
    Color(0xFF3A200B),
    Color(0xFF3A0B2A),
    Color(0xFF0B3A3A),
    Color(0xFF2E1F14)
)
val nombresColor = listOf(
    "Negro", "Azul", "Violeta", "Verde", "Gris",
    "Rojo", "Naranja", "Rosa", "Turquesa", "Marrón"
)

val acentos = listOf(
    0xFF751BCE.toInt(),
    0xFF9B5CF6.toInt(),
    0xFFC335DF.toInt(),
    0xFFEC4899.toInt(),
    0xFFE5484D.toInt(),
    0xFFF59E0B.toInt(),
    0xFF2EAD5B.toInt(),
    0xFF00A6A6.toInt(),
    0xFF2F6BFF.toInt()
)

val paletaReloj = listOf(
    Color(0xFFFFFFFF), Color(0xFFFFE082), Color(0xFFFBBF24), Color(0xFFFF8A65),
    Color(0xFFFF5252), Color(0xFFF48FB1), Color(0xFFCE93D8), Color(0xFF9FA8DA),
    Color(0xFF64B5F6), Color(0xFF4DD0E1), Color(0xFF80CBC4), Color(0xFFA5D6A7),
    Color(0xFFC5E1A5), Color(0xFFBDBDBD), Color(0xFF000000)
)

val sombraTexto = Shadow(color = Color(0xAA000000), offset = Offset(0f, 2f), blurRadius = 6f)

val dockClaves = listOf(
    listOf("Teléfono", "Telefono", "Phone", "Llamadas"),
    listOf("Mensajes", "Messages"),
    listOf("Cámara", "Camara", "Camera"),
    listOf("Brave", "Chrome", "Internet", "Samsung Internet", "Firefox")
)
val inicioClaves = listOf(
    listOf("Galería", "Galeria", "Gallery"),
    listOf("Reloj", "Clock"),
    listOf("Calendario", "Calendar"),
    listOf("Mis archivos", "Files", "Archivos"),
    listOf("WhatsApp"),
    listOf("Instagram"),
    listOf("Ajustes", "Settings"),
    listOf("Play Store", "Google Play Store")
)

// Primero busca el nombre exacto y, si no lo encuentra, uno que empiece igual
fun buscarApp(apps: List<AppItem>, claves: List<String>): AppItem? =
    claves.firstNotNullOfOrNull { c -> apps.firstOrNull { it.label.equals(c, ignoreCase = true) } }
        ?: claves.firstNotNullOfOrNull { c -> apps.firstOrNull { it.label.startsWith(c, ignoreCase = true) } }

fun formaIcono(i: Int): Shape = when (i) {
    1 -> CircleShape
    2 -> RoundedCornerShape(14.dp)
    3 -> RoundedCornerShape(4.dp)
    else -> RectangleShape
}

// Pantallas de ajustes: 0 cerrado, 1 menú, 2 Apariencia, 4 Animaciones, 5 Reloj
fun atrasDe(p: Int): Int = when (p) {
    5 -> 2
    2 -> 1
    4 -> 1
    else -> 0
}