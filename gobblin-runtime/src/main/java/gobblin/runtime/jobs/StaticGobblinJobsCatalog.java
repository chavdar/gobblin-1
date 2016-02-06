/**
 *
 */
package gobblin.runtime.jobs;

import gobblin.runtime.api.GobblinJob;

import org.slf4j.Logger;

import com.google.common.base.Optional;

/**
 * A catalog for an unmodifiable set of immutable {@link GobblinJob} instances.
 */
public class StaticGobblinJobsCatalog extends AbstractGobblinJobsCatalog {

  /** Create a catalog for a single job */
  public StaticGobblinJobsCatalog(Optional<Logger> log, GobblinJob job) {
    super(log);
    addJob(job);
  }

  public StaticGobblinJobsCatalog(Optional<Logger> log, GobblinJob... jobs) {
    super(log);
    for (GobblinJob job: jobs) {
      addJob(job);
    }
  }

}
