package com.oddfactory.epica;
import android.app.*;
import android.content.*;
import android.content.pm.ServiceInfo;
import android.content.res.Configuration;
import android.graphics.*;
import android.graphics.drawable.GradientDrawable;
import android.os.*;
import android.provider.Settings;
import android.view.*;
import android.widget.*;

public class EpicaService extends Service {
 public static volatile boolean running=false;
 public static java.lang.ref.WeakReference<EpicaService> instance=new java.lang.ref.WeakReference<>(null);
 final Handler handler=new Handler(Looper.getMainLooper());
 WindowManager wm;WindowManager.LayoutParams position,panelPosition;LocalStage stage;LinearLayout panel;TextView track;BodyTargetWindow bodyInput;int[] bodyRectangle;
 MediaLink media;EpiconTroop troop;boolean visible=false,registered=false;int commandVersion=0;String pendingAction="01";
 boolean hideAfterExit=false;Runnable pendingHold;TapSequence taps;View.OnTouchListener touchController;
 void cancelTap(){if(taps!=null)taps.cancel();}
 void cancelInput(){cancelTap();if(pendingHold!=null)handler.removeCallbacks(pendingHold);pendingHold=null;if(stage!=null)stage.drag(false);if(troop!=null)troop.drag(false);if(touchController!=null&&stage!=null){MotionEvent event=MotionEvent.obtain(0,SystemClock.uptimeMillis(),MotionEvent.ACTION_CANCEL,0,0,0);touchController.onTouch(stage,event);event.recycle();}}
 final Runnable update=()->{if(stage!=null)stage.media();if(track!=null){org.json.JSONObject s=media.status();track.setText(!s.optString("title").isEmpty()?s.optString("title"):ReleaseGeometry.playback(s.optBoolean("access"),s.optBoolean("session"),s.optInt("state"),s.optString("error")));}};
 final Runnable poll=new Runnable(){public void run(){
  if(!running)return;if(!DisplayAccessService.available()){stopSelf();return;}
  boolean next=getSystemService(PowerManager.class).isInteractive()&&!getSystemService(KeyguardManager.class).isKeyguardLocked();
  if(next!=visible){visible=next;stage.setVisibility(visible?View.VISIBLE:View.GONE);if(!visible){cancelInput();closePanel();}if(bodyInput!=null)bodyInput.setVisible(visible);stage.setActive(visible);if(troop!=null)troop.visible(visible);}
  if(visible){media.refresh();update.run();}
  handler.postDelayed(this,visible?700:1500);
 }};
 final BroadcastReceiver screen=new BroadcastReceiver(){public void onReceive(Context c,Intent i){handler.removeCallbacks(poll);handler.post(poll);}};
 public static void close(Context c){c.getSharedPreferences("epica",0).edit().putBoolean("enabled",false).apply();c.stopService(new Intent(c,EpicaService.class));}
 @Override public void onCreate(){super.onCreate();media=MediaLink.get(this);}
 @Override public int onStartCommand(Intent i,int flags,int id){
  if(i!=null&&"STOP".equals(i.getAction())){if(running)perform("06");else stopSelf();return START_NOT_STICKY;}
  DisplayAccessService host=DisplayAccessService.current();if(host==null||!getSharedPreferences("epica",0).getBoolean("enabled",false)){stopSelf();return START_NOT_STICKY;}
  if(running)return START_NOT_STICKY;
  wm=host.displayWindows();
  NotificationManager nm=getSystemService(NotificationManager.class);NotificationChannel ch=new NotificationChannel("conductor","에피카 화면 표시",NotificationManager.IMPORTANCE_LOW);ch.setSound(null,null);ch.enableVibration(false);nm.createNotificationChannel(ch);
  PendingIntent open=PendingIntent.getActivity(this,0,new Intent(this,MainActivity.class),PendingIntent.FLAG_IMMUTABLE|PendingIntent.FLAG_UPDATE_CURRENT);
  PendingIntent stop=PendingIntent.getService(this,1,new Intent(this,EpicaService.class).setAction("STOP"),PendingIntent.FLAG_IMMUTABLE|PendingIntent.FLAG_UPDATE_CURRENT);
  Notification n=new Notification.Builder(this,"conductor").setSmallIcon(R.drawable.ic_epica).setContentTitle("에피카 화면 표시").setContentText("터치: 재생·정지 · 두 번: 음악 종료 선택 · 세 번: 설정 메뉴").setOngoing(true).setOnlyAlertOnce(true).setContentIntent(open).addAction(new Notification.Action.Builder(null,"종료 인사",stop).build()).build();
  if(Build.VERSION.SDK_INT>=34)startForeground(12,n,ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE);else startForeground(12,n);
  try{
   stage=new LocalStage(host);stage.setContentDescription("에피카 그림");stage.setExitListener(()->{if(hideAfterExit)close(this);});position=params(dp(288),dp(192));restore();installTouch();bodyInput=new BodyTargetWindow(this,"에피카 몸 안쪽 · 터치 조작",touchController,()->perform(media.playing()?"05":"02"));stage.setBodyTargetListener(this::acceptBodyTarget);
   stage.setVisibility(View.GONE);stage.setActive(false);wm.addView(stage,position);try{troop=new EpiconTroop(this);troop.visible(false);stage.setFollowerListener(troop::state);}catch(Exception failure){throw new IllegalStateException("Character followers unavailable",failure);}
   running=true;instance=new java.lang.ref.WeakReference<>(this);stage.whenReady(()->{if(!stage.hasAction())stage.action(pendingAction);stage.media();});
   IntentFilter f=new IntentFilter();f.addAction(Intent.ACTION_SCREEN_OFF);f.addAction(Intent.ACTION_SCREEN_ON);f.addAction(Intent.ACTION_USER_PRESENT);if(Build.VERSION.SDK_INT>=33)registerReceiver(screen,f,Context.RECEIVER_NOT_EXPORTED);else registerReceiver(screen,f);registered=true;
   media.observe(update);handler.post(poll);
  }catch(RuntimeException e){android.util.Log.e("Epica","Overlay could not start",e);stopSelf();}
  return START_NOT_STICKY;
 }
 WindowManager.LayoutParams params(int w,int h){return windowParams(w,h,false);}
 WindowManager.LayoutParams inputParams(int w,int h){return windowParams(w,h,true);}
 WindowManager.LayoutParams windowParams(int w,int h,boolean interactive){
  int flags=WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE|WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL|WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED|WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN;if(!interactive)flags|=WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE;
  WindowManager.LayoutParams p=new WindowManager.LayoutParams(w,h,WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,flags,PixelFormat.TRANSLUCENT);
  p.setFitInsetsTypes(0);p.layoutInDisplayCutoutMode=WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS;p.gravity=Gravity.TOP|Gravity.LEFT;p.alpha=1f;p.setTitle("에피카 음악 지휘");return p;
 }
 void acceptBodyTarget(String encoded){try{org.json.JSONArray r=new org.json.JSONArray(encoded);if(r.length()!=4){bodyRectangle=null;updateBodyTarget();return;}int[] next={r.getInt(0),r.getInt(1),r.getInt(2),r.getInt(3)};if(next[0]<0||next[1]<0||next[2]>960||next[3]>640||next[2]<=next[0]||next[3]<=next[1])return;bodyRectangle=next;updateBodyTarget();}catch(org.json.JSONException invalid){bodyRectangle=null;updateBodyTarget();}}
 void updateBodyTarget(){if(bodyInput!=null&&position!=null)bodyInput.update(position.x,position.y,OpaqueTargetMath.map(bodyRectangle,960,640,position.width,position.height));}
 boolean hasInputTarget(){return bodyInput!=null&&bodyInput.ready();}
 Rect area(){WindowMetrics m=wm.getCurrentWindowMetrics();Rect b=m.getBounds();Insets insets=m.getWindowInsets().getInsetsIgnoringVisibility(WindowInsets.Type.systemBars()|WindowInsets.Type.displayCutout());return new Rect(insets.left,insets.top,b.width()-insets.right,b.height()-insets.bottom);}
 void clamp(){Rect a=area();int[] size=ReleaseGeometry.fit(a.width(),a.height(),dp(288*SizeSettings.main(this)));position.width=size[0];position.height=size[1];position.x=ReleaseGeometry.clamp(position.x,a.left,a.right-position.width);position.y=ReleaseGeometry.clamp(position.y,a.top,a.bottom-position.height);}
 public void resize(){if(stage==null)return;cancelInput();closePanel();clamp();wm.updateViewLayout(stage,position);updateBodyTarget();save();if(troop!=null)troop.resized();}
 void restore(){Rect a=area();android.content.SharedPreferences p=getSharedPreferences("epica",0);position.x=a.left+Math.round(p.getFloat("x",.98f)*Math.max(0,a.width()-position.width));position.y=a.top+Math.round(p.getFloat("y",.72f)*Math.max(0,a.height()-position.height));clamp();}
 void save(){Rect a=area();getSharedPreferences("epica",0).edit().putFloat("x",(position.x-a.left)/(float)Math.max(1,a.width()-position.width)).putFloat("y",(position.y-a.top)/(float)Math.max(1,a.height()-position.height)).apply();}
 public void animate(String id){perform(id);}
 void perform(String id){cancelTap();if(id.equals("06")){closeMusic(true);return;}hideAfterExit=false;if(id.equals("02")){commandVersion++;pendingAction=id;closePanel();if(media.command(id,this))stage.action(id);else Toast.makeText(this,media.status().optString("error"),Toast.LENGTH_LONG).show();return;}withCue(id,()->{if(!media.command(id,this))Toast.makeText(this,media.status().optString("error"),Toast.LENGTH_LONG).show();});}
 void closeMusic(boolean hide){if(stage==null)return;cancelTap();closePanel();commandVersion++;pendingAction="06";hideAfterExit=hide;stage.actionThen("06",()->{if(running&&!media.stopMusic())Toast.makeText(this,media.status().optString("error"),Toast.LENGTH_LONG).show();});if(hide){int version=commandVersion;handler.postDelayed(()->{if(version==commandVersion&&running)close(this);},6500);}}
 public void withCue(String id,Runnable command){if(stage==null)return;hideAfterExit=id.equals("06");commandVersion++;pendingAction=id;closePanel();stage.actionThen(id,()->{if(running)command.run();});if(id.equals("06")){int version=commandVersion;handler.postDelayed(()->{if(version==commandVersion&&running)close(this);},6500);}}
 void installTouch(){
  taps=new TapSequence(new TapSequence.Scheduler(){public void post(Runnable r,long delay){handler.postDelayed(r,delay);}public void remove(Runnable r){handler.removeCallbacks(r);}},ViewConfiguration.getDoubleTapTimeout(),ViewConfiguration.get(this).getScaledDoubleTapSlop(),()->{if(running&&visible)perform(media.playing()?"05":"02");},()->{if(running&&visible)showCloseChoices();},()->{if(running&&visible)showPanel();});
  touchController=new View.OnTouchListener(){
  float x,y;int baseX,baseY;long start;boolean moving;int pointer=-1;View owner;
  public boolean onTouch(View view,MotionEvent e){
   switch(e.getActionMasked()){
    case MotionEvent.ACTION_DOWN:if(pointer>=0)return true;owner=view;pointer=e.getPointerId(0);x=e.getRawX();y=e.getRawY();baseX=position.x;baseY=position.y;start=SystemClock.uptimeMillis();moving=false;taps.down(start,x,y);if(pendingHold!=null)handler.removeCallbacks(pendingHold);pendingHold=()->{if(pointer<0||!running||!visible)return;cancelTap();moving=true;stage.drag(true);if(troop!=null)troop.drag(true);closePanel();};handler.postDelayed(pendingHold,ViewConfiguration.getLongPressTimeout());return true;
    case MotionEvent.ACTION_MOVE:
     if(owner!=view||e.getPointerId(e.getActionIndex())!=pointer)return true;if(!moving&&Math.hypot(e.getRawX()-x,e.getRawY()-y)>ViewConfiguration.get(EpicaService.this).getScaledTouchSlop()){if(pendingHold!=null)handler.removeCallbacks(pendingHold);pendingHold=null;}
     if(moving){cancelTap();float effectiveScale=position.width/(float)dp(288);stage.dragPull((e.getRawY()-y)/Math.max(1,dp(48*effectiveScale)));position.x=baseX+Math.round(e.getRawX()-x);position.y=baseY+Math.round(e.getRawY()-y);clamp();wm.updateViewLayout(stage,position);updateBodyTarget();}return true;
    case MotionEvent.ACTION_UP:
     if(owner!=view||pointer<0||e.getPointerId(e.getActionIndex())!=pointer)return true;pointer=-1;owner=null;if(pendingHold!=null)handler.removeCallbacks(pendingHold);pendingHold=null;if(moving){moving=false;stage.drag(false);if(troop!=null)troop.drag(false);save();return true;}float dx=e.getRawX()-x,dy=e.getRawY()-y;
     if(Math.abs(dx)>dp(36)&&Math.abs(dx)>Math.abs(dy)*1.3){cancelTap();perform(dx>0?"03":"04");return true;}
     if(Math.hypot(dx,dy)<dp(14))taps.up(SystemClock.uptimeMillis());else cancelTap();return true;
    case MotionEvent.ACTION_CANCEL:pointer=-1;owner=null;cancelTap();if(pendingHold!=null)handler.removeCallbacks(pendingHold);pendingHold=null;if(moving){moving=false;stage.drag(false);if(troop!=null)troop.drag(false);}return true;
    case MotionEvent.ACTION_POINTER_DOWN:cancelInput();return true;
    case MotionEvent.ACTION_POINTER_UP:if(e.getPointerId(e.getActionIndex())==pointer){pointer=-1;owner=null;cancelTap();if(pendingHold!=null)handler.removeCallbacks(pendingHold);pendingHold=null;moving=false;stage.drag(false);if(troop!=null)troop.drag(false);}return true;
    default:return true;
   }
  }
 };}
 void showPanel(){
  cancelTap();if(!visible)return;if(panel!=null){closePanel();return;}
  panel=new LinearLayout(this);panel.setOrientation(LinearLayout.VERTICAL);panel.setPadding(dp(10),dp(8),dp(10),dp(8));
  GradientDrawable bg=new GradientDrawable();bg.setColor(Color.rgb(255,249,240));bg.setCornerRadius(dp(12));bg.setStroke(dp(1),Color.rgb(180,151,200));panel.setBackground(bg);panel.setElevation(dp(4));
  track=new TextView(this);track.setTextColor(Color.rgb(91,66,109));track.setMaxLines(2);panel.addView(track);
  String[][] labels={{"앱 실행","재생"},{"다음 곡","이전 곡"},{"일시정지","종료 인사"}};String[][] ids={{"01","02"},{"03","04"},{"05","06"}};
  for(int row=0;row<3;row++){LinearLayout line=new LinearLayout(this);for(int col=0;col<2;col++){String action=ids[row][col];Button button=new Button(this);button.setText(labels[row][col]);button.setTextSize(13);button.setOnClickListener(v->perform(action));line.addView(button,new LinearLayout.LayoutParams(0,dp(44),1));}panel.addView(line);}
  Button musicOptions=new Button(this);musicOptions.setText("음악 종료 방식 선택");musicOptions.setOnClickListener(v->showCloseChoices());panel.addView(musicOptions);
  LinearLayout line=new LinearLayout(this);Button settings=new Button(this);settings.setText("설정");settings.setTextSize(13);settings.setOnClickListener(v->{closePanel();startActivity(new Intent(this,MainActivity.class).putExtra("showSettings",true).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));});line.addView(settings,new LinearLayout.LayoutParams(0,dp(44),1));Button exit=new Button(this);exit.setText("에피카 종료");exit.setTextSize(13);exit.setOnClickListener(v->exitOwn());line.addView(exit,new LinearLayout.LayoutParams(0,dp(44),1));panel.addView(line);Button hide=new Button(this);hide.setText("계속하기");hide.setOnClickListener(v->closePanel());panel.addView(hide);
  addPanelWindow(360);update.run();
 }
 View panelWindow;
 void addPanelWindow(int desiredHeight){UiFonts.apply(panel);ScrollView scroll=new ScrollView(this);scroll.setFillViewport(false);scroll.addView(panel);panelWindow=scroll;Rect a=area();panelPosition=inputParams(Math.max(1,Math.min(dp(280),a.width())),Math.max(1,Math.min(dp(desiredHeight),a.height())));panelPosition.x=ReleaseGeometry.clamp(position.x,a.left,a.right-panelPosition.width);panelPosition.y=ReleaseGeometry.clamp(position.y>=a.top+panelPosition.height?position.y-panelPosition.height:position.y+position.height,a.top,a.bottom-panelPosition.height);try{wm.addView(scroll,panelPosition);}catch(RuntimeException e){panel=null;panelWindow=null;track=null;}}
 void exitOwn(){cancelInput();commandVersion++;media.cancelPending();close(this);MainActivity activity=MainActivity.instance.get();if(activity!=null)activity.finishAndRemoveTask();}
 void showCloseChoices(){
  cancelTap();if(!visible)return;closePanel();panel=new LinearLayout(this);panel.setOrientation(LinearLayout.VERTICAL);panel.setPadding(dp(10),dp(8),dp(10),dp(8));GradientDrawable bg=new GradientDrawable();bg.setColor(Color.rgb(255,249,240));bg.setCornerRadius(dp(12));bg.setStroke(dp(1),Color.rgb(180,151,200));panel.setBackground(bg);panel.setElevation(dp(4));TextView title=new TextView(this);title.setText("종료 방식 선택");title.setTextColor(Color.rgb(91,66,109));title.setTextSize(16);panel.addView(title);Button keep=new Button(this);keep.setText("유튜브 뮤직 종료");keep.setAllCaps(false);keep.setOnClickListener(v->closeMusic(false));panel.addView(keep,new LinearLayout.LayoutParams(-1,dp(52)));Button hide=new Button(this);hide.setText("유튜브 뮤직 종료 + 홈에서 없애기");hide.setAllCaps(false);hide.setTextSize(13);hide.setOnClickListener(v->closeMusic(true));panel.addView(hide,new LinearLayout.LayoutParams(-1,dp(60)));Button cancel=new Button(this);cancel.setText("취소");cancel.setOnClickListener(v->closePanel());panel.addView(cancel,new LinearLayout.LayoutParams(-1,dp(44)));addPanelWindow(250);
 }
 void closePanel(){if(panelWindow!=null){try{wm.removeView(panelWindow);}catch(IllegalArgumentException ignored){}panelWindow=null;}else if(panel!=null){try{wm.removeView(panel);}catch(IllegalArgumentException ignored){}}panel=null;track=null;}
 @Override public void onConfigurationChanged(Configuration c){super.onConfigurationChanged(c);if(stage!=null){cancelInput();closePanel();restore();wm.updateViewLayout(stage,position);updateBodyTarget();if(troop!=null)troop.resized();}}
 @Override public void onDestroy(){running=false;instance.clear();cancelInput();commandVersion++;handler.removeCallbacksAndMessages(null);media.cancelPending();media.unobserve(update);if(registered)unregisterReceiver(screen);closePanel();if(bodyInput!=null)bodyInput.close();if(troop!=null)troop.close();if(stage!=null){try{wm.removeView(stage);}catch(IllegalArgumentException ignored){}stage.dispose();}stopForeground(STOP_FOREGROUND_REMOVE);super.onDestroy();}
 int dp(float x){return Math.round(x*getResources().getDisplayMetrics().density);}
 @Override public IBinder onBind(Intent i){return null;}
}
