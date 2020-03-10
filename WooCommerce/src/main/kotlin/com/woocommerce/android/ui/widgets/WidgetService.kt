package com.woocommerce.android.ui.widgets

import android.content.Intent
import android.widget.RemoteViewsService

class WidgetService : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory {
        val wideView = intent.getBooleanExtra(IS_WIDE_VIEW_KEY, true)
        return TodayWidgetListProvider(this.applicationContext, intent)
    }
}
