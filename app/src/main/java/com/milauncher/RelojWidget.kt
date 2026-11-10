package com.milauncher

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.delay

@Composable
fun Reloj(ajustes: Ajustes, modifier: Modifier = Modifier) {
    var ahora by remember { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            ahora = System.currentTimeMillis()
            delay(10_000)
        }
    }
    val patron = if (ajustes.relojFormato24) "HH:mm" else "h:mm a"
    val hora = remember(ahora, patron) {
        SimpleDateFormat(patron, Locale.getDefault()).format(Date(ahora))
    }
    val fecha = remember(ahora) {
        SimpleDateFormat("EEEE, d 'de' MMMM", Locale.forLanguageTag("es"))
            .format(Date(ahora))
            .replaceFirstChar { it.uppercase() }
    }
    val color = Color(ajustes.relojColor)
    val peso = when (ajustes.relojGrosor) {
        1 -> FontWeight.Normal
        2 -> FontWeight.Bold
        else -> FontWeight.Light
    }
    val fuente = when (ajustes.relojFuente) {
        1 -> FontFamily.Serif
        2 -> FontFamily.Monospace
        3 -> FontFamily.Cursive
        else -> FontFamily.Default
    }
    val sombra = if (ajustes.relojSombra) sombraTexto else null
    val alineacion = when (ajustes.relojPosicion) {
        1, 3 -> Alignment.CenterHorizontally
        2 -> Alignment.End
        else -> Alignment.Start
    }

    Column(modifier = modifier, horizontalAlignment = alineacion) {
        Text(
            text = hora,
            color = color,
            fontSize = ajustes.relojTamano.sp,
            fontWeight = peso,
            fontFamily = fuente,
            style = TextStyle(shadow = sombra)
        )
        if (ajustes.relojFecha) {
            Text(
                text = fecha,
                color = color,
                fontSize = (ajustes.relojTamano / 3.5f).coerceAtLeast(13f).sp,
                fontFamily = fuente,
                style = TextStyle(shadow = sombra)
            )
        }
    }
}