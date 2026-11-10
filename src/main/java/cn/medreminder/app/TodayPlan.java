package cn.medreminder.app;
import java.time.*;
import java.util.*;
/** Read-only view of exact Core epochs and explicit Store logs. Never changes schedules. */
public final class TodayPlan {
 public static final class Row {
  public final Store.Med med; public final long plannedAt; public final String key; public final Store.Log log; public final boolean skipped;
  Row(Store.Med m,long at,Store.Log l,boolean skipped){med=m;plannedAt=at;key=Core.occurrence(m.id,m.revision,at);log=l;this.skipped=skipped;}
 }
 public final List<Row> hero=new ArrayList<>(),overdue=new ArrayList<>(),upcoming=new ArrayList<>(),completed=new ArrayList<>(),skipped=new ArrayList<>(),later=new ArrayList<>(),otherDue=new ArrayList<>();
 public int total,invalid;
 public int pending(){return total-completed.size()-skipped.size();}
 private static List<Long> onDate(Store.Med m,LocalDate d,String z){return Store.FREQUENCY.equals(m.scheduleMode)?Core.frequencyOnDate(m.anchor,m.frequencyDays,m.frequencyCount,d,z):Core.fixedOnDate(m.times,d,z);}
 private static Store.Log find(List<Store.Log> logs,Store.Med m,long at){for(Store.Log l:logs)if(!l.manual&&l.medId==m.id&&l.plannedAt==at)return l;return null;}
 private static long next(Store.Med m,long now,String z){if(Store.FREQUENCY.equals(m.scheduleMode))return Core.nextFrequency(m.anchor,m.frequencyDays,m.frequencyCount,now,z);long best=Long.MAX_VALUE;for(String t:Core.normalizeTimes(m.times).split(","))best=Math.min(best,Core.nextTime(t,now,z));return best;}
 public static TodayPlan build(List<Store.Med> meds,List<Store.Log> logs,long now,String zone){
  return build(meds,logs,Collections.<Store.Skip>emptyList(),now,zone);
 }
 public static TodayPlan build(List<Store.Med> meds,List<Store.Log> logs,List<Store.Skip> skips,long now,String zone){
  TodayPlan p=new TodayPlan();LocalDate day=Instant.ofEpochMilli(now).atZone(ZoneId.of(zone)).toLocalDate();List<Row> all=new ArrayList<>();
  for(Store.Med m:meds){if(!m.active)continue;try{
   TreeSet<Long> times=new TreeSet<>(onDate(m,day,zone));times.addAll(onDate(m,day.plusDays(1),zone));
   long at=next(m,now==Long.MIN_VALUE?now:now-1,zone);while(find(logs,m,at)!=null||isSkipped(skips,m,at))at=next(m,at,zone);times.add(at);
   for(long t:times)all.add(new Row(m,t,find(logs,m,t),isSkipped(skips,m,t)));
  }catch(RuntimeException e){p.invalid++;}}
  all.sort(Comparator.comparingLong((Row r)->r.plannedAt).thenComparingLong(r->r.med.id).thenComparingLong(r->r.med.revision));
  long focus=Long.MAX_VALUE;for(Row r:all)if(r.log==null&&!r.skipped&&r.plannedAt>=now){focus=r.plannedAt;break;}
  for(Row r:all){boolean today=Instant.ofEpochMilli(r.plannedAt).atZone(ZoneId.of(zone)).toLocalDate().equals(day);if(today)p.total++;
   if(r.log==null&&!r.skipped&&r.plannedAt==focus)p.hero.add(r);
   else if(today){if(r.log!=null)p.completed.add(r);else if(r.skipped)p.skipped.add(r);else if(r.plannedAt<now)p.overdue.add(r);else p.upcoming.add(r);}
   else p.later.add(r);
  }return p;
 }
 private static boolean isSkipped(List<Store.Skip> skips,Store.Med m,long at){for(Store.Skip s:skips)if(s.medId==m.id&&s.plannedAt==at)return true;return false;}
 public long nextRefreshDelay(long now){long delay=refreshDelay(now);if(!hero.isEmpty()){long at=hero.get(0).plannedAt;if(at>=now&&at<Long.MAX_VALUE)delay=Math.min(delay,at-now+1);}return Math.max(1,delay);}
 public static long refreshDelay(long now){return 60000-Math.floorMod(now,60000);}
}
