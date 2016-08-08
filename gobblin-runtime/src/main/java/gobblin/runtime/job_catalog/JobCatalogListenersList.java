package gobblin.runtime.job_catalog;

import java.util.List;
import java.util.concurrent.ExecutorService;

import org.slf4j.Logger;

import com.google.common.base.Function;
import com.google.common.base.Objects;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;

import gobblin.runtime.api.JobCatalog;
import gobblin.runtime.api.JobCatalogListener;
import gobblin.runtime.api.JobCatalogListenersContainer;
import gobblin.runtime.api.JobSpec;
import gobblin.util.callbacks.Callback;
import gobblin.util.callbacks.CallbackFactory;
import gobblin.util.callbacks.CallbacksDispatcher;

/** A helper class to manage a list of {@link JobCatalogListener}s for a
 * {@link JobCatalog}. It will dispatch the callbacks to each listener sequentially.*/
public class JobCatalogListenersList implements JobCatalogListener, JobCatalogListenersContainer {
  private final CallbacksDispatcher<JobCatalogListener> _disp;

  public JobCatalogListenersList() {
    this(Optional.<Logger>absent());
  }

  public JobCatalogListenersList(Optional<Logger> log) {
    _disp = new CallbacksDispatcher<JobCatalogListener>(Optional.<ExecutorService>absent(), log);
  }

  public Logger getLog() {
    return _disp.getLog();
  }

  public synchronized List<JobCatalogListener> getListeners() {
    return _disp.getListeners();
  }

  @Override
  public synchronized void addListener(JobCatalogListener newListener) {
    _disp.addListener(newListener);
  }

  @Override
  public synchronized void removeListener(JobCatalogListener oldListener) {
    _disp.removeListener(oldListener);
  }

  @Override
  public synchronized void onAddJob(JobSpec addedJob) {
    Preconditions.checkNotNull(addedJob);
    try {
      _disp.execCallbacks(new JobCatalogCallbackFactory(new AddJobCallback(addedJob)));
    } catch (InterruptedException e) {
      getLog().warn("onAddJob interrupted.");
    }
  }

  @Override
  public synchronized void onDeleteJob(JobSpec deletedJob) {
    Preconditions.checkNotNull(deletedJob);
    try {
      _disp.execCallbacks(new JobCatalogCallbackFactory(new DeleteJobCallback(deletedJob)));
    } catch (InterruptedException e) {
      getLog().warn("onDeleteJob interrupted.");
    }
  }

  @Override
  public synchronized void onUpdateJob(JobSpec originalJob, JobSpec updatedJob) {
    Preconditions.checkNotNull(originalJob);
    Preconditions.checkNotNull(updatedJob);
    try {
      _disp.execCallbacks(new JobCatalogCallbackFactory(new UpdateJobCallback(originalJob, updatedJob)));
    } catch (InterruptedException e) {
      getLog().warn("onUpdateJob interrupted.");
    }
  }

  public void callbackOneListener(Function<JobCatalogListener, Void> callback,
                                  JobCatalogListener listener) {
    try {
      _disp.execCallbacks(new JobCatalogCallbackFactory(callback), listener);
    } catch (InterruptedException e) {
      getLog().warn("callback interrupted: "+ callback);
    }
  }


  public static class AddJobCallback extends Callback<JobCatalogListener, Void> {
    private final JobSpec _addedJob;
    public AddJobCallback(JobSpec addedJob) {
      super(Objects.toStringHelper("onAddJob").add("addedJob", addedJob).toString());
      _addedJob = addedJob;
    }

    @Override public Void apply(JobCatalogListener listener) {
      listener.onAddJob(_addedJob);
      return null;
    }
  }

  public static class DeleteJobCallback extends Callback<JobCatalogListener, Void> {
    private final JobSpec _deletedJob;

    public DeleteJobCallback(JobSpec deletedJob) {
      super(Objects.toStringHelper("onDeleteJob").add("deletedJob", deletedJob).toString());
      _deletedJob = deletedJob;
    }

    @Override public Void apply(JobCatalogListener listener) {
      listener.onDeleteJob(_deletedJob);
      return null;
    }
  }

  public static class UpdateJobCallback extends Callback<JobCatalogListener, Void> {
    private final JobSpec _originalJob;
    private final JobSpec _updatedJob;
    public UpdateJobCallback(JobSpec originalJob, JobSpec updatedJob) {
      super(Objects.toStringHelper("onUpdateJob").add("originalJob", originalJob)
                   .add("updatedJob", updatedJob).toString());
      _originalJob = originalJob;
      _updatedJob = updatedJob;
    }

    @Override
    public Void apply(JobCatalogListener listener) {
      listener.onUpdateJob(_originalJob, _updatedJob);
      return null;
    }
  }

  public static class JobCatalogCallbackFactory implements CallbackFactory<JobCatalogListener, Void> {
    private final Function<JobCatalogListener, Void> _callback;

    public JobCatalogCallbackFactory(Function<JobCatalogListener, Void> callback) {
      _callback = callback;
    }

    @Override public Function<JobCatalogListener, Void> createCallbackRunnable() {
      return _callback;
    }
  }


}
