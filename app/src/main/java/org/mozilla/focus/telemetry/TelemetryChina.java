package org.mozilla.focus.telemetry;

import android.os.SystemClock;
import androidx.annotation.CheckResult;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;
import org.mozilla.telemetry.Telemetry;
import org.mozilla.telemetry.TelemetryHolder;
import org.mozilla.telemetry.config.TelemetryConfiguration;
import org.mozilla.focus.telemetry.TelemetryChinaPingBuilder;
import org.mozilla.telemetry.util.StringUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by mozillabeijing on 2018/4/9.
 */

public class TelemetryChina {

    private static final long startTime = SystemClock.elapsedRealtime();

    private static final int MAX_LENGTH_CATEGORY = 30;
    private static final int MAX_LENGTH_METHOD = 20;
    private static final int MAX_LENGTH_OBJECT = 20;
    private static final int MAX_LENGTH_VALUE = 800;
    private static final int MAX_EXTRA_KEYS = 10;
    private static final int MAX_LENGTH_EXTRA_KEY = 15;
    private static final int MAX_LENGTH_EXTRA_VALUE = 80;

    @CheckResult
    public static TelemetryChina create(@NonNull String category, @NonNull String method, @Nullable String object) {
        return new TelemetryChina(category, method, object, null);
    }

    @CheckResult
    public static TelemetryChina create(@NonNull String category, @NonNull String method, @Nullable String object, String value) {
        return new TelemetryChina(category, method, object, value);
    }

    private final long timestamp;
    private final String category;
    private final String method;
    private @Nullable final String object;
    private @Nullable String value;
    private final Map<String, Object> extras;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    private TelemetryChina(@NonNull String category, @NonNull String method, @Nullable String object, @Nullable String value) {
        timestamp = SystemClock.elapsedRealtime() - startTime;

        this.category = StringUtils.safeSubstring(category, 0, MAX_LENGTH_CATEGORY);
        this.method = StringUtils.safeSubstring(method, 0, MAX_LENGTH_METHOD);
        this.object = object == null ? null : StringUtils.safeSubstring(object, 0, MAX_LENGTH_OBJECT);
        this.value = value == null ? null : StringUtils.safeSubstring(value, 0, MAX_LENGTH_VALUE);
        this.extras = new HashMap<>();
    }

    public TelemetryChina extra(String key, String value) {
        if (extras.size() > MAX_EXTRA_KEYS) {
            throw new IllegalArgumentException("Exceeding limit of " + MAX_EXTRA_KEYS + " extra keys");
        }

        extras.put(StringUtils.safeSubstring(key, 0, MAX_LENGTH_EXTRA_KEY),
                StringUtils.safeSubstring(value, 0, MAX_LENGTH_EXTRA_VALUE));

        return this;
    }

    public void queue() {
        queueChina(this);
        Log.e("HttpCNTracking","queue");
    }

    public void queueChina(final TelemetryChina china){
        final Telemetry telemetry = TelemetryHolder.get();
        final TelemetryConfiguration configuration = telemetry.getConfiguration();
        final TelemetryChinaPingBuilder chinaPingBuilder = new TelemetryChinaPingBuilder(configuration);

        if (!configuration.isCollectionEnabled()) {
            return;
        }

        executor.submit(new Runnable() {
            @Override
            public void run() {
                ChinaMeasurement measurement =chinaPingBuilder.getChinaMeasurement();

                measurement.add(china);

                if (measurement.getChinaCount() >= 1) {
                    telemetry.queuePing(TelemetryChinaPingBuilder.TYPE);
                }
            }
        });

        Log.e("HttpCNTracking","queueChina");
        return;
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY)
    public String toJSON() {
        final JSONArray array = new JSONArray();

        array.put(timestamp);
        array.put(category);
        array.put(method);
        array.put(object);

        if (value != null) {
            array.put(value);
        }

        if (extras != null && !extras.isEmpty()) {
            if (value == null) {
                array.put(null);
            }

            array.put(new JSONObject(extras));
        }

        return array.toString();
    }
}
