package com.safecallkids.app.monitoring

import android.content.Context
import android.os.Build
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.safecallkids.app.BuildConfig

object CrashReporter {
    fun configure(context: Context) {
        val crashlytics = FirebaseCrashlytics.getInstance()
        crashlytics.setCrashlyticsCollectionEnabled(true)
        crashlytics.setCustomKey("application_id", BuildConfig.APPLICATION_ID)
        crashlytics.setCustomKey("version_name", BuildConfig.VERSION_NAME)
        crashlytics.setCustomKey("version_code", BuildConfig.VERSION_CODE)
        crashlytics.setCustomKey("build_type", if (BuildConfig.DEBUG) "debug" else "release")
        crashlytics.setCustomKey("android_sdk", Build.VERSION.SDK_INT)
        crashlytics.setCustomKey("manufacturer", Build.MANUFACTURER.orEmpty())
        crashlytics.setCustomKey("model", Build.MODEL.orEmpty())
        crashlytics.setCustomKey("locale", context.resources.configuration.locales[0]?.toLanguageTag().orEmpty())
        crashlytics.log("SafeCall application started")
    }

    fun recordNonFatal(tag: String, throwable: Throwable) {
        FirebaseCrashlytics.getInstance().apply {
            setCustomKey("last_non_fatal_tag", tag)
            recordException(throwable)
        }
    }
}
