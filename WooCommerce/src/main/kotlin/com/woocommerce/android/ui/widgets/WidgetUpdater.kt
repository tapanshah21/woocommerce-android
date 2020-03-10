package com.woocommerce.android.ui.widgets

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import com.woocommerce.android.AppPrefsWrapper
import javax.inject.Inject

interface WidgetUpdater {
    fun updateAppWidget(
        context: Context,
        appWidgetId: Int,
        appWidgetManager: AppWidgetManager? = null
    )

    fun componentName(context: Context): ComponentName

    fun delete(appWidgetId: Int)

    class StatsWidgetUpdaters
    @Inject constructor(
        private val todayWidgetUpdater: TodayWidgetUpdater,
        private val appPrefsWrapper: AppPrefsWrapper,
        private val context: Context
    ) {
        private val widgetUpdaters = listOf(
                todayWidgetUpdater
        )

        fun update(context: Context) {
            widgetUpdaters.forEach {
                val appWidgetManager = AppWidgetManager.getInstance(context)
                val allWidgetIds = appWidgetManager.getAppWidgetIds(it.componentName(context))
                for (appWidgetId in allWidgetIds) {
                    it.updateAppWidget(context, appWidgetId, appWidgetManager)
                }
            }
        }

        fun updateTodayWidget(siteId: Long) {
            todayWidgetUpdater.update(siteId)
        }

        private fun WidgetUpdater.update(
            siteId: Long
        ) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val allWidgetIds = appWidgetManager.getAppWidgetIds(this.componentName(context))
            for (appWidgetId in allWidgetIds) {
                val widgetSiteId = appPrefsWrapper.getAppWidgetSiteId(appWidgetId)
                if (siteId == widgetSiteId) {
                    this.updateAppWidget(context, appWidgetId, appWidgetManager)
                }
            }
        }
    }
}
