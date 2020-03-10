package com.woocommerce.android

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Injectable wrapper around AppPrefs.
 *
 * AppPrefs interface is consisted of static methods, which make the client code difficult to test/mock.
 * Main purpose of this wrapper is to make testing easier.
 *
 */
@Singleton
class AppPrefsWrapper @Inject constructor() {
    fun getAppWidgetSiteId(appWidgetId: Int) = AppPrefs.getStatsWidgetSelectedSiteId(appWidgetId)
    fun setAppWidgetSiteId(siteId: Long, appWidgetId: Int) = AppPrefs.setStatsWidgetSelectedSiteId(siteId, appWidgetId)
    fun removeAppWidgetSiteId(appWidgetId: Int) = AppPrefs.removeStatsWidgetSelectedSiteId(appWidgetId)

    fun hasAppWidgetData(appWidgetId: Int): Boolean {
        return AppPrefs.getStatsWidgetHasData(appWidgetId)
    }

    fun setAppWidgetHasData(hasData: Boolean, appWidgetId: Int) {
        AppPrefs.setStatsWidgetHasData(hasData, appWidgetId)
    }

    fun removeAppWidgetHasData(appWidgetId: Int) = AppPrefs.removeStatsWidgetHasData(appWidgetId)
}
