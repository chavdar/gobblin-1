package gobblin.runtime.api;

import java.util.Properties;

import com.typesafe.config.Config;

/** Describes Gobblin Job Definition */
public interface JobDefinition {

  String getName();

  Config getJobConfig();

  Properties getJobProperties();

}
