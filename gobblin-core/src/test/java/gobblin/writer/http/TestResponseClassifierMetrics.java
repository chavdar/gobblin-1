package gobblin.writer.http;

import org.testng.Assert;
import org.testng.annotations.Test;

import gobblin.metrics.MetricContext;
import gobblin.metrics.Tag;

/** Unit tests for {@link ResponseClassifierMetrics} */
public class TestResponseClassifierMetrics {

  @Test
  public void testCount() {
    MetricContext ctx = MetricContext.builder("test")
        .addTag(new Tag<String>("tag1", "value1"))
        .build();
    ResponseClassifierMetrics m = new ResponseClassifierMetrics(ctx);
    for (ResponseAction action: ResponseAction.values()) {
      m.count(action, action.ordinal() + 1);
    }
    for (ResponseAction action: ResponseAction.values()) {
      long count = ctx.meter(ResponseClassifierMetrics.meterName(action)).getCount();
      Assert.assertEquals(count, action.ordinal() + 1, "action=" + action);
    }
  }
}
