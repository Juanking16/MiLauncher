package com.milauncher.widgets

import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetHostView
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.ComponentName
import android.content.Context
import android.os.Bundle

/** Envoltorio de las APIs oficiales de widgets (AppWidgetHost / AppWidgetManager). */
class WidgetHostManager(private val context: Context) {

    val appWidgetManager: AppWidgetManager = AppWidgetManager.getInstance(context)
    val host: AppWidgetHost = AppWidgetHost(context, APPWIDGET_HOST_ID)

    companion object {
        const val APPWIDGET_HOST_ID = 1029
    }

    fun empezarEscuchar() {
        host.startListening()
    }

    fun pararEscuchar() {
        host.stopListening()
    }

    fun listarWidgets(): List<AppWidgetProviderInfo> = appWidgetManager.installedProviders

    /** Reserva un id y enlaza el proveedor (pide bind automáticamente en Android 9+). */
    fun asignarYEnlazar(info: AppWidgetProviderInfo): Int? {
        val id = host.allocateAppWidgetId()
        val ok = appWidgetManager.bindAppWidgetIdIfAllowed(id, info.provider)
        if (!ok) {
            // Devolver el id para que la activity pida permiso con ACTION_APPWIDGET_BIND
            return -id
        }
        return id
    }

    fun crearVista(id: Int): AppWidgetHostView? {
        val info = appWidgetManager.getAppWidgetInfo(id) ?: return null
        return host.createView(context, id, info)
    }

    fun redimensionar(id: Int, anchoDp: Int, altoDp: Int) {
        val info = appWidgetManager.getAppWidgetInfo(id) ?: return
        val opts = Bundle().apply {
            putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, anchoDp)
            putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, altoDp)
            putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH, anchoDp)
            putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, altoDp)
        }
        appWidgetManager.updateAppWidgetOptions(id, opts)
    }

    fun eliminar(id: Int) {
        host.deleteAppWidgetId(id)
    }

    fun eliminarTodos() {
        host.hostIds.toList().forEach { host.deleteAppWidgetId(it) }
    }
}
