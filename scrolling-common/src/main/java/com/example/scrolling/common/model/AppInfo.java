package com.example.scrolling.common.model;

/**
 * Application info data class for listing test modules
 */
public class AppInfo {
    private String appName;
    private String appFeature;
    private String packageName;

    public AppInfo(String appName, String appFeature, String packageName) {
        this.appName = appName;
        this.appFeature = appFeature;
        this.packageName = packageName;
    }

    public String getAppName() {
        return appName;
    }

    public void setAppName(String appName) {
        this.appName = appName;
    }

    public String getAppFeature() {
        return appFeature;
    }

    public void setAppFeature(String appFeature) {
        this.appFeature = appFeature;
    }

    public String getPackageName() {
        return packageName;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }
}
