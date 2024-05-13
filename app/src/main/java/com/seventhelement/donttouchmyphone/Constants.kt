package com.seventhelement.donttouchmyphone

import com.seventhelement.data1

object Constants {
     val list= ArrayList<data1>();
     fun getlist():ArrayList<data1>{
         list.add(data1("alarm",R.drawable.alarm));
         list.add(data1("applause", R.drawable.applause));
         list.add(data1("telephone", R.drawable.call));
         list.add(data1("cartoon", R.drawable.cartoon));
         list.add(data1("dog", R.drawable.dog));
         list.add(data1("gaming", R.drawable.gaming));
         list.add(data1("bike", R.drawable.bike));
         list.add(data1("buzzer", R.drawable.buzzer));
         list.add(data1("car", R.drawable.car));
         list.add(data1("iphone_alarm", R.drawable.iphone));
         list.add(data1("machine_gun", R.drawable.machin_gun));
         list.add(data1("meow", R.drawable.cat));
         list.add(data1("sniper", R.drawable.sniper));
         return list;
     }
}