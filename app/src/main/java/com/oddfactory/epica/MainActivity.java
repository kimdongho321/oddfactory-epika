package com.oddfactory.epica;
import android.Manifest;
import android.app.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.media.AudioManager;
import android.net.Uri;
import android.os.*;
import android.provider.Settings;
import android.view.*;
import android.widget.*;

public class MainActivity extends Activity {
 public static java.lang.ref.WeakReference<MainActivity> instance=new java.lang.ref.WeakReference<>(null);
 LocalStage stage;LinearLayout root,stageSlot;TextView status;PlayerScreen screen;MediaLink media;boolean active=false;
 boolean overlayPermissionPending=false,overlayStarting=false,musicPermissionPending=false,noticeLaunchPending=false;
 final Handler handler=new Handler(Looper.getMainLooper());
 final Runnable update=()->updateStatus();
 final Runnable poll=new Runnable(){public void run(){if(!active)return;media.refresh();updateStatus();handler.postDelayed(this,800);}};
 @Override public void onCreate(Bundle b){
  super.onCreate(b);instance=new java.lang.ref.WeakReference<>(this);media=MediaLink.get(this);setVolumeControlStream(AudioManager.STREAM_MUSIC);getWindow().setDecorFitsSystemWindows(false);getWindow().setStatusBarColor(Color.rgb(250,246,255));getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
  screen=new PlayerScreen(this);ScrollView scroll=screen.scroll;root=screen.root;
  setContentView(scroll);View decor=getWindow().getDecorView();decor.setOnApplyWindowInsetsListener((v,insets)->{android.graphics.Insets bars=insets.getInsets(WindowInsets.Type.systemBars()|WindowInsets.Type.displayCutout()|WindowInsets.Type.ime());scroll.setPadding(bars.left,bars.top,bars.right,bars.bottom);return WindowInsets.CONSUMED;});decor.requestApplyInsets();
  importShare(getIntent());if(getIntent().getBooleanExtra("showSettings",false))screen.tab(3);
 }
 void refreshStage(){
  stageSlot.setLayoutParams(new LinearLayout.LayoutParams(-1,dp(EpicaService.running||overlayStarting?42:180)));
  if(EpicaService.running||overlayStarting){if(stage!=null){stageSlot.removeView(stage);stage.dispose();stage=null;}if(stageSlot.getChildCount()==0){TextView t=new TextView(this);t.setText("에피카가 화면 위에서 함께하고 있어요");t.setTextColor(Color.rgb(126,100,151));UiFonts.apply(t);stageSlot.addView(t);}return;}
  if(stage==null){stageSlot.removeAllViews();stage=new LocalStage(this);stage.setExitListener(()->{if(previewHideAfterExit)finishAndRemoveTask();});stage.setMenuListener(()->showCharacterMenu());stage.setDoubleListener(()->showMusicChoices());stage.setTapListener(()->action(media.playing()?"05":"02"));stageSlot.addView(stage,new LinearLayout.LayoutParams(-1,-1));stage.whenReady(()->{stage.evaluateJavascript("window.epicaSizes("+SizeSettings.main(this)+","+SizeSettings.pets(this)+")",null);if(!stage.hasAction())stage.action("01");stage.media();});}
 }
 boolean previewHideAfterExit;
 void action(String id){
  previewHideAfterExit=id.equals("06");
  if(EpicaService.instance.get()!=null)EpicaService.instance.get().animate(id);
  else if(stage!=null){if(id.equals("02")){if(media.command(id,this))stage.action(id);else Toast.makeText(this,media.status().optString("error"),Toast.LENGTH_LONG).show();updateStatus();}else stage.actionThen(id,()->{if(!media.command(id,this))Toast.makeText(this,media.status().optString("error"),Toast.LENGTH_LONG).show();updateStatus();});}
 }
 void selectTrack(long id,String motion){
  Runnable command=()->{if(!media.selectQueue(id))Toast.makeText(this,media.status().optString("error"),Toast.LENGTH_LONG).show();updateStatus();};
  if(EpicaService.instance.get()!=null)EpicaService.instance.get().withCue(motion,command);else if(stage!=null)stage.actionThen(motion,command);
 }
 void startOverlay(){
  if(!DisplayAccessService.available()){openOverlaySettings(true);return;}
  if(!media.access()){
   AlertDialog dialog=new AlertDialog.Builder(this).setTitle("음악 연결 안내").setMessage("알림 접근을 허용하면 유튜브 뮤직의 재생 상태와 음악 조작을 연결해요.\n\n허용한 뒤 유튜브 뮤직에서 곡을 한 번 선택해 주세요. 연결 없이 실행하면 캐릭터만 표시되고 음악은 조작하지 않아요.")
    .setPositiveButton("음악 연결 설정",(d,w)->openMusicAccess(true)).setNeutralButton("캐릭터만 실행",(d,w)->requestNoticePermission(true)).setNegativeButton("취소",null).create();dialog.show();UiFonts.dialog(dialog);return;
  }
  requestNoticePermission(true);
 }
 void openOverlaySettings(boolean resume){AlertDialog dialog=new AlertDialog.Builder(this).setTitle("화면 표시 전용 접근성 설정").setMessage("에피카 그림을 선명하게 표시하면서 투명한 여백의 터치를 아래 앱으로 통과시키기 위해 ‘에피카 화면 표시’를 사용해요.\n\n이 서비스는 다른 앱의 화면·텍스트를 읽거나 이벤트를 수집하지 않고 제스처를 전송하지 않아요. 음악 연결은 별도의 알림 접근을 사용해요.\n\n다음 설정 목록에서 ‘에피카 화면 표시’를 직접 켜 주세요.").setPositiveButton("화면 표시 설정 열기",(d,w)->{overlayPermissionPending=resume;try{startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));}catch(ActivityNotFoundException missing){overlayPermissionPending=false;Toast.makeText(this,"기기에서 접근성 설정을 열 수 없어요.",Toast.LENGTH_LONG).show();}}).setNegativeButton("취소",null).create();dialog.show();UiFonts.dialog(dialog);}
 void waitForDisplay(){long until=SystemClock.uptimeMillis()+4000;handler.post(new Runnable(){public void run(){if(!active||isFinishing()||isDestroyed())return;if(DisplayAccessService.available()){startOverlay();return;}if(SystemClock.uptimeMillis()<until){handler.postDelayed(this,100);return;}updateStatus();Toast.makeText(MainActivity.this,"‘에피카 화면 표시’ 연결을 확인한 뒤 실행을 다시 눌러 주세요.",Toast.LENGTH_LONG).show();}});}
 void openMusicAccess(boolean resume){musicPermissionPending=resume;try{startActivity(new Intent(Settings.ACTION_NOTIFICATION_LISTENER_DETAIL_SETTINGS).putExtra(Settings.EXTRA_NOTIFICATION_LISTENER_COMPONENT_NAME,new ComponentName(this,MusicListener.class).flattenToString()));}catch(ActivityNotFoundException missing){try{startActivity(new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS));}catch(ActivityNotFoundException unavailable){musicPermissionPending=false;Toast.makeText(this,"기기에서 알림 접근 설정을 열 수 없어요.",Toast.LENGTH_LONG).show();}}}
 void requestNoticePermission(boolean launch){noticeLaunchPending=launch;if(Build.VERSION.SDK_INT>=33&&checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED){requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS},12);return;}noticeLaunchPending=false;if(launch)startOverlayDisplay(true);}
 @Override public void onRequestPermissionsResult(int request,String[] permissions,int[] grants){super.onRequestPermissionsResult(request,permissions,grants);if(request==12){boolean launch=noticeLaunchPending;noticeLaunchPending=false;if(launch&&DisplayAccessService.available())startOverlayDisplay(true);}}
 void showFontNotice(){String text;try(java.io.InputStream in=getAssets().open("fonts/NOTICE-Maplestory.txt");java.io.ByteArrayOutputStream out=new java.io.ByteArrayOutputStream()){byte[] buffer=new byte[2048];int n;while((n=in.read(buffer))!=-1)out.write(buffer,0,n);text=new String(out.toByteArray(),java.nio.charset.StandardCharsets.UTF_8);}catch(java.io.IOException missing){text="메이플스토리 서체 · © NEXON Korea Corporation";}AlertDialog d=new AlertDialog.Builder(this).setTitle("서체 저작권 안내").setMessage(text).setPositiveButton("닫기",null).create();d.show();UiFonts.dialog(d);}
 void showMusicChoices(){
  AlertDialog d=new AlertDialog.Builder(this).setTitle("종료 방식 선택")
   .setAdapter(UiFonts.items(this,android.R.layout.simple_list_item_1,new String[]{"유튜브 뮤직 종료","유튜브 뮤직 종료 + 홈에서 없애기"}),(dialog,item)->{
    EpicaService s=EpicaService.instance.get();if(s!=null){s.closeMusic(item==1);return;}
    if(stage!=null){previewHideAfterExit=item==1;stage.actionThen("06",()->{media.stopMusic();updateStatus();});}
   }).setNegativeButton("취소",null).create();d.show();UiFonts.dialog(d);
 }
 void showCharacterMenu(){AlertDialog d=new AlertDialog.Builder(this).setTitle("에피카 메뉴").setAdapter(UiFonts.items(this,android.R.layout.simple_list_item_1,new String[]{"설정","유튜브 뮤직 열기","종료"}),(dialog,item)->{if(item==0)screen.tab(3);else if(item==1)media.command("01",this);else{EpicaService.close(this);media.cancelPending();finishAndRemoveTask();}}).setNegativeButton("계속하기",null).create();d.show();UiFonts.dialog(d);}
 void startOverlayDisplay(boolean leavePlayer){
  if(!DisplayAccessService.available())return;
  try{overlayStarting=true;refreshStage();getSharedPreferences("epica",0).edit().putBoolean("enabled",true).apply();startForegroundService(new Intent(this,EpicaService.class));long deadline=SystemClock.uptimeMillis()+15000;handler.post(new Runnable(){public void run(){if(isFinishing()||isDestroyed())return;EpicaService s=EpicaService.instance.get();if(s!=null&&s.stage!=null&&s.stage.hasVisibleFrame()&&s.hasInputTarget()){overlayStarting=false;if(leavePlayer)finish();else refreshStage();return;}if(SystemClock.uptimeMillis()<deadline)handler.postDelayed(this,80);else{overlayStarting=false;EpicaService.close(MainActivity.this);handler.postDelayed(()->refreshStage(),120);Toast.makeText(MainActivity.this,"화면 표시와 캐릭터 입력 영역을 준비하지 못했어요. 설정을 확인해 주세요.",Toast.LENGTH_LONG).show();}}});}catch(RuntimeException e){overlayStarting=false;refreshStage();Toast.makeText(this,"화면 표시를 시작하지 못했어요. 권한을 확인해 주세요.",Toast.LENGTH_LONG).show();}
 }
 void updateStatus(){
  if(status==null)return;org.json.JSONObject s=media.status();if(screen!=null)screen.update(s);
  if(stage!=null)stage.media();
 }
 TextView text(String value,int size,int color){TextView t=new TextView(this);t.setText(value);t.setTextSize(size);t.setTextColor(color);t.setPadding(0,dp(4),0,dp(6));root.addView(t);return t;}
 void button(String label,View.OnClickListener listener){Button b=new Button(this);b.setText(label);b.setAllCaps(false);b.setOnClickListener(listener);root.addView(b,new LinearLayout.LayoutParams(-1,dp(50)));}
 void row(String[] labels,String[] ids){LinearLayout row=new LinearLayout(this);for(int i=0;i<labels.length;i++){String id=ids[i];Button b=new Button(this);b.setText(labels[i]);b.setAllCaps(false);b.setOnClickListener(v->action(id));row.addView(b,new LinearLayout.LayoutParams(0,dp(52),1));}root.addView(row);}
 int dp(float x){return Math.round(x*getResources().getDisplayMetrics().density);}
 void importShare(Intent i){if(Intent.ACTION_SEND.equals(i.getAction())){String value=i.getStringExtra(Intent.EXTRA_TEXT);if(value!=null)screen.saveLink(i.getStringExtra(Intent.EXTRA_SUBJECT)==null?"공유한 음악":i.getStringExtra(Intent.EXTRA_SUBJECT),value);i.setAction(Intent.ACTION_MAIN);}}
 @Override protected void onNewIntent(Intent i){super.onNewIntent(i);setIntent(i);importShare(i);}
 @Override protected void onResume(){super.onResume();active=true;boolean restore=getIntent().getBooleanExtra("restoreOverlay",false),requested=restore||overlayPermissionPending,fromMusic=musicPermissionPending;getIntent().removeExtra("restoreOverlay");overlayPermissionPending=false;musicPermissionPending=false;refreshStage();if(requested)waitForDisplay();else if(fromMusic&&media.access())requestNoticePermission(true);if(stage!=null)stage.setActive(true);media.observe(update);handler.removeCallbacks(poll);handler.post(poll);}
 @Override public void onConfigurationChanged(android.content.res.Configuration c){super.onConfigurationChanged(c);getWindow().getDecorView().requestApplyInsets();if(stage!=null)stage.cancelInput();refreshStage();}
 @Override protected void onPause(){active=false;media.unobserve(update);handler.removeCallbacks(poll);if(stage!=null)stage.setActive(false);super.onPause();}
 @Override protected void onDestroy(){instance.clear();handler.removeCallbacksAndMessages(null);media.unobserve(update);if(stage!=null)stage.dispose();super.onDestroy();}
}
