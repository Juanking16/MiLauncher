package com.milauncher

import android.app.ActivityOptions
import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class AppItem(
    val label: String,
    val packageName: String,
    val activityName: String,
    val icon: ImageBitmap
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { HomeScreen() }
    }

    private suspend fun loadApps(): List<AppItem> = withContext(Dispatchers.Default) {
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        packageManager.queryIntentActivities(intent, 0)
            .filter { it.activityInfo.packageName != packageName }
            .map {
                AppItem(
                    label = it.loadLabel(packageManager).toString(),
                    packageName = it.activityInfo.packageName,
                    activityName = it.activityInfo.name,
                    icon = it.loadIcon(packageManager).toBitmap(128, 128).asImageBitmap()
                )
            }
            .sortedBy { it.label.lowercase() }
    }

    private fun openApp(app: AppItem, estilo: Int) {
        val intent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
            component = ComponentName(app.packageName, app.activityName)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        val opciones = when (estilo) {
            1 -> ActivityOptions.makeCustomAnimation(this, R.anim.zoom_in, R.anim.zoom_out)
            2 -> ActivityOptions.makeCustomAnimation(this, R.anim.fade_in, R.anim.fade_out)
            3 -> ActivityOptions.makeCustomAnimation(this, R.anim.slide_in, R.anim.slide_out)
            else -> null
        }
        startActivity(intent, opciones?.toBundle())
    }

    @Composable
    fun HomeScreen() {
        val store = remember { SettingsStore(applicationContext) }
        val ajustes by store.flow.collectAsState(initial = Ajustes())
        val scope = rememberCoroutineScope()
        var apps by remember { mutableStateOf<List<AppItem>>(emptyList()) }
        var verAjustes by remember { mutableStateOf(false) }
        var verCajon by remember { mutableStateOf(false) }
        var busqueda by remember { mutableStateOf("") }
        val filtradas = remember(apps, busqueda) {
            if (busqueda.isBlank()) apps
            else apps.filter { it.label.contains(busqueda, ignoreCase = true) }
        }

        LaunchedEffect(Unit) { apps = loadApps() }
        LaunchedEffect(verCajon) { if (!verCajon) busqueda = "" }
        BackHandler(enabled = verAjustes) { verAjustes = false }
        BackHandler(enabled = verCajon && !verAjustes) { verCajon = false }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = ajustes.fondoOscuro))
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            if (verAjustes) {
                PantallaAjustes(
                    ajustes = ajustes,
                    onCambio = { nuevo -> scope.launch { store.guardar(nuevo) } },
                    onCerrar = { verAjustes = false }
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            detectVerticalDragGestures { _, dy ->
                                if (dy < -20f) verCajon = true
                            }
                        }
                ) {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Juan Launcher", color = Color.White, fontSize = 28.sp)
                        Spacer(Modifier.height(8.dp))
                        Text("Desliza hacia arriba", color = Color(0xAAFFFFFF), fontSize = 14.sp)
                    }
                    Text(
                        text = "⌃",
                        color = Color.White,
                        fontSize = 30.sp,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .clickable { verCajon = true }
                            .padding(20.dp)
                    )
                    Text(
                        text = "⚙",
                        color = Color.White,
                        fontSize = 24.sp,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .clickable { verAjustes = true }
                            .padding(14.dp)
                    )
                }

                AnimatedVisibility(
                    visible = verCajon,
                    enter = entradaCajon(ajustes.animCajon),
                    exit = salidaCajon(ajustes.animCajon)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .imePadding()
                            .background(Color.Black.copy(alpha = 0.85f))
                    ) {
                        Text(
                            text = "⌄",
                            color = Color.White,
                            fontSize = 30.sp,
                            modifier = Modifier
                                .align(Alignment.CenterHorizontally)
                                .clickable { verCajon = false }
                                .padding(10.dp)
                        )
                        BasicTextField(
                            value = busqueda,
                            onValueChange = { busqueda = it },
                            singleLine = true,
                            textStyle = TextStyle(color = Color.White, fontSize = 16.sp),
                            cursorBrush = SolidColor(Color.White),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                                .clip(RoundedCornerShape(24.dp))
                                .background(Color(0x33FFFFFF))
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            decorationBox = { campo ->
                                if (busqueda.isEmpty()) {
                                    Text("Buscar apps…", color = Color(0x99FFFFFF), fontSize = 16.sp)
                                }
                                campo()
                            }
                        )
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(ajustes.columnas),
                            modifier = Modifier.fillMaxSize().padding(8.dp)
                        ) {
                            items(filtradas) { app ->
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier
                                        .clickable {
                                            verCajon = false
                                            openApp(app, ajustes.animacion)
                                        }
                                        .padding(vertical = 10.dp)
                                ) {
                                    Image(
                                        bitmap = app.icon,
                                        contentDescription = app.label,
                                        modifier = Modifier.size(ajustes.tamanoIcono.dp)
                                    )
                                    if (ajustes.mostrarNombres) {
                                        Spacer(Modifier.height(4.dp))
                                        Text(
                                            text = app.label,
                                            color = Color.White,
                                            fontSize = 11.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @Composable
    fun PantallaAjustes(
        ajustes: Ajustes,
        onCambio: (Ajustes) -> Unit,
        onCerrar: () -> Unit
    ) {
        var columnas by remember { mutableStateOf(ajustes.columnas.toFloat()) }
        var tamano by remember { mutableStateOf(ajustes.tamanoIcono.toFloat()) }
        var fondo by remember { mutableStateOf(ajustes.fondoOscuro) }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp)
        ) {
            Text(
                text = "← Ajustes",
                color = Color.White,
                fontSize = 22.sp,
                modifier = Modifier.clickable { onCerrar() }.padding(vertical = 8.dp)
            )
            Spacer(Modifier.height(16.dp))

            Text("Columnas: ${columnas.toInt()}", color = Color.White)
            Slider(
                value = columnas,
                onValueChange = { columnas = it },
                onValueChangeFinished = { onCambio(ajustes.copy(columnas = columnas.toInt())) },
                valueRange = 3f..6f,
                steps = 2
            )

            Text("Tamaño de iconos: ${tamano.toInt()}", color = Color.White)
            Slider(
                value = tamano,
                onValueChange = { tamano = it },
                onValueChangeFinished = { onCambio(ajustes.copy(tamanoIcono = tamano.toInt())) },
                valueRange = 36f..72f
            )

            Text("Oscuridad del fondo: ${(fondo * 100).toInt()}%", color = Color.White)
            Slider(
                value = fondo,
                onValueChange = { fondo = it },
                onValueChangeFinished = { onCambio(ajustes.copy(fondoOscuro = fondo)) },
                valueRange = 0f..0.9f
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Mostrar nombres", color = Color.White, modifier = Modifier.weight(1f))
                Switch(
                    checked = ajustes.mostrarNombres,
                    onCheckedChange = { onCambio(ajustes.copy(mostrarNombres = it)) }
                )
            }

            Spacer(Modifier.height(16.dp))
            Text("Animación al abrir apps", color = Color.White)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                listOf("Ninguna", "Zoom", "Fade", "Deslizar").forEachIndexed { i, nombre ->
                    Text(
                        text = nombre,
                        color = if (ajustes.animacion == i) Color(0xFFFBBF24) else Color.White,
                        fontSize = 15.sp,
                        modifier = Modifier
                            .clickable { onCambio(ajustes.copy(animacion = i)) }
                            .padding(vertical = 8.dp)
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
            Text("Animación del cajón de apps", color = Color.White)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                listOf("Deslizar", "Zoom", "Fade", "Rebote").forEachIndexed { i, nombre ->
                    Text(
                        text = nombre,
                        color = if (ajustes.animCajon == i) Color(0xFFFBBF24) else Color.White,
                        fontSize = 15.sp,
                        modifier = Modifier
                            .clickable { onCambio(ajustes.copy(animCajon = i)) }
                            .padding(vertical = 8.dp)
                    )
                }
            }
        }
    }
}

fun entradaCajon(estilo: Int): EnterTransition = when (estilo) {
    0 -> slideInVertically(tween(350)) { it } + fadeIn(tween(350))
    1 -> scaleIn(tween(300), initialScale = 0.8f) + fadeIn(tween(300))
    2 -> fadeIn(tween(400))
    else -> slideInVertically(
        spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)
    ) { it }
}

fun salidaCajon(estilo: Int): ExitTransition = when (estilo) {
    0 -> slideOutVertically(tween(300)) { it } + fadeOut(tween(300))
    1 -> scaleOut(tween(250), targetScale = 0.8f) + fadeOut(tween(250))
    2 -> fadeOut(tween(300))
    else -> slideOutVertically(tween(300)) { it } + fadeOut(tween(300))
}