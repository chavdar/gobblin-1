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
package gobblin.runtime.std;

import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.Test;

import gobblin.runtime.JobState.RunningState;
import gobblin.runtime.api.JobExecutionState;
import gobblin.runtime.api.JobExecutionStateListener;
import gobblin.runtime.api.JobSpec;

/**
 * Unit tests for {@link JobExecutionStateListeners}.
 */
public class TestJobExecutionStateListeners {

  @Test
  public void testHappyPath() {
    final Logger log = LoggerFactory.getLogger(getClass() + ".testHappyPath");
    JobExecutionStateListeners listeners = new JobExecutionStateListeners(log);

    JobSpec js1 = JobSpec.builder("gobblin:test/job").build();
    JobExecutionUpdatable je1 = JobExecutionUpdatable.createFromJobSpec(js1);

    JobExecutionStateListener l1 = Mockito.mock(JobExecutionStateListener.class);
    JobExecutionStateListener l2 = Mockito.mock(JobExecutionStateListener.class);
    JobExecutionStateListener l3 = Mockito.mock(JobExecutionStateListener.class);

    listeners.registerStateListener(l1);

    JobExecutionState state = new JobExecutionState(js1, je1);
    state.setStatus(RunningState.RUNNING);
    listeners.onStatusChange(state, RunningState.PENDING, RunningState.RUNNING);

    listeners.registerStateListener(l2);
    listeners.registerStateListener(l3);
    state.setStage("Stage1");
    listeners.onStageTransition(state, JobExecutionState.UKNOWN_STAGE, "Stage1");

    listeners.unregisterStateListener(l2);
    listeners.onMetadataChange(state, "key", "oldValue", "newValue");

    Mockito.verify(l1).onStatusChange(Mockito.eq(state),
        Mockito.eq(RunningState.PENDING), Mockito.eq(RunningState.RUNNING));
    Mockito.verify(l1).onStageTransition(Mockito.eq(state),
        Mockito.eq(JobExecutionState.UKNOWN_STAGE), Mockito.eq("Stage1"));
    Mockito.verify(l1).onMetadataChange(Mockito.eq(state),
        Mockito.eq("key"), Mockito.eq("oldValue"), Mockito.eq("newValue"));
    Mockito.verify(l2).onStageTransition(Mockito.eq(state),
        Mockito.eq(JobExecutionState.UKNOWN_STAGE), Mockito.eq("Stage1"));
    Mockito.verify(l3).onStageTransition(Mockito.eq(state),
        Mockito.eq(JobExecutionState.UKNOWN_STAGE), Mockito.eq("Stage1"));
    Mockito.verify(l3).onMetadataChange(Mockito.eq(state),
        Mockito.eq("key"), Mockito.eq("oldValue"), Mockito.eq("newValue"));

    Mockito.verifyNoMoreInteractions(l1);
    Mockito.verifyNoMoreInteractions(l2);
    Mockito.verifyNoMoreInteractions(l3);
  }

}
