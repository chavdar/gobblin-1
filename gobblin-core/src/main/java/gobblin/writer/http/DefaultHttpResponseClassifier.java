package gobblin.writer.http;

import org.apache.http.HttpResponse;

import com.google.common.base.Optional;

import gobblin.annotation.Alias;
import gobblin.annotation.Alpha;

/**
 * A simple implementation of a SimpleHttpResponseClassifier
 * TODO Make this configurable
 */
@Alpha
@Alias(value="default")
public class DefaultHttpResponseClassifier implements HttpResponseClassifier {

  private Optional<ResponseClassifierMetrics> _metrics;

  public DefaultHttpResponseClassifier() {
    this(Optional.<ResponseClassifierMetrics>absent());
  }

  public DefaultHttpResponseClassifier(ResponseClassifierMetrics metrics) {
    this(Optional.of(metrics));
  }

  public DefaultHttpResponseClassifier(Optional<ResponseClassifierMetrics> metrics) {
    _metrics = metrics;
  }

  @Override
  public ResponseAction classify(HttpResponse response) {
    final int statusCode = response.getStatusLine().getStatusCode();
    ResponseAction res = ResponseAction.FAIL;
    if (statusCode < 200) {
      res = ResponseAction.LOG_WARN;
    }
    else if (statusCode < 300) {
      res = ResponseAction.ACCEPT;
    }
    else if (statusCode < 400) {
      res = ResponseAction.LOG_WARN;
    }
    else if (statusCode < 500) {
      res = ResponseAction.LOG_WARN;
    }
    else if (statusCode < 600) {
      res = ResponseAction.RETRY;
    }
    if (_metrics.isPresent()) {
      _metrics.get().count(res, 1);
    }

    return res;
  }

  @Override
  public void setMetrics(Optional<ResponseClassifierMetrics> metrics) {
    _metrics = metrics;
  }

}
