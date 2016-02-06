package gobblin.runtime.api;

import gobblin.runtime.JobLauncherFactory;

import java.util.concurrent.ExecutorService;

/**
 * Controls the execution of jobs.
 */
public interface GobblinJobsDriver {
  GobblinJobsCatalog getJobsCatalog();
  ExecutorService getJobsExecutorService();
  JobLauncherFactory getJobLauncherFactory();
  void runJobs();
}
