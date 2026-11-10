package cn.medreminder.app;
import java.time.*;
import java.util.*;
public class TodayThemeTest {
 static int checks;
 static void ok(boolean v,String s){checks++;if(!v)throw new AssertionError(s);}
 static long at(String s,String z){return LocalDateTime.parse(s).atZone(ZoneId.of(z)).toInstant().toEpochMilli();}
 static Store.Med med(long id,String times){Store.Med m=new Store.Med();m.id=id;m.revision=1;m.name="测试药"+id;m.dose="1片";m.active=true;m.scheduleMode=Store.FIXED;m.times=times;return m;}
 static Store.Log log(Store.Med m,long p,long a,boolean manual){Store.Log l=new Store.Log();l.medId=m.id;l.plannedAt=p;l.actualAt=a;l.manual=manual;l.occurrence=Core.occurrence(m.id,1,p);return l;}
 public static void main(String[]args){
 String z="Asia/Shanghai";long now=at("2026-11-04T10:00",z);Store.Med a=med(2,"08:00,12:00"),b=med(1,"09:00,12:00,18:00");List<Store.Med> meds=Arrays.asList(a,b);List<Store.Log> logs=new ArrayList<>();
 TodayPlan p=TodayPlan.build(meds,logs,now,z);
 ok(p.total==5&&p.pending()==5,"overview");ok(p.hero.size()==2&&p.hero.get(0).med.id==1&&p.hero.get(1).med.id==2,"all simultaneous stable IDs");ok(p.overdue.size()==2&&p.overdue.get(0).med.id==2,"global overdue order");ok(p.upcoming.size()==1&&p.upcoming.get(0).plannedAt==at("2026-11-04T18:00",z),"hero not duplicated");
 logs.add(log(a,at("2026-11-04T08:00",z),now,true));p=TodayPlan.build(meds,logs,now,z);ok(p.completed.isEmpty(),"independent excluded");logs.add(log(a,at("2026-11-04T08:00",z),at("2026-11-03T23:00",z),false));p=TodayPlan.build(meds,logs,now,z);ok(p.completed.size()==1&&p.pending()==4,"linked early selected date");
 logs.add(log(b,at("2026-11-05T09:00",z),now,false));p=TodayPlan.build(meds,logs,at("2026-11-05T00:00",z),z);ok(p.total==5&&p.completed.size()==1,"midnight recompute not actual date");
 a.active=false;b.active=false;p=TodayPlan.build(meds,logs,now,z);ok(p.total==0&&p.hero.isEmpty(),"paused empty");
 Store.Med f=med(3,"");f.scheduleMode=Store.FREQUENCY;f.anchor=at("2026-11-06T08:00",z)+123;f.frequencyDays=1;f.frequencyCount=7;
 p=TodayPlan.build(Arrays.asList(f),logs,now,z);ok(p.total==0&&p.hero.get(0).plannedAt==f.anchor,"future anchor exact milliseconds");
 f.anchor=at("2026-11-04T00:00",z)+123;p=TodayPlan.build(Arrays.asList(f),logs,now,z);ok(p.total==7&&p.hero.get(0).plannedAt==Core.frequencyAt(f.anchor,1,7,3),"fractional epoch preserved");
 for(String date:new String[]{"2026-03-08T12:00","2026-11-01T12:00"}){String dst="America/New_York";long t=at(date,dst);p=TodayPlan.build(Arrays.asList(f),logs,t,dst);ok(p.total==Core.frequencyToday(f.anchor,1,7,t,dst).size(),"DST local day");}
 Store.Med single=med(5,"08:00");long todayAt=at("2026-11-04T08:00",z),tomorrowAt=at("2026-11-05T08:00",z);
 List<Store.Log> confirmed=new ArrayList<>();confirmed.add(log(single,todayAt,now,false));confirmed.add(log(single,tomorrowAt,now,false));
 p=TodayPlan.build(Arrays.asList(single),confirmed,now,z);ok(p.total==1&&p.pending()==0&&p.completed.size()==1,"all today complete");ok(p.hero.get(0).plannedAt==at("2026-11-06T08:00",z),"skip confirmed tomorrow");ok(p.later.size()==1&&p.later.get(0).log!=null,"confirmed future kept once");
 Store.Skip skippedSlot=new Store.Skip();skippedSlot.medId=single.id;skippedSlot.revision=single.revision;skippedSlot.plannedAt=todayAt;skippedSlot.occurrence=Core.occurrence(single.id,single.revision,todayAt);skippedSlot.medName=single.name;
 p=TodayPlan.build(Arrays.asList(single),Collections.<Store.Log>emptyList(),Arrays.asList(skippedSlot),at("2026-11-04T10:00",z),z);ok(p.total==1&&p.skipped.size()==1&&p.pending()==0&&p.completed.isEmpty(),"skip is separate from actual records and totals");ok(p.hero.size()==1&&p.hero.get(0).plannedAt==tomorrowAt,"skip does not occupy hero or shift tomorrow");
 p=TodayPlan.build(Collections.emptyList(),confirmed,now,z);ok(p.hero.isEmpty()&&p.total==0,"no plans");
 p=TodayPlan.build(meds,logs,now,z);ok(p.hero.isEmpty(),"paused no hero");a.active=true;b.active=true;
 p=TodayPlan.build(meds,Collections.emptyList(),at("2026-11-04T12:00",z),z);ok(p.hero.size()==2&&p.overdue.size()==2,"exact now in hero");p=TodayPlan.build(meds,Collections.emptyList(),at("2026-11-04T12:01",z),z);ok(p.hero.size()==1&&p.overdue.size()==4,"minute crossing recomputes partition");
 ok(logs.size()==3&&a.revision==1,"presentation does not mutate source");
 Store.Med dense=med(4,"");dense.scheduleMode=Store.FREQUENCY;dense.anchor=at("2026-11-01T00:00",z);dense.frequencyDays=1;dense.frequencyCount=1000;p=TodayPlan.build(Arrays.asList(dense),logs,now,z);ok(p.total==1000,"no cap");
 for(boolean system:new boolean[]{false,true}){ok(Appearance.dark("system",system)==system,"system");ok(!Appearance.dark("light",system),"light override");ok(Appearance.dark("dark",system),"dark override");ok(Appearance.dark("unknown",system)==system,"fallback");Appearance.Palette c=Appearance.palette(system);for(int[]pair:new int[][]{{c.ink,c.bg},{c.muted,c.surface},{c.accent,c.surface},{c.onAccent,c.accent}})ok(Appearance.contrast(pair[0],pair[1])>=4.5,"contrast");}
 p=TodayPlan.build(Arrays.asList(single),Collections.emptyList(),todayAt-123,z);ok(p.nextRefreshDelay(todayAt-123)==123,"exact occurrence boundary refresh");ok(p.nextRefreshDelay(todayAt)==1,"crossing now refresh");
 ok(TodayPlan.refreshDelay(now)>0&&TodayPlan.refreshDelay(now)<=60000,"bounded minute refresh");
 System.out.println("TodayThemeTest OK ("+checks+" assertions)");
 }
}
