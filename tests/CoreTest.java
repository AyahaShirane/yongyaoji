package cn.medreminder.app;
import java.time.*;import java.util.*;
public class CoreTest {
 static void eq(Object a,Object b){if(!Objects.equals(a,b))throw new AssertionError(a+" != "+b);} static void yes(boolean v){if(!v)throw new AssertionError();}
 static void invalid(String s){try{Core.normalizeTimes(s);throw new AssertionError("accepted "+s);}catch(IllegalArgumentException expected){}}
 public static void main(String[]x){
  eq(Core.normalizeTimes("20:00， 08:00,08:00"),"08:00,20:00");
  for(String bad:new String[]{"","8:00","24:00","12:60","12:30,","12:30,,13:00","12:30pm"})invalid(bad);
  yes(Core.validDose("0.5 mg"));yes(Core.validDose("2 tablets"));yes(!Core.validDose("0 mg"));yes(!Core.validDose("-2 tablets"));yes(!Core.validDose("-0.1"));yes(Core.validDose(".01 ml"));
  eq(Core.occurrence(4,3,123L),"4:3:123");
  Core.ConfirmationDefaults defaults=Core.confirmationDefaults(1793865634123L,"1 mg");eq(defaults.actualAt,1793865634123L);eq(defaults.dose,"1 mg");eq(Core.confirmationDefaults(1793865634124L,"0.5 mg").dose,"0.5 mg");
  String zone="Asia/Shanghai";eq(Core.nextTime("08:00",1793750340000L,zone),1793750400000L);eq(Core.nextTime("08:00",1793793660000L,zone),1793836800000L);
  long now=Instant.parse("2026-11-03T23:59:00Z").toEpochMilli();eq(Core.nextTime("08:00",now,zone),Instant.parse("2026-11-04T00:00:00Z").toEpochMilli());eq(Core.todayTimes("08:00,20:00",now,zone).size(),2);
  ZoneId ny=ZoneId.of("America/New_York");long beforeGap=ZonedDateTime.of(2026,3,8,1,30,0,0,ny).toInstant().toEpochMilli();eq(Core.nextTime("02:30",beforeGap,"America/New_York"),ZonedDateTime.of(2026,3,8,3,30,0,0,ny).toInstant().toEpochMilli());
  long beforeOverlap=ZonedDateTime.of(2026,11,1,0,30,0,0,ny).toInstant().toEpochMilli();eq(Core.nextTime("01:30",beforeOverlap,"America/New_York"),ZonedDateTime.of(2026,11,1,1,30,0,0,ny).toInstant().toEpochMilli());
  long anchor=1793750400000L;
  eq(Core.initialFrequencyAnchor(1793758725678L,"Asia/Shanghai",0),1793758680000L);
  eq(Core.initialFrequencyAnchor(1793758725678L,"Asia/Shanghai",1793758725678L),1793758725678L);
  List<Long> fractionalCandidates=Core.frequencyOnDate(1793635200000L,1,7,LocalDate.of(2026,11,3),"Asia/Shanghai");
  eq(fractionalCandidates,Arrays.asList(1793635200000L,1793647542857L,1793659885714L,1793672228571L,1793684571428L,1793696914285L,1793709257142L));
  eq(Core.fixedOnDate("08:00,20:00",LocalDate.of(2026,11,3),"Asia/Shanghai").size(),2);
  // Immutable v1.2 early-confirmation fixture: select the second occurrence,
  // record earlier than planned, and leave the following occurrence anchored.
  long fixtureSelected=1793808000000L,fixtureActual=1793807100000L,fixtureNext=1793836800000L;
  eq(Core.frequencyAt(1793750400000L,1,3,2),fixtureSelected);yes(fixtureActual<fixtureSelected);
  eq(Core.frequencyAt(1793750400000L,1,3,3),fixtureNext);
  long[] oneDayThree={1793750400000L,1793779200000L,1793808000000L,1793836800000L,1793865600000L,1793894400000L,1793923200000L};for(int i=0;i<oneDayThree.length;i++)eq(Core.frequencyAt(anchor,1,3,i),oneDayThree[i]);
  long[] twoDayThree={1793750400000L,1793808000000L,1793865600000L,1793923200000L,1793980800000L,1794038400000L,1794096000000L};for(int i=0;i<twoDayThree.length;i++)eq(Core.frequencyAt(anchor,2,3,i),twoDayThree[i]);
  eq(Core.frequencyAt(anchor,7,1,0),1793750400000L);eq(Core.frequencyAt(anchor,7,1,1),1794355200000L);eq(Core.frequencyAt(anchor,7,1,2),1794960000000L);
  eq(Core.frequencyAt(anchor,1,3,1),anchor+28800000L);
  eq(Core.nextFrequency(anchor,1,3,anchor,"UTC"),anchor+28800000L);
  eq(Core.nextFrequency(anchor,1,3,anchor-1,"UTC"),anchor);
  eq(Core.frequencyAt(anchor,2,3,2),anchor+115200000L);
  eq(Core.frequencyAt(anchor,7,1,2),anchor+1209600000L);
  eq(Core.frequencyAt(anchor,1,7,21),anchor+259200000L);
  long[] oneDay={anchor,anchor+28800000L,anchor+57600000L,anchor+86400000L,anchor+115200000L,anchor+144000000L,anchor+172800000L};for(int i=0;i<oneDay.length;i++)eq(Core.frequencyAt(anchor,1,3,i),oneDay[i]);
  eq(Core.frequencyAt(anchor,2,3,6),anchor+345600000L);
  eq(Core.frequencyAt(anchor,7,1,2),1794960000000L);
  yes(Core.frequencyToday(anchor,1,3,anchor+3600000L,"UTC").size()==3);
  yes(Core.frequencyToday(anchor,1,3,anchor-3600000L,"UTC").isEmpty());
  long dst=ZonedDateTime.of(2026,3,7,12,0,0,0,ny).toInstant().toEpochMilli();eq(Core.frequencyAt(dst,1,1,1)-Core.frequencyAt(dst,1,1,0),86400000L);
  long fallAnchor=ZonedDateTime.of(2026,10,31,0,0,0,0,ny).toInstant().toEpochMilli();long fallNow=ZonedDateTime.of(2026,11,1,12,0,0,0,ny).toInstant().toEpochMilli();LocalDate fallDate=Instant.ofEpochMilli(fallNow).atZone(ny).toLocalDate();long fallFrom=fallDate.atStartOfDay(ny).toInstant().toEpochMilli(),fallTo=fallDate.plusDays(1).atStartOfDay(ny).toInstant().toEpochMilli();List<Long> fallOccurrences=Core.frequencyToday(fallAnchor,1,1000,fallNow,"America/New_York");eq(fallTo-fallFrom,90000000L);eq(fallOccurrences.size(),1042);for(int i=0;i<fallOccurrences.size();i++){long k=1000L+i;eq(fallOccurrences.get(i),fallAnchor+(k*86400000L)/1000L);yes(fallOccurrences.get(i)>=fallFrom&&fallOccurrences.get(i)<fallTo);}
  eq(Core.frequencyPreview(anchor,1,3,anchor,"UTC",5).size(),5);
  long ancient=1L;long ancientNow=System.currentTimeMillis();yes(Core.nextFrequency(ancient,1,3,ancientNow,"UTC")>ancientNow);
  try{Core.frequencyAt(anchor,0,3,0);throw new AssertionError("invalid frequency accepted");}catch(IllegalArgumentException expected){}
  try{Core.frequencyAt(anchor,1,3,Long.MAX_VALUE);throw new AssertionError("overflow accepted");}catch(IllegalArgumentException expected){}
  System.out.println("CoreTest OK (immutable parent fixture expectations, validation, midnight and DST)");
 }
}
