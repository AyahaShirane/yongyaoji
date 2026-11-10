package cn.medreminder.app;

import java.time.*;
import java.util.*;

/** Durable presentation selection. It never writes a dose, skip, schedule, or alarm. */
public final class TodayFocus {
    public interface State { String get(); void put(String value); }
    private final State state;
    public TodayFocus(State state){this.state=state;}

    public TodayPlan apply(TodayPlan source,List<Store.Med> meds,List<Store.Log> logs,List<Store.Skip> skips,long now,String zone){
        List<TodayPlan.Row> rows=new ArrayList<>();
        addRows(rows,source.hero);addRows(rows,source.overdue);addRows(rows,source.upcoming);
        addRows(rows,source.completed);addRows(rows,source.skipped);addRows(rows,source.later);

        long epoch=-1;Set<Long> members=new TreeSet<>();String saved=state.get();
        if(saved!=null&&!saved.isEmpty())try{String[] p=saved.split("\\|",2);epoch=Long.parseLong(p[0]);for(String id:p[1].split(","))members.add(Long.parseLong(id));}catch(Exception ignored){epoch=-1;members.clear();}
        long priorEpoch=epoch;boolean explicitResolution=false,invalidated=false;
        if(epoch>0&&!members.isEmpty()){
            Set<Long> pending=new TreeSet<>();int resolved=0;
            for(long id:members){Store.Med med=findMed(meds,id);if(med==null||!scheduled(med,epoch,zone)){invalidated=true;continue;}
                Store.Log log=findLog(logs,id,epoch);boolean skipped=isSkipped(skips,id,epoch);
                if(log!=null||skipped){resolved++;addRow(rows,new TodayPlan.Row(med,epoch,log,skipped));}
                else{pending.add(id);addRow(rows,new TodayPlan.Row(med,epoch,null,false));}
            }
            members=pending;
            if(!members.isEmpty()){
                // Keep the unresolved remainder of an exact same-time group pinned.
            }else if(!invalidated&&resolved>0){explicitResolution=true;epoch=-1;}
            else epoch=-1;
        }else{epoch=-1;members.clear();}

        if(epoch<0&&explicitResolution){
            List<TodayPlan.Row> next=nextAfter(meds,logs,skips,priorEpoch,zone);
            long candidate=Long.MAX_VALUE;for(TodayPlan.Row r:next)candidate=Math.min(candidate,r.plannedAt);
            if(candidate!=Long.MAX_VALUE){epoch=candidate;for(TodayPlan.Row r:next)if(r.plannedAt==epoch){members.add(r.med.id);addRow(rows,r);}}
        }
        if(epoch<0){
            // Invalidation and first-open both reselect from the current-day window.
            epoch=bootstrap(rows,now,zone);members.clear();
            if(epoch>0)for(TodayPlan.Row r:rows)if(r.plannedAt==epoch&&r.log==null&&!r.skipped)members.add(r.med.id);
        }
        state.put(epoch>0&&!members.isEmpty()?encode(epoch,members):"");

        rows.sort(Comparator.comparingLong((TodayPlan.Row r)->r.plannedAt).thenComparingLong(r->r.med.id));
        TodayPlan out=new TodayPlan();out.total=source.total;out.invalid=source.invalid;
        LocalDate today=Instant.ofEpochMilli(now).atZone(ZoneId.of(zone)).toLocalDate();
        for(TodayPlan.Row r:rows){boolean isToday=Instant.ofEpochMilli(r.plannedAt).atZone(ZoneId.of(zone)).toLocalDate().equals(today);
            if(r.plannedAt==epoch&&members.contains(r.med.id)&&r.log==null&&!r.skipped)out.hero.add(r);
            else if(r.log!=null){if(isToday)out.completed.add(r);else out.later.add(r);}
            else if(r.skipped){if(isToday)out.skipped.add(r);else out.later.add(r);}
            else if(r.plannedAt<=now){if(isToday&&r.plannedAt>epoch)out.otherDue.add(r);else if(isToday)out.overdue.add(r);else out.later.add(r);}
            else if(isToday)out.upcoming.add(r);else out.later.add(r);
        }
        return out;
    }

