package com.oddfactory.epica;
import android.content.Context;
import android.graphics.Color;
import android.net.Uri;
import android.os.*;
import android.webkit.*;

public class LocalStage extends WebView {
 private final Handler handler=new Handler(Looper.getMainLooper());
 private boolean ready=false,active=true,dead=false,visibleFrame=false;
 private Runnable onReady=()->{},onExit=()->{},onMenu=()->{},onDouble=()->{},onTap=()->{},pendingCommand=null;
 private int actionTicket=0,cuedTicket=-1;
 private String pendingAction=null;
 private java.util.function.Consumer<String> followerListener=value->{};
 private java.util.function.Consumer<String> bodyTargetListener=value->{};
 private boolean externalFollowers=false,bodyTargets=false;
 private boolean dragHeld=false,pullPosted=false;private float pendingPull=0;
 private final Runnable nativeFrame=new Runnable(){public void run(){if(dead||!active||!ready)return;evaluateJavascript("window.epicaNativeFrame&&window.epicaNativeFrame()",null);invalidate();handler.postDelayed(this,33);}};
 private final Runnable sendPull=()->{pullPosted=false;if(ready&&!dead&&dragHeld)evaluateJavascript("window.epicaDragPull("+pendingPull+")",null);};
 public LocalStage(Context c){
  super(c);setBackgroundColor(Color.TRANSPARENT);setVerticalScrollBarEnabled(false);setHorizontalScrollBarEnabled(false);setOverScrollMode(OVER_SCROLL_NEVER);
  WebSettings s=getSettings();s.setJavaScriptEnabled(true);s.setAllowFileAccess(false);s.setAllowContentAccess(false);s.setAllowFileAccessFromFileURLs(false);s.setAllowUniversalAccessFromFileURLs(false);s.setBlockNetworkLoads(true);s.setDomStorageEnabled(false);s.setSupportZoom(false);s.setTextZoom(100);
  setWebViewClient(new WebViewClient(){
   @Override public boolean shouldOverrideUrlLoading(WebView v,WebResourceRequest r){return true;}
   @Override public WebResourceResponse shouldInterceptRequest(WebView v,WebResourceRequest r){Uri u=r.getUrl();String path=u.getPath();if("GET".equals(r.getMethod())&&"https".equals(u.getScheme())&&"appassets.androidplatform.net".equals(u.getHost())&&path!=null&&path.startsWith("/assets/web/")&&path.matches("/[A-Za-z0-9_./-]+")&&!path.contains("..")){try{String mime=path.endsWith(".js")?"application/javascript":path.endsWith(".json")?"application/json":path.endsWith(".png")?"image/png":"text/html";return new WebResourceResponse(mime,path.endsWith(".png")?null:"UTF-8",getContext().getAssets().open(path.substring(8)));}catch(java.io.IOException e){android.util.Log.e("EpicaStage","Local asset unavailable");}}return new WebResourceResponse("text/plain","UTF-8",new java.io.ByteArrayInputStream(new byte[0]));}
  });
  addJavascriptInterface(new Object(){
   @JavascriptInterface public void stageVisible(){handler.post(()->{if(!dead)visibleFrame=true;});}
   @JavascriptInterface public void stageReady(){handler.post(()->{if(dead)return;ready=true;visibleFrame=true;setActive(active);sendAction();onReady.run();if(dragHeld){evaluateJavascript("window.epicaDrag(true)",null);dragPull(pendingPull);}});}
   @JavascriptInterface public void stageError(String e){android.util.Log.e("EpicaStage","Local assets could not load: "+e);}
   @JavascriptInterface public void motionCue(String id,int ticket,String node){handler.post(()->{if(dead||ticket!=actionTicket||ticket==cuedTicket||!id.equals(pendingAction))return;cuedTicket=ticket;Runnable command=pendingCommand;pendingCommand=null;if(command!=null)command.run();});}
   // External negative-ticket farewells end on screen; only an explicit local
   // close request may remove the overlay or finish the activity.
   @JavascriptInterface public void stageExited(int ticket){handler.post(()->{if(!dead&&ticket==actionTicket)onExit.run();});}
   @JavascriptInterface public void followerPose(String state){handler.post(()->{if(!dead)followerListener.accept(state);});}
   @JavascriptInterface public void characterDouble(){handler.post(()->{if(!dead&&active)onDouble.run();});}
   @JavascriptInterface public void characterMenu(){handler.post(()->{if(!dead&&active)onMenu.run();});}
   @JavascriptInterface public void characterTap(){handler.post(()->{if(!dead&&active)onTap.run();});}
   @JavascriptInterface public void bodyTarget(String rectangle){if(rectangle==null||rectangle.length()>100)return;handler.post(()->{if(!dead&&active)bodyTargetListener.accept(rectangle);});}
  },"Android");
  loadUrl("https://appassets.androidplatform.net/assets/web/stage.html");
 }
 public void whenReady(Runnable r){onReady=r;if(ready)r.run();}
 public void setExitListener(Runnable r){onExit=r;}
 public void setMenuListener(Runnable r){onMenu=r;}
 public void setDoubleListener(Runnable r){onDouble=r;}
 public void setTapListener(Runnable r){onTap=r;}
 public void cancelInput(){drag(false);if(ready&&!dead)evaluateJavascript("window.epicaCancelInput&&window.epicaCancelInput()",null);}
 @Override public boolean performClick(){super.performClick();return true;}
 public void action(String action){actionThen(action,null);}
 public void actionThen(String action,Runnable command){if(dead)return;actionTicket++;pendingAction=action;pendingCommand=command;sendAction();}
 public boolean hasAction(){return pendingAction!=null;}
 public void setFollowerListener(java.util.function.Consumer<String> listener){followerListener=listener;externalFollowers=true;if(ready)evaluateJavascript("window.epicaExternalFollowers(true)",null);}
 public void setBodyTargetListener(java.util.function.Consumer<String> listener){bodyTargetListener=listener;bodyTargets=true;if(ready)evaluateJavascript("window.epicaBodyTargets(true)",null);}
 public void drag(boolean held){dragHeld=held;handler.removeCallbacks(sendPull);pullPosted=false;if(!held)pendingPull=0;if(ready&&!dead)evaluateJavascript("window.epicaDrag("+held+")",null);}
 public void dragPull(float vertical){pendingPull=Math.max(-1f,Math.min(1f,vertical));if(ready&&!dead&&dragHeld&&!pullPosted){pullPosted=true;handler.postDelayed(sendPull,16);}}
 private void sendAction(){if(ready&&!dead&&pendingAction!=null)evaluateJavascript("window.epicaAction("+org.json.JSONObject.quote(pendingAction)+","+actionTicket+")",null);}
 public void media(){if(ready&&!dead)evaluateJavascript("window.epicaMedia("+MediaLink.get(getContext()).status()+")",null);}
 public void setActive(boolean value){active=value;handler.removeCallbacks(nativeFrame);if(dead)return;if(!value)cancelInput();if(value)onResume();if(!ready){if(!value)onPause();return;}float density=getResources().getDisplayMetrics().density;android.view.ViewConfiguration vc=android.view.ViewConfiguration.get(getContext());evaluateJavascript("window.EPICA_INPUT={tap:"+android.view.ViewConfiguration.getDoubleTapTimeout()+",hold:"+android.view.ViewConfiguration.getLongPressTimeout()+",slop:"+(vc.getScaledTouchSlop()/density)+",doubleSlop:"+(vc.getScaledDoubleTapSlop()/density)+"}",null);evaluateJavascript("window.epicaNativeClock(true)",null);if(externalFollowers)evaluateJavascript("window.epicaExternalFollowers(true)",null);if(bodyTargets)evaluateJavascript("window.epicaBodyTargets(true)",null);evaluateJavascript("window.epicaActive("+value+")",null);if(value)handler.post(nativeFrame);else onPause();}
 public boolean isReady(){return ready;}
 public boolean hasVisibleFrame(){return visibleFrame;}
 public void dispose(){cancelInput();dead=true;pendingCommand=null;handler.removeCallbacksAndMessages(null);removeJavascriptInterface("Android");stopLoading();destroy();}
}
