package cn.medreminder.app;

/** Limits blocked notification recovery to an explicitly retained, still-eligible occurrence. */
public final class NotificationRetry {
    private NotificationRetry() {}
    public static boolean shouldRetry(boolean occurrenceStillEligible, boolean notificationsEnabled) {
        return occurrenceStillEligible && notificationsEnabled;
    }
}