    private static long bootstrap(List<TodayPlan.Row> rows,long now,String zone){
        LocalDate today=Instant.ofEpochMilli(now).atZone(ZoneId.of(zone)).toLocalDate();long latestDue=-1,nearest=Long.MAX_VALUE;
        for(TodayPlan.Row r:rows)if(r.log==null&&!r.skipped){boolean current=Instant.ofEpochMilli(r.plannedAt).atZone(ZoneId.of(zone)).toLocalDate().equals(today);if(current&&r.plannedAt<=now)latestDue=Math.max(latestDue,r.plannedAt);else if(r.plannedAt>now)nearest=Math.min(nearest,r.plannedAt);}
        return latestDue>0?latestDue:(nearest==Long.MAX_VALUE?-1:nearest);
    }
    /** One arithmetic successor per active medication; handled consecutive slots are skipped by exact-key lookup. */
    private static List<TodayPlan.Row> nextAfter(List<Store.Med> meds,List<Store.Log> logs,List<Store.Skip> skips,long after,String zone){
        List<TodayPlan.Row> out=new ArrayList<>();
        for(Store.Med med:meds)if(med.active)try{
            Set<Long> handled=new HashSet<>();for(Store.Log l:logs)if(!l.manual&&l.medId==med.id&&l.plannedAt>after)handled.add(l.plannedAt);for(Store.Skip s:skips)if(s.medId==med.id&&s.plannedAt>after)handled.add(s.plannedAt);
            long at=next(med,after,zone);while(handled.contains(at))at=next(med,at,zone);out.add(new TodayPlan.Row(med,at,null,false));
        }catch(RuntimeException ignored){}
        return out;
    }
    private static long next(Store.Med med,long after,String zone){if(Store.FREQUENCY.equals(med.scheduleMode))return Core.nextFrequency(med.anchor,med.frequencyDays,med.frequencyCount,after,zone);long best=Long.MAX_VALUE;for(String t:Core.normalizeTimes(med.times).split(","))best=Math.min(best,Core.nextTime(t,after,zone));return best;}
    private static boolean scheduled(Store.Med med,long at,String zone){try{if(Store.FREQUENCY.equals(med.scheduleMode))return Core.isFrequencyOccurrence(med.anchor,med.frequencyDays,med.frequencyCount,at);LocalDate day=Instant.ofEpochMilli(at).atZone(ZoneId.of(zone)).toLocalDate();return Core.fixedOnDate(med.times,day,zone).contains(at);}catch(RuntimeException e){return false;}}
    private static Store.Med findMed(List<Store.Med> meds,long id){for(Store.Med med:meds)if(med.id==id&&med.active)return med;return null;}
    private static Store.Log findLog(List<Store.Log> logs,long id,long at){for(Store.Log log:logs)if(!log.manual&&log.medId==id&&log.plannedAt==at)return log;return null;}
    private static boolean isSkipped(List<Store.Skip> skips,long id,long at){for(Store.Skip skip:skips)if(skip.medId==id&&skip.plannedAt==at)return true;return false;}
    private static void addRows(List<TodayPlan.Row> out,List<TodayPlan.Row> add){for(TodayPlan.Row row:add)addRow(out,row);}
    private static void addRow(List<TodayPlan.Row> out,TodayPlan.Row row){for(TodayPlan.Row prior:out)if(prior.med.id==row.med.id&&prior.plannedAt==row.plannedAt)return;out.add(row);}
    private static String encode(long epoch,Set<Long> ids){StringBuilder b=new StringBuilder().append(epoch).append('|');for(long id:ids){if(b.charAt(b.length()-1)!='|')b.append(',');b.append(id);}return b.toString();}
}
