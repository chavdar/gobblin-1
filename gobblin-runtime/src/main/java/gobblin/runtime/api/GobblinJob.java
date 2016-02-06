package gobblin.runtime.api;

import java.util.Properties;

import com.typesafe.config.Config;

public interface GobblinJob {

  /** Current job config */
  Config getJobConfig();

  Properties getJobConfigProperties();

  String getJobName();

  long getLastModificationTime();

}
