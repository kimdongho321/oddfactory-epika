package com.oddfactory.epica;
import android.app.*;
import android.content.*;
import android.graphics.*;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.provider.Settings;
import android.view.*;
import android.widget.*;
import android.media.session.PlaybackState;
import org.json.*;
import java.util.regex.*;

/** Local music controls, the session's published queue, and explicit saved links. */
final class PlayerScreen {
 final MainActivity a;final LinearLayout root,body,player,queue,library,settings,queueRows,links,custom;
 final TextView title,artist,time,message,permissionState;final ImageView art;final SeekBar seek;final Button play;
 final ScrollView scroll;boolean scrubbing=false;String queueSignature="",customSignature="";Bitmap lastArt;
 PlayerScreen(MainActivity activity){
  a=activity;scroll=new ScrollView(a);scroll.setFillViewport(true);root=column();root.setPadding(d(20),d(20),d(20),d(24));root.setBackgroundColor(Color.rgb(255,249,240));scroll.addView(root);
  label(root,"EPICA · MUSIC",12,0xff9478a7);label(root,"에피카 뮤직",27,0xff4c3168);
  Button execute=button("실행",v->a.startOverlay());execute.setTextSize(27);execute.setTypeface(UiFonts.face(a,true));GradientDrawable launchBg=new GradientDrawable();launchBg.setColor(0xffe8d5ff);launchBg.setCornerRadius(d(20));launchBg.setStroke(d(2),0xff9478a7);execute.setBackground(launchBg);root.addView(execute,new LinearLayout.LayoutParams(-1,d(72)));
  label(root,"화면 위 에피카로 재생·정지와 곡 넘김을 조작해요.\n더블터치: 음악 종료 선택 · 세 번 터치: 설정 메뉴 · 길게 누르고 끌기: 볼 잡고 이동",14,0xff62417c);
  permissionState=label(root,"음악 연결 상태 확인 중",13,0xff806793);
  LinearLayout advanced=column();advanced.setVisibility(View.GONE);Button guide=button("권한 안내 · 고급 설정 펼치기",v->{boolean show=advanced.getVisibility()!=View.VISIBLE;advanced.setVisibility(show?View.VISIBLE:View.GONE);((Button)v).setText(show?"권한 안내 · 고급 설정 접기":"권한 안내 · 고급 설정 펼치기");});root.addView(guide,new LinearLayout.LayoutParams(-1,-2));root.addView(advanced);
  label(advanced,"1. 화면 표시 전용 접근성 서비스를 켜면 그림과 입력을 분리해 화면 위에 표시해요.\n2. 알림 접근을 허용하면 유튜브 뮤직의 음악 세션을 연결해요.\n3. 유튜브 뮤직에서 곡을 한 번 선택하면 재생 상태가 표시돼요.\n화면 표시 서비스는 다른 앱 내용이나 동작을 읽지 않아요. PC·ADB·사용정보 권한은 필요하지 않아요.",13,0xff806793);
  add(advanced,"화면 표시 전용 접근성 설정",v->a.openOverlaySettings(false));add(advanced,"유튜브 뮤직 알림 접근 설정",v->a.openMusicAccess(false));add(advanced,"실행 알림 표시 허용",v->a.requestNoticePermission(false));
  LinearLayout tabs=new LinearLayout(a);root.addView(tabs);String[] labels={"재생","대기열","보관함","설정"};
  body=column();root.addView(body);player=column();queue=column();library=column();settings=column();for(LinearLayout p:new LinearLayout[]{player,queue,library,settings})body.addView(p);
  for(int i=0;i<4;i++){final int index=i;Button b=button(labels[i],v->tab(index));b.setTextSize(13);tabs.addView(b,new LinearLayout.LayoutParams(0,d(48),1));}
  a.stageSlot=new LinearLayout(a);a.stageSlot.setGravity(Gravity.CENTER);player.addView(a.stageSlot,new LinearLayout.LayoutParams(-1,d(220)));
  LinearLayout card=column();GradientDrawable bg=new GradientDrawable();bg.setColor(0xfff1e9fb);bg.setCornerRadius(d(18));card.setBackground(bg);card.setPadding(d(16),d(16),d(16),d(16));player.addView(card);
  art=new ImageView(a);art.setScaleType(ImageView.ScaleType.CENTER_CROP);art.setContentDescription("현재 곡 앨범 이미지");card.addView(art,new LinearLayout.LayoutParams(-1,d(150)));art.setVisibility(View.GONE);
  title=label(card,"유튜브 뮤직에서 곡을 골라 주세요",21,0xff4c3168);artist=label(card,"",14,0xff806793);message=label(card,"",13,0xff806793);a.status=message;
  seek=new SeekBar(a);seek.setMax(1000);card.addView(seek,new LinearLayout.LayoutParams(-1,d(42)));time=label(card,"0:00 / 0:00",12,0xff806793);
  seek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){public void onStartTrackingTouch(SeekBar s){scrubbing=true;}public void onStopTrackingTouch(SeekBar s){long duration=a.media.status().optLong("duration");a.media.seek(duration*s.getProgress()/1000);scrubbing=false;}public void onProgressChanged(SeekBar s,int value,boolean user){if(user){long duration=a.media.status().optLong("duration");time.setText(stamp(duration*value/1000)+" / "+stamp(duration));}}});
  LinearLayout controls=new LinearLayout(a);card.addView(controls);controls.addView(button("이전",v->a.action("04")),new LinearLayout.LayoutParams(0,d(56),1));play=button("재생",v->a.action(a.media.playing()?"05":"02"));controls.addView(play,new LinearLayout.LayoutParams(0,d(56),1));controls.addView(button("다음",v->a.action("03")),new LinearLayout.LayoutParams(0,d(56),1));
  custom=new LinearLayout(a);card.addView(custom);add(player,"유튜브 뮤직 열기",v->a.media.command("01",a));
  label(queue,"현재 재생 대기열",23,0xff4c3168);label(queue,"유튜브 뮤직이 공유한 목록이에요. 곡을 누르면 이동해요.",13,0xff806793);add(queue,"목록 새로고침",v->{queueSignature="";update(a.media.status());});queueRows=column();queue.addView(queueRows);
  label(library,"내 보관함",23,0xff4c3168);label(library,"곡·재생목록 링크를 이 기기에 저장해요. 유튜브 뮤직의 공유 → 에피카 뮤직으로도 추가할 수 있어요.",14,0xff806793);add(library,"곡 / 재생목록 링크 추가",v->linkDialog());links=column();library.addView(links);renderLinks();
  label(settings,"캐릭터 크기",23,0xff4c3168);slider(settings,"에피카",65,150,"mainScale",SizeSettings.main(a));slider(settings,"에피콘",65,200,"petScale",SizeSettings.pets(a));
  label(settings,"화면 위 에피카의 몸 안쪽을 터치해 재생·정지, 좌우 밀기로 곡 이동, 두 번 터치하면 음악 종료 방식을, 세 번 터치하면 종료·설정 메뉴를 열어요. 길게 누르고 끌면 볼을 잡고 이동하고, 에피콘은 각자 뒤따라와요.",14,0xff806793);
  add(settings,"화면 위에 에피카 띄우기",v->a.startOverlay());add(settings,"종료 인사하고 내리기",v->a.action("06"));add(settings,"유튜브 뮤직 연동 설정",v->a.startActivity(new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)));add(settings,"화면 표시 전용 접근성 설정",v->a.openOverlaySettings(false));
  label(settings,"이 앱의 정지 버튼은 지휘봉을 내리는 순간 음악을 멈춰요. 유튜브 뮤직에서 직접 정지하면 음악은 즉시 멈추고 에피카가 이어서 반응해요.",13,0xff806793);
  label(settings,"Google 계정 보관함의 직접 수정은 아직 연결되어 있지 않아요. 내 보관함은 이 기기의 링크 목록이며, 재생 대기열은 유튜브 뮤직에서 받아와요.",12,0xff806793);label(settings,"알림 접근은 유튜브 뮤직의 음악 세션에만 사용해요.",12,0xff806793);add(settings,"서체 저작권 안내",v->a.showFontNotice());
  UiFonts.apply(root);
  tab(0);
 }
 void tab(int n){int i=0;for(LinearLayout p:new LinearLayout[]{player,queue,library,settings})p.setVisibility(i++==n?View.VISIBLE:View.GONE);if(n==1){queueSignature="";update(a.media.status());}scroll.smoothScrollTo(0,0);}
 void update(JSONObject s){title.setText(s.optString("title").isEmpty()?"유튜브 뮤직에서 곡을 골라 주세요":s.optString("title"));artist.setText(s.optString("artist"));String error=s.optString("error");String state=ReleaseGeometry.playback(s.optBoolean("access"),s.optBoolean("session"),s.optInt("state"),error);message.setText(state);permissionState.setText((DisplayAccessService.available()?"화면 표시 연결됨":"화면 표시 설정 필요")+" · "+state);play.setText(s.optBoolean("playing")?"일시정지":"재생");long duration=s.optLong("duration"),position=s.optLong("position");seek.setEnabled(s.optBoolean("seekable")&&duration>0);if(!scrubbing){seek.setProgress(duration>0?(int)(position*1000/duration):0);time.setText(stamp(position)+" / "+stamp(duration));}Bitmap b=a.media.artwork();if(b!=lastArt){lastArt=b;art.setImageBitmap(b);art.setVisibility(b==null?View.GONE:View.VISIBLE);}
  JSONObject q=a.media.queue();String signature=q.toString();if(!signature.equals(queueSignature)){queueSignature=signature;queueRows.removeAllViews();JSONArray items=q.optJSONArray("items");if(items==null||items.length()==0)label(queueRows,"유튜브 뮤직이 공개한 대기열이 없어요.",14,0xff806793);else{long current=q.optLong("activeId",-1);int currentIndex=-1;for(int i=0;i<items.length();i++)if(items.optJSONObject(i).optLong("id")==current)currentIndex=i;for(int i=0;i<items.length();i++){JSONObject item=items.optJSONObject(i);long id=item.optLong("id");String motion=i<currentIndex?"04":"03";add(queueRows,(id==current?"♫ ":"")+item.optString("title")+"\n"+item.optString("artist"),v->a.selectTrack(id,motion));}}}
  String cs=a.media.customActions().toString();if(!cs.equals(customSignature)){customSignature=cs;custom.removeAllViews();for(PlaybackState.CustomAction action:a.media.customActions()){String name=action.getName().toString();if(name.contains("셔플")||name.contains("반복")){Button cb=button(name,v->a.media.custom(action));cb.setTextSize(11);custom.addView(cb,new LinearLayout.LayoutParams(0,d(46),1));}}}
 }
 void slider(LinearLayout panel,String name,int min,int max,String key,float value){TextView caption=label(panel,name+" · "+Math.round(value*100)+"%",16,0xff4c3168);SeekBar bar=new SeekBar(a);bar.setMin(min);bar.setMax(max);bar.setProgress(Math.round(value*100));bar.setContentDescription(name+" 크기");panel.addView(bar,new LinearLayout.LayoutParams(-1,d(48)));bar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){public void onStartTrackingTouch(SeekBar b){}public void onStopTrackingTouch(SeekBar b){}public void onProgressChanged(SeekBar b,int p,boolean user){caption.setText(name+" · "+p+"%");if(user){SizeSettings.set(a,key,p/100f);if(a.stage!=null)a.stage.evaluateJavascript("window.epicaSizes("+SizeSettings.main(a)+","+SizeSettings.pets(a)+")",null);}}});}
 void linkDialog(){LinearLayout fields=column();fields.setPadding(d(24),0,d(24),0);EditText name=new EditText(a);name.setHint("저장할 이름");EditText url=new EditText(a);url.setHint("YouTube Music 곡 / 재생목록 링크");url.setInputType(android.text.InputType.TYPE_CLASS_TEXT|android.text.InputType.TYPE_TEXT_VARIATION_URI);fields.addView(name);fields.addView(url);AlertDialog dialog=new AlertDialog.Builder(a).setTitle("보관함에 추가").setView(fields).setNegativeButton("취소",null).setPositiveButton("저장",null).create();dialog.setOnShowListener(v->{UiFonts.dialog(dialog);dialog.getButton(-1).setOnClickListener(b->{if(saveLink(name.getText().toString(),url.getText().toString()))dialog.dismiss();});});dialog.show();}
 boolean saveLink(String name,String text){String link=extractLink(text);if(link==null){Toast.makeText(a,"YouTube Music 또는 YouTube의 곡·재생목록 링크를 넣어 주세요.",Toast.LENGTH_LONG).show();return false;}JSONArray items=stored();for(int i=0;i<items.length();i++)if(link.equals(items.optJSONObject(i).optString("url"))){tab(2);return true;}JSONObject item=new JSONObject();try{item.put("name",name.trim().isEmpty()?"저장한 음악":name.trim());item.put("url",link);items.put(item);SizeSettings.prefs(a).edit().putString("musicLinks",items.toString()).apply();}catch(JSONException ignored){}renderLinks();tab(2);return true;}
 static String extractLink(String text){Matcher m=Pattern.compile("https://[^\\s<>]+",Pattern.CASE_INSENSITIVE).matcher(text);while(m.find()){String candidate=m.group().replaceAll("[)\\].,]+$","");Uri u=Uri.parse(candidate);String host=u.getHost(),path=u.getPath();if(host!=null&&(host.equals("music.youtube.com")||host.equals("www.youtube.com")||host.equals("youtube.com")||host.equals("youtu.be"))&&path!=null&&(host.equals("youtu.be")&&path.length()>1||path.equals("/watch")&&u.getQueryParameter("v")!=null||path.equals("/playlist")&&u.getQueryParameter("list")!=null))return candidate;}return null;}
 JSONArray stored(){try{return new JSONArray(SizeSettings.prefs(a).getString("musicLinks","[]"));}catch(JSONException e){return new JSONArray();}}
 void renderLinks(){links.removeAllViews();JSONArray items=stored();if(items.length()==0)label(links,"저장한 링크가 아직 없어요.",14,0xff806793);for(int i=0;i<items.length();i++){JSONObject item=items.optJSONObject(i);final int index=i;LinearLayout row=new LinearLayout(a);Button open=button(item.optString("name"),v->{try{a.startActivity(new Intent(Intent.ACTION_VIEW,Uri.parse(item.optString("url"))).setPackage(MediaLink.TARGET));}catch(ActivityNotFoundException e){Toast.makeText(a,"유튜브 뮤직을 설치해 주세요.",Toast.LENGTH_LONG).show();}});row.addView(open,new LinearLayout.LayoutParams(0,d(52),1));row.addView(button("삭제",v->{JSONArray next=stored();next.remove(index);SizeSettings.prefs(a).edit().putString("musicLinks",next.toString()).apply();renderLinks();}),new LinearLayout.LayoutParams(d(70),d(52)));links.addView(row);}}
 String stamp(long ms){long n=Math.max(0,ms/1000);return String.format(java.util.Locale.ROOT,"%d:%02d",n/60,n%60);}
 LinearLayout column(){LinearLayout l=new LinearLayout(a);l.setOrientation(LinearLayout.VERTICAL);return l;}
 TextView label(LinearLayout p,String text,int size,int color){TextView t=new TextView(a);t.setText(text);t.setTextSize(size);t.setTextColor(color);t.setTypeface(UiFonts.face(a,size>=21));t.setPadding(0,d(6),0,d(7));p.addView(t);return t;}
 Button button(String text,View.OnClickListener click){Button b=new Button(a);b.setText(text);b.setAllCaps(false);b.setTextColor(0xff62417c);b.setTypeface(UiFonts.face(a,false));b.setOnClickListener(click);return b;}
 void add(LinearLayout p,String text,View.OnClickListener click){p.addView(button(text,click),new LinearLayout.LayoutParams(-1,-2));}
 int d(float n){return a.dp(n);}
}
