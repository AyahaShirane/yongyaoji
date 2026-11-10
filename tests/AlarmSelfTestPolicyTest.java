package cn.medreminder.app;
public final class AlarmSelfTestPolicyTest {
 static void check(boolean b){if(!b)throw new AssertionError();}
 static void eq(Object a,Object b){if(!a.equals(b))throw new AssertionError(a+" != "+b);}
 public static void main(String[] args){
  AlarmSelfTestLifecycle idle=AlarmSelfTestLifecycle.restore(0,0,"idle",0,"",-1,0);
  AlarmSelfTestLifecycle g1=idle.start(1,60000);
  AlarmSelfTestLifecycle g1fired=g1.received(1,60000,60010,false).notification("submitted").foregroundEntry(61000);
  check(g1fired.actual==60010&&g1fired.firstReturn==61000&&g1fired.notification.equals("submitted"));
  AlarmSelfTestLifecycle g2=g1fired.start(2,120000);
  check(g2.actual==0&&g2.notification.isEmpty()&&g2.firstReturn==0&&g2.receiverForeground==-1);
  check(g2.received(1,60000,130000,false)==g2); // replaced generation callback ignored
  check(g2.state.equals("pending")&&g2.actual==0&&g2.notification.isEmpty());
  check(g2.received(2,120000,119999,false)==g2); // early delivery ignored
  AlarmSelfTestLifecycle fired=g2.received(2,120000,120350,false);
  check(fired!=g2&&fired.generation==2&&fired.state.equals("fired")&&fired.actual==120350);
  check(fired.received(2,120000,121000,false)==fired); // duplicate cannot transition/send twice
  check(fired.notification.isEmpty());
  AlarmSelfTestLifecycle canceled=fired.notification("submitted").cancel();
  check(canceled.generation==3&&canceled.state.equals("cancelled")&&canceled.due==0&&canceled.actual==0&&canceled.notification.isEmpty()&&canceled.firstReturn==0);
  check(canceled.received(2,120000,130000,false)==canceled);
  AlarmSelfTestLifecycle missed=idle.start(1,60000);
  eq(missed.displayState(61000),"not_received");
  check(missed.actual==0&&missed.notification.isEmpty()); // reopen alone never succeeds or posts
  AlarmSelfTestLifecycle entryFirst=idle.start(1,60000).foregroundEntry(61000).received(1,60000,62000,false);
  check(entryFirst.firstReturn<entryFirst.actual);
  AlarmSelfTestLifecycle receiverFirst=idle.start(1,60000).received(1,60000,61000,false).foregroundEntry(62000);
  check(receiverFirst.actual<receiverFirst.firstReturn);
  check(receiverFirst.notification.isEmpty());
  System.out.println("AlarmSelfTestPolicyTest OK (production lifecycle transitions)");
 }
}
