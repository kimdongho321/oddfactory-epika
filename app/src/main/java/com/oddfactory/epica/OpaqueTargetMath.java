package com.oddfactory.epica;
final class OpaqueTargetMath {
 static int[] largest(int[] pixels,int width,int height,int threshold,int inset){
  if(width<1||height<1||pixels.length<width*height)return null;
  int[] bars=new int[width],stack=new int[width+1];int best=0;int[] result=null;
  for(int y=0;y<height;y++){
   for(int x=0;x<width;x++)bars[x]=(pixels[y*width+x]>>>24)>=threshold?bars[x]+1:0;
   int top=-1;
   for(int x=0;x<=width;x++){
    int current=x==width?0:bars[x];
    while(top>=0&&bars[stack[top]]>current){int column=stack[top--],h=bars[column],left=top<0?0:stack[top]+1,area=(x-left)*h;if(area>best){best=area;result=new int[]{left,y-h+1,x,y+1};}}
    stack[++top]=x;
   }
  }
  if(result==null||result[2]-result[0]<=2*inset||result[3]-result[1]<=2*inset)return null;
  return new int[]{result[0]+inset,result[1]+inset,result[2]-inset,result[3]-inset};
 }
 static int[] map(int[] rectangle,int sourceWidth,int sourceHeight,int width,int height){
  if(rectangle==null||rectangle.length!=4||sourceWidth<1||sourceHeight<1||width<1||height<1)return null;
  double scale=Math.min(width/(double)sourceWidth,height/(double)sourceHeight),x=(width-sourceWidth*scale)/2,y=(height-sourceHeight*scale)/2;
  int left=(int)Math.ceil(x+rectangle[0]*scale),top=(int)Math.ceil(y+rectangle[1]*scale),right=(int)Math.floor(x+rectangle[2]*scale),bottom=(int)Math.floor(y+rectangle[3]*scale);
  return right>left&&bottom>top?new int[]{left,top,right,bottom}:null;
 }
}
