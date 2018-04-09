package org.mozilla.focus.telemetry;

import android.content.SharedPreferences;
import android.util.Log;
import androidx.annotation.VisibleForTesting;

import org.json.JSONArray;
import org.json.JSONException;
import org.mozilla.telemetry.config.TelemetryConfiguration;
import org.mozilla.telemetry.event.TelemetryEvent;
import org.mozilla.telemetry.measurement.EventsMeasurement;
import org.mozilla.telemetry.measurement.TelemetryMeasurement;
import org.mozilla.telemetry.util.IOUtils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;

/**
 * Created by mozillabeijing on 2018/4/9.
 */

public class ChinaMeasurement extends TelemetryMeasurement{

    private static final String LOG_TAG = ChinaMeasurement.class.getSimpleName();

    private static final int VERSION = 1;
    private static final String FIELD_NAME = "chinas";

    private static final String PREFERENCE_CHINA_COUNT = "china_count";

    private TelemetryConfiguration configuration;

    public ChinaMeasurement(TelemetryConfiguration configuration) {
        super(FIELD_NAME);

        this.configuration = configuration;
    }

    public void add(final TelemetryChina china) {
        saveChinaToDisk(china);
    }
    @Override
    public Object flush() {
        return readAndClearChinasFromDisk();
    }

    private synchronized JSONArray readAndClearChinasFromDisk() {
        final JSONArray chinas = new JSONArray();
        final File file = getChinaFile();

        FileInputStream stream = null;

        try {
            stream = new FileInputStream(file);

            final BufferedReader reader = new BufferedReader(new InputStreamReader(stream));

            String line;

            while ((line = reader.readLine()) != null) {
                try {
                    JSONArray china = new JSONArray(line);
                    chinas.put(china);

                    resetChinaCount();
                } catch (JSONException e) {
                    // Let's log a warning and move on. This event is lost.
                    Log.w(LOG_TAG, "Could not parse China event from disk", e);
                }
            }
        } catch (FileNotFoundException e) {
            // This shouldn't happen because we do not create event pings if there are no events.
            // However in case the file disappears: Continue with no events.
            return new JSONArray();
        } catch (IOException e) {
            // Handling this exception at this time is tricky: We might have been able to read some
            // events at the time this exception occurred. We can either try to add them to the
            // ping and remove the file or we retry later again.

            // We just log an error here. This means we are going to continue building the ping
            // with the events we were able to read from disk. The events file will be removed and
            // we might potentially lose events that we couldn't ready because of the exception.
            Log.w(LOG_TAG, "IOException while reading  China events from disk", e);
        } finally {
            IOUtils.safeClose(stream);

            if (!file.delete()) {
                Log.w(LOG_TAG, " China Events file could not be deleted");
            }
        }

        return chinas;
    }

    @VisibleForTesting
    File getChinaFile() {
        return new File(configuration.getDataDirectory(), "chinas" + VERSION);
    }

    private synchronized void saveChinaToDisk(TelemetryChina china) {
        FileOutputStream stream = null;

        try {
            stream = new FileOutputStream(getChinaFile(), true);

            final BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(stream));
            writer.write(china.toJSON());
            writer.newLine();
            writer.flush();
            writer.close();

            countChina();
        } catch (IOException e) {
            Log.w(LOG_TAG, "IOException while writing China event to disk", e);
            throw new AssertionError("BOING");
        } finally {
            IOUtils.safeClose(stream);
        }
    }

    private synchronized void countChina() {
        final SharedPreferences preferences = configuration.getSharedPreferences();

        long count = preferences.getLong(PREFERENCE_CHINA_COUNT, 0);

        preferences.edit()
                .putLong(PREFERENCE_CHINA_COUNT, ++count)
                .apply();
    }

    private synchronized void resetChinaCount() {
        final SharedPreferences preferences = configuration.getSharedPreferences();

        preferences.edit()
                .putLong(PREFERENCE_CHINA_COUNT, 0)
                .apply();
    }

    public long getChinaCount() {
        final SharedPreferences preferences = configuration.getSharedPreferences();

        return preferences.getLong(PREFERENCE_CHINA_COUNT, 0);
    }
}
