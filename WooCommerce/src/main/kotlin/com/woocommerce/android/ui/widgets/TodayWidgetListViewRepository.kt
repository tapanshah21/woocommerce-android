package com.woocommerce.android.ui.widgets

import com.woocommerce.android.AppPrefs
import com.woocommerce.android.annotations.OpenClassOnDebug
import com.woocommerce.android.model.RequestResult
import com.woocommerce.android.model.RequestResult.ERROR
import com.woocommerce.android.model.RequestResult.NO_ACTION_NEEDED
import com.woocommerce.android.model.RequestResult.RETRY
import com.woocommerce.android.model.RequestResult.SUCCESS
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.util.WooLog
import com.woocommerce.android.util.WooLog.T.DASHBOARD
import com.woocommerce.android.util.suspendCoroutineWithTimeout
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.action.WCStatsAction.FETCH_NEW_VISITOR_STATS
import org.wordpress.android.fluxc.action.WCStatsAction.FETCH_REVENUE_STATS
import org.wordpress.android.fluxc.generated.WCStatsActionBuilder
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.fluxc.store.WCStatsStore
import org.wordpress.android.fluxc.store.WCStatsStore.FetchNewVisitorStatsPayload
import org.wordpress.android.fluxc.store.WCStatsStore.FetchRevenueStatsPayload
import org.wordpress.android.fluxc.store.WCStatsStore.OnWCRevenueStatsChanged
import org.wordpress.android.fluxc.store.WCStatsStore.OnWCStatsChanged
import org.wordpress.android.fluxc.store.WCStatsStore.OrderStatsErrorType.PLUGIN_NOT_ACTIVE
import org.wordpress.android.fluxc.store.WCStatsStore.StatsGranularity
import org.wordpress.android.fluxc.store.WooCommerceStore
import org.wordpress.android.fluxc.utils.DateUtils
import javax.inject.Inject
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume

@OpenClassOnDebug
class TodayWidgetListViewRepository @Inject constructor(
    private val dispatcher: Dispatcher,
    private val selectedSite: SelectedSite,
    private val accountStore: AccountStore,
    private val statsStore: WCStatsStore,
    private val wooCommerceStore: WooCommerceStore // Required to ensure the WooCommerceStore is initialized!
) {
    companion object {
        private const val ACTION_TIMEOUT = 10L * 1000
    }

    private var isFetchingData: Boolean = false

    private val startDate: String by lazy {
        DateUtils.getStartDateForSite(selectedSite.get(), DateUtils.getStartOfCurrentDay())
    }

    private val endDate: String by lazy {
        DateUtils.getEndDateForSite(selectedSite.get())
    }

    private var continuationFetchTodayRevenue: Continuation<Boolean>? = null
    private var continuationFetchTodayVisitors: Continuation<Boolean>? = null

    init {
        dispatcher.register(this)
    }

    fun onCleanup() {
        dispatcher.unregister(this)
    }

    fun userIsLoggedIn() = accountStore.hasAccessToken()

    suspend fun fetchTodayStats(): RequestResult {
        return if (!isFetchingData) {
            isFetchingData = true

            return coroutineScope {
                val isUserLoggedIn = userIsLoggedIn()
                if (isUserLoggedIn) {
                    var fetchedRevenueStats = false
                    var fetchedVisitorStats = false

                    val fetchRevenueStats = async {
                        fetchedRevenueStats = fetchRevenueStats()
                    }
                    val fetchVisitorStats = async {
                        fetchedVisitorStats = fetchVisitorStats()
                    }
                    fetchRevenueStats.await()
                    fetchVisitorStats.await()

                    if (fetchedRevenueStats && fetchedVisitorStats) SUCCESS else RETRY
                } else ERROR
            }
        } else NO_ACTION_NEEDED
    }

    /**
     * Fires the request to fetch the revenue stats info for a site
     *
     * @return the result of the action as a [Boolean]
     */
    private suspend fun fetchRevenueStats(): Boolean {
        return try {
            suspendCoroutineWithTimeout<Boolean>(ACTION_TIMEOUT) {
                continuationFetchTodayRevenue = it

                val visitsPayload = FetchNewVisitorStatsPayload(selectedSite.get(), StatsGranularity.DAYS, true)
                dispatcher.dispatch(WCStatsActionBuilder.newFetchNewVisitorStatsAction(visitsPayload))
            } ?: false // request timed out
        } catch (e: CancellationException) {
            WooLog.e(DASHBOARD, "Exception encountered while fetching visitor stats", e)
            false
        }
    }

    /**
     * Fires the request to fetch the visitor stats info for the current day, for a site
     *
     * @return the result of the action as a [Boolean]
     */
    private suspend fun fetchVisitorStats(): Boolean {
        return try {
            suspendCoroutineWithTimeout<Boolean>(ACTION_TIMEOUT) {
                continuationFetchTodayVisitors = it

                val statsPayload = FetchRevenueStatsPayload(selectedSite.get(), StatsGranularity.DAYS, forced = true)
                dispatcher.dispatch(WCStatsActionBuilder.newFetchRevenueStatsAction(statsPayload))
            } ?: false // request timed out
        } catch (e: CancellationException) {
            WooLog.e(DASHBOARD, "Exception encountered while fetching visitor stats", e)
            false
        }
    }

    fun getTodayRevenueStats() = statsStore.getRawRevenueStats(
            selectedSite.get(), StatsGranularity.DAYS, startDate, endDate
    )

    fun getTodayVisitorStats(): String {
        val date = DateUtils.getDateTimeForSite(selectedSite.get(), "yyyy-MM-dd", DateUtils.getStartOfCurrentDay())
        val visitorStats = statsStore.getNewVisitorStats(
                selectedSite.get(), StatsGranularity.DAYS, "1", date, false
        )
        return visitorStats.values.sum().toString()
    }

    fun getStatsCurrency() = wooCommerceStore.getSiteSettings(selectedSite.get())?.currencyCode

    @Suppress("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onWCRevenueStatsChanged(event: OnWCRevenueStatsChanged) {
        if (event.causeOfChange == FETCH_REVENUE_STATS) {
            if (event.isError) {
                if (event.error.type == PLUGIN_NOT_ACTIVE) {
                    AppPrefs.setIsUsingV4Api(false)
                }
                continuationFetchTodayRevenue?.resume(false)
            } else {
                continuationFetchTodayRevenue?.resume(true)
            }
            continuationFetchTodayRevenue = null
        }
    }

    @Suppress("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onWCStatsChanged(event: OnWCStatsChanged) {
        if (event.causeOfChange == FETCH_NEW_VISITOR_STATS) {
            if (event.isError) {
                // Error fetching visitor stats
                continuationFetchTodayVisitors?.resume(false)
            } else {
                continuationFetchTodayVisitors?.resume(true)
            }
            continuationFetchTodayVisitors = null
        }
    }
}
