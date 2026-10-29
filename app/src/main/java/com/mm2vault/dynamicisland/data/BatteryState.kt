package com.mm2vault.dynamicisland.data

import android.content.Intent
import android.os.BatteryManager

enum class ChargeState { CHARGING, FULL, DISCHARGING, UNKNOWN }
enum class PlugType { NONE, AC, USB, WIRELESS, UNKNOWN }

data class BatteryState(
    val percent: Int = -1,
    val charge: ChargeState = ChargeState.UNKNOWN,
    val plug: PlugType = PlugType.NONE,
    /** Battery temperature in tenths of °C, or Int.MIN_VALUE when unknown. */
    val tempTenths: Int = Int.MIN_VALUE,
) {
    val hasTemp get() = tempTenths != Int.MIN_VALUE

    companion object {
        fun from(intent: Intent?): BatteryState {
            if (intent == null) return BatteryState()
            val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
            val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
            val plugged = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0)
            val temp = if (intent.hasExtra(BatteryManager.EXTRA_TEMPERATURE))
                intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, Int.MIN_VALUE) else Int.MIN_VALUE
            val percent = percentOf(level, scale)
            return BatteryState(percent, chargeOf(status, percent), plugOf(plugged), temp)
        }

        fun percentOf(level: Int, scale: Int): Int =
            if (level < 0 || scale <= 0) -1 else (level * 100 / scale).coerceIn(0, 100)

        fun chargeOf(status: Int, percent: Int): ChargeState = when (status) {
            BatteryManager.BATTERY_STATUS_FULL -> ChargeState.FULL
            BatteryManager.BATTERY_STATUS_CHARGING -> if (percent >= 100) ChargeState.FULL else ChargeState.CHARGING
            BatteryManager.BATTERY_STATUS_DISCHARGING,
            BatteryManager.BATTERY_STATUS_NOT_CHARGING -> ChargeState.DISCHARGING
            else -> ChargeState.UNKNOWN
        }

        fun plugOf(plugged: Int): PlugType = when {
            plugged == 0 -> PlugType.NONE
            plugged and BatteryManager.BATTERY_PLUGGED_WIRELESS != 0 -> PlugType.WIRELESS
            plugged and BatteryManager.BATTERY_PLUGGED_AC != 0 -> PlugType.AC
            plugged and BatteryManager.BATTERY_PLUGGED_USB != 0 -> PlugType.USB
            else -> PlugType.UNKNOWN
        }
    }
}
