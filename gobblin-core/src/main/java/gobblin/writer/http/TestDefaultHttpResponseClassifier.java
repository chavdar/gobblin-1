package gobblin.writer.http;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.ProtocolVersion;
import org.apache.http.message.BasicHttpResponse;
import org.testng.Assert;
import org.testng.annotations.Test;

import gobblin.metrics.MetricContext;
import gobblin.metrics.Tag;

/** Unit tests for {@link DefaultHttpResponseClassifier} */
public class TestDefaultHttpResponseClassifier {

  @Test
  public void testDefault() {
    MetricContext ctx = MetricContext.builder("test")
        .addTag(Tag.fromString("tag1:value1"))
        .build();
    ResponseClassifierMetrics metrics = new ResponseClassifierMetrics(ctx);
    DefaultHttpResponseClassifier classifier = new DefaultHttpResponseClassifier(metrics);
    final ProtocolVersion HTTP_1_1 = new ProtocolVersion("http",1, 1);
    HttpResponse resp = new BasicHttpResponse(HTTP_1_1, HttpStatus.SC_OK, "OK");

    Assert.assertEquals(ResponseAction.ACCEPT, classifier.classify(resp));
    Assert.assertEquals(ctx.meter(ResponseClassifierMetrics.meterName(ResponseAction.ACCEPT)).getCount(),
                        1L);
    Assert.assertEquals(ResponseAction.ACCEPT, classifier.classify(resp));
    Assert.assertEquals(ctx.meter(ResponseClassifierMetrics.meterName(ResponseAction.ACCEPT)).getCount(),
                        2L);

    resp = new BasicHttpResponse(HTTP_1_1, HttpStatus.SC_INTERNAL_SERVER_ERROR,
                                "Internal Server Error");
    Assert.assertEquals(ResponseAction.RETRY, classifier.classify(resp));
    Assert.assertEquals(ctx.meter(ResponseClassifierMetrics.meterName(ResponseAction.ACCEPT)).getCount(),
                        2L);
    Assert.assertEquals(ctx.meter(ResponseClassifierMetrics.meterName(ResponseAction.RETRY)).getCount(),
                        1L);
  }

}
