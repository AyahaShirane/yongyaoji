package cn.medreminder.app;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/** Presentation helpers for actual-use timestamps; storage remains millisecond precise. */
public final class ActualTime {
    private static final DateTimeFormatter DISPLAY = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm", Locale.ROOT);
    private ActualTime() {}
    public static String display(long millis, ZoneId zone) { return DISPLAY.format(Instant.ofEpochMilli(millis).atZone(zone)); }
    public static boolean sameDisplayedMinute(long a, long b, ZoneId zone) {
        ZonedDateTime x=Instant.ofEpochMilli(a).atZone(zone), y=Instant.ofEpochMilli(b).atZone(zone);
        return x.getYear()==y.getYear() && x.getDayOfYear()==y.getDayOfYear() && x.getHour()==y.getHour() && x.getMinute()==y.getMinute();
    }
    public static long unchangedSaveValue(long original, String entered, ZoneId zone) {
        return DISPLAY.format(Instant.ofEpochMilli(original).atZone(zone)).equals(entered) ? original : -1L;
    }
}
