package cn.medreminder.app;

/** Stable, locale-independent identifiers for the Activity's saved navigation state. */
final class UiState {
    static final String TODAY="today", MEDICATIONS="medications", STATS="stats", HISTORY="history", SETTINGS="settings";
    static final String ALL="all", MONTH="month", LAST_30="last30";
    private UiState() {}
    static String page(String saved) {
        if (TODAY.equals(saved)||MEDICATIONS.equals(saved)||STATS.equals(saved)||HISTORY.equals(saved)||SETTINGS.equals(saved)) return saved;
        if ("今日".equals(saved)) return TODAY;
        if ("药品".equals(saved)) return MEDICATIONS;
        if ("统计".equals(saved)) return STATS;
        if ("记录".equals(saved)) return HISTORY;
        if ("设置".equals(saved)) return SETTINGS;
        return TODAY;
    }
    static String range(String saved) {
        if (ALL.equals(saved)||MONTH.equals(saved)||LAST_30.equals(saved)) return saved;
        if ("本月".equals(saved)) return MONTH;
        if ("近30天".equals(saved)) return LAST_30;
        return ALL;
    }
}
