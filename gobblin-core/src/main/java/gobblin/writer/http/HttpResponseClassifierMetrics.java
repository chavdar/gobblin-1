/**
 *
 */
package gobblin.writer.http;

import java.util.IdentityHashMap;
import java.util.Map;

import com.codahale.metrics.Meter;

import gobblin.metrics.MetricContext;

/**
 * A class to keep track of metrics for a response classifier.
 */
public class HttpResponseClassifierMetrics {
  public static final String METRIC_NAME_PREFIX =
      HttpResponseClassifierMetrics.class.getPackage().getName() + ".";
  private final MetricContext _ctx;
  private final Map<ResponseAction, Meter> _meters;

  public HttpResponseClassifierMetrics(MetricContext ctx) {
    _ctx = ctx.childBuilder(HttpResponseClassifierMetrics.class.getSimpleName()).build();
    _meters = new IdentityHashMap<>();
    for (ResponseAction value: ResponseAction.values()) {
      _meters.put(value, _ctx.meter(METRIC_NAME_PREFIX + value.name()));
    }
  }

  public void count(ResponseAction action, int numRecords) {
    _meters.get(action).mark(numRecords);
  }

  @Override
  public String toString() {
    return "HttpResponseClassifierMetrics[" + _ctx + "]:" + _meters;
  }

}
