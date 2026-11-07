package com.oddfactory.epica;
final class ReleaseGeometry {
 static int clamp(int value,int low,int high){return Math.max(low,Math.min(value,Math.max(low,high)));}
 static int[] fit(int availableWidth,int availableHeight,int desiredWidth){
  int width=Math.max(1,Math.min(desiredWidth,Math.min(Math.max(1,availableWidth),Math.max(1,availableHeight)*3/2)));
  return new int[]{width,Math.max(1,Math.min(Math.max(1,availableHeight),width*2/3))};
 }
 static String playback(boolean access,boolean session,int state,String error){
  if(error!=null&&!error.isEmpty())return error;
  if(!access)return "음악 미연결 · 알림 접근을 허용해 주세요";
  if(!session)return "음악 미연결 · 유튜브 뮤직에서 곡을 선택해 주세요";
  switch(state){case 3:return "재생 중";case 2:return "일시정지";case 6:case 8:case 9:return "음악 준비 중";case 7:return "음악 앱 오류 · 유튜브 뮤직을 확인해 주세요";case 1:return "재생 종료";default:return "재생 상태 확인 중";}
 }
}
