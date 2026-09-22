package com.example.core.termux

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager

object TermuxBridge {
    const val TERMUX_PACKAGE = "com.termux"

    fun isTermuxInstalled(context: Context): Boolean {
        return try {
            context.packageManager.getPackageInfo(TERMUX_PACKAGE, 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }

    fun getLaunchIntent(context: Context): Intent? {
        return context.packageManager.getLaunchIntentForPackage(TERMUX_PACKAGE)
    }
}
