package cn.medreminder.app;
import java.util.*;
public final class AlarmRegressionTest {
 static void eq(Object a,Object b){if(!Objects.equals(a,b))throw new AssertionError(a+" != "+b);}
 public static void main(String[] args){
  // A boot replay must schedule desired slots even when preference markers survived reboot.
  Set<String> desired=new LinkedHashSet<>(Arrays.asList("7:2:1000","7:2:2000"));
  Set<String> marked=new HashSet<>(desired);
  eq(Alarms.slotsToSchedule(desired,marked),desired);
  // Editing a regimen replaces obsolete revision slots without dropping its new revision.
  Set<String> old=new LinkedHashSet<>(Arrays.asList("7:2:1000"));
  Set<String> fresh=new LinkedHashSet<>(Arrays.asList("7:3:3000"));
  Alarms.SlotPlan plan=Alarms.reconcileSlots(old,fresh);
  eq(plan.cancel,old); eq(plan.schedule,fresh);
  System.out.println("AlarmRegressionTest OK");
 }
}
