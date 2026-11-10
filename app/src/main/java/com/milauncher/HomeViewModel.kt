package com.milauncher

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class HomeUiState(
    val isLoading: Boolean = true,
    val ajustes: Ajustes = Ajustes(),
    val appsBase: List<AppItem> = emptyList(),
    val apps: List<AppItem> = emptyList(),
    val packs: List<PaqueteIconos> = emptyList(),
    val dockApps: List<AppItem> = emptyList(),
    val inicioApps: List<AppItem> = emptyList(),
    val packNuevo: PaqueteIconos? = null
)

class HomeViewModel(application: Application) : AndroidViewModel(application) {
    private val store = SettingsStore(application)
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            store.flow.collectLatest { ajustesCargados ->
                val ajustes = ajustesCargados ?: Ajustes()
                _uiState.update { it.copy(ajustes = ajustes, isLoading = false) }
                recalcularListas()
            }
        }
    }

    fun verificarCambios(resumeTick: Int, acciones: Acciones) {
        viewModelScope.launch {
            try {
                val nuevasClaves = withContext(Dispatchers.IO) { acciones.clavesApps() }
                val actualesClaves = _uiState.value.appsBase.map { it.packageName + "/" + it.activityName }.toSet()
                
                if (nuevasClaves != actualesClaves || _uiState.value.appsBase.isEmpty()) {
                    val nuevasApps = withContext(Dispatchers.IO) { acciones.cargarApps() }
                    _uiState.update { it.copy(appsBase = nuevasApps) }
                    aplicarPackActual(acciones)
                }
                
                val detectados = withContext(Dispatchers.IO) { acciones.detectarPacks() }
                _uiState.update { it.copy(packs = detectados) }
                revisarPacksNuevos(detectados)
                
            } catch (e: Exception) {
                android.util.Log.e("MiLauncher", "Error al cargar apps", e)
            }
        }
    }

    private fun aplicarPackActual(acciones: Acciones) {
        val state = _uiState.value
        viewModelScope.launch {
            try {
                val appsFinales = if (state.ajustes.iconPack.isBlank()) {
                    state.appsBase
                } else {
                    acciones.aplicarPack(state.appsBase, state.ajustes.iconPack)
                }
                _uiState.update { it.copy(apps = appsFinales) }
                recalcularListas()
            } catch (e: Exception) {
                android.util.Log.e("MiLauncher", "Error al aplicar pack", e)
            }
        }
    }

    private fun revisarPacksNuevos(detectados: List<PaqueteIconos>) {
        val state = _uiState.value
        val vistos = state.ajustes.packsVistos
        if (vistos == null) {
            guardarAjustes(state.ajustes.copy(packsVistos = detectados.joinToString(",") { it.paquete }))
        } else {
            val conocidos = vistos.split(",").filter { it.isNotBlank() }
            val nuevo = detectados.firstOrNull { it.paquete !in conocidos }
            if (nuevo != null) _uiState.update { it.copy(packNuevo = nuevo) }
        }
    }

    fun agregarAlInicio(app: AppItem) {
        val state = _uiState.value
        if (state.inicioApps.any { it.packageName == app.packageName } || state.inicioApps.size >= 11) return
        val nuevoInicio = state.inicioApps.filter { it.packageName != app.packageName } + app
        val nuevoDock = state.dockApps.filter { it.packageName != app.packageName }
        guardarListas(nuevoInicio, nuevoDock)
    }

    fun agregarAlDock(app: AppItem) {
        val state = _uiState.value
        if (state.dockApps.any { it.packageName == app.packageName } || state.dockApps.size >= 5) return
        val nuevoInicio = state.inicioApps.filter { it.packageName != app.packageName }
        val nuevoDock = state.dockApps.filter { it.packageName != app.packageName } + app
        guardarListas(nuevoInicio, nuevoDock)
    }

    fun quitarApp(app: AppItem) {
        val state = _uiState.value
        val nuevoInicio = state.inicioApps.filter { it.packageName != app.packageName }
        val nuevoDock = state.dockApps.filter { it.packageName != app.packageName }
        guardarListas(nuevoInicio, nuevoDock)
    }

    fun resolverPackNuevo(aplicar: Boolean, acciones: Acciones) {
        val state = _uiState.value
        val p = state.packNuevo ?: return
        val vistos = (state.ajustes.packsVistos ?: "").split(",").filter { it.isNotBlank() }
        val todos = (vistos + p.paquete).distinct().joinToString(",")
        val nuevosAjustes = if (aplicar) state.ajustes.copy(iconPack = p.paquete, packsVistos = todos) else state.ajustes.copy(packsVistos = todos)
        
        guardarAjustes(nuevosAjustes)
        _uiState.update { it.copy(packNuevo = null) }
        if (aplicar) aplicarPackActual(acciones)
    }

    fun actualizarAjustes(nuevosAjustes: Ajustes) {
        guardarAjustes(nuevosAjustes)
    }

    private fun guardarListas(inicio: List<AppItem>, dock: List<AppItem>) {
        val ajustes = _uiState.value.ajustes.copy(
            inicioPkgs = inicio.joinToString(",") { it.packageName },
            dockPkgs = dock.joinToString(",") { it.packageName }
        )
        guardarAjustes(ajustes)
    }

    private fun guardarAjustes(ajustes: Ajustes) {
        viewModelScope.launch(Dispatchers.IO) {
            store.guardar(ajustes)
        }
    }

    private fun recalcularListas() {
        val state = _uiState.value
        val apps = state.apps
        val ajustes = state.ajustes

        val dockAuto = dockClaves.mapNotNull { buscarApp(apps, it) }.distinctBy { it.packageName }
        val dockCfg = ajustes.dockPkgs
        val inicioCfg = ajustes.inicioPkgs

        val dockApps = if (dockCfg == null) dockAuto else dockCfg.split(",")
            .filter { it.isNotBlank() }.mapNotNull { p -> apps.firstOrNull { it.packageName == p } }

        val inicioApps = if (inicioCfg == null) {
            inicioClaves.mapNotNull { buscarApp(apps, it) }
                .distinctBy { it.packageName }
                .filter { a -> dockApps.none { d -> d.packageName == a.packageName } }
                .take(7)
        } else {
            inicioCfg.split(",").filter { it.isNotBlank() }
                .mapNotNull { p -> apps.firstOrNull { it.packageName == p } }
        }

        _uiState.update { it.copy(dockApps = dockApps, inicioApps = inicioApps) }
    }
}