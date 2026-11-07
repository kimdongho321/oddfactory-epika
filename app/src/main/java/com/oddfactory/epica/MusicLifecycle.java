package com.oddfactory.epica;

/** One farewell per music episode, including delayed MediaSession callbacks. */
final class MusicLifecycle {
 static final int NONE=0,STOPPED=1,PAUSED=2,PLAYING=3,BUFFERING=6,CONNECTING=8;
 int sequence=0;String reason="";boolean emitted=false,experienced=false,awaitingStop=false;
 long stoppedAt=-1,resumeUntil=0;
 void emit(String why){if(emitted)return;emitted=true;reason=why;sequence++;}
 void sessionStarted(){emitted=false;experienced=false;awaitingStop=false;stoppedAt=-1;resumeUntil=0;}
 void localPlay(long now){emitted=false;awaitingStop=false;stoppedAt=-1;resumeUntil=now+1800;}
 void localStop(){awaitingStop=true;emit("epica-close");}
 void sessionGone(String why){if(experienced||awaitingStop)emit(why);awaitingStop=false;stoppedAt=-1;}
 void notificationGone(){if(experienced)emit("notification-removed");}
 void observe(int state,long now){
  if(state==PLAYING||state==BUFFERING||state==CONNECTING){experienced=true;stoppedAt=-1;if(!awaitingStop)emitted=false;return;}
  if(state==PAUSED){experienced=true;awaitingStop=false;stoppedAt=-1;return;}
  awaitingStop=false;
  if(state==STOPPED&&experienced&&now>=resumeUntil){if(stoppedAt<0)stoppedAt=now;else if(now-stoppedAt>=650)emit("stopped");}else stoppedAt=-1;
 }
}
