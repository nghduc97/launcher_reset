package top.lazyc97.launcher_reset

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import android.widget.Toast
import io.flutter.Log
import java.util.concurrent.TimeUnit

const val WIDGET_KILL_LAUNCHER_INTENT = "WIDGET_KILL_LAUNCHER_INTENT"

/**
 * Implementation of App Widget functionality.
 */
class OneTapWidget : AppWidgetProvider() {
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        // There may be multiple widgets active, so update all of them
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onEnabled(context: Context) {
        // Enter relevant functionality for when the first widget is created
    }

    override fun onDisabled(context: Context) {
        // Enter relevant functionality for when the last widget is disabled
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent?.action == WIDGET_KILL_LAUNCHER_INTENT) {
            try {
                val psProc = ProcessBuilder("su", "-c", "ps -o pid,cmd").start()
                psProc.waitFor(10, TimeUnit.SECONDS)
                val psOut = psProc.inputStream.bufferedReader().use { it.readText() }
                val lines = psOut.split("\n")

                val spaceRegex = Regex("\\s+")
                var foundPid = ""
                for (line in lines) {
                    if (line.endsWith("oid.launcher3")) {
                        val parts = line.split(spaceRegex, limit = 2)
                        foundPid = parts[0]
                        Log.i("PidLookup", "name=${parts[1]} pid=${foundPid}")
                        break
                    }
                }

                Log.i("WidgetClick", psOut)
                if (foundPid.isEmpty()) {
                    throw RuntimeException("process not found")
                }
                val killProc = ProcessBuilder("su", "-c", "kill $foundPid").start()
                killProc.waitFor(10, TimeUnit.SECONDS)
            } catch (ex: Exception) {
                Log.e("WidgetClick", ex.toString())
            }
        }
        super.onReceive(context, intent)
    }
}

internal fun updateAppWidget(
    context: Context,
    appWidgetManager: AppWidgetManager,
    appWidgetId: Int
) {
    val widgetText = context.getString(R.string.appwidget_text)
    // Construct the RemoteViews object
    val views = RemoteViews(context.packageName, R.layout.one_tap_widget)
    views.setTextViewText(R.id.appwidget_text, widgetText)

    val intent = Intent(context, OneTapWidget::class.java)
    intent.action = WIDGET_KILL_LAUNCHER_INTENT
    val pendingIntent = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_IMMUTABLE)
    views.setOnClickPendingIntent(R.id.appwidget_text, pendingIntent)

    // Instruct the widget manager to update the widget
    appWidgetManager.updateAppWidget(appWidgetId, views)
}