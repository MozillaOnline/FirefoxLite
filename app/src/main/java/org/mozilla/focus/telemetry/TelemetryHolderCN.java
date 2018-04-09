package org.mozilla.focus.telemetry;

public class TelemetryHolderCN {
    private static TelemetryCN telemetry;

    public static void set(TelemetryCN telemetry) {
        TelemetryHolderCN.telemetry = telemetry;
    }

    public static TelemetryCN get() {
        if (telemetry == null) {
            throw new IllegalStateException("You need to call set() on TelemetryHolder in your application class");
        }

        return telemetry;
    }
}