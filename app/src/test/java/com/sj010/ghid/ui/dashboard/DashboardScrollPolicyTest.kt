package com.sj010.ghid.ui.dashboard

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DashboardScrollPolicyTest {

    @Test
    fun quitButtonStaysHiddenUntilListReachesBottom() {
        assertFalse(DashboardScrollPolicy.showQuitButton(itemCount = 3, canScrollForward = true))
        assertTrue(DashboardScrollPolicy.showQuitButton(itemCount = 3, canScrollForward = false))
    }

    @Test
    fun quitButtonIsHiddenWhenListIsEmpty() {
        assertFalse(DashboardScrollPolicy.showQuitButton(itemCount = 0, canScrollForward = false))
    }
}
