package org.mozilla.focus.telemetry;

/**
 * Created by mozillabeijing on 2018/4/9.
 */

import org.mozilla.telemetry.config.TelemetryConfiguration;
import org.mozilla.telemetry.measurement.CreatedTimestampMeasurement;
import org.mozilla.telemetry.measurement.DeviceMeasurement;
import org.mozilla.telemetry.measurement.EventsMeasurement;
import org.mozilla.telemetry.measurement.LocaleMeasurement;
import org.mozilla.telemetry.measurement.OperatingSystemMeasurement;
import org.mozilla.telemetry.measurement.OperatingSystemVersionMeasurement;
import org.mozilla.telemetry.measurement.SequenceMeasurement;
import org.mozilla.telemetry.measurement.SettingsMeasurement;
import org.mozilla.telemetry.measurement.TimezoneOffsetMeasurement;
import org.mozilla.telemetry.ping.TelemetryPingBuilder;

public class TelemetryChinaPingBuilder extends TelemetryPingBuilder {
    public static final String TYPE = "focus-eventChina";
    private static final int VERSION = 3;

    private ChinaMeasurement chinasMeasurement;

    public TelemetryChinaPingBuilder(TelemetryConfiguration configuration) {
        super(configuration, TYPE, VERSION);

        addMeasurement(new DeviceMeasurement());
        /*addMeasurement(new LocaleMeasurement());
        addMeasurement(new OperatingSystemMeasurement());
        addMeasurement(new OperatingSystemVersionMeasurement());
        addMeasurement(new CreatedTimestampMeasurement());
        addMeasurement(new TimezoneOffsetMeasurement());
        addMeasurement(new SettingsMeasurement(configuration));*/
        addMeasurement(chinasMeasurement = new ChinaMeasurement(configuration));
    }

    public ChinaMeasurement getChinaMeasurement() {
        return chinasMeasurement;
    }

    @Override
    public boolean canBuild() {
        //return chinasMeasurement.getChinaCount() >= getConfiguration().getMinimumEventsForUpload();
        return chinasMeasurement.getChinaCount() >= 1;
    }

    @Override
    protected String getUploadPath(final String documentId) {
        return super.getUploadPath(documentId) + "?v=4";
    }
}
