/**
 *
 */
package gobblin.writer.http;

import java.util.IdentityHashMap;
import java.util.Map;

import com.codahale.metrics.Meter;
import com.google.common.annotations.VisibleForTesting;

import gobblin.metrics.MetricContext;

/**
 * A class to keep track of metrics for a response classifier.
 */
public class ResponseClassifierMetrics {
  public static final String METRIC_NAME_PREFIX =
      ResponseClassifierMetrics.class.getPackage().getName() + ".";
  private final MetricContext _ctx;
  private final Map<ResponseAction, Meter> _meters;

  public ResponseClassifierMetrics(MetricContext ctx) {
    _ctx = ctx.childBuilder(ResponseClassifierMetrics.class.getSimpleName()).build();
    _meters = new IdentityHashMap<>();
    for (ResponseAction value: ResponseAction.values()) {
      _meters.put(value, _ctx.meter(meterName(value)));
    }
  }

  public void count(ResponseAction action, int numRecords) {
    _meters.get(action).mark(numRecords);
  }

  @Override
  public String toString() {
    return "HttpResponseClassifierMetrics[" + _ctx + "]:" + _meters;
  }

  @VisibleForTesting
  static String meterName(ResponseAction value) {
    return METRIC_NAME_PREFIX + value.name();
  }

}
