package gobblin.runtime.jobs;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;

public class FsDirJobsCatalog extends AbstractGobblinJobsCatalog {

  private final FileSystem _jobsRootFS;
  private final Path _jobsRootDir;
  private final WatchService _fsWatchService;
  private final WatchKey _jobsRootWatchKey;
  private final ConcurrentHashMap<String, String> _fileToJobName = new ConcurrentHashMap<>();
  private final Thread _jobsRootWatcher;

  public FsDirJobsCatalog(FileSystem jobsRootFS, Path jobsRootDir) throws IOException {
    super(Optional.<Logger>of(
            LoggerFactory.getLogger(FsDirJobsCatalog.class.getName() + "." +
                                    jobsRootDir.getFileName())));
    _jobsRootFS = jobsRootFS;
    _jobsRootDir = jobsRootDir;
    _fsWatchService = _jobsRootFS.newWatchService();
    _jobsRootWatchKey = _jobsRootDir.register(_fsWatchService,
            StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_DELETE,
            StandardWatchEventKinds.ENTRY_MODIFY);
    _jobsRootWatcher = new Thread(new JobsRootWatcher(), FsDirJobsCatalog.class.getSimpleName() +
            " for " + jobsRootDir);
    _jobsRootWatcher.setDaemon(true);
    _jobsRootWatcher.start();
    scanExistingJobs();
  }

  private void scanExistingJobs() {
     for (File file: _jobsRootDir.toFile().listFiles()) {
       addJobFromFile(file);
     }
  }

  class JobsRootWatcher implements Runnable {
    @Override
    public void run() {
      while (true) {
        try {
          WatchKey nextWatchKey = _fsWatchService.take();
          if (nextWatchKey == _jobsRootWatchKey) {
            // We care about this event
            for (WatchEvent<?> event: nextWatchKey.pollEvents()) {
              if (event == StandardWatchEventKinds.OVERFLOW) {
                getLog().warn("Watcher for jobs directory overflow events: " + event.count());
                continue;
              }
              try {
                @SuppressWarnings("unchecked")
                WatchEvent<Path> pathEvent = (WatchEvent<Path>)event;
                if (StandardWatchEventKinds.ENTRY_CREATE == pathEvent) {
                  addJobFromFile(pathEvent.context().toFile());
                } else if (StandardWatchEventKinds.ENTRY_DELETE == pathEvent) {
                  removeJobForFile(pathEvent);
                }
                else if (StandardWatchEventKinds.ENTRY_MODIFY == pathEvent) {
                  changeJobForFile(pathEvent);
                }
                else {
                  getLog().warn("Unknown file change event: " + event);
                }
              }
              catch (RuntimeException e) {
                // For example we may fail to parse a job file
                getLog().warn("Error processing file change event " + event + ": " + e, e);
              }
            }
          }
          nextWatchKey.reset();
        } catch (InterruptedException e) {
          getLog().info("Watcher for jobs directory " + _jobsRootDir + " interrupted. Exiting...");
          _jobsRootWatchKey.cancel();
        }
      }
    }

  }

  protected boolean isJobFile(File file) {
    //TODO add possibility by filtering, say using a glob
    return true;
  }

  public void changeJobForFile(WatchEvent<Path> pathEvent) {
    File jobFile = pathEvent.context().toFile();
    if (! isJobFile(jobFile)) {
      return;
    }
    DefaultGobblinJobImpl newJob;
    try {
      String jobFileName = jobFile.getName();
      newJob = DefaultGobblinJobImpl.createFromJobFile(pathEvent.context());
      // Account for a race condition where a file may change between the time we start the
      // watcher and we scan the directory.
      if (_fileToJobName.putIfAbsent(jobFileName, newJob.getJobName()) == null) {
        // The file existed but scanExistingJobs hasn't scanned it yet. Just treat it as a new
        // job file.
        addJob(newJob);
      }
      else {
        // We know about this file
        changeJob(newJob);
        _fileToJobName.put(jobFile.getName(), newJob.getJobName());
      }
    } catch (IOException e) {
      getLog().error("Failed to create job for path: " + pathEvent.context());
    }
  }


  private void addJobFromFile(File jobFile) {
    if (! isJobFile(jobFile)) {
      return;
    }
    DefaultGobblinJobImpl newJob;
    try {
      String jobFileName = jobFile.getName();
      newJob = DefaultGobblinJobImpl.createFromJobFile(jobFile);
      // Account for a race condition where a file may appear between the time we start the
      // watcher and we scan the directory.
      if (_fileToJobName.putIfAbsent(jobFileName, newJob.getJobName()) == null) {
        addJob(newJob);
      }
    } catch (IOException e) {
      getLog().error("Failed to create job for path: " + jobFile);
    }
  }

  public void removeJobForFile(WatchEvent<Path> pathEvent) {
    File jobFile = pathEvent.context().toFile();
    if (! isJobFile(jobFile)) {
      return;
    }
    String fileName = jobFile.getName();
    String jobName = _fileToJobName.get(fileName);
    if (null != jobName) {
      getLog().warn("Unknown job file: " + pathEvent.count());
    }
    else {
      dropJob(jobName);
    }

  }

}
