package gobblin.runtime.api;

import gobblin.runtime.JobContext;
import gobblin.runtime.JobState;

import com.typesafe.config.Config;

/** Similar to {@link JobContext} */
public interface GobblinJobInstance {

  GobblinJob getJob();

  String getJobId();

  public long getStartTime();

  public long getEndTime();

  public long getDuration();

  /** The job config at the time the instance was created */
  Config getConfig();

  JobState.RunningState getRunningState();

  JobState getRuntimeState();

  int getTaskCount();
}
