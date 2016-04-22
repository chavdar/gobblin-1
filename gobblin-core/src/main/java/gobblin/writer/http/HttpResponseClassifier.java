/**
 *
 */
package gobblin.writer.http;

import org.apache.http.HttpResponse;

import com.google.common.base.Optional;

/**
 * Determines the action to take with respect to an HttpResonse when writing to an HTTP server.
 */
public interface HttpResponseClassifier {
  ResponseAction classify(HttpResponse response);
  void setMetrics(Optional<ResponseClassifierMetrics> metrics);
}
