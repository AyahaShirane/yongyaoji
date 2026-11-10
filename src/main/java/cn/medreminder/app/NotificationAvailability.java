package cn.medreminder.app;

/** Combines the three ordinary notification gates checked before scheduled posts. */
public final class NotificationAvailability {
    private NotificationAvailability() {}
    public static boolean enabled(boolean appNotifications, boolean runtimePermission, boolean channelEnabled) {
        return appNotifications && runtimePermission && channelEnabled;
    }
}
