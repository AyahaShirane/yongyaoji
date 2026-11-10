package cn.medreminder.app;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.ResolverStyle;
import java.util.*;
import java.math.BigInteger;

public final class Core {
    private static final BigInteger BI_LONG_MAX=BigInteger.valueOf(Long.MAX_VALUE), BI_LONG_MIN=BigInteger.valueOf(Long.MIN_VALUE);
    private static final long DAY_MS=86400000L;
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm").withResolverStyle(ResolverStyle.STRICT);
    private Core() {}

    /** Defaults for an explicit plan confirmation; preserves the exact dialog-open millisecond. */
    public static final class ConfirmationDefaults { public final long actualAt; public final String dose; private ConfirmationDefaults(long at,String d){actualAt=at;dose=d;} }
    public static ConfirmationDefaults confirmationDefaults(long openedAt,String selectedPlanDose){if(openedAt<=0||selectedPlanDose==null||!validDose(selectedPlanDose))throw new IllegalArgumentException("confirmation defaults require a valid selected plan");return new ConfirmationDefaults(openedAt,selectedPlanDose);}

    public static String normalizeTimes(String csv) {
        if (csv == null) throw new IllegalArgumentException("times required");
        TreeSet<String> sorted = new TreeSet<>();
        for (String raw : csv.split("[,，]", -1)) {
            String value = raw.trim();
            try {
                if (!value.matches("[0-9]{2}:[0-9]{2}")) throw new DateTimeException("format");
                LocalTime.parse(value, TIME);
                sorted.add(value);
            } catch (DateTimeException ex) { throw new IllegalArgumentException("Invalid time: " + value, ex); }
        }
        if (sorted.isEmpty() || sorted.contains("")) throw new IllegalArgumentException("times required");
        return String.join(",", sorted);
    }

    public static boolean validDose(String dose) {
        if (dose == null || dose.trim().isEmpty()) return false;
        String s = dose.trim();
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("^([+-]?(?:\\d+(?:\\.\\d*)?|\\.\\d+))\\s*.*$").matcher(s);
        if (m.matches()) {
            try { return new java.math.BigDecimal(m.group(1)).signum() > 0; }
            catch (NumberFormatException e) { return false; }
        }
        return true;
    }

    public static long nextTime(String hhmm, long now, String zoneId) {
        LocalTime t = parseTime(hhmm);
        ZoneId zone = ZoneId.of(zoneId);
        ZonedDateTime current = Instant.ofEpochMilli(now).atZone(zone);
        LocalDate date = current.toLocalDate();
        ZonedDateTime candidate = date.atTime(t).atZone(zone);
        if (!candidate.toInstant().isAfter(Instant.ofEpochMilli(now))) candidate = date.plusDays(1).atTime(t).atZone(zone);
        return candidate.toInstant().toEpochMilli();
    }

    public static List<Long> todayTimes(String csv, long now, String zoneId) {
        List<Long> out = new ArrayList<>();
        LocalDate date = Instant.ofEpochMilli(now).atZone(ZoneId.of(zoneId)).toLocalDate();
        for (String s : normalizeTimes(csv).split(",")) out.add(date.atTime(parseTime(s)).atZone(ZoneId.of(zoneId)).toInstant().toEpochMilli());
        Collections.sort(out);
        return out;
    }

