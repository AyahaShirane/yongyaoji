package cn.medreminder.app;
public final class NotificationSeamTest {
 static void eq(Object a,Object b){if(!a.equals(b))throw new AssertionError(a+" != "+b);}
 public static void main(String[] args){
  final int[] calls={0};
  final boolean[] enabled={false};
  eq(NotificationSubmission.submit(enabled[0],()->calls[0]++),"blocked_notifications_disabled");eq(calls[0],0);
  enabled[0]=true;if(NotificationRetry.shouldRetry(true,enabled[0]))eq(NotificationSubmission.submit(enabled[0],()->calls[0]++),"submitted_to_os");eq(calls[0],1);
  eq(NotificationSubmission.submit(true,()->{throw new IllegalStateException();}),"notify_failed");
  if(NotificationRetry.shouldRetry(true,false)||NotificationRetry.shouldRetry(false,true)||!NotificationRetry.shouldRetry(true,true))throw new AssertionError("retry eligibility policy");
  if(NotificationAvailability.enabled(false,true,true)||NotificationAvailability.enabled(true,false,true)||NotificationAvailability.enabled(true,true,false)||!NotificationAvailability.enabled(true,true,true))throw new AssertionError("app, runtime, and channel notification gates");
  final int[] exact={0},fallback={0};
  AlarmRegistration.Registrar registrar=new AlarmRegistration.Registrar(){public void exact(){exact[0]++;throw new SecurityException();}public void fallback(){fallback[0]++;}};
  eq(AlarmRegistration.register(true,registrar),"fallback");eq(exact[0],1);eq(fallback[0],1);
  eq(AlarmRegistration.register(false,registrar),"fallback");
  eq(AlarmRegistration.register(true,new AlarmRegistration.Registrar(){public void exact(){throw new SecurityException();}public void fallback(){throw new IllegalStateException();}}),"failed");
  System.out.println("NotificationSeamTest OK (disabled-to-enabled retry, exact fallback, registration failure)");
 }
}
