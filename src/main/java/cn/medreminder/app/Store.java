package cn.medreminder.app;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import java.util.*;
import java.util.UUID;
import java.time.*;

public final class Store extends SQLiteOpenHelper {
    private static final String DB="med_reminder.db";
    public static final String FIXED="FIXED", FREQUENCY="FREQUENCY";
    public static final class Med { public long id,revision,anchor; public int frequencyDays,frequencyCount; public String name,dose,times,notes,scheduleMode; public boolean active; }
    public static final class Log { public long id,medId,plannedAt,actualAt; public String medName,dose,notes,occurrence; public boolean manual; }
    public static final class Skip { public long id,medId,revision,plannedAt; public String medName,occurrence; }
    public Store(Context c){super(c,DB,null,4);}
    @Override public void onCreate(SQLiteDatabase d){
        d.execSQL("CREATE TABLE meds(_id INTEGER PRIMARY KEY AUTOINCREMENT,revision INTEGER NOT NULL,name TEXT NOT NULL,dose TEXT NOT NULL,times TEXT NOT NULL,notes TEXT NOT NULL,active INTEGER NOT NULL)");
        d.execSQL("CREATE TABLE logs(_id INTEGER PRIMARY KEY AUTOINCREMENT,med_id INTEGER NOT NULL,planned_at INTEGER NOT NULL,actual_at INTEGER NOT NULL,med_name TEXT NOT NULL,dose TEXT NOT NULL,notes TEXT NOT NULL,occurrence TEXT NOT NULL UNIQUE,manual INTEGER NOT NULL)");
        d.execSQL("CREATE INDEX logs_actual ON logs(actual_at DESC)");
        d.execSQL("CREATE UNIQUE INDEX logs_planned_occurrence ON logs(med_id,planned_at) WHERE manual=0");
        d.execSQL("ALTER TABLE meds ADD COLUMN schedule_mode TEXT NOT NULL DEFAULT 'FIXED'");
        d.execSQL("ALTER TABLE meds ADD COLUMN anchor_at INTEGER NOT NULL DEFAULT 0");
        d.execSQL("ALTER TABLE meds ADD COLUMN frequency_days INTEGER NOT NULL DEFAULT 0");
        d.execSQL("ALTER TABLE meds ADD COLUMN frequency_count INTEGER NOT NULL DEFAULT 0");
        d.execSQL("CREATE TABLE skipped(_id INTEGER PRIMARY KEY AUTOINCREMENT,med_id INTEGER NOT NULL,revision INTEGER NOT NULL,planned_at INTEGER NOT NULL,occurrence TEXT NOT NULL UNIQUE,med_name TEXT NOT NULL)");
        d.execSQL("CREATE UNIQUE INDEX skipped_med_planned ON skipped(med_id,planned_at)");
    }
    @Override public void onUpgrade(SQLiteDatabase d,int oldV,int newV){
        if(oldV<2){d.execSQL("UPDATE logs SET manual=1,planned_at=0,occurrence='manual:migrated:'||_id WHERE manual=0 AND _id NOT IN (SELECT MIN(_id) FROM logs WHERE manual=0 GROUP BY med_id,planned_at)");d.execSQL("CREATE UNIQUE INDEX logs_planned_occurrence ON logs(med_id,planned_at) WHERE manual=0");}
        if(oldV<3){d.execSQL("ALTER TABLE meds ADD COLUMN schedule_mode TEXT NOT NULL DEFAULT 'FIXED'");d.execSQL("ALTER TABLE meds ADD COLUMN anchor_at INTEGER NOT NULL DEFAULT 0");d.execSQL("ALTER TABLE meds ADD COLUMN frequency_days INTEGER NOT NULL DEFAULT 0");d.execSQL("ALTER TABLE meds ADD COLUMN frequency_count INTEGER NOT NULL DEFAULT 0");}
        if(oldV<4){d.execSQL("CREATE TABLE skipped(_id INTEGER PRIMARY KEY AUTOINCREMENT,med_id INTEGER NOT NULL,revision INTEGER NOT NULL,planned_at INTEGER NOT NULL,occurrence TEXT NOT NULL UNIQUE,med_name TEXT NOT NULL)");d.execSQL("CREATE UNIQUE INDEX skipped_med_planned ON skipped(med_id,planned_at)");}
        if(newV>4)throw new IllegalStateException("No migration available");
    }
    private static Med medRow(Cursor c){Med m=new Med();m.id=c.getLong(0);m.revision=c.getLong(1);m.name=c.getString(2);m.dose=c.getString(3);m.times=c.getString(4);m.notes=c.getString(5);m.active=c.getInt(6)!=0;m.scheduleMode=c.getString(7);m.anchor=c.getLong(8);m.frequencyDays=c.getInt(9);m.frequencyCount=c.getInt(10);return m;}
    private static Log logRow(Cursor c){Log l=new Log();l.id=c.getLong(0);l.medId=c.getLong(1);l.plannedAt=c.getLong(2);l.actualAt=c.getLong(3);l.medName=c.getString(4);l.dose=c.getString(5);l.notes=c.getString(6);l.occurrence=c.getString(7);l.manual=c.getInt(8)!=0;return l;}
    public List<Med> meds(boolean activeOnly){List<Med> out=new ArrayList<>();Cursor c=getReadableDatabase().query("meds",null,activeOnly?"active=1":null,null,null,null,"name COLLATE NOCASE,_id");try{while(c.moveToNext())out.add(medRow(c));}finally{c.close();}return out;}
    public Med med(long id){Cursor c=getReadableDatabase().query("meds",null,"_id=?",new String[]{String.valueOf(id)},null,null,null);try{return c.moveToFirst()?medRow(c):null;}finally{c.close();}}
    /** Backward-compatible save: legacy fixed plans accept times; existing frequency plans retain their regimen. */
    public long saveMed(long id,String name,String dose,String times,String notes,boolean active){if(id>0){Med old=med(id);if(old!=null&&FREQUENCY.equals(old.scheduleMode))return saveSchedule(id,name,dose,old.times,notes,active,FREQUENCY,old.anchor,old.frequencyDays,old.frequencyCount);}return saveFixedMed(id,name,dose,times,notes,active);}
    public long saveMetadata(long id,String name,String dose,String notes,boolean active){Med m=med(id);if(m==null)throw new IllegalArgumentException("Medication not found");return saveSchedule(id,name,dose,m.times,notes,active,m.scheduleMode,m.anchor,m.frequencyDays,m.frequencyCount);}
    public long saveFixedMed(long id,String name,String dose,String times,String notes,boolean active){String normalized=Core.normalizeTimes(times);return saveSchedule(id,name,dose,normalized,notes,active,FIXED,0,0,0);}
    public long saveFrequencyMed(long id,String name,String dose,long anchor,int days,int count,String notes,boolean active){Core.frequencyAt(anchor,days,count,0);return saveSchedule(id,name,dose,"",notes,active,FREQUENCY,anchor,days,count);}
    private long saveSchedule(long id,String name,String dose,String times,String notes,boolean active,String mode,long anchor,int days,int count){
        if(name==null||name.trim().isEmpty()||!Core.validDose(dose))throw new IllegalArgumentException("name and positive dose required");
        SQLiteDatabase d=getWritableDatabase();d.beginTransaction();try{
            ContentValues v=new ContentValues();v.put("name",name.trim());v.put("dose",dose.trim());v.put("times",times);v.put("notes",notes==null?"":notes.trim());v.put("active",active?1:0);v.put("schedule_mode",mode);v.put("anchor_at",anchor);v.put("frequency_days",days);v.put("frequency_count",count);
            long result;if(id==0){v.put("revision",1);result=d.insertOrThrow("meds",null,v);}else{Med old=medIn(d,id);if(old==null)throw new IllegalArgumentException("Medication not found");v.put("revision",old.revision+1);d.update("meds",v,"_id=?",new String[]{String.valueOf(id)});result=id;}d.setTransactionSuccessful();return result;
        }finally{d.endTransaction();}}
    private Med medIn(SQLiteDatabase d,long id){Cursor c=d.query("meds",null,"_id=?",new String[]{String.valueOf(id)},null,null,null);try{return c.moveToFirst()?medRow(c):null;}finally{c.close();}}
    public void deleteMed(long id){SQLiteDatabase d=getWritableDatabase();ContentValues v=new ContentValues();v.put("active",0);d.beginTransaction();try{d.delete("meds","_id=?",new String[]{String.valueOf(id)});d.setTransactionSuccessful();}finally{d.endTransaction();}}
    public List<Log> logs(){List<Log> out=new ArrayList<>();Cursor c=getReadableDatabase().query("logs",null,null,null,null,null,"actual_at DESC,_id DESC");try{while(c.moveToNext())out.add(logRow(c));}finally{c.close();}return out;}
    public Log log(long id){return queryLog("_id=?",new String[]{String.valueOf(id)});}
    public Log forOccurrence(String key){
        Log exact=queryLog("occurrence=?",new String[]{key});if(exact!=null)return exact;
        String[] parts=key==null?new String[0]:key.split(":",-1);if(parts.length!=3)return null;
        try{return queryLog("med_id=? AND planned_at=? AND manual=0",new String[]{parts[0],parts[2]});}catch(NumberFormatException e){return null;}
    }
    private static Skip skipRow(Cursor c){Skip s=new Skip();s.id=c.getLong(0);s.medId=c.getLong(1);s.revision=c.getLong(2);s.plannedAt=c.getLong(3);s.occurrence=c.getString(4);s.medName=c.getString(5);return s;}
    public List<Skip> skips(){List<Skip> out=new ArrayList<>();Cursor c=getReadableDatabase().query("skipped",null,null,null,null,null,"planned_at DESC,_id DESC");try{while(c.moveToNext())out.add(skipRow(c));}finally{c.close();}return out;}
    private static long[] slotFromKey(String key){if(key==null)return null;String[] parts=key.split(":",-1);if(parts.length!=3)return null;try{long medId=Long.parseLong(parts[0]);Long.parseLong(parts[1]);long plannedAt=Long.parseLong(parts[2]);return medId>0&&plannedAt>0?new long[]{medId,plannedAt}:null;}catch(NumberFormatException invalid){return null;}}
    public Skip skip(String key){long[] slot=slotFromKey(key);return slot==null?null:skip(slot[0],slot[1]);}
    public Skip skip(long medId,long plannedAt){Cursor c=getReadableDatabase().query("skipped",null,"med_id=? AND planned_at=?",new String[]{String.valueOf(medId),String.valueOf(plannedAt)},null,null,null);try{return c.moveToFirst()?skipRow(c):null;}finally{c.close();}}
    public boolean isSkipped(String key){return skip(key)!=null;}
    public boolean isSkipped(long medId,long plannedAt){return skip(medId,plannedAt)!=null;}
    /** Explicitly excludes one exact scheduled occurrence, without creating an administration log. */
    public long skipOccurrence(long medId,long revision,String occurrence,long plannedAt,String zoneId){
        if(plannedAt<=0||!Core.occurrence(medId,revision,plannedAt).equals(occurrence))return -1;
        SQLiteDatabase d=getWritableDatabase();d.beginTransaction();try{Med m=medIn(d,medId);if(m==null||!m.active||m.revision!=revision||!matchesSchedule(m,plannedAt,zoneId))return -1;
            Cursor existing=d.query("logs",new String[]{"_id"},"med_id=? AND planned_at=? AND manual=0",new String[]{String.valueOf(medId),String.valueOf(plannedAt)},null,null,null);try{if(existing.moveToFirst())return -1;}finally{existing.close();}
            Skip prior=skipInSlot(d,medId,plannedAt);if(prior!=null){d.setTransactionSuccessful();return prior.id;}
            ContentValues v=new ContentValues();v.put("med_id",medId);v.put("revision",revision);v.put("planned_at",plannedAt);v.put("occurrence",occurrence);v.put("med_name",m.name);d.insertWithOnConflict("skipped",null,v,SQLiteDatabase.CONFLICT_IGNORE);
            Cursor c=d.query("skipped",new String[]{"_id"},"med_id=? AND planned_at=?",new String[]{String.valueOf(medId),String.valueOf(plannedAt)},null,null,null);try{if(!c.moveToFirst())return -1;long id=c.getLong(0);d.setTransactionSuccessful();return id;}finally{c.close();}
        }finally{d.endTransaction();}}
    public boolean undoSkip(long medId,long revision,String occurrence){return undoSkip(medId,revision,occurrence,TimeZone.getDefault().getID());}
    public boolean undoSkip(long medId,long revision,String occurrence,String zoneId){long[] slot=slotFromKey(occurrence);if(slot==null||slot[0]!=medId||!Core.occurrence(medId,revision,slot[1]).equals(occurrence))return false;SQLiteDatabase d=getWritableDatabase();d.beginTransaction();try{Med m=medIn(d,medId);Skip s=skipInSlot(d,medId,slot[1]);if(m==null||!m.active||m.revision!=revision||s==null||!matchesSchedule(m,slot[1],zoneId))return false;d.delete("skipped","_id=?",new String[]{String.valueOf(s.id)});d.setTransactionSuccessful();return true;}finally{d.endTransaction();}}
    private Skip skipInSlot(SQLiteDatabase d,long medId,long plannedAt){Cursor c=d.query("skipped",null,"med_id=? AND planned_at=?",new String[]{String.valueOf(medId),String.valueOf(plannedAt)},null,null,null);try{return c.moveToFirst()?skipRow(c):null;}finally{c.close();}}
    public boolean isScheduled(long medId,long revision,long plannedAt,String zoneId){Med m=med(medId);return m!=null&&m.revision==revision&&matchesSchedule(m,plannedAt,zoneId);}
    private static boolean matchesSchedule(Med m,long plannedAt,String zoneId){if(plannedAt<=0||zoneId==null)return false;try{if(FREQUENCY.equals(m.scheduleMode))return Core.isFrequencyOccurrence(m.anchor,m.frequencyDays,m.frequencyCount,plannedAt);ZoneId zone=ZoneId.of(zoneId);LocalDate date=Instant.ofEpochMilli(plannedAt).atZone(zone).toLocalDate();for(String t:Core.normalizeTimes(m.times).split(",")){LocalTime time=LocalTime.parse(t);if(date.atTime(time).atZone(zone).toInstant().toEpochMilli()==plannedAt)return true;}return false;}catch(RuntimeException invalid){return false;}}
    private Log queryLog(String where,String[] args){Cursor c=getReadableDatabase().query("logs",null,where,args,null,null,null);try{return c.moveToFirst()?logRow(c):null;}finally{c.close();}}
    public long confirm(long medId,long revision,String occurrence,long plannedAt,long actualAt){
        return confirm(medId,revision,occurrence,plannedAt,actualAt,null);
    }
    /** Atomically records an explicitly supplied actual dose and time for the exact planned occurrence. */
    public long confirm(long medId,long revision,String occurrence,long plannedAt,long actualAt,String actualDose){
        if(plannedAt<=0||actualAt<=0||actualAt>System.currentTimeMillis()||(actualDose!=null&&!Core.validDose(actualDose))||!Core.occurrence(medId,revision,plannedAt).equals(occurrence))return -1;
        SQLiteDatabase d=getWritableDatabase();d.beginTransaction();try{Med m=medIn(d,medId);if(m==null||!m.active||m.revision!=revision||skipInSlot(d,medId,plannedAt)!=null)return -1;
            ContentValues v=new ContentValues();v.put("med_id",medId);v.put("planned_at",plannedAt);v.put("actual_at",actualAt);v.put("med_name",m.name);v.put("dose",(actualDose==null?m.dose:actualDose).trim());v.put("notes",m.notes);v.put("occurrence",occurrence);v.put("manual",0);
            d.insertWithOnConflict("logs",null,v,SQLiteDatabase.CONFLICT_IGNORE);Cursor c=d.query("logs",new String[]{"_id"},"med_id=? AND planned_at=? AND manual=0",new String[]{String.valueOf(medId),String.valueOf(plannedAt)},null,null,null);try{if(!c.moveToFirst())return -1;long result=c.getLong(0);d.setTransactionSuccessful();return result;}finally{c.close();}
        }finally{d.endTransaction();}}
    /** Explicit backfill linked to a user-selected scheduled occurrence; idempotent per med+planned timestamp. */
    public long linkedBackfill(long medId,long revision,long plannedAt,long actualAt,String dose,String notes,String zoneId){
        if(plannedAt<=0||actualAt<=0||actualAt>System.currentTimeMillis()||!Core.validDose(dose))return -1;
        SQLiteDatabase d=getWritableDatabase();d.beginTransaction();try{Med m=medIn(d,medId);if(m==null||m.revision!=revision||!matchesSchedule(m,plannedAt,zoneId))return -1;if(skipInSlot(d,medId,plannedAt)!=null)return -1;
            String occurrence=Core.occurrence(medId,revision,plannedAt);ContentValues v=new ContentValues();v.put("med_id",medId);v.put("planned_at",plannedAt);v.put("actual_at",actualAt);v.put("med_name",m.name);v.put("dose",dose.trim());v.put("notes",notes==null?"":notes.trim());v.put("occurrence",occurrence);v.put("manual",0);
            d.insertWithOnConflict("logs",null,v,SQLiteDatabase.CONFLICT_IGNORE);Cursor c=d.query("logs",new String[]{"_id"},"med_id=? AND planned_at=? AND manual=0",new String[]{String.valueOf(medId),String.valueOf(plannedAt)},null,null,null);try{if(!c.moveToFirst())return -1;long result=c.getLong(0);d.setTransactionSuccessful();return result;}finally{c.close();}
        }finally{d.endTransaction();}}
    public long manualLog(long medId,String dose,long actualAt,String notes){if(actualAt<=0||!Core.validDose(dose))throw new IllegalArgumentException("valid dose and actual time required");SQLiteDatabase d=getWritableDatabase();d.beginTransaction();try{Med m=medIn(d,medId);if(m==null)throw new IllegalArgumentException("Medication not found");ContentValues v=new ContentValues();v.put("med_id",medId);v.put("planned_at",0);v.put("actual_at",actualAt);v.put("med_name",m.name);v.put("dose",dose.trim());v.put("notes",notes==null?"":notes.trim());v.put("occurrence","manual:"+UUID.randomUUID());v.put("manual",1);long id=d.insertOrThrow("logs",null,v);d.setTransactionSuccessful();return id;}finally{d.endTransaction();}}
    public void updateLog(long id,long actualAt,String dose,String notes){if(actualAt<=0||!Core.validDose(dose))throw new IllegalArgumentException("valid dose and actual time required");ContentValues v=new ContentValues();v.put("actual_at",actualAt);v.put("dose",dose.trim());v.put("notes",notes==null?"":notes.trim());if(getWritableDatabase().update("logs",v,"_id=?",new String[]{String.valueOf(id)})==0)throw new IllegalArgumentException("Log not found");}
    public void deleteLog(long id){getWritableDatabase().delete("logs","_id=?",new String[]{String.valueOf(id)});}
}
