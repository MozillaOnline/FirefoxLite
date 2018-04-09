package org.mozilla.focus.telemetry;

/**
 * Created by mozillabeijing on 2018/4/9.
 */

import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.app.job.JobInfo.Builder;
import android.content.ComponentName;
import android.content.Context;
import android.util.Log;

import org.mozilla.telemetry.config.TelemetryConfiguration;
import org.mozilla.telemetry.schedule.TelemetryScheduler;
import org.mozilla.focus.telemetry.TelemetryJobServiceCN;
import org.mozilla.telemetry.schedule.jobscheduler.TelemetryJobService;

public class JobSchedulerTelemetrySchedulerCN implements TelemetryScheduler {
    public static final int JOB_ID = 43;

    @Override
    public void scheduleUpload(TelemetryConfiguration configuration) {

        final ComponentName jobService = new ComponentName(configuration.getContext(), TelemetryJobServiceCN.class);

        final JobInfo jobInfo = (new Builder(JOB_ID, jobService)).setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY).setPersisted(true).setBackoffCriteria(configuration.getInitialBackoffForUpload(), JobInfo.BACKOFF_POLICY_EXPONENTIAL).build();

        final JobScheduler scheduler = (JobScheduler)configuration.getContext().getSystemService(Context.JOB_SCHEDULER_SERVICE);

        scheduler.schedule(jobInfo);
        Log.e("HttpCNTracking","JobSchedulerTelemetrySchedulerCN");
    }
}
