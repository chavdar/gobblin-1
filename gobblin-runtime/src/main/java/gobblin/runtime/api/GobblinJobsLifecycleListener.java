package gobblin.runtime.api;

public interface GobblinJobsLifecycleListener {

  /** For debugging purposes. Need not be unique */
  String getName();

  void onJobCreate(GobblinJob newJob);

  void onJobDrop(GobblinJob oldJob);

  void onJobChange(GobblinJob changedJob);

}
