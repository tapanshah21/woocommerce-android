package com.woocommerce.android.util

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.View
import android.widget.ImageView.ScaleType
import android.widget.ImageView.ScaleType.CENTER
import android.widget.ImageView.ScaleType.CENTER_CROP
import android.widget.ImageView.ScaleType.CENTER_INSIDE
import android.widget.ImageView.ScaleType.FIT_CENTER
import android.widget.ImageView.ScaleType.FIT_END
import android.widget.ImageView.ScaleType.FIT_START
import android.widget.ImageView.ScaleType.FIT_XY
import android.widget.ImageView.ScaleType.MATRIX
import android.widget.RemoteViews
import com.bumptech.glide.request.target.AppWidgetTarget
import com.woocommerce.android.R
import com.woocommerce.android.WooCommerce
import com.woocommerce.android.di.GlideApp
import com.woocommerce.android.di.GlideRequest
import com.woocommerce.android.ui.main.MainActivity
import com.woocommerce.android.ui.widgets.IS_WIDE_VIEW_KEY
import com.woocommerce.android.ui.widgets.SITE_ID_KEY
import com.woocommerce.android.ui.widgets.WidgetService
import com.woocommerce.android.viewmodel.ResourceProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.wordpress.android.fluxc.model.SiteModel
import java.util.Date
import javax.inject.Inject
import kotlin.random.Random

private const val MIN_WIDTH = 250
private const val ICON_MAX_DIMENSION = 100

class WidgetUtils
@Inject constructor() {
    fun isWidgetWiderThanLimit(
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        minWidthLimit: Int = MIN_WIDTH
    ): Boolean {
        val minWidth = appWidgetManager.getAppWidgetOptions(appWidgetId)
                .getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 300)
        return minWidth > minWidthLimit
    }

    fun setSiteIcon(
        siteModel: SiteModel?,
        context: Context,
        views: RemoteViews,
        appWidgetId: Int
    ) {
        views.setViewVisibility(R.id.widget_site_icon, View.VISIBLE)
        GlobalScope.launch(Dispatchers.Main) {
            val awt = AppWidgetTarget(context, R.id.widget_site_icon, views, appWidgetId)
            GlideApp.with(context)
                    .asBitmap()
                    .load(siteModel?.iconUrl ?: "")
                    .applyScaleType(FIT_START)
                    .applySize(ICON_MAX_DIMENSION, ICON_MAX_DIMENSION)
                    .into(awt)
        }
    }

    fun showError(
        appWidgetManager: AppWidgetManager,
        views: RemoteViews,
        appWidgetId: Int,
        networkAvailable: Boolean,
        hasAccessToken: Boolean,
        resourceProvider: ResourceProvider,
        context: Context,
        widgetType: Class<*>
    ) {
        views.setViewVisibility(R.id.widget_site_icon, View.GONE)
        views.setOnClickPendingIntent(
                R.id.widget_title_container,
                PendingIntent.getActivity(
                        context,
                        0,
                        Intent(),
                        PendingIntent.FLAG_UPDATE_CURRENT
                )
        )
        views.setViewVisibility(R.id.widget_content, View.GONE)
        views.setViewVisibility(R.id.widget_error, View.VISIBLE)
        val errorMessage = if (!networkAvailable) {
            R.string.stats_widget_error_no_network
        } else if (!hasAccessToken) {
            R.string.stats_widget_error_no_access_token
        } else {
            R.string.stats_widget_error_no_data
        }
        views.setTextViewText(
                R.id.widget_error_message,
                resourceProvider.getString(errorMessage)
        )
        val pendingSync = getRetryIntent(context, widgetType, appWidgetId)
        views.setOnClickPendingIntent(R.id.widget_error, pendingSync)
        appWidgetManager.updateAppWidget(appWidgetId, views)
    }

    fun getRetryIntent(
        context: Context,
        widgetType: Class<*>,
        appWidgetId: Int
    ): PendingIntent? {
        val intentSync = Intent(context, widgetType)
        intentSync.action = AppWidgetManager.ACTION_APPWIDGET_UPDATE

        intentSync.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        return PendingIntent.getBroadcast(
                context,
                Random(appWidgetId).nextInt(),
                intentSync,
                PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    fun showList(
        appWidgetManager: AppWidgetManager,
        views: RemoteViews,
        context: Context,
        appWidgetId: Int,
        siteId: Int,
        isWideView: Boolean
    ) {
        views.setPendingIntentTemplate(R.id.widget_content, getPendingTemplate(context))
        views.setViewVisibility(R.id.widget_content, View.VISIBLE)
        views.setViewVisibility(R.id.widget_error, View.GONE)
        val listIntent = Intent(context, WidgetService::class.java)
        listIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        listIntent.putExtra(SITE_ID_KEY, siteId)
        listIntent.putExtra(IS_WIDE_VIEW_KEY, isWideView)
        listIntent.data = Uri.parse(
                listIntent.toUri(Intent.URI_INTENT_SCHEME)
        )
        views.setRemoteAdapter(R.id.widget_content, listIntent)
        appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetId, R.id.widget_content)
        appWidgetManager.updateAppWidget(appWidgetId, views)
    }

    fun getPendingSelfIntent(
        context: Context,
        localSiteId: Int
    ): PendingIntent {
        val intent = Intent(context, MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        intent.putExtra(WooCommerce.LOCAL_SITE_ID, localSiteId)
        return PendingIntent.getActivity(
                context,
                getRandomId(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    private fun getPendingTemplate(context: Context): PendingIntent {
        val intent = Intent(context, MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        return PendingIntent.getActivity(
                context,
                getRandomId(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    private fun getRandomId(): Int {
        return Random(Date().time).nextInt()
    }

    private fun <T : Any> GlideRequest<T>.applyScaleType(
        scaleType: ScaleType
    ): GlideRequest<T> {
        return when (scaleType) {
            CENTER_CROP -> this.centerCrop()
            CENTER_INSIDE -> this.centerInside()
            FIT_CENTER -> this.fitCenter()
            CENTER -> this
            FIT_END,
            FIT_START,
            FIT_XY,
            MATRIX -> {
                org.wordpress.android.util.AppLog.e(org.wordpress.android.util.AppLog.T.UTILS, kotlin.String.format("ScaleType %s is not supported.", scaleType.toString()))
                this
            }
        }
    }

    private fun <T : Any> GlideRequest<T>.applySize(width: Int?, height: Int?): GlideRequest<T> {
        return if (width != null && height != null) {
            this.override(width, height)
        } else {
            this
        }
    }
}
