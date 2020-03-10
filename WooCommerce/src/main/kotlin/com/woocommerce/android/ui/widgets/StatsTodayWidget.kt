package com.woocommerce.android.ui.widgets

import com.woocommerce.android.di.AppComponent
import javax.inject.Inject

class StatsTodayWidget : StatsWidget() {
    @Inject lateinit var todayWidgetUpdater: TodayWidgetUpdater
    override val widgetUpdater: WidgetUpdater
        get() = todayWidgetUpdater

    override fun inject(appComponent: AppComponent) {
        appComponent.inject(this)
    }
}
