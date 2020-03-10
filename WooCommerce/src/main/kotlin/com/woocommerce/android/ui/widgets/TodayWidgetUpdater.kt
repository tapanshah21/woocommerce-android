package com.woocommerce.android.ui.widgets

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.widget.RemoteViews
import com.woocommerce.android.AppPrefsWrapper
import com.woocommerce.android.R
import com.woocommerce.android.tools.NetworkStatus
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.util.WidgetUtils
import com.woocommerce.android.viewmodel.ResourceProvider
import org.wordpress.android.fluxc.store.AccountStore
import javax.inject.Inject

class TodayWidgetUpdater
@Inject constructor(
    private val appPrefsWrapper: AppPrefsWrapper,
    private val selectedSite: SelectedSite,
    private val accountStore: AccountStore,
    private val networkStatus: NetworkStatus,
    private val resourceProvider: ResourceProvider,
    private val widgetUtils: WidgetUtils
) : WidgetUpdater {
    override fun updateAppWidget(
        context: Context,
        appWidgetId: Int,
        appWidgetManager: AppWidgetManager?
    ) {
        val widgetManager = appWidgetManager ?: AppWidgetManager.getInstance(context)
        val isWideView = widgetUtils.isWidgetWiderThanLimit(
                widgetManager,
                appWidgetId
        )
        val networkAvailable = networkStatus.isConnected()
        val hasAccessToken = accountStore.hasAccessToken()
        val widgetHasData = appPrefsWrapper.hasAppWidgetData(appWidgetId)

        val views = RemoteViews(context.packageName, R.layout.stats_widget_list_light)
        views.setTextViewText(R.id.widget_title, resourceProvider.getString(R.string.today))
        if (networkAvailable && hasAccessToken) {
            val siteModel = selectedSite.get()
            widgetUtils.setSiteIcon(siteModel, context, views, appWidgetId)
            siteModel.let {
                views.setOnClickPendingIntent(
                        R.id.widget_title_container,
                        widgetUtils.getPendingSelfIntent(context, siteModel.id)
                )
            }
            widgetUtils.showList(
                    widgetManager,
                    views,
                    context,
                    appWidgetId,
                    siteModel.id,
                    isWideView
            )
        } else if (!widgetHasData || !hasAccessToken) {
            widgetUtils.showError(
                    widgetManager,
                    views,
                    appWidgetId,
                    networkAvailable,
                    hasAccessToken,
                    resourceProvider,
                    context,
                    StatsTodayWidget::class.java
            )
        }
    }

    override fun componentName(context: Context) = ComponentName(context, StatsTodayWidget::class.java)

    override fun delete(appWidgetId: Int) {
        appPrefsWrapper.removeAppWidgetSiteId(appWidgetId)
        appPrefsWrapper.removeAppWidgetHasData(appWidgetId)
    }
}
