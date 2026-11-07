package com.oddfactory.epica;
import android.service.notification.NotificationListenerService;
public class MusicListener extends NotificationListenerService {
 @Override public void onListenerConnected(){ MediaLink.get(this).refresh(); }
 @Override public void onListenerDisconnected(){ MediaLink.get(this).refresh(); }
 private boolean targetMedia(android.service.notification.StatusBarNotification n){return n!=null&&MediaLink.TARGET.equals(n.getPackageName())&&n.getNotification()!=null&&n.getNotification().extras!=null&&n.getNotification().extras.containsKey(android.app.Notification.EXTRA_MEDIA_SESSION);}
 @Override public void onNotificationPosted(android.service.notification.StatusBarNotification n){if(targetMedia(n))MediaLink.get(this).targetNotificationPosted();}
 @Override public void onNotificationRemoved(android.service.notification.StatusBarNotification n){if(targetMedia(n))MediaLink.get(this).targetNotificationRemoved();}
 // Only package and media-session marker are used; notification text is never read/logged.
}
