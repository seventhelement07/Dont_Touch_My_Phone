package com.seventhelement.donttouchmyphone

import com.seventhelement.data1

object Constants {
    val list: ArrayList<data1> = arrayListOf(
        data1("alarm", R.drawable.alarm),
        data1("applause", R.drawable.applause),
        data1("telephone", R.drawable.call),
        data1("cartoon", R.drawable.cartoon),
        data1("dog", R.drawable.dog),
        data1("gaming", R.drawable.gaming),
        data1("bike", R.drawable.bike),
        data1("buzzer", R.drawable.buzzer),
        data1("car", R.drawable.car),
        data1("iphone_alarm", R.drawable.iphone),
        data1("machine_gun", R.drawable.machin_gun),
        data1("meow", R.drawable.cat),
        data1("sniper", R.drawable.sniper)
    )
    fun getlist(): ArrayList<data1> = list

    // Sensitivity slider index 0=Low, 1=Medium, 2=High
    val SENSITIVITY_THRESHOLDS = floatArrayOf(1.5f, 0.8f, 0.4f)
}
