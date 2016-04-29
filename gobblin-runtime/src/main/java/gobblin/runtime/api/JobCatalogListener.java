package gobblin.runtime.api;

public interface JobCatalogListener {

  void onJobAdded(JobDefinition newJob);

  void onJobRemoved(JobDefinition removedJob);

  void onJobChanged(JobDefinition changedJob);

}
