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

import java.util.Collection;

/**
 * Access to Gobblin job definitions
 *
 *  <p>Implementations: FileListJobCatalog, PosixDirectoryJobCatalog,
 *     HadoopPathListJobCatalog, HadoopFSDirectoryJobCatalog,
 *     JdbcJobCatalog, InMemoryJobCatalog, GlobMonitoringJobCatalogDecorator,
 *     KafkaEventMonitoringJobCatalogDecorator
 * */
public interface JobCatalog {

  /**
   * On registration, the listener will be notified about all existing jobs using
   * {@link JobCatalogListener#onJobAdded(JobDefinition)}
   */
  void registerListener(JobCatalogListener newListener);

  void unregisterListener(JobCatalogListener newListener);

  Collection<JobDefinition> getAllJobs();

}
