package com.mm2vault.dynamicisland

import android.os.BatteryManager
import com.mm2vault.dynamicisland.data.BatteryState
import com.mm2vault.dynamicisland.data.ChargeState
import com.mm2vault.dynamicisland.data.PlugType
import org.junit.Assert.assertEquals
import org.junit.Test

class BatteryStateTest {
    @Test fun percentScales() {
        assertEquals(50, BatteryState.percentOf(50, 100))
        assertEquals(25, BatteryState.percentOf(1, 4))
        assertEquals(-1, BatteryState.percentOf(-1, 100))
        assertEquals(-1, BatteryState.percentOf(10, 0))
    }

    @Test fun chargeStates() {
        assertEquals(ChargeState.CHARGING, BatteryState.chargeOf(BatteryManager.BATTERY_STATUS_CHARGING, 60))
        assertEquals(ChargeState.FULL, BatteryState.chargeOf(BatteryManager.BATTERY_STATUS_CHARGING, 100))
        assertEquals(ChargeState.FULL, BatteryState.chargeOf(BatteryManager.BATTERY_STATUS_FULL, 100))
        assertEquals(ChargeState.DISCHARGING, BatteryState.chargeOf(BatteryManager.BATTERY_STATUS_DISCHARGING, 60))
        assertEquals(ChargeState.UNKNOWN, BatteryState.chargeOf(-1, 60))
    }

    @Test fun plugTypes() {
        assertEquals(PlugType.NONE, BatteryState.plugOf(0))
        assertEquals(PlugType.AC, BatteryState.plugOf(BatteryManager.BATTERY_PLUGGED_AC))
        assertEquals(PlugType.USB, BatteryState.plugOf(BatteryManager.BATTERY_PLUGGED_USB))
        assertEquals(PlugType.WIRELESS, BatteryState.plugOf(BatteryManager.BATTERY_PLUGGED_WIRELESS))
        assertEquals(PlugType.WIRELESS, BatteryState.plugOf(BatteryManager.BATTERY_PLUGGED_WIRELESS or BatteryManager.BATTERY_PLUGGED_USB))
    }
}
