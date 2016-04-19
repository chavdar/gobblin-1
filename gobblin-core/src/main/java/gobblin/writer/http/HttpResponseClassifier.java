/**
 *
 */
package gobblin.writer.http;

import org.apache.http.HttpResponse;

/**
 * Determines the action to take with respect to an HttpResonse when writing to an HTTP server.
 */
public interface HttpResponseClassifier {
  ResponseAction classify(HttpResponse response);
}
