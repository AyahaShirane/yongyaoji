package cn.medreminder.app;
/** Production policy: user-visible alarm clock first, then exact idle, then inexact. */
public final class AlarmClockRegistration {
 public interface Registrar { void alarmClock(); void exactIdle(); void inexact(); }
 private AlarmClockRegistration() {}
 public static String register(boolean exactAllowed,Registrar r){
  if(exactAllowed)try{r.alarmClock();return "alarm_clock";}catch(RuntimeException denied){}
  if(exactAllowed)try{r.exactIdle();return "exact_idle";}catch(RuntimeException denied){}
  try{r.inexact();return "inexact";}catch(RuntimeException failed){return "failed";}
 }
}
