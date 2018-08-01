package com.woocommerce.android.ui.orders

import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTracker.Stat
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.util.WooLog
import com.woocommerce.android.util.WooLog.T
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.action.WCOrderAction.FETCH_ORDERS
import org.wordpress.android.fluxc.action.WCOrderAction.UPDATE_ORDER_STATUS
import org.wordpress.android.fluxc.generated.WCOrderActionBuilder
import org.wordpress.android.fluxc.model.WCOrderModel
import org.wordpress.android.fluxc.store.WCOrderStore
import org.wordpress.android.fluxc.store.WCOrderStore.FetchOrdersPayload
import org.wordpress.android.fluxc.store.WCOrderStore.OnOrderChanged
import javax.inject.Inject

class OrderListPresenter @Inject constructor(
    private val dispatcher: Dispatcher,
    private val orderStore: WCOrderStore,
    private val selectedSite: SelectedSite
) : OrderListContract.Presenter {
    companion object {
        private val TAG: String = OrderListPresenter::class.java.simpleName
    }

    private var orderView: OrderListContract.View? = null
    private var isLoadingOrders = false
    private var isLoadingMoreOrders = false
    private var canLoadMore = false

    override fun takeView(view: OrderListContract.View) {
        orderView = view
        dispatcher.register(this)
    }

    override fun dropView() {
        orderView = null
        dispatcher.unregister(this)
    }

    override fun loadOrders(filterByStatus: String?, forceRefresh: Boolean) {
        if (forceRefresh) {
            isLoadingOrders = true
            orderView?.setLoadingIndicator(active = true)
            val payload = FetchOrdersPayload(selectedSite.get(), filterByStatus)
            dispatcher.dispatch(WCOrderActionBuilder.newFetchOrdersAction(payload))
        } else {
            fetchAndLoadOrdersFromDb(filterByStatus, isForceRefresh = false)
        }
    }

    override fun isLoading(): Boolean {
        return isLoadingOrders || isLoadingMoreOrders
    }

    override fun canLoadMore(): Boolean {
        return canLoadMore
    }

    override fun loadMoreOrders(filterByStatus: String?) {
        orderView?.setLoadingMoreIndicator(true)
        isLoadingMoreOrders = true
        val payload = FetchOrdersPayload(selectedSite.get(), filterByStatus, loadMore = true)
        dispatcher.dispatch(WCOrderActionBuilder.newFetchOrdersAction(payload))
    }

    @Suppress("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onOrderChanged(event: OnOrderChanged) {
        when (event.causeOfChange) {
            FETCH_ORDERS -> {
                if (event.isError) {
                    WooLog.e(T.ORDERS, "$TAG - Error fetching orders : ${event.error.message}")
                    orderView?.showLoadOrdersError()
                } else {
                    canLoadMore = event.canLoadMore
                    val isForceRefresh = !isLoadingMoreOrders
                    fetchAndLoadOrdersFromDb(event.statusFilter, isForceRefresh)
                }

                if (isLoadingMoreOrders) {
                    isLoadingMoreOrders = false
                    orderView?.setLoadingMoreIndicator(active = false)
                } else {
                    isLoadingOrders = false
                    orderView?.setLoadingIndicator(active = false)
                }
            }
            // A child fragment made a change that requires a data refresh.
            UPDATE_ORDER_STATUS -> orderView?.refreshFragmentState()
            else -> {}
        }
    }

    override fun openOrderDetail(order: WCOrderModel) {
        AnalyticsTracker.trackWithSiteDetails(Stat.OPENED_ORDER_DETAIL, selectedSite.get())
        orderView?.openOrderDetail(order)
    }

    /**
     * Fetch orders from the local database.
     *
     * @param filterByStatus If not null, only pull orders whose status matches this filter. Default null.
     * @param isForceRefresh True if orders were refreshed from the API, else false.
     */
    override fun fetchAndLoadOrdersFromDb(filterByStatus: String?, isForceRefresh: Boolean) {
        val orders = filterByStatus?.let {
            orderStore.getOrdersForSite(selectedSite.get(), it)
        } ?: orderStore.getOrdersForSite(selectedSite.get())
        orderView?.let { view ->
            if (orders.count() > 0) {
                view.showOrders(orders, filterByStatus, isForceRefresh)
            } else {
                view.showNoOrders()
            }
        }
    }
}
