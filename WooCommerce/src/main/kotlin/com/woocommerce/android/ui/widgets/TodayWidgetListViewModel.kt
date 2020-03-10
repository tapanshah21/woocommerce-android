package com.woocommerce.android.ui.widgets

import androidx.annotation.LayoutRes
import com.woocommerce.android.AppPrefsWrapper
import com.woocommerce.android.R
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.util.CurrencyFormatter
import com.woocommerce.android.viewmodel.ResourceProvider
import kotlinx.coroutines.runBlocking
import org.wordpress.android.fluxc.model.WCRevenueStatsModel
import javax.inject.Inject

class TodayWidgetListViewModel @Inject constructor(
    private val resourceProvider: ResourceProvider,
    private val appPrefsWrapper: AppPrefsWrapper,
    private val currencyFormatter: CurrencyFormatter,
    private val selectedSite: SelectedSite,
    private val repository: TodayWidgetListViewRepository
) {
    private var siteId: Int? = null
    private var appWidgetId: Int? = null

    private val mutableData = mutableListOf<TodayItemUiModel>()
    val data: List<TodayItemUiModel> = mutableData

    fun start(siteId: Int, appWidgetId: Int) {
        this.siteId = siteId
        this.appWidgetId = appWidgetId
    }

    fun onDataSetChanged(onError: (appWidgetId: Int) -> Unit) {
        runBlocking {
            repository.fetchTodayStats()
        }
        val revenueStats = repository.getTodayRevenueStats()
        val visitorStats = repository.getTodayVisitorStats()
        val currencyCode = repository.getStatsCurrency()
        val uiModels = buildListItemUiModel(revenueStats, visitorStats, currencyCode)
        if (uiModels != data) {
            mutableData.clear()
            mutableData.addAll(uiModels)
            appWidgetId?.let {
                appPrefsWrapper.setAppWidgetHasData(true, it)
            }
        } else if (!repository.userIsLoggedIn()) {
            appWidgetId?.let { nonNullAppWidgetId ->
                onError(nonNullAppWidgetId)
            }
        }
    }

    private fun buildListItemUiModel(
        revenueStats: WCRevenueStatsModel?,
        visitorCount: String,
        currencyCode: String?
    ): List<TodayItemUiModel> {
        val layout = R.layout.stats_views_widget_item_light
        val grossRevenue = revenueStats?.getTotal()?.totalSales ?: 0.0
        val orderCount = revenueStats?.getTotal()?.ordersCount ?: 0
        val localSiteId = selectedSite.get().siteId.toInt()

        val formatCurrencyForDisplay = currencyFormatter::formatCurrencyRounded

        return listOf(
                TodayItemUiModel(
                        layout,
                        localSiteId,
                        resourceProvider.getString(R.string.dashboard_stats_revenue),
                        formatCurrencyForDisplay(grossRevenue, currencyCode.orEmpty())
                ),
                TodayItemUiModel(
                        layout,
                        localSiteId,
                        resourceProvider.getString(R.string.dashboard_stats_orders),
                        orderCount.toString()
                ),
                TodayItemUiModel(
                        layout,
                        localSiteId,
                        resourceProvider.getString(R.string.dashboard_stats_visitors),
                        visitorCount
                )
        )
    }

    data class TodayItemUiModel(
        @LayoutRes val layout: Int,
        val localSiteId: Int,
        val key: String,
        val value: String
    )
}
