package com.seventhelement.donttouchmyphone

import android.content.Context

public class  MySharedPreference {
    private val PREF_NAME = "MyPrefs"
    private val KEY_INT = "my_int_key"

    fun saveInt(context: Context, value: Int) {
        val sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putInt(KEY_INT, value)
        editor.apply() // or editor.commit(); if immediate effect is necessary
    }

    fun getInt(context: Context): Int {
        val sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        return sharedPreferences.getInt(KEY_INT, -1) // default value is -1
    }
}