package com.milauncher

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun PantallaAjustes(
    pantalla: Int,
    ajustes: Ajustes,
    packs: List<PaqueteIconos>,
    onCambio: (Ajustes) -> Unit,
    onIr: (Int) -> Unit,
    onBuscarPacks: () -> Unit
) {
    var columnas by remember { mutableStateOf(ajustes.columnas.toFloat()) }
    var tamano by remember { mutableStateOf(ajustes.tamanoIcono.toFloat()) }
    var fondo by remember { mutableStateOf(ajustes.fondoOscuro) }
    var relojTam by remember { mutableStateOf(ajustes.relojTamano.toFloat()) }

    val titulo = when (pantalla) {
        1 -> "Ajustes"
        2 -> "Apariencia"
        5 -> "Reloj"
        else -> "Animaciones"
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
    ) {
        Text(
            text = "←  $titulo",
            color = Paleta.Blanco,
            fontSize = 22.sp,
            modifier = Modifier
                .clickable { onIr(atrasDe(pantalla)) }
                .padding(vertical = 8.dp)
        )
        Spacer(Modifier.height(8.dp))

        when (pantalla) {
            1 -> {
                ItemMenu("Apariencia", "Temas, colores y fondos") { onIr(2) }
                ItemMenu("Animaciones", "Transiciones y efectos") { onIr(4) }
            }
            2 -> {
                ItemMenu("Reloj", "Estilo, tamaño y formato") { onIr(5) }

                Selector(
                    "Fondo del inicio",
                    listOf("Fondo de pantalla", "Negro AMOLED"),
                    ajustes.fondoModo
                ) { onCambio(ajustes.copy(fondoModo = it)) }

                Spacer(Modifier.height(16.dp))
                Text("Color de acento", color = Paleta.Blanco, fontSize = 15.sp)
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    acentos.forEach { a ->
                        Muestra(Color(a), ajustes.acento == a) {
                            onCambio(ajustes.copy(acento = a))
                        }
                    }
                }

                val indicePack = packs.indexOfFirst { it.paquete == ajustes.iconPack }
                    .let { if (it < 0) 0 else it + 1 }
                Selector(
                    "Paquete de iconos",
                    listOf("Predeterminado") + packs.map { it.nombre },
                    indicePack
                ) { i ->
                    onCambio(ajustes.copy(iconPack = if (i == 0) "" else packs[i - 1].paquete))
                }
                if (packs.isEmpty()) {
                    Text(
                        "No hay paquetes de iconos instalados.",
                        color = Paleta.GrisLavanda,
                        fontSize = 12.sp
                    )
                }
                Spacer(Modifier.height(8.dp))
                ItemMenu("Buscar paquetes en Play Store") { onBuscarPacks() }

                Selector(
                    "Forma de iconos",
                    listOf("Original", "Círculo", "Redondeado", "Cuadrado"),
                    ajustes.forma
                ) { onCambio(ajustes.copy(forma = it)) }

                if (ajustes.fondoModo == 0) {
                    Selector("Color del fondo", nombresColor, ajustes.colorFondo) {
                        onCambio(ajustes.copy(colorFondo = it))
                    }
                    Deslizador(
                        texto = "Oscuridad del fondo: ${(fondo * 100).toInt()}%",
                        valor = fondo,
                        rango = 0f..0.9f,
                        onValor = { fondo = it },
                        onFin = { onCambio(ajustes.copy(fondoOscuro = fondo)) }
                    )
                }

                Deslizador(
                    texto = "Columnas: ${columnas.toInt()}",
                    valor = columnas,
                    rango = 3f..6f,
                    pasos = 2,
                    onValor = { columnas = it },
                    onFin = { onCambio(ajustes.copy(columnas = columnas.toInt())) }
                )
                Deslizador(
                    texto = "Tamaño de iconos: ${tamano.toInt()}",
                    valor = tamano,
                    rango = 36f..72f,
                    onValor = { tamano = it },
                    onFin = { onCambio(ajustes.copy(tamanoIcono = tamano.toInt())) }
                )
                Spacer(Modifier.height(6.dp))
                Interruptor("Mostrar nombres de las apps", ajustes.mostrarNombres) {
                    onCambio(ajustes.copy(mostrarNombres = it))
                }
            }
            5 -> {
                Reloj(
                    ajustes = ajustes.copy(mostrarReloj = true),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp)
                )
                Interruptor("Mostrar reloj en el inicio", ajustes.mostrarReloj) {
                    onCambio(ajustes.copy(mostrarReloj = it))
                }
                Interruptor("Mostrar fecha", ajustes.relojFecha) {
                    onCambio(ajustes.copy(relojFecha = it))
                }
                Interruptor("Sombra del texto", ajustes.relojSombra) {
                    onCambio(ajustes.copy(relojSombra = it))
                }
                Selector(
                    "Formato",
                    listOf("24 horas", "12 horas"),
                    if (ajustes.relojFormato24) 0 else 1
                ) { onCambio(ajustes.copy(relojFormato24 = it == 0)) }
                Selector(
                    "Fuente",
                    listOf("Normal", "Serif", "Monospace", "Cursiva"),
                    ajustes.relojFuente
                ) { onCambio(ajustes.copy(relojFuente = it)) }
                Selector("Grosor", listOf("Fino", "Normal", "Negrita"), ajustes.relojGrosor) {
                    onCambio(ajustes.copy(relojGrosor = it))
                }
                Selector(
                    "Posición",
                    listOf("Arriba izq.", "Arriba centro", "Arriba der.", "Centro"),
                    ajustes.relojPosicion
                ) { onCambio(ajustes.copy(relojPosicion = it)) }

                Deslizador(
                    texto = "Tamaño: ${relojTam.toInt()}",
                    valor = relojTam,
                    rango = 32f..96f,
                    onValor = { relojTam = it },
                    onFin = { onCambio(ajustes.copy(relojTamano = relojTam.toInt())) }
                )

                Spacer(Modifier.height(12.dp))
                Text("Color", color = Paleta.Blanco, fontSize = 15.sp)
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    paletaReloj.forEach { c ->
                        Muestra(c, ajustes.relojColor == c.toArgb()) {
                            onCambio(ajustes.copy(relojColor = c.toArgb()))
                        }
                    }
                }

                val actual = Color(ajustes.relojColor)
                var rojo by remember(ajustes.relojColor) { mutableStateOf(actual.red) }
                var verde by remember(ajustes.relojColor) { mutableStateOf(actual.green) }
                var azul by remember(ajustes.relojColor) { mutableStateOf(actual.blue) }

                Spacer(Modifier.height(12.dp))
                Text("Color personalizado", color = Paleta.GrisLavanda, fontSize = 13.sp)
                Deslizador(
                    texto = "Rojo: ${(rojo * 255).toInt()}",
                    valor = rojo,
                    rango = 0f..1f,
                    onValor = { rojo = it },
                    onFin = { onCambio(ajustes.copy(relojColor = Color(rojo, verde, azul).toArgb())) }
                )
                Deslizador(
                    texto = "Verde: ${(verde * 255).toInt()}",
                    valor = verde,
                    rango = 0f..1f,
                    onValor = { verde = it },
                    onFin = { onCambio(ajustes.copy(relojColor = Color(rojo, verde, azul).toArgb())) }
                )
                Deslizador(
                    texto = "Azul: ${(azul * 255).toInt()}",
                    valor = azul,
                    rango = 0f..1f,
                    onValor = { azul = it },
                    onFin = { onCambio(ajustes.copy(relojColor = Color(rojo, verde, azul).toArgb())) }
                )
            }
            else -> {
                Selector(
                    "Transición entre páginas",
                    listOf("Deslizar", "Libro", "Cubo", "Zoom"),
                    ajustes.transPagina
                ) { onCambio(ajustes.copy(transPagina = it)) }
                Text(
                    when (ajustes.transPagina) {
                        1 -> "Las páginas giran como las hojas de un libro."
                        2 -> "Las páginas giran como las caras de un cubo."
                        3 -> "La página se encoge y se desvanece al pasar."
                        else -> "Deslizamiento simple."
                    },
                    color = Paleta.GrisLavanda,
                    fontSize = 12.sp
                )
                Selector(
                    "Animación al abrir apps",
                    listOf(
                        "Ninguna", "Zoom", "Fade", "Deslizar", "Desde el icono",
                        "Revelar", "Subir", "Rebote", "Giro"
                    ),
                    ajustes.animacion
                ) { onCambio(ajustes.copy(animacion = it)) }
                Text(
                    when (ajustes.animacion) {
                        4 -> "La app crece desde el icono que tocas."
                        5 -> "La app se destapa desde el icono."
                        6 -> "La app sube desde abajo."
                        7 -> "La app aparece con un pequeño rebote."
                        8 -> "La app entra girando suavemente."
                        else -> "Animación simple de ventana."
                    },
                    color = Paleta.GrisLavanda,
                    fontSize = 12.sp
                )
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}