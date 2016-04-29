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

import gobblin.runtime.JobLauncherFactory;

import com.typesafe.config.Config;

/**
 * The context for a Gobblin process.
 *
 * <p>Implementations: CliRuntimeEnvironment, AzkabanRuntimeEnvironment,
 *    StandaloneRuntimeEnvironment, OozieRuntimeEnvironment, InMemoryRuntimeEnvironment
 *
 * <p>Lifecycle of a runtime:
 * <ol>
 *  <li>GobblinStarter created and run
 *  <li>GobblinStarter instantiates a {@link RuntimeEnvironment}
 *  <li>Register GobblinJobCatalog listener
 *    <ul>
 *      <li>For every new job, schedule job
 *      <li>For every removed job, unschedule job
 *      <li>For every changed job, unschedule and then schedule again
 *    </ul>
 * </ol>
 */
public interface RuntimeEnvironment {

  Config getStartupConfig();

  /** Gobblin Job definitions */
  JobCatalog getJobCatalog();

  /** Used to schedule jobs */
  GobblinJobScheduler getJobScheduler();

  JobLauncherFactory getJobLauncherFactory();
}
