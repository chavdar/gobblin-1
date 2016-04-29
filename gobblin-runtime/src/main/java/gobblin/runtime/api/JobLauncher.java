package gobblin.runtime.api;

/**
 * <p>Implementations: MRJobLauncher, ThreadJobLauncher, YarnJobLauncher, HelixClusterJobLauncher,
 *    StandardJobMultiLauncher
 *
 */
public interface JobLauncher extends gobblin.runtime.JobLauncher {
  JobDefinition getJobDefinition();
}
