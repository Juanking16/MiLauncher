package com.milauncher

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.abs

// Efecto al pasar de página: 0 Deslizar, 1 Libro, 2 Cubo, 3 Zoom
fun Modifier.efectoPagina(estado: PagerState, pagina: Int, estilo: Int): Modifier =
    graphicsLayer {
        val p = ((pagina - estado.currentPage) - estado.currentPageOffsetFraction)
            .coerceIn(-1f, 1f)
        val a = abs(p)
        when (estilo) {
            1 -> {
                cameraDistance = 12f * density
                transformOrigin = TransformOrigin(if (p < 0f) 1f else 0f, 0.5f)
                rotationY = -p * 60f
                alpha = 1f - a * 0.35f
            }
            2 -> {
                cameraDistance = 12f * density
                transformOrigin = TransformOrigin(if (p < 0f) 1f else 0f, 0.5f)
                rotationY = p * 90f
                alpha = 1f - a * 0.2f
            }
            3 -> {
                scaleX = 1f - 0.25f * a
                scaleY = 1f - 0.25f * a
                alpha = 1f - a * 0.6f
            }
            else -> {}
        }
    }

@Composable
fun Selector(
    titulo: String,
    opciones: List<String>,
    actual: Int,
    onElegir: (Int) -> Unit
) {
    val tema = LocalTema.current
    Spacer(Modifier.height(16.dp))
    Text(titulo, color = Paleta.Blanco, fontSize = 15.sp)
    Spacer(Modifier.height(8.dp))
    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        opciones.forEachIndexed { i, nombre ->
            val elegido = actual == i
            Text(
                text = nombre,
                color = if (elegido) Color.White else Paleta.GrisLavanda,
                fontSize = 14.sp,
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(if (elegido) tema.acento else Paleta.Superficie)
                    .clickable { onElegir(i) }
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            )
        }
    }
}

@Composable
fun Interruptor(texto: String, valor: Boolean, onCambio: (Boolean) -> Unit) {
    val tema = LocalTema.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(texto, color = Paleta.Blanco, fontSize = 15.sp, modifier = Modifier.weight(1f))
        Switch(
            checked = valor,
            onCheckedChange = onCambio,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = tema.acento,
                uncheckedThumbColor = Paleta.GrisLavanda,
                uncheckedTrackColor = Paleta.Superficie,
                uncheckedBorderColor = Paleta.GrisLavanda
            )
        )
    }
}

@Composable
fun Deslizador(
    texto: String,
    valor: Float,
    rango: ClosedFloatingPointRange<Float>,
    pasos: Int = 0,
    onValor: (Float) -> Unit,
    onFin: () -> Unit
) {
    val tema = LocalTema.current
    Spacer(Modifier.height(10.dp))
    Text(texto, color = Paleta.Blanco, fontSize = 14.sp)
    Slider(
        value = valor,
        onValueChange = onValor,
        onValueChangeFinished = onFin,
        valueRange = rango,
        steps = pasos,
        colors = SliderDefaults.colors(
            thumbColor = tema.acentoClaro,
            activeTrackColor = tema.acento,
            inactiveTrackColor = Paleta.Superficie
        )
    )
}

@Composable
fun Muestra(color: Color, elegido: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(color)
            .border(
                width = if (elegido) 3.dp else 1.dp,
                color = if (elegido) Color.White else Color(0x55FFFFFF),
                shape = CircleShape
            )
            .clickable { onClick() }
    )
}

@Composable
fun ItemMenu(texto: String, subtitulo: String = "", onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Paleta.Superficie)
            .clickable { onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(texto, color = Paleta.Blanco, fontSize = 16.sp)
            if (subtitulo.isNotEmpty()) {
                Text(subtitulo, color = Paleta.GrisLavanda, fontSize = 13.sp)
            }
        }
        Text("›", color = Paleta.GrisLavanda, fontSize = 22.sp)
    }
}

