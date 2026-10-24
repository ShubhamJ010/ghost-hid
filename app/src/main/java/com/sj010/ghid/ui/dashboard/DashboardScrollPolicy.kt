package com.sj010.ghid.ui.dashboard

internal object DashboardScrollPolicy {
    fun showQuitButton(itemCount: Int, canScrollForward: Boolean): Boolean {
        return itemCount > 0 && !canScrollForward
    }
}
