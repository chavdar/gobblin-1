package gobblin.runtime.jobs;

import gobblin.configuration.ConfigurationKeys;
import gobblin.runtime.api.GobblinJob;
import gobblin.runtime.api.UpdatableGobblinJob;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;
import java.util.Properties;

import com.google.common.base.Preconditions;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigValue;

/**
 * Default implementation for the {@link GobblinJob}.
 */
public class DefaultGobblinJobImpl implements UpdatableGobblinJob {
  private final String _jobName;
  private Config _jobConfig;
  private Properties _jobConfigProps;
  private long _lastModificationTime;

  private DefaultGobblinJobImpl(String jobName, Config jobConfig, Properties jobConfigProps,
                                long lastModificationTime) {
    _jobName = jobName;
    _jobConfig = jobConfig;
    _jobConfigProps = jobConfigProps;
    _lastModificationTime = lastModificationTime;
  }

  @Override
  public Config getJobConfig() {
    return _jobConfig;
  }

  @Override
  public Properties getJobConfigProperties() {
    return new Properties(_jobConfigProps);
  }

  @Override
  public String getJobName() {
    return _jobName;
  }

  @Override
  public long getLastModificationTime() {
    return _lastModificationTime;
  }

  @Override
  public void update(Config jobConfig, Properties jobConfigProps, Long modificationTimeMs) {
    Preconditions.checkArgument(null != jobConfig || null != jobConfigProps);
    _lastModificationTime = null != modificationTimeMs ? modificationTimeMs.longValue() :
          System.currentTimeMillis();
    _jobConfig = null != jobConfig ? jobConfig : ConfigFactory.parseProperties(jobConfigProps);
    _jobConfigProps = null != jobConfigProps ? jobConfigProps : configToProps(jobConfig);
  }

  static protected Properties configToProps(Config cfg) {
    Properties props = new Properties();
    for (Map.Entry<String, ConfigValue> entry: cfg.entrySet()) {
      props.put(entry.getKey(), entry.getValue());
    }
    return props;
  }

  public static DefaultGobblinJobImpl createFromConfig(String jobName, Config jobConfig,
          long lastModificationTime) {
    return new DefaultGobblinJobImpl(jobName, jobConfig, null, lastModificationTime);
  }

  public static DefaultGobblinJobImpl createFromConfigProps(String jobName,
          Properties jobConfigProps, long lastModificationTime) {
    return new DefaultGobblinJobImpl(jobName, null, jobConfigProps, lastModificationTime);
  }

  public static DefaultGobblinJobImpl createFromJobFile(File jobFile)
          throws FileNotFoundException, IOException {
    Properties props = new Properties();
    try (FileReader jobFileReader = new FileReader(jobFile)) {
      props.load(jobFileReader);
    }
    String jobName = props.getProperty(ConfigurationKeys.JOB_NAME_KEY);
    long lastModificationTime = jobFile.lastModified();
    return createFromConfigProps(jobName, props, lastModificationTime);
  }


  public static DefaultGobblinJobImpl createFromJobFile(String jobFilePath)
          throws FileNotFoundException, IOException {
    return createFromJobFile(new File(jobFilePath));
  }

  public static DefaultGobblinJobImpl createFromJobFile(Path jobFilePath)
          throws FileNotFoundException, IOException {
    return createFromJobFile(jobFilePath.toFile());
  }


  public static DefaultGobblinJobImpl[] createFromJobFiles(File... jobFiles)
          throws FileNotFoundException, IOException {
    DefaultGobblinJobImpl[] res = new DefaultGobblinJobImpl[jobFiles.length];
    int idx = 0;
    for (File jobFile: jobFiles) {
      res[idx++] = createFromJobFile(jobFile);
    }
    return res;
  }

  public static DefaultGobblinJobImpl[] createFromJobFiles(String... jobFiles)
          throws FileNotFoundException, IOException {
    DefaultGobblinJobImpl[] res = new DefaultGobblinJobImpl[jobFiles.length];
    int idx = 0;
    for (String jobFile: jobFiles) {
      res[idx++] = createFromJobFile(jobFile);
    }
    return res;
  }

  public static DefaultGobblinJobImpl[] createFromJobFiles(Path... jobFiles)
          throws FileNotFoundException, IOException {
    DefaultGobblinJobImpl[] res = new DefaultGobblinJobImpl[jobFiles.length];
    int idx = 0;
    for (Path jobFile: jobFiles) {
      res[idx++] = createFromJobFile(jobFile);
    }
    return res;
  }

  @Override
  public String toString() {
    return "DefaultGobblinJobImpl [_jobName=" + _jobName + ", _jobConfig=" + _jobConfig
            + ", _lastModificationTime=" + _lastModificationTime + "]";
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((_jobName == null) ? 0 : _jobName.hashCode());
    return result;
  }
}
