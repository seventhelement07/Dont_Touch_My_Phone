package com.seventhelement.donttouchmyphone

import android.content.Context
import java.security.SecureRandom

class MySharedPreference {
    private val PREF_NAME = "MyPrefs"
    private val KEY_INT = "my_int_key"
    private val KEY_PIN = "alarm_pin_hash"
    private val KEY_PIN_SALT = "alarm_pin_salt"
    private val KEY_SENSITIVITY = "sensitivity_index"
    private val KEY_TRIGGER_COUNT = "trigger_count"
    private val KEY_RATED = "has_rated"
    private val KEY_ONBOARDING_DONE = "onboarding_done"
    private val KEY_ARMED = "is_armed"

    fun saveInt(context: Context, value: Int) {
        prefs(context).edit().putInt(KEY_INT, value).apply()
    }

    fun getInt(context: Context): Int {
        return prefs(context).getInt(KEY_INT, -1)
    }

    fun savePin(context: Context, hashedPin: String) {
        prefs(context).edit().putString(KEY_PIN, hashedPin).apply()
    }

    fun getPin(context: Context): String? {
        return prefs(context).getString(KEY_PIN, null)
    }

    fun hasPin(context: Context): Boolean = getPin(context) != null

    /**
     * One-time migration: a PIN saved before salting was added has no [KEY_PIN_SALT] entry.
     * Its hash can't be salted retroactively, so clear it and make the user set a fresh PIN —
     * safer than leaving a PIN that will never again match [checkPin]-style comparisons, or
     * leaving the unsalted hash in place.
     */
    fun migrateLegacyPinIfNeeded(context: Context) {
        val hasLegacyPin = getPin(context) != null &&
            prefs(context).getString(KEY_PIN_SALT, null) == null
        if (hasLegacyPin) {
            prefs(context).edit().remove(KEY_PIN).apply()
        }
    }

    /** Returns this install's PIN salt, generating and persisting a new random one on first use. */
    fun getOrCreateSalt(context: Context): String {
        val existing = prefs(context).getString(KEY_PIN_SALT, null)
        if (existing != null) return existing
        val bytes = ByteArray(16).also { SecureRandom().nextBytes(it) }
        val salt = bytes.joinToString("") { "%02x".format(it) }
        prefs(context).edit().putString(KEY_PIN_SALT, salt).apply()
        return salt
    }

    fun saveSensitivityIndex(context: Context, index: Int) {
        prefs(context).edit().putInt(KEY_SENSITIVITY, index).apply()
    }

    fun getSensitivityIndex(context: Context): Int {
        return prefs(context).getInt(KEY_SENSITIVITY, 1)
    }

    fun incrementTriggerCount(context: Context) {
        val count = getTriggerCount(context)
        prefs(context).edit().putInt(KEY_TRIGGER_COUNT, count + 1).apply()
    }

    fun getTriggerCount(context: Context): Int {
        return prefs(context).getInt(KEY_TRIGGER_COUNT, 0)
    }

    fun setHasRated(context: Context) {
        prefs(context).edit().putBoolean(KEY_RATED, true).apply()
    }

    fun hasRated(context: Context): Boolean {
        return prefs(context).getBoolean(KEY_RATED, false)
    }

    fun isFirstLaunch(context: Context): Boolean =
        !prefs(context).getBoolean(KEY_ONBOARDING_DONE, false)

    fun setOnboardingDone(context: Context) {
        prefs(context).edit().putBoolean(KEY_ONBOARDING_DONE, true).apply()
    }

    /** Whether monitoring was armed the last time the user touched the Start/Stop control. */
    fun setArmed(context: Context, armed: Boolean) {
        prefs(context).edit().putBoolean(KEY_ARMED, armed).apply()
    }

    fun isArmed(context: Context): Boolean =
        prefs(context).getBoolean(KEY_ARMED, false)

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
}
