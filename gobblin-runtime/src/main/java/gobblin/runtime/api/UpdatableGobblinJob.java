package gobblin.runtime.api;

import java.util.Properties;

import com.typesafe.config.Config;

public interface UpdatableGobblinJob extends GobblinJob {
  /**
   * Only one of jobConfig and jobConfigProps can be null.
   * @param jobConfig           the job typesafe {@link Config}; if null, the jobConfigProps will be
   *                            used.
   * @param jobConfigProps      the job configuration as properties; if null, jobConfig will be used
   * @param modificationTimeMs  the time when modification occured, if null, current time will be
   *                            used
   */
  void update(Config jobConfig, Properties jobConfigProps, Long modificationTimeMs);
}
