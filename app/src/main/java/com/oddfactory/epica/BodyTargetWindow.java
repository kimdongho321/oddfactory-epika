package com.oddfactory.epica;
import android.view.*;
final class BodyTargetWindow {
 final EpicaService service;final View view;WindowManager.LayoutParams layout;
 private int[] rect;private int x,y;private boolean visible,added,closed;
 BodyTargetWindow(EpicaService owner,String description,View.OnTouchListener listener,Runnable click){service=owner;view=new View(owner);view.setContentDescription(description);view.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);view.setOnTouchListener(listener);view.setOnClickListener(v->click.run());}
 boolean ready(){return added&&visible;}
 void setVisible(boolean value){visible=value;sync();}
 void update(int originX,int originY,int[] opaqueRectangle){x=originX;y=originY;rect=opaqueRectangle;sync();}
 void moveTo(int originX,int originY){x=originX;y=originY;sync();}
 private void sync(){
  if(closed)return;
  if(!visible||rect==null){remove();return;}
  int width=rect[2]-rect[0],height=rect[3]-rect[1];if(width<1||height<1){remove();return;}
  try{
   if(layout==null)layout=service.inputParams(width,height);
   boolean changed=layout.x!=x+rect[0]||layout.y!=y+rect[1]||layout.width!=width||layout.height!=height;
   layout.x=x+rect[0];layout.y=y+rect[1];layout.width=width;layout.height=height;
   if(!added){service.wm.addView(view,layout);added=true;}else if(changed)service.wm.updateViewLayout(view,layout);
  }catch(RuntimeException unavailable){remove();closed=true;service.stopSelf();}
 }
 private void remove(){if(added){try{service.wm.removeView(view);}catch(IllegalArgumentException ignored){}added=false;}}
 void close(){closed=true;remove();rect=null;}
}
