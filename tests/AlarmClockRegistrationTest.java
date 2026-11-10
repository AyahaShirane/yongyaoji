package cn.medreminder.app;
public final class AlarmClockRegistrationTest {
 static void eq(String a,String b){if(!a.equals(b))throw new AssertionError(a+" != "+b);}
 public static void main(String[] x){
  final StringBuilder calls=new StringBuilder();
  eq(AlarmClockRegistration.register(true,new AlarmClockRegistration.Registrar(){public void alarmClock(){calls.append('c');}public void exactIdle(){calls.append('e');}public void inexact(){calls.append('i');}}),"alarm_clock");eq(calls.toString(),"c");
  calls.setLength(0);eq(AlarmClockRegistration.register(true,new AlarmClockRegistration.Registrar(){public void alarmClock(){calls.append('c');throw new SecurityException();}public void exactIdle(){calls.append('e');}public void inexact(){calls.append('i');}}),"exact_idle");eq(calls.toString(),"ce");
  calls.setLength(0);eq(AlarmClockRegistration.register(true,new AlarmClockRegistration.Registrar(){public void alarmClock(){calls.append('c');throw new SecurityException();}public void exactIdle(){calls.append('e');throw new SecurityException();}public void inexact(){calls.append('i');}}),"inexact");eq(calls.toString(),"cei");
  calls.setLength(0);eq(AlarmClockRegistration.register(true,new AlarmClockRegistration.Registrar(){public void alarmClock(){calls.append('c');throw new SecurityException();}public void exactIdle(){calls.append('e');throw new SecurityException();}public void inexact(){calls.append('i');throw new SecurityException();}}),"failed");eq(calls.toString(),"cei");
  calls.setLength(0);eq(AlarmClockRegistration.register(false,new AlarmClockRegistration.Registrar(){public void alarmClock(){calls.append('c');}public void exactIdle(){calls.append('e');}public void inexact(){calls.append('i');}}),"inexact");eq(calls.toString(),"i");
  calls.setLength(0);eq(AlarmClockRegistration.register(false,new AlarmClockRegistration.Registrar(){public void alarmClock(){}public void exactIdle(){throw new RuntimeException();}public void inexact(){calls.append('i');throw new RuntimeException();}}),"failed");
  System.out.println("AlarmClockRegistrationTest OK");
 }
}
