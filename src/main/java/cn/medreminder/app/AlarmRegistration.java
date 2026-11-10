package cn.medreminder.app;

/** Production registration path with observable exact, fallback, and failed outcomes. */
public final class AlarmRegistration {
    public interface Registrar { void exact(); void fallback(); }
    private AlarmRegistration() {}
    public static String register(boolean exactAllowed, Registrar registrar) {
        if (exactAllowed) try { registrar.exact(); return "exact"; } catch (RuntimeException denied) { }
        try { registrar.fallback(); return "fallback"; } catch (RuntimeException failure) { return "failed"; }
    }
}
