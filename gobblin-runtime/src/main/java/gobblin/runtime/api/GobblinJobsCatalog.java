package gobblin.runtime.api;

import java.util.Collection;

import com.google.common.base.Optional;

public interface GobblinJobsCatalog {

  Collection<GobblinJob> getCurrentJobs();

  /** @return the current set of jobs*/
  void registerJobsListener(GobblinJobsLifecycleListener jobsListener,
                            Optional<Collection<GobblinJob>> knownJobs);

  void removeJobsListener(GobblinJobsLifecycleListener jobsListener);

}
