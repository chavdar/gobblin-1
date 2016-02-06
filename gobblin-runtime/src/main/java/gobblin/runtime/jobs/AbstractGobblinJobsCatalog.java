package gobblin.runtime.jobs;

import gobblin.runtime.api.GobblinJob;
import gobblin.runtime.api.GobblinJobNameComparator;
import gobblin.runtime.api.GobblinJobsCatalog;
import gobblin.runtime.api.GobblinJobsLifecycleListener;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;

public abstract class AbstractGobblinJobsCatalog implements GobblinJobsCatalog {
  private final Logger _log;
  private final Map<String, GobblinJob> _jobs = new HashMap<>();
  private final List<GobblinJobsLifecycleListener> _lifecycleListeners = new ArrayList<>();

  public Logger getLog() {
    return _log;
  }

  protected AbstractGobblinJobsCatalog(Optional<Logger> log) {
    _log = log.isPresent() ? log.get() : LoggerFactory.getLogger(AbstractGobblinJobsCatalog.class);
  }

  @Override
  public synchronized Collection<GobblinJob> getCurrentJobs() {
    return Collections.unmodifiableCollection(_jobs.values());
  }

  @Override
  public synchronized void registerJobsListener(GobblinJobsLifecycleListener jobsListener,
          Optional<Collection<GobblinJob>> knownJobs) {
    // We have to find the diff between the known jobs and the current jobs and trigger the
    // appropriate callbacks.
    GobblinJobNameComparator jobNameCmp = new GobblinJobNameComparator();
    List<GobblinJob> sortedKnownJobs =
            new ArrayList<GobblinJob>(knownJobs.isPresent() ? knownJobs.get() :
              Collections.<GobblinJob>emptyList());
    Collections.sort(sortedKnownJobs, jobNameCmp);

    List<GobblinJob> sortedCurrentJobs = new ArrayList<>(_jobs.values());
    Collections.sort(sortedCurrentJobs, jobNameCmp);

    Iterator<GobblinJob> knownIter = sortedKnownJobs.iterator();
    Iterator<GobblinJob> currentIter = sortedCurrentJobs.iterator();
    while (knownIter.hasNext() && currentIter.hasNext()) {
      GobblinJob knownJob = knownIter.next();
      GobblinJob currentJob = currentIter.next();
      int cmpRes = jobNameCmp.compare(knownJob, currentJob);
      if (cmpRes < 0) {
        triggerDropJobCallback(knownJob, jobsListener);
      }
      else if (cmpRes > 0) {
        triggerCreateCallback(knownJob, jobsListener);
      }
      else if (knownJob.getLastModificationTime() < currentJob.getLastModificationTime()){
        triggerChangeJobCallback(currentJob, jobsListener);
      }
    }
    _lifecycleListeners.add(jobsListener);
  }

  @Override
  public synchronized void removeJobsListener(GobblinJobsLifecycleListener jobsListener) {
    _lifecycleListeners.remove(jobsListener);
  }

  protected void addJob(GobblinJob newJob) {
    if (_jobs.containsKey(newJob.getJobName())) {
      throw new IllegalArgumentException("Job already exists: " + newJob.getJobName());
    }
    _jobs.put(newJob.getJobName(), newJob);
    triggerCreateCallback(newJob);
  }

  protected void changeJob(GobblinJob changedJob) {
    _jobs.put(changedJob.getJobName(), changedJob);
    triggerChangeJobCallback(changedJob);
  }

  protected void dropJob(String jobName) {
    GobblinJob job = null;
    if ((job = _jobs.remove(jobName)) != null) {
      triggerDropJobCallback(job);
    }
    else {
      _log.warn("Unable to find job with name " + jobName);
    }
  }

  protected void dropJob(GobblinJob oldJob) {
    dropJob(oldJob.getJobName());
  }

  private void triggerCreateCallback(GobblinJob newJob) {
    for (GobblinJobsLifecycleListener listener: _lifecycleListeners) {
      triggerCreateCallback(newJob, listener);
    }
  }

  private void triggerCreateCallback(GobblinJob newJob, GobblinJobsLifecycleListener listener) {
    try {
      listener.onJobCreate(newJob);
    }
    catch (RuntimeException e) {
      _log.error("onJobCreate(" + newJob +
              ") for lifecycle jobs listener " + listener.getName() + " failed: " + e, e);
    }
  }


 private void triggerDropJobCallback(GobblinJob oldJob) {
    for (GobblinJobsLifecycleListener listener: _lifecycleListeners) {
      triggerDropJobCallback(oldJob, listener);
    }
  }

private void triggerDropJobCallback(GobblinJob oldJob, GobblinJobsLifecycleListener listener) {
  try {
    listener.onJobDrop(oldJob);
  }
  catch (RuntimeException e) {
    _log.error("onJobDrop(" + oldJob +
            ") for lifecycle jobs listener " + listener.getName() + " failed: " + e, e);
  }
}

 private void triggerChangeJobCallback(GobblinJob changedJob) {
   for (GobblinJobsLifecycleListener listener: _lifecycleListeners) {
     triggerChangeJobCallback(changedJob, listener);
   }
 }

private void triggerChangeJobCallback(GobblinJob changedJob, GobblinJobsLifecycleListener listener) {
  try {
     listener.onJobChange(changedJob);
   }
   catch (RuntimeException e) {
     _log.error("onJobChange(" + changedJob +
             ") for lifecycle jobs listener " + listener.getName() + " failed: " + e, e);
   }
}

}
