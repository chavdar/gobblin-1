/*
 * Copyright (C) 2014-2016 LinkedIn Corp. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use
 * this file except in compliance with the License. You may obtain a copy of the
 * License at  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed
 * under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 * CONDITIONS OF ANY KIND, either express or implied.
 */
package gobblin.runtime.api;

import java.util.concurrent.TimeoutException;


/**
 * Schedules gobblin jobs for execution
 *
 * <p>Implementations: SynchronousJobScheduler, ThreadPoolJobScheduler, QuartzJobScheduler,
 * */
public interface GobblinJobScheduler {

  void scheduleJob(JobLauncher job);

  /** True if the job is scheduled to run or is running */
  boolean isJobScheduled(String jobName);

  /** True if the job is running */
  boolean isJobRunning(String jobName);

  /**
   * Un-schedules the job
   * @param jobName             the name of the job to un-schedule
   * @param stopTimeoutMs   number of milliseconds to wait for the job to complete if the job is
   *                        running before canceling it; no effect otherwise;
   * @param abortTimeoutMs  number of milliseconds to wait for the job to die after canceling it
   *                        before aborting the call with a {@link TimeoutException}
   * @throws TimeoutException if the job was running and failed to stop within the specified timeout
   * @throws InterruptedException if the kill or abort wait were interrupted
   */
  void unscheduleJob(String jobName, long stopTimeoutMs, long abortTimeoutMs)
       throws TimeoutException, InterruptedException;

  /**
   * Shuts down the scheduler.
   * @param stopTimeoutMs   number of milliseconds to wait for all running jobs to complete
   *                        running before canceling them;
   * @param abortTimeoutMs  number of milliseconds to wait for the cancelled jobs to die
   *                        before aborting the call with a {@link TimeoutException}
   * @throws TimeoutException if the job was running and failed to stop within the specified timeout
   * @throws InterruptedException if the kill or abort wait were interrupted
   */
  void shutdown(long stopTimeoutMs, long abortTimeoutMs)
       throws TimeoutException, InterruptedException;
}
