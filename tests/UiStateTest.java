package cn.medreminder.app;

/** Host-side checks for locale-independent restoration of pre-ID saved state. */
public final class UiStateTest {
    private static void equal(String expected, String actual) {
        if (!expected.equals(actual)) throw new AssertionError("expected " + expected + ", got " + actual);
    }
    public static void main(String[] args) {
        equal(UiState.TODAY, UiState.page("今日"));
        equal(UiState.MEDICATIONS, UiState.page("药品"));
        equal(UiState.STATS, UiState.page("统计"));
        equal(UiState.HISTORY, UiState.page("记录"));
        equal(UiState.SETTINGS, UiState.page("设置"));
        equal(UiState.TODAY, UiState.page(null));
        equal(UiState.TODAY, UiState.page("unknown"));
        equal(UiState.TODAY, UiState.page(UiState.TODAY));
        equal(UiState.MONTH, UiState.range("本月"));
        equal(UiState.LAST_30, UiState.range("近30天"));
        equal(UiState.ALL, UiState.range(null));
        equal(UiState.ALL, UiState.range("unknown"));
        equal(UiState.HISTORY, UiState.page(UiState.HISTORY));
        equal(UiState.LAST_30, UiState.range(UiState.LAST_30));
    }
}
