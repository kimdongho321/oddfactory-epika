package com.oddfactory.epica;
import android.accessibilityservice.*;
import android.content.Intent;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;

/** Own display windows only. No app event subscriptions or content capabilities. */
public final class DisplayAccessService extends AccessibilityService {
 private static volatile DisplayAccessService connected;
 static DisplayAccessService current(){return connected;}
 static boolean available(){return connected!=null;}
 WindowManager displayWindows(){return getSystemService(WindowManager.class);}
 @Override protected void onServiceConnected(){
  AccessibilityServiceInfo info=getServiceInfo();
  if(info==null)return;
  info.eventTypes=0;info.flags=0;info.notificationTimeout=0;info.feedbackType=AccessibilityServiceInfo.FEEDBACK_GENERIC;setServiceInfo(info);
  if(info.getCapabilities()!=0)return;
  connected=this;
 }
 @Override public void onAccessibilityEvent(AccessibilityEvent ignored){}
 @Override public void onInterrupt(){EpicaService.close(this);}
 @Override public boolean onUnbind(Intent intent){if(connected==this)connected=null;EpicaService.close(this);return super.onUnbind(intent);}
 @Override public void onDestroy(){if(connected==this)connected=null;EpicaService.close(this);super.onDestroy();}
}
