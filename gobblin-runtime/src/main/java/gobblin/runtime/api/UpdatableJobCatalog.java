package gobblin.runtime.api;

/** CRUD API for the JobCatalog */
public interface UpdatableJobCatalog extends JobCatalog {
  void insertJobDefinition(JobDefinition newJob);
  void deleteJobDefinition(String jobName);
  void updateJobDefinition(JobDefinition modifiedJob);
}
