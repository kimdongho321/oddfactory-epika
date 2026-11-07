package com.oddfactory.epica;
import android.graphics.*;
import android.os.*;
import android.util.LruCache;
import android.view.*;
import org.json.*;
import java.util.*;
import java.util.concurrent.*;

/** Three independently moving, opaque character windows. Taps open the menu. */
final class EpiconTroop {
 final EpicaService service;final Handler handler=new Handler(Looper.getMainLooper());
 final ExecutorService decoder=Executors.newSingleThreadExecutor();final Set<String> loading=new HashSet<>();
 final LruCache<String,Bitmap> bitmaps=new LruCache<String,Bitmap>(48*1024*1024){protected int sizeOf(String k,Bitmap b){return b.getAllocationByteCount();}};
 final Pet[] pets=new Pet[3];final Map<String,Path> clips=new HashMap<>();JSONObject data;String state="ready";boolean active=true,dead=false,dragging=false;long last=0,clock=0;
 final Runnable tick=new Runnable(){public void run(){if(dead||!active)return;long now=SystemClock.uptimeMillis();float dt=last==0?0:Math.min(.05f,(now-last)/1000f);last=now;clock=now;for(Pet p:pets)p.advance(dt,now);handler.postDelayed(this,16);}};
 EpiconTroop(EpicaService s)throws Exception{service=s;try(java.io.InputStream in=s.getAssets().open("web/chase-data.json")){java.io.ByteArrayOutputStream out=new java.io.ByteArrayOutputStream();byte[] block=new byte[8192];int count;while((count=in.read(block))!=-1)out.write(block,0,count);data=new JSONObject(out.toString("UTF-8"));}try{for(int i=0;i<3;i++){pets[i]=new Pet(i);pets[i].attach();}preload();handler.post(tick);}catch(RuntimeException failure){close();throw failure;}}
 void preload(){bitmap(data.optJSONObject("atlas").optJSONObject("ready").optString("file"));for(String group:new String[]{"atlas","runs"}){JSONObject entries=data.optJSONObject(group);Iterator<String> keys=entries.keys();while(keys.hasNext())bitmap(entries.optJSONObject(keys.next()).optString("file"));}}
 void state(String value){if(data.optJSONObject("atlas").has(value)){state=value;bitmap(data.optJSONObject("atlas").optJSONObject(value).optString("file"));for(Pet p:pets)p.view.invalidate();}}
 void drag(boolean held){dragging=held;long t=SystemClock.uptimeMillis();for(Pet p:pets){if(held)p.startAt=t+new int[]{80,140,230}[p.index];}}
 void visible(boolean value){active=value;last=0;handler.removeCallbacks(tick);for(Pet p:pets){if(!value)p.cancelGesture();p.view.setVisibility(value?View.VISIBLE:View.GONE);if(p.input!=null)p.input.setVisible(value);}if(value)handler.post(tick);}
 void resized(){dragging=false;for(Pet p:pets)p.resize();}
 Bitmap bitmap(String file){Bitmap b=bitmaps.get(file);if(b!=null)return b;if(dead||!loading.add(file))return null;decoder.execute(()->{Bitmap next=null;try(java.io.InputStream in=service.getAssets().open("web/"+file)){BitmapFactory.Options o=new BitmapFactory.Options();o.inScaled=false;o.inSampleSize=2;o.inMutable=true;o.inPreferredConfig=Bitmap.Config.ARGB_8888;next=BitmapFactory.decodeStream(in,null,o);if(next!=null){int[] pixels=new int[next.getWidth()*next.getHeight()];next.getPixels(pixels,0,next.getWidth(),0,0,next.getWidth(),next.getHeight());for(int j=0;j<pixels.length;j++)if((pixels[j]>>>24)>=224)pixels[j]|=0xff000000;next.setPixels(pixels,0,next.getWidth(),0,0,next.getWidth(),next.getHeight());}}catch(Exception e){android.util.Log.e("EpiconTroop",file,e);}Bitmap result=next;handler.post(()->{loading.remove(file);if(dead){if(result!=null)result.recycle();return;}if(result!=null)bitmaps.put(file,result);for(Pet p:pets)p.view.invalidate();});});return null;}
 void close(){dead=true;handler.removeCallbacksAndMessages(null);decoder.shutdownNow();for(Pet p:pets){if(p==null)continue;p.cancelGesture();if(p.input!=null)p.input.close();p.view.clearMask();try{service.wm.removeView(p.view);}catch(IllegalArgumentException ignored){}}bitmaps.evictAll();}
 Path clip(String key,int index,JSONObject m){String cache=key+index;Path p=clips.get(cache);if(p!=null)return p;p=new Path();int h=m.optInt("height"),w=m.optInt("width");JSONArray seams=m.optJSONArray("seams"),left=index==0?null:seams.optJSONArray(index-1),right=index==2?null:seams.optJSONArray(index);p.moveTo(left==null?0:left.optInt(0),0);for(int y=1;y<h;y++)p.lineTo(left==null?0:left.optInt(y),y);for(int y=h-1;y>=0;y--)p.lineTo(right==null?w:right.optInt(y),y);p.close();clips.put(cache,p);return p;}
 JSONArray snapshot(){JSONArray a=new JSONArray();for(Pet p:pets){JSONObject x=new JSONObject();try{x.put("x",p.x);x.put("y",p.y);x.put("targetX",p.tx);x.put("targetY",p.ty);x.put("running",p.running);x.put("speedDp",data.optJSONArray("followSpeedsDp").optInt(p.index));}catch(Exception ignored){}a.put(x);}return a;}
 final class Pet {
  final int index;final PetView view;WindowManager.LayoutParams pos;BodyTargetWindow input;TapSequence taps;int pointer=-1;float touchX,touchY;boolean touchMoved;float x,y,tx,ty;boolean initialized=false,running=false,right=true;long startAt=0;
  Pet(int i){index=i;view=new PetView(this);}
  float scale(){return SizeSettings.pets(service);}
  void attach(){pos=service.params(service.dp(32*scale()),service.dp(36*scale()));pos.alpha=1f;pos.setTitle("에피콘 "+(index+1));view.setContentDescription("에피콘 그림");target();x=tx;y=ty;initialized=true;place();service.wm.addView(view,pos);taps=new TapSequence(new TapSequence.Scheduler(){public void post(Runnable r,long delay){handler.postDelayed(r,delay);}public void remove(Runnable r){handler.removeCallbacks(r);}},ViewConfiguration.getDoubleTapTimeout(),ViewConfiguration.get(service).getScaledDoubleTapSlop(),()->service.showPanel(),()->service.showCloseChoices(),()->service.showPanel());input=new BodyTargetWindow(service,"에피콘 몸 안쪽 · 터치 메뉴 · 두 번 음악 종료 선택 · 세 번 설정 메뉴",(v,event)->touch(event),()->service.showPanel());input.setVisible(active);}
  boolean touch(MotionEvent e){switch(e.getActionMasked()){
   case MotionEvent.ACTION_DOWN:if(pointer>=0)return true;pointer=e.getPointerId(0);touchX=e.getRawX();touchY=e.getRawY();touchMoved=false;taps.down(SystemClock.uptimeMillis(),touchX,touchY);return true;
   case MotionEvent.ACTION_MOVE:if(pointer>=0&&Math.hypot(e.getRawX()-touchX,e.getRawY()-touchY)>ViewConfiguration.get(service).getScaledTouchSlop()){touchMoved=true;taps.cancel();}return true;
   case MotionEvent.ACTION_UP:if(pointer==e.getPointerId(e.getActionIndex())){pointer=-1;if(!touchMoved)taps.up(SystemClock.uptimeMillis());else taps.cancel();}return true;
   case MotionEvent.ACTION_POINTER_DOWN:case MotionEvent.ACTION_POINTER_UP:case MotionEvent.ACTION_CANCEL:cancelGesture();return true;
  }return true;}
  void cancelGesture(){pointer=-1;touchMoved=false;if(taps!=null)taps.cancel();}
  void target(){float main=service.position.width/(288*service.getResources().getDisplayMetrics().density),pet=scale();Rect area=service.area();float nearest=service.position.x+service.dp(105*main-31*pet);tx=nearest-service.dp((2-index)*22*pet);ty=service.position.y+service.dp(160.8f*main-32*pet);tx=Math.max(area.left,Math.min(tx,area.right-pos.width));ty=Math.max(area.top,Math.min(ty,area.bottom-pos.height));}
  void resize(){cancelGesture();if(input!=null)input.update(pos.x,pos.y,null);view.clearMask();pos.width=service.dp(32*scale());pos.height=service.dp(36*scale());target();Rect safe=service.area();x=ReleaseGeometry.clamp(Math.round(x),safe.left,safe.right-pos.width);y=ReleaseGeometry.clamp(Math.round(y),safe.top,safe.bottom-pos.height);place();service.wm.updateViewLayout(view,pos);view.invalidate();}
  void place(){pos.x=Math.round(x);pos.y=Math.round(y);if(input!=null)input.moveTo(pos.x,pos.y);}
  void advance(float dt,long now){target();float dx=tx-x,dy=ty-y,dist=(float)Math.hypot(dx,dy);running=dist>service.dp(.8f);if(running&&now>=startAt){float step=Math.min(dist,service.dp(data.optJSONArray("followSpeedsDp").optInt(index))*dt);if(Math.abs(dx)>service.dp(1))right=dx>0;x+=dx/dist*step;y+=dy/dist*step;place();service.wm.updateViewLayout(view,pos);}view.invalidate();}
 }
 final class PetView extends View {
  final String[][] sweat={{"..####...",".######..","########.","########.",".########","..######.","....###.."},{"...#####.......",".#########.....","###########....","#############..","###############","#############..",".##########....","...#####......."},{"........#",".....####","...######","..#######",".#######.","#######..","######...","#####....",".###.....","..#......"}};
  final Pet pet;final Paint paint=new Paint();Bitmap shownBitmap,maskBitmap;Canvas maskCanvas;int[] maskPixels;final Map<String,int[]> targetRects=new HashMap<>();JSONObject shownMeta;String shownKey;boolean shownRun;int drawCount=0,fallbackCount=0;
  PetView(Pet p){super(service);pet=p;paint.setFilterBitmap(false);paint.setAntiAlias(false);paint.setAlpha(255);}
  @Override protected void onDraw(Canvas c){super.onDraw(c);float density=getResources().getDisplayMetrics().density,s=pet.scale();boolean chase=pet.running||dragging;JSONObject m;Bitmap b;float factor,pivot,base;int left=0,right;
   String key=chase?"run-"+(pet.right?"right":"left")+"-"+(int)((clock/105+pet.index)%4):state;m=data.optJSONObject(chase?"runs":"atlas").optJSONObject(key);b=bitmap(m.optString("file"));
   if(b!=null){shownBitmap=b;shownMeta=m;shownKey=key;shownRun=chase;}else if(shownBitmap!=null){b=shownBitmap;m=shownMeta;key=shownKey;chase=shownRun;fallbackCount++;}else return;drawCount++;
   drawBody(c,b,m,key,chase);updateInput(b,m,key,chase);
   if(chase){float unit=Math.max(1,service.dp(.45f*s));for(int i=0;i<3;i++){float phase=((clock+pet.index*97+i*170)%650)/650f;boolean outwardRight=!pet.right;float px=service.dp((16+(outwardRight?1:-1)*(new float[]{5,7,5.5f}[i]+phase*2.4f))*s),py=service.dp(new float[]{.5f,5,9.5f}[i]*s);drawSweat(c,px,py,unit,outwardRight,i);}}
  }
  void drawBody(Canvas c,Bitmap b,JSONObject m,String key,boolean chase){float density=getResources().getDisplayMetrics().density,s=pet.scale(),factor,pivot,base;
   if(chase){factor=.0762f*.3f*density*s;pivot=(float)m.optDouble("pivot",620);base=(float)m.optDouble("baseline",1316);}else{pivot=(float)m.optJSONArray("pivots").optDouble(pet.index);base=(float)m.optDouble("baseline");factor=(float)data.optDouble("idleScale")*.3f*density*s;}
   float center=getWidth()/2f,baseline=service.dp(32*s);c.save();c.translate(center-pivot*factor,baseline-base*factor);c.scale(factor,factor);if(!chase)c.clipPath(clip(key,pet.index,m));c.scale((float)m.optInt("width",chase?1120:1958)/b.getWidth(),(float)m.optInt("height",chase?1404:803)/b.getHeight());c.drawBitmap(b,0,0,paint);c.restore();
  }
  void clearMask(){targetRects.clear();if(maskBitmap!=null){maskBitmap.recycle();maskBitmap=null;maskCanvas=null;maskPixels=null;}}
  void updateInput(Bitmap b,JSONObject m,String key,boolean chase){
   if(pet.input==null||!active||dead||getWidth()<1||getHeight()<1)return;
   String cacheKey=key+":"+getWidth()+":"+getHeight();int[] rectangle=targetRects.get(cacheKey);
   if(rectangle==null){
    if(maskBitmap==null||maskBitmap.getWidth()!=getWidth()||maskBitmap.getHeight()!=getHeight()){clearMask();maskBitmap=Bitmap.createBitmap(getWidth(),getHeight(),Bitmap.Config.ARGB_8888);maskCanvas=new Canvas(maskBitmap);maskPixels=new int[getWidth()*getHeight()];}
    maskBitmap.eraseColor(Color.TRANSPARENT);drawBody(maskCanvas,b,m,key,chase);maskBitmap.getPixels(maskPixels,0,getWidth(),0,0,getWidth(),getHeight());rectangle=OpaqueTargetMath.largest(maskPixels,getWidth(),getHeight(),240,1);if(rectangle!=null)targetRects.put(cacheKey,rectangle);
   }
   pet.input.update(pet.pos.x,pet.pos.y,rectangle);
  }
  void drawSweat(Canvas c,float x,float y,float unit,boolean outwardRight,int kind){String[] shape=sweat[kind];for(int row=0;row<shape.length;row++)for(int col=0;col<shape[0].length();col++){if(shape[row].charAt(col)!='#')continue;boolean edge=row==0||row==shape.length-1||col==0||col==shape[0].length()-1||shape[row-1].charAt(col)!='#'||shape[row+1].charAt(col)!='#'||shape[row].charAt(col-1)!='#'||shape[row].charAt(col+1)!='#';paint.setColor(edge?Color.rgb(121,83,74):row==(int)(shape.length*.3)&&col>=2&&col<=4?Color.rgb(214,251,255):row>=shape.length*.6?Color.rgb(40,175,224):Color.rgb(67,213,245));float xx=Math.round(x+(outwardRight?shape[0].length()-1-col:col-shape[0].length())*unit),yy=Math.round(y+row*unit);c.drawRect(xx,yy,xx+unit,yy+unit,paint);}}
 }
}
