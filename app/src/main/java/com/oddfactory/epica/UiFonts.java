package com.oddfactory.epica;
import android.content.Context;
import android.graphics.Typeface;
import android.view.*;
import android.widget.*;
import android.app.AlertDialog;

final class UiFonts {
 private static Typeface regular,bold;
 static synchronized Typeface face(Context c,boolean heavy){
  if(regular==null){regular=Typeface.createFromAsset(c.getAssets(),"fonts/Maplestory-Light.ttf");bold=Typeface.createFromAsset(c.getAssets(),"fonts/Maplestory-Bold.ttf");}
  return heavy?bold:regular;
 }
 static void apply(View view){
  if(view instanceof TextView){TextView text=(TextView)view;boolean heavy=text.getTypeface()!=null&&text.getTypeface().isBold();text.setTypeface(face(view.getContext(),heavy));}
  if(view instanceof ViewGroup){ViewGroup group=(ViewGroup)view;for(int i=0;i<group.getChildCount();i++)apply(group.getChildAt(i));}
 }
 static ArrayAdapter<CharSequence> items(Context context,int layout,CharSequence[] values){
  return new ArrayAdapter<CharSequence>(context,layout,values){
   @Override public View getView(int position,View recycled,ViewGroup parent){View row=super.getView(position,recycled,parent);apply(row);return row;}
   @Override public View getDropDownView(int position,View recycled,ViewGroup parent){View row=super.getDropDownView(position,recycled,parent);apply(row);return row;}
  };
 }
 static void dialog(AlertDialog dialog){if(dialog.getWindow()!=null)apply(dialog.getWindow().getDecorView());}
}
