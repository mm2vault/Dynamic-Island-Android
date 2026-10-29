package com.mm2vault.dynamicisland.data

import android.content.Intent
import android.os.BatteryManager

enum class ChargeState { CHARGING, FULL, DISCHARGING, UNKNOWN }

data class BatteryState(val percent: Int = -1, val charge: ChargeState = ChargeState.UNKNOWN) {
    companion object {
        fun from(intent: Intent?): BatteryState {
            if (intent == null) return BatteryState()
            val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
            val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
            val percent = percentOf(level, scale)
            return BatteryState(percent, chargeOf(status, percent))
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
    }
}
