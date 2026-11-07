package com.oddfactory.epica;
import android.content.Context;
import android.content.SharedPreferences;
final class SizeSettings {
 static SharedPreferences prefs(Context c){return c.getSharedPreferences("epica",0);}
 static float main(Context c){return Math.max(.65f,Math.min(1.5f,prefs(c).getFloat("mainScale",1f)));}
 static float pets(Context c){return Math.max(.65f,Math.min(2f,prefs(c).getFloat("petScale",1f)));}
 static void set(Context c,String key,float value){prefs(c).edit().putFloat(key,value).apply();EpicaService s=EpicaService.instance.get();if(s!=null)s.resize();}
}
