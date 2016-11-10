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

package gobblin.util.executors;

import java.util.Iterator;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.Test;

import com.google.common.base.Predicate;
import com.google.common.util.concurrent.ThreadFactoryBuilder;

import gobblin.testing.AssertWithBackoff;


public class IteratorExecutorTest {

  private final AtomicInteger nextCallCount = new AtomicInteger(0);
  private final AtomicInteger completedCount = new AtomicInteger(0);
  private final Logger log = LoggerFactory.getLogger(IteratorExecutorTest.class);

  @Test
  public void test() throws Exception {

    TestIterator iterator = new TestIterator(5);

    final IteratorExecutor<Void> executor = new IteratorExecutor<>(iterator, 2, new ThreadFactoryBuilder().build());

    log.info("Starting executor");
    ExecutorService singleThread = Executors.newSingleThreadExecutor();
    final Future<?> future = singleThread.submit(new Runnable() {
      @Override
      public void run() {
        try {
          executor.execute();
        } catch (InterruptedException ie) {
          throw new RuntimeException(ie);
        }
      }
    });

    log.info("Only two threads, so exactly two tasks retrieved");
    verify(2, 0);
    Assert.assertFalse(future.isDone());
    Assert.assertTrue(iterator.hasNext());

    log.info("end one of the tasks");
    iterator.endOneCallable();

    log.info("three tasks retrieved, one completed");
    verify(3, 1);
    Assert.assertFalse(future.isDone());
    Assert.assertTrue(iterator.hasNext());

    log.info("end two tasks");
    iterator.endOneCallable();
    iterator.endOneCallable();

    log.info("5 (all) tasks retrieved, 3 completed");
    verify(5, 3);
    Assert.assertFalse(future.isDone());
    Assert.assertFalse(iterator.hasNext());

    log.info("end two tasks");
    iterator.endOneCallable();
    iterator.endOneCallable();

    log.info("all tasks completed, check future is done");
    verify(5, 5);

    AssertWithBackoff.assertTrue(new Predicate<Void>() {
      @Override public boolean apply(Void input) {
        return future.isDone();
      }
    }, 10000, "future done", log, 2, 1000);

    Assert.assertTrue(future.isDone());

    log.info("done.");
  }

  /**
   * Verify exactly retrieved tasks have been retrieved, and exactly completed tasks have completed.
   * @throws Exception
   */
  private void verify(final int retrieved, final int completed) throws Exception {
    AssertWithBackoff.assertTrue(new Predicate<Void>() {
      @Override public boolean apply(Void input) {
        log.debug("Waiting for {}={} and {}={}", retrieved, nextCallCount.get(), completed,
                  completedCount.get());
        return (nextCallCount.get() == retrieved && completedCount.get() == completed);
      }
    }, 30000, "Waiting for callcount", log, 1.5, 1000);
//    for (int i = 0; i < 20; i++) {
//      if (nextCallCount.get() >= retrieved || completedCount.get() >= completed) {
//        break;
//      }
//      Thread.sleep(100);
//    }
//    Thread.sleep(100);
//    Assert.assertEquals(nextCallCount.get(), retrieved);
//    Assert.assertEquals(completedCount.get(), completed);
  }

  /**
   * Runnable iterator. Runnables block until a call to {@link #endOneCallable()}.
   */
  private class TestIterator implements Iterator<Callable<Void>> {

    private final int maxCallables;
    private Lock lock = new ReentrantLock();
    private Condition condition = lock.newCondition();

    public TestIterator(int maxCallables) {
      this.maxCallables = maxCallables;
    }

    @Override
    public boolean hasNext() {
      return nextCallCount.get() < maxCallables;
    }

    @Override
    public Callable<Void> next() {
      nextCallCount.incrementAndGet();

      return new Callable<Void>() {
        @Override
        public Void call()
            throws Exception {

          log.debug("Blocking at {}", nextCallCount.get());
          lock.lock();
          condition.await();
          lock.unlock();

          completedCount.incrementAndGet();
          log.debug("Completed {}", completedCount.get());
          return null;
        }
      };
    }

    public void endOneCallable() {
      log.debug("End one {}", nextCallCount.get());
      lock.lock();
      condition.signal();
      lock.unlock();
    }

    @Override
    public void remove() {
      throw new UnsupportedOperationException();
    }
  }

}
