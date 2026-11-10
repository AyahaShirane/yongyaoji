package cn.medreminder.app;

/** Small production-used state machine for one self-test generation. */
public final class AlarmSelfTestLifecycle {
 public final long generation,due,actual,firstReturn;
 public final String state,notification;
 public final int receiverForeground;
 private AlarmSelfTestLifecycle(long g,long d,String s,long a,String n,int rf,long r){generation=g;due=d;state=s;actual=a;notification=n;receiverForeground=rf;firstReturn=r;}
 public static AlarmSelfTestLifecycle restore(long g,long d,String s,long a,String n,int rf,long r){return new AlarmSelfTestLifecycle(g,d,s,a,n,rf,r);}
 public AlarmSelfTestLifecycle start(long g,long planned){return new AlarmSelfTestLifecycle(g,planned,"pending",0,"",-1,0);}
 public AlarmSelfTestLifecycle cancel(){return new AlarmSelfTestLifecycle(generation+1,0,"cancelled",0,"",-1,0);}
 public AlarmSelfTestLifecycle foregroundEntry(long at){if(firstReturn!=0||!("pending".equals(state)||"fired".equals(state)))return this;return new AlarmSelfTestLifecycle(generation,due,state,actual,notification,receiverForeground,at);}
 public boolean accepts(long callbackGeneration,long callbackDue,long now){return callbackGeneration==generation&&callbackDue==due&&"pending".equals(state)&&due>0&&now>=due;}
 public AlarmSelfTestLifecycle received(long callbackGeneration,long callbackDue,long now,boolean foreground){if(!accepts(callbackGeneration,callbackDue,now))return this;return new AlarmSelfTestLifecycle(generation,due,"fired",now,notification,foreground?1:0,firstReturn);}
 public AlarmSelfTestLifecycle notification(String outcome){return new AlarmSelfTestLifecycle(generation,due,state,actual,outcome,receiverForeground,firstReturn);}
 public String displayState(long now){return "pending".equals(state)&&due>0&&now>due?"not_received":state;}
}