    /** Elapsed-time frequency schedule. A day is exactly 24 elapsed hours. */
    public static long frequencyAt(long anchor,int days,int count,long index) {
        validateFrequency(anchor,days,count,index);
        BigInteger period=BigInteger.valueOf(days).multiply(BigInteger.valueOf(DAY_MS));
        BigInteger offset=BigInteger.valueOf(index).multiply(period).divide(BigInteger.valueOf(count));
        BigInteger value=BigInteger.valueOf(anchor).add(offset);
        if(value.compareTo(BI_LONG_MAX)>0||value.compareTo(BI_LONG_MIN)<0)throw new IllegalArgumentException("frequency time overflow");
        return value.longValue();
    }
    /** Returns an existing frequency anchor unchanged, or a new visible local-minute anchor. */
    public static long initialFrequencyAnchor(long now,String zoneId,long existingAnchor) {
        if(existingAnchor>0)return existingAnchor;
        if(zoneId==null||zoneId.trim().isEmpty())throw new IllegalArgumentException("time zone required");
        return Instant.ofEpochMilli(now).atZone(ZoneId.of(zoneId)).withSecond(0).withNano(0).toInstant().toEpochMilli();
    }
    private static void validateFrequency(long anchor,int days,int count,long index) {
        if(anchor<=0||days<1||days>36500||count<1||count>1000||index<0)throw new IllegalArgumentException("frequency values out of supported range");
    }
    private static long firstIndexAtOrAfter(long anchor,int days,int count,long time) {
        BigInteger delta=BigInteger.valueOf(time).subtract(BigInteger.valueOf(anchor));
        if(delta.signum()<=0)return 0;
        BigInteger period=BigInteger.valueOf(days).multiply(BigInteger.valueOf(DAY_MS));
        BigInteger n=delta.multiply(BigInteger.valueOf(count)).add(period).subtract(BigInteger.ONE).divide(period);
        if(n.compareTo(BI_LONG_MAX)>0)throw new IllegalArgumentException("frequency index overflow");
        return n.longValue();
    }
    public static long nextFrequency(long anchor,int days,int count,long now,String zoneId) {
        if(zoneId==null||zoneId.trim().isEmpty())throw new IllegalArgumentException("time zone required");
        long idx=firstIndexAtOrAfter(anchor,days,count,now);
        long at=frequencyAt(anchor,days,count,idx);
        if(at<=now)at=frequencyAt(anchor,days,count,idx+1);
        return at;
    }
    public static boolean isFrequencyOccurrence(long anchor,int days,int count,long plannedAt) {
        if(plannedAt<anchor)return false;
        long index=firstIndexAtOrAfter(anchor,days,count,plannedAt);
        return frequencyAt(anchor,days,count,index)==plannedAt;
    }
    public static List<Long> frequencyToday(long anchor,int days,int count,long now,String zoneId) {
        if(zoneId==null||zoneId.trim().isEmpty())throw new IllegalArgumentException("time zone required");
        ZoneId zone=ZoneId.of(zoneId); LocalDate date=Instant.ofEpochMilli(now).atZone(zone).toLocalDate();
        return frequencyOnDate(anchor,days,count,date,zoneId);
    }
    /** Exact elapsed-time occurrences in a selected local calendar date, half-open at next midnight. */
    public static List<Long> frequencyOnDate(long anchor,int days,int count,LocalDate date,String zoneId) {
        if(zoneId==null||zoneId.trim().isEmpty()||date==null)throw new IllegalArgumentException("date and time zone required");
        ZoneId zone=ZoneId.of(zoneId);
        long from=date.atStartOfDay(zone).toInstant().toEpochMilli(), to=date.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli();
        long idx=firstIndexAtOrAfter(anchor,days,count,from),end=firstIndexAtOrAfter(anchor,days,count,to); List<Long> out=new ArrayList<>();
        for(long index=idx;index<end;index++)out.add(frequencyAt(anchor,days,count,index));
        return out;
    }
    /** Exact fixed-clock occurrences on a selected local calendar date. */
    public static List<Long> fixedOnDate(String csv,LocalDate date,String zoneId) {
        if(date==null||zoneId==null||zoneId.trim().isEmpty())throw new IllegalArgumentException("date and time zone required");
        ZoneId zone=ZoneId.of(zoneId);List<Long> out=new ArrayList<>();
        for(String value:normalizeTimes(csv).split(","))out.add(date.atTime(parseTime(value)).atZone(zone).toInstant().toEpochMilli());
        Collections.sort(out);return out;
    }
    public static List<Long> frequencyPreview(long anchor,int days,int count,long now,String zoneId,int limit) {
        if(limit<1||limit>100)throw new IllegalArgumentException("preview size out of range");
        long idx=firstIndexAtOrAfter(anchor,days,count,now);List<Long> out=new ArrayList<>();
        for(int i=0;i<limit;i++)out.add(frequencyAt(anchor,days,count,idx+i));return out;
    }
    public static long frequencyIntervalMillis(int days,int count) {
        if(days<1||days>36500||count<1||count>1000)throw new IllegalArgumentException("frequency values out of supported range");
        return BigInteger.valueOf(days).multiply(BigInteger.valueOf(DAY_MS)).divide(BigInteger.valueOf(count)).longValue();
    }

    public static String occurrence(long medId, long revision, long plannedAt) { return medId + ":" + revision + ":" + plannedAt; }
    private static LocalTime parseTime(String s) {
        try { if (s == null || !s.matches("[0-9]{2}:[0-9]{2}")) throw new DateTimeException("format"); return LocalTime.parse(s, TIME); }
        catch (DateTimeException e) { throw new IllegalArgumentException("Invalid time: " + s, e); }
    }
}
