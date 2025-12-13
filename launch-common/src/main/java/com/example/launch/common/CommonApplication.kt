package com.example.launch.common

import android.app.Application
import android.os.SystemClock

open class CommonApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        
        // Determine Load Type from BuildConfig of the app module 
        // (This relies on the app module generating the BuildConfig with LOAD_TYPE, 
        // OR we can try to look it up dynamically via reflection if class is compatible, 
        // or just rely on the flavor matching logic if we access common's BuildConfig.
        // Wait, common's BuildConfig.LOAD_TYPE works because of flavor propagation!)
        
        val loadTypeStr = try {
            BuildConfig.LOAD_TYPE
        } catch (e: Exception) {
            "LIGHT"
        }
        
        val loadType = try {
            LoadSimulator.LoadType.valueOf(loadTypeStr)
        } catch (e: Exception) {
            LoadSimulator.LoadType.LIGHT
        }

        // Simulate Application Init Load (Blocking Main Thread)
        LoadSimulator.onApplicationCreate(this, loadType)
    }
}
