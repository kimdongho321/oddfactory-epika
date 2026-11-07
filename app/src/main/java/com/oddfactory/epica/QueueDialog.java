package com.oddfactory.epica;
import android.app.*;
import android.content.*;
import org.json.*;

final class QueueDialog {
 static void show(MainActivity activity){
  JSONObject state=MediaLink.get(activity).queue();JSONArray items=state.optJSONArray("items");
  if(items==null||items.length()==0){AlertDialog empty=new AlertDialog.Builder(activity).setTitle("현재 재생 대기열").setMessage(state.optBoolean("session")?"유튜브 뮤직에서 현재 대기열을 공유하지 않고 있어요. 유튜브 뮤직에서 재생목록을 선택한 뒤 다시 열어 주세요.":"유튜브 뮤직에서 곡이나 재생목록을 한 번 선택해 주세요.").setPositiveButton("유튜브 뮤직 열기",(d,w)->activity.action("01")).setNegativeButton("닫기",null).create();empty.show();UiFonts.dialog(empty);return;}
  String[] labels=new String[items.length()];int current=-1;long active=state.optLong("activeId",-1);
  for(int i=0;i<items.length();i++){JSONObject item=items.optJSONObject(i);boolean selected=item.optLong("id")==active;if(selected)current=i;labels[i]=(selected?"♫ ":"")+(i+1)+". "+item.optString("title")+"\n"+item.optString("artist");}
  final int currentIndex=current;
  AlertDialog choices=new AlertDialog.Builder(activity).setTitle("현재 재생 대기열 · "+items.length()+"곡").setSingleChoiceItems(UiFonts.items(activity,android.R.layout.simple_list_item_single_choice,labels),current,(dialog,which)->{JSONObject item=items.optJSONObject(which);dialog.dismiss();activity.selectTrack(item.optLong("id"),which<currentIndex?"04":"03");}).setPositiveButton("새로고침",(d,w)->show(activity)).setNeutralButton("뮤직에서 곡 추가",(d,w)->activity.action("01")).setNegativeButton("닫기",null).create();choices.show();UiFonts.dialog(choices);
 }
}
