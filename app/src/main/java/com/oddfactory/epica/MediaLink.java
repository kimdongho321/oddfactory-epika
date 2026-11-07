package com.oddfactory.epica;
import android.app.NotificationManager;
import android.content.*;
import android.media.MediaMetadata;
import android.media.session.*;
import android.os.*;
import org.json.JSONObject;
import org.json.JSONArray;
import java.util.*;

public final class MediaLink {
 public static final String TARGET="com.google.android.apps.youtube.music";
 private static MediaLink instance;
 private final Context context;
 private final Handler handler=new Handler(Looper.getMainLooper());
 private final Set<Runnable> observers=new HashSet<>();
 private MediaController controller;
 private String error="";
 private long errorUntil=0;
 private boolean registered=false;
 private int commandEpoch=0;
 private String lastTrackKey="",trackMotion="",trackSource="";
 private int lastTrackIndex=-1,trackSequence=0;
 private long lastTrackQueueId=-1;
 private long localSkipUntil=0;
 private int notificationEpoch=0;private final MusicLifecycle lifecycle=new MusicLifecycle();
 private void detectLifecycle(){PlaybackState p=controller==null?null:controller.getPlaybackState();if(p==null)return;long before=lifecycle.stoppedAt;lifecycle.observe(p.getState(),SystemClock.uptimeMillis());if(before<0&&lifecycle.stoppedAt>=0)handler.postDelayed(this::notifyChange,700);}
 public void targetNotificationPosted(){if(!observers.isEmpty())notificationEpoch++;}
 public void targetNotificationRemoved(){if(observers.isEmpty())return;int epoch=++notificationEpoch;handler.postDelayed(()->{if(epoch!=notificationEpoch||observers.isEmpty())return;refresh();PlaybackState p=controller==null?null:controller.getPlaybackState();if(p!=null&&(p.getState()==PlaybackState.STATE_PAUSED||p.getState()==PlaybackState.STATE_STOPPED||p.getState()==PlaybackState.STATE_NONE))lifecycle.notificationGone();notifyChange();},500);}
 private final MediaController.Callback callback=new MediaController.Callback(){
  @Override public void onPlaybackStateChanged(PlaybackState s){notifyChange();}
  @Override public void onMetadataChanged(MediaMetadata m){notifyChange();}
  @Override public void onQueueChanged(List<MediaSession.QueueItem> q){notifyChange();}
  @Override public void onSessionDestroyed(){lifecycle.sessionGone("session-destroyed");controller=null;notifyChange();}
 };
 private final MediaSessionManager.OnActiveSessionsChangedListener sessions=items->{select(items);notifyChange();};
 private MediaLink(Context c){context=c.getApplicationContext();}
 public static synchronized MediaLink get(Context c){if(instance==null)instance=new MediaLink(c);return instance;}
 public boolean access(){return context.getSystemService(NotificationManager.class).isNotificationListenerAccessGranted(new ComponentName(context,MusicListener.class));}
 public void observe(Runnable r){observers.add(r);refresh();}
 public void unobserve(Runnable r){observers.remove(r);if(observers.isEmpty()){cancelPending();if(registered){context.getSystemService(MediaSessionManager.class).removeOnActiveSessionsChangedListener(sessions);registered=false;}if(controller!=null){controller.unregisterCallback(callback);controller=null;}}}
 public void cancelPending(){commandEpoch++;notificationEpoch++;handler.removeCallbacksAndMessages(null);}
 private void notifyChange(){detectLifecycle();detectTrackChange();for(Runnable r:new ArrayList<>(observers))r.run();}
 private void detectTrackChange(){
  if(controller==null)return;MediaMetadata metadata=controller.getMetadata();if(metadata==null)return;
  String title=safe(metadata.getString(MediaMetadata.METADATA_KEY_TITLE)),artist=safe(metadata.getString(MediaMetadata.METADATA_KEY_ARTIST)),mediaId=safe(metadata.getString(MediaMetadata.METADATA_KEY_MEDIA_ID));
  String key=mediaId.isEmpty()?title+"|"+artist:mediaId;if(key.isEmpty()||key.equals("|"))return;
  int index=-1;List<MediaSession.QueueItem> list=controller.getQueue();PlaybackState playback=controller.getPlaybackState();long activeId=playback==null?-1:playback.getActiveQueueItemId();
  if(list!=null)for(int i=0;i<list.size();i++){android.media.MediaDescription d=list.get(i).getDescription();if((!mediaId.isEmpty()&&mediaId.equals(d.getMediaId()))||(!title.isEmpty()&&title.equals(String.valueOf(d.getTitle())))){index=i;break;}}
  if(index<0&&list!=null&&activeId>=0)for(int i=0;i<list.size();i++)if(list.get(i).getQueueId()==activeId){index=i;break;}
  if(!lastTrackKey.isEmpty()&&!lastTrackKey.equals(key)){
   int previousIndex=lastTrackIndex;if(list!=null&&lastTrackQueueId>=0)for(int i=0;i<list.size();i++)if(list.get(i).getQueueId()==lastTrackQueueId){previousIndex=i;break;}
   trackMotion=previousIndex>=0&&index>=0&&index<previousIndex?"04":"03";
   trackSource=SystemClock.uptimeMillis()<localSkipUntil?"epica":"external";localSkipUntil=0;trackSequence++;
  }
  lastTrackKey=key;if(index>=0){lastTrackIndex=index;lastTrackQueueId=list.get(index).getQueueId();}
 }
 private void select(List<MediaController> list){
  MediaController next=null;for(MediaController c:list)if(TARGET.equals(c.getPackageName())){next=c;break;}
  if(controller!=null&&next!=null&&controller.getSessionToken().equals(next.getSessionToken()))return;
  if(controller!=null){if(next==null&&access())lifecycle.sessionGone("session-removed");controller.unregisterCallback(callback);}
  controller=next;lastTrackKey="";lastTrackIndex=-1;lastTrackQueueId=-1;if(controller!=null){lifecycle.sessionStarted();controller.registerCallback(callback,handler);detectLifecycle();detectTrackChange();}
 }
 public void refresh(){
  if(observers.isEmpty())return;
  MediaSessionManager manager=context.getSystemService(MediaSessionManager.class);
  try {
   if(!access()){if(registered){manager.removeOnActiveSessionsChangedListener(sessions);registered=false;}select(Collections.emptyList());return;}
   ComponentName listener=new ComponentName(context,MusicListener.class);
   if(!registered&&!observers.isEmpty()){manager.addOnActiveSessionsChangedListener(sessions,listener,handler);registered=true;}
   select(manager.getActiveSessions(listener));
  }catch(SecurityException e){select(Collections.emptyList());}
 }
 public boolean playing(){PlaybackState s=controller==null?null:controller.getPlaybackState();return s!=null&&s.getState()==PlaybackState.STATE_PLAYING;}
 public JSONObject status(){
  detectLifecycle();detectTrackChange();JSONObject out=new JSONObject();try{
   PlaybackState s=controller==null?null:controller.getPlaybackState();MediaMetadata m=controller==null?null:controller.getMetadata();
   out.put("access",access());out.put("session",controller!=null);out.put("state",s==null?0:s.getState());out.put("playing",playing());
   out.put("title",m==null?"":safe(m.getString(MediaMetadata.METADATA_KEY_TITLE)));out.put("artist",m==null?"":safe(m.getString(MediaMetadata.METADATA_KEY_ARTIST)));
   out.put("error",SystemClock.uptimeMillis()<errorUntil?error:"");out.put("overlay",EpicaService.running);
   out.put("trackSequence",trackSequence);out.put("trackMotion",trackMotion);out.put("trackSource",trackSource);
   out.put("closeSequence",lifecycle.sequence);out.put("closeReason",lifecycle.reason);
   long duration=m==null?0:m.getLong(MediaMetadata.METADATA_KEY_DURATION),position=s==null?0:s.getPosition();if(s!=null&&playing())position+=(long)((SystemClock.elapsedRealtime()-s.getLastPositionUpdateTime())*s.getPlaybackSpeed());
   out.put("position",Math.max(0,duration>0?Math.min(duration,position):position));out.put("duration",duration);out.put("seekable",s!=null&&(s.getActions()&PlaybackState.ACTION_SEEK_TO)!=0);
   out.put("version","1.7.1");out.put("privilegedMediaControl",context.checkSelfPermission("android.permission.MEDIA_CONTENT_CONTROL")==android.content.pm.PackageManager.PERMISSION_GRANTED);
  }catch(Exception ignored){}return out;
 }
 static String safe(String s){return s==null?"":s;}
 public android.graphics.Bitmap artwork(){MediaMetadata m=controller==null?null:controller.getMetadata();if(m==null)return null;android.graphics.Bitmap b=m.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART);return b==null?m.getBitmap(MediaMetadata.METADATA_KEY_ART):b;}
 public boolean seek(long millis){refresh();if(controller==null)return false;PlaybackState s=controller.getPlaybackState();if(s==null||(s.getActions()&PlaybackState.ACTION_SEEK_TO)==0)return false;controller.getTransportControls().seekTo(Math.max(0,millis));return true;}
 public List<PlaybackState.CustomAction> customActions(){PlaybackState s=controller==null?null:controller.getPlaybackState();return s==null?Collections.emptyList():s.getCustomActions();}
 public void custom(PlaybackState.CustomAction a){if(controller!=null)controller.getTransportControls().sendCustomAction(a,a.getExtras());}
 public JSONObject queue(){
  refresh();JSONObject out=new JSONObject();JSONArray list=new JSONArray();try{
   out.put("access",access());out.put("session",controller!=null);
   if(controller!=null){PlaybackState state=controller.getPlaybackState();out.put("activeId",state==null?-1:state.getActiveQueueItemId());CharSequence title=controller.getQueueTitle();out.put("title",title==null?"현재 재생 대기열":title.toString());List<MediaSession.QueueItem> items=controller.getQueue();if(items!=null)for(MediaSession.QueueItem item:items){android.media.MediaDescription d=item.getDescription();JSONObject track=new JSONObject();track.put("id",item.getQueueId());track.put("title",d.getTitle()==null?"제목 없음":d.getTitle().toString());track.put("artist",d.getSubtitle()==null?"":d.getSubtitle().toString());list.put(track);}}
   out.put("items",list);
  }catch(Exception ignored){}return out;
 }
 public boolean selectQueue(long queueId){
  refresh();try{
   if(controller==null)throw new IllegalStateException("유튜브 뮤직에서 곡을 먼저 선택해 주세요.");
   boolean found=false;List<MediaSession.QueueItem> list=controller.getQueue();if(list!=null)for(MediaSession.QueueItem item:list)if(item.getQueueId()==queueId)found=true;
   if(!found)throw new IllegalStateException("대기열이 바뀌었어요. 목록을 새로고침해 주세요.");
   int epoch=++commandEpoch;lifecycle.localPlay(SystemClock.uptimeMillis());localSkipUntil=SystemClock.uptimeMillis()+2500;controller.getTransportControls().skipToQueueItem(queueId);
   handler.postDelayed(()->{if(epoch!=commandEpoch)return;refresh();if(controller!=null)controller.getTransportControls().play();},220);
   error="";notifyChange();return true;
  }catch(RuntimeException e){error=e.getMessage();errorUntil=SystemClock.uptimeMillis()+6000;notifyChange();return false;}
 }
 public boolean command(String action,Context launchContext){
  refresh();error="";commandEpoch++;
  try {
   if(action.equals("01")){
    Intent i=context.getPackageManager().getLaunchIntentForPackage(TARGET);if(i==null)throw new IllegalStateException("유튜브 뮤직을 먼저 설치해 주세요.");
    i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);launchContext.startActivity(i);notifyChange();return true;
   }
   if(action.equals("06")){
    return stopMusic();
   }
   if(!access())throw new IllegalStateException("유튜브 뮤직 연동을 위해 알림 접근을 켜 주세요.");
   if(controller==null)throw new IllegalStateException("유튜브 뮤직에서 곡을 한 번 선택해 주세요.");
   MediaController.TransportControls controls=controller.getTransportControls();
   switch(action){
    case "02":lifecycle.localPlay(SystemClock.uptimeMillis());controls.play();break;
    case "03":localSkipUntil=SystemClock.uptimeMillis()+2500;controls.skipToNext();break;
    case "04":localSkipUntil=SystemClock.uptimeMillis()+2500;controls.skipToPrevious();break;
    case "05":controls.pause();break;
    default:throw new IllegalArgumentException("지원하지 않는 동작");
   }notifyChange();return true;
  }catch(RuntimeException e){error=e.getMessage()==null?"요청을 처리하지 못했어요.":e.getMessage();errorUntil=SystemClock.uptimeMillis()+6000;notifyChange();return false;}
 }
 public boolean stopMusic(){refresh();commandEpoch++;try{if(!access()||controller==null)throw new IllegalStateException("음악 미연결 · 유튜브 뮤직에서 곡을 먼저 선택해 주세요.");controller.getTransportControls().pause();controller.getTransportControls().stop();lifecycle.localStop();notifyChange();return true;}catch(RuntimeException e){error=e.getMessage()==null?"음악 종료 요청을 보내지 못했어요.":e.getMessage();errorUntil=SystemClock.uptimeMillis()+6000;notifyChange();return false;}}
}
