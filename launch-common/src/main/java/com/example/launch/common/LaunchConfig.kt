package com.example.launch.common

import android.content.Intent

object LaunchConfig {
    const val EXTRA_LOAD_TYPE = "load"
    const val EXTRA_START_TYPE = "start_type"

    fun getLoadType(intent: Intent): LoadSimulator.LoadType {
        val typeStr = intent.getStringExtra(EXTRA_LOAD_TYPE)?.uppercase()
        return try {
            if (typeStr != null) LoadSimulator.LoadType.valueOf(typeStr) else LoadSimulator.LoadType.LIGHT
        } catch (e: Exception) {
            LoadSimulator.LoadType.LIGHT
        }
    }

    fun getStartType(intent: Intent): String {
        return intent.getStringExtra(EXTRA_START_TYPE) ?: "cold"
    }
}
