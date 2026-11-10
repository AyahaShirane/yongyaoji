package cn.medreminder.app;

/** Small production seam so host tests can exercise suppression and retry behavior. */
public final class NotificationSubmission {
    public interface Submitter { void submit(); }
    private NotificationSubmission() {}
    public static String submit(boolean enabled, Submitter submitter) {
        if (!enabled) return "blocked_notifications_disabled";
        try { submitter.submit(); return "submitted_to_os"; }
        catch (RuntimeException failure) { return "notify_failed"; }
    }
}
