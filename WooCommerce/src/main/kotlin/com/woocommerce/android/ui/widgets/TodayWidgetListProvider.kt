package com.woocommerce.android.ui.widgets

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import android.widget.RemoteViewsService.RemoteViewsFactory
import com.woocommerce.android.R
import com.woocommerce.android.WooCommerce
import javax.inject.Inject

class TodayWidgetListProvider(val context: Context, intent: Intent) : RemoteViewsFactory {
    @Inject lateinit var viewModel: TodayWidgetListViewModel
    @Inject lateinit var widgetUpdater: TodayWidgetUpdater
    private val siteId: Int = intent.getIntExtra(SITE_ID_KEY, 0)
    private val appWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, -1)

    init {
        (context.applicationContext as WooCommerce).component.inject(this)
    }

    override fun onCreate() {
        viewModel.start(siteId, appWidgetId)
    }

    override fun getLoadingView(): RemoteViews? = null

    override fun onDataSetChanged() {
        viewModel.onDataSetChanged { appWidgetId ->
            widgetUpdater.updateAppWidget(
                    context,
                    appWidgetId = appWidgetId
            )
        }
    }

    override fun hasStableIds(): Boolean = true

    override fun getViewTypeCount(): Int = 1

    override fun onDestroy() {
    }

    override fun getCount(): Int {
        return viewModel.data.size
    }

    override fun getItemId(position: Int): Long {
        return viewModel.data[position].key.hashCode().toLong()
    }

    override fun getViewAt(position: Int): RemoteViews {
        val uiModel = viewModel.data[position]
        val rv = RemoteViews(context.packageName, uiModel.layout)
        rv.setTextViewText(R.id.period, uiModel.key)
        rv.setTextViewText(R.id.value, uiModel.value)

        val intent = Intent()
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        intent.putExtra(WooCommerce.LOCAL_SITE_ID, uiModel.localSiteId)
        rv.setOnClickFillInIntent(R.id.container, intent)
        return rv
    }
}