@Composable
fun Puntos(total: Int, actual: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.Center
    ) {
        repeat(total.coerceAtMost(12)) { i ->
            Box(
                modifier = Modifier
                    .padding(horizontal = 3.dp)
                    .size(if (i == actual) 8.dp else 6.dp)
                    .clip(CircleShape)
                    .background(if (i == actual) Color.White else Color(0x66FFFFFF))
            )
        }
    }
}

@Composable
fun IconoAtajo(
    atajo: Atajo,
    tamano: Int,
    forma: Shape,
    mostrarNombre: Boolean,
    modifier: Modifier = Modifier,
    alMantener: (() -> Unit)? = null
) {
    val caja = remember { Caja() }
    val haptic = LocalHapticFeedback.current
    val accion = rememberUpdatedState(atajo.accion)
    val mantener = rememberUpdatedState(alMantener)
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { accion.value.invoke(caja.r) },
                    onLongPress = {
                        val m = mantener.value
                        if (m != null) {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            m.invoke()
                        }
                    }
                )
            }
            .padding(vertical = 4.dp)
    ) {
        Image(
            bitmap = atajo.icono,
            contentDescription = atajo.etiqueta,
            modifier = Modifier
                .size(tamano.dp)
                .clip(forma)
                .onGloballyPositioned { caja.r = it.boundsInWindow() }
        )
        if (mostrarNombre) {
            Spacer(Modifier.height(4.dp))
            Text(
                text = atajo.etiqueta,
                color = Color.White,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = TextStyle(shadow = sombraTexto)
            )
        }
    }
}

@Composable
fun MenuApp(app: AppItem, opciones: List<Opcion>, onCerrar: () -> Unit) {
    val fuenteFondo = remember { MutableInteractionSource() }
    val fuenteTarjeta = remember { MutableInteractionSource() }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xAA000000))
            .clickable(interactionSource = fuenteFondo, indication = null) { onCerrar() },
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 36.dp)
                .widthIn(min = 260.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(Paleta.Superficie)
                .clickable(interactionSource = fuenteTarjeta, indication = null) { }
                .padding(vertical = 14.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
            ) {
                Image(
                    bitmap = app.icon,
                    contentDescription = app.label,
                    modifier = Modifier.size(44.dp)
                )
                Spacer(Modifier.width(14.dp))
                Text(
                    text = app.label,
                    color = Paleta.Blanco,
                    fontSize = 17.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(Modifier.height(6.dp))
            opciones.forEach { op ->
                Text(
                    text = op.texto,
                    color = if (op.peligro) Paleta.Peligro else Paleta.Blanco,
                    fontSize = 16.sp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { op.accion() }
                        .padding(horizontal = 20.dp, vertical = 14.dp)
                )
            }
        }
    }
}

@Composable
fun AvisoPack(nombre: String, onAplicar: () -> Unit, onCerrar: () -> Unit) {
    val tema = LocalTema.current
    val fuenteFondo = remember { MutableInteractionSource() }
    val fuenteTarjeta = remember { MutableInteractionSource() }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xAA000000))
            .clickable(interactionSource = fuenteFondo, indication = null) { },
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 32.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(Paleta.Superficie)
                .clickable(interactionSource = fuenteTarjeta, indication = null) { }
                .padding(24.dp)
        ) {
            Text("Nuevo paquete de iconos", color = Paleta.Blanco, fontSize = 19.sp)
            Spacer(Modifier.height(10.dp))
            Text(
                "Instalaste «" + nombre + "». ¿Quieres usarlo en Juan Launcher?",
                color = Paleta.GrisLavanda,
                fontSize = 15.sp
            )
            Spacer(Modifier.height(20.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Text(
                    text = "Ahora no",
                    color = Paleta.Blanco,
                    fontSize = 16.sp,
                    modifier = Modifier
                        .clickable { onCerrar() }
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                )
                Text(
                    text = "Aplicar",
                    color = tema.acentoClaro,
                    fontSize = 16.sp,
                    modifier = Modifier
                        .clickable { onAplicar() }
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                )
            }
        }
    }
}