package cn.medreminder.app;
import java.math.BigDecimal;
import java.util.*;

public final class DoseStatsTest {
 static void eq(Object a,Object b){if(!Objects.equals(a,b))throw new AssertionError(a+" != "+b);}
 static void yes(boolean v){if(!v)throw new AssertionError();}
 static Store.Log log(long id,String name,String dose,long planned,long actual,boolean manual){Store.Log l=new Store.Log();l.medId=id;l.medName=name;l.dose=dose;l.plannedAt=planned;l.actualAt=actual;l.manual=manual;return l;}
 static DoseStats.Group group(DoseStats.Summary s,long id,String unit){for(DoseStats.Group g:s.groups)if(g.medId==id&&g.unit.equals(unit))return g;throw new AssertionError("missing group "+id+" "+unit);}
 public static void main(String[] args){
  List<Store.Log> rows=new ArrayList<>();rows.add(log(1,"Fixture","0.1 mg",10,200,true));rows.add(log(1,"Fixture","0.2mg",20,300,false));rows.add(log(1,"Fixture","1片",30,400,true));rows.add(log(1,"Fixture","2 片",40,500,true));rows.add(log(2,"Fixture","0.5mg",50,600,false));rows.add(log(2,"Fixture","1 mg",60,700,false));rows.add(log(3,"Other","1 mg",70,800,false));rows.add(log(3,"Other","1片",80,900,false));
  for(String text:new String[]{"半片","按医嘱","1片（10mg/片）","1 mg + 2 mg"})rows.add(log(1,"Fixture",text,1000,1000,true));
  rows.add(log(4,"Range","9 mg",5000,500,false));rows.add(log(4,"Range","1 mg",50,5000,false));
  DoseStats.Summary all=DoseStats.summarize(rows,0,Long.MAX_VALUE);eq(group(all,1,"mg").totalText(),"0.3");eq(group(all,1,"mg").records,2);eq(group(all,1,"片").totalText(),"3");eq(group(all,2,"mg").totalText(),"1.5");eq(all.groupsFor(3).size(),2);eq(group(all,2,"mg").medName,"Fixture");eq(all.uncountedFor(1),4);
  DoseStats.Summary range=DoseStats.summarize(rows,100,1000);eq(range.groupsFor(4).size(),1);eq(group(range,4,"mg").totalText(),"9");
  yes(DoseStats.parseAmount("0.5 mg")!=null);yes(DoseStats.parseAmount("1片")!=null);yes(DoseStats.parseAmount("2 粒")!=null);yes(DoseStats.parseAmount("5 mL")!=null);for(String bad:new String[]{"2","半片","按医嘱","1片（10mg/片）","1 mg + 2 mg"})yes(DoseStats.parseAmount(bad)==null);
  System.out.println("DoseStatsTest OK (exact decimal, stable med+unit groups, explicit uncounted and actual-time range)");
 }
}
