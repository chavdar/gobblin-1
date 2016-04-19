package gobblin.writer.http;

/** How to handle the HTTP responses. Used in conjunction with {@link HttpResponseClassifier} */
public enum ResponseAction {
  /** Accept the response as successful. */
  ACCEPT,
  /** Write the record to an error storage and move on. */
  STASH,
  /** Write a warning log and move on. */
  LOG_WARN,
  /** Ignore the record. */
  IGNORE,
  /** Retry writing the record */
  RETRY,
  /** Fail writing the record and throw an exception */
  FAIL
}