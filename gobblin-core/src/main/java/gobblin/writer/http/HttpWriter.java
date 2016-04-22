package gobblin.writer.http;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.apache.commons.httpclient.HttpClient;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.protocol.HTTP;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigValue;

import gobblin.annotation.Alpha;
import gobblin.config.ConfigBuilder;
import gobblin.configuration.State;
import gobblin.instrumented.writer.InstrumentedDataWriter;

/**
 * Writes record to an HTTP server.
 * Internally, the implementation uses {@link HttpClient}. Its configuration can be customizes using
 * the {@link #HTTPCLIENT_CONF_KEY} config object. The properties inside that object will be passed
 * using {@link HttpClientBuilder#useSystemProperties()}.
 * */
@Alpha
public class HttpWriter<D> extends InstrumentedDataWriter<D> {
  public static final String CONF_PREFIX = "gobblin.writer.http.";

  public static final String HTTPCLIENT_CONF_KEY = "httpclient_conf";
  public static final String KEEP_ALIVE_ENABLED_KEY = "keep_alive_enabled";
  public static final String METHOD_KEY = "method";
  public static final String SERVERS_KEY = "servers";
  public static final String PUBLISH_PATH_KEY = "publish_path";

  public static final Config DEFAULTS =
      ConfigBuilder.create(HttpWriter.class.getName() + " defaults")
                   .addPrimitive(KEEP_ALIVE_ENABLED_KEY, Boolean.TRUE)
                   .addPrimitive(METHOD_KEY, HttpPost.METHOD_NAME)
                   .addList(SERVERS_KEY, Arrays.asList("http://localhost"))
                   .addPrimitive(PUBLISH_PATH_KEY, "/")
                   .build();

  protected final CloseableHttpClient _client;
  protected final HttpEntityEnclosingRequestBase _httpRequestTemplate;
  protected final Logger _log;
  protected final boolean _debugLogEnabled;
  private final List<String> _httpServers;
  private final String _httpMethod;
  private final String _publishPath;
  private final HttpResponseClassifier _responseClassifier;

  private int _curHttpServerIdx;
  private HttpHost _curHttpHost;
  private URI _publishUrl;
  private long _numRecordsWritten = 0;
  private long _numBytesWritten = 0;
  private Optional<ResponseClassifierMetrics> _responseMetrics;


  /**
   * Standard constructor to be used in Gobblin Jobs
   */
  public HttpWriter(State state) {
    this(state,
         Optional.<Logger>absent(),
         Optional.<HttpResponseClassifier>absent(),
         Optional.<CloseableHttpClient>absent());
  }

  private static HttpResponseClassifier createResponseClassifierFromConfig(Config cfg) {
    //FIXME
    return new DefaultHttpResponseClassifier();
  }

  /**
   * Creates a new instance of the HTTP Writer
   * @param state               the job state
   * @param log                 logger inject; if absent, a default static one will be used
   * @param responseClassifier  response classifier inject; if not specified, one will be created
   *                            from the state
   * @param httpClientInject    HTTP client inject; if not specified, a standard one will be used
   */
  HttpWriter(State state, Optional<Logger> log, Optional<HttpResponseClassifier> responseClassifier,
             Optional<CloseableHttpClient> httpClientInject) {
    super(state);
    _log = log.isPresent() ? log.get() : LoggerFactory.getLogger(HttpWriter.class);
    _debugLogEnabled = _log.isDebugEnabled();
    Config cfg = convertStateToConfig(state).withFallback(DEFAULTS);
    if (_debugLogEnabled) {
      _log.debug("Config: " + cfg);
    }
    setSystemPropertiesForHttpClient(cfg);

    _httpServers = cfg.getStringList(SERVERS_KEY);
    Preconditions.checkArgument(_httpServers.size() > 0, "No HTTP servers specified");
    setCurServerIdx((new Random()).nextInt(_httpServers.size()));

    _httpMethod = cfg.getString(METHOD_KEY).toUpperCase();
    Preconditions.checkArgument(HttpPost.METHOD_NAME.equals(_httpMethod)
                                || HttpPut.METHOD_NAME.equals(_httpMethod),
                                "Only PUT or POST are supported: " + _httpMethod);

    _publishPath = cfg.getString(PUBLISH_PATH_KEY);
    try {
      _publishUrl = new URI(_publishPath);
    }
    catch (URISyntaxException e) {
      throw new RuntimeException("Unable to create publish URL: " + e, e);
    }

    _client = httpClientInject.isPresent()
        ? httpClientInject.get()
        : HttpClientBuilder.create().disableCookieManagement().useSystemProperties().build();
    _httpRequestTemplate = createHttpRequestTemplate(cfg);

    _responseMetrics = isInstrumentationEnabled()
        ? Optional.of(new ResponseClassifierMetrics(getMetricContext()))
        : Optional.<ResponseClassifierMetrics>absent();
    _responseClassifier = responseClassifier.isPresent()
        ? responseClassifier.get()
        : createResponseClassifierFromConfig(cfg);
    _responseClassifier.setMetrics(_responseMetrics);
  }

  private void setCurServerIdx(int httpServerIdx) {
    _curHttpServerIdx = httpServerIdx;
    _curHttpHost = new HttpHost(_httpServers.get(_curHttpServerIdx));
  }

  @VisibleForTesting
  HttpEntityEnclosingRequestBase createHttpRequestTemplate(Config cfg) {
    HttpEntityEnclosingRequestBase res = null;
    if (HttpPost.METHOD_NAME.equals(_httpMethod)) {
      res = new HttpPost(_publishUrl);
    }
    else if (HttpPut.METHOD_NAME.equals(_httpMethod)) {
      res = new HttpPut(_publishUrl);
    }
    else {
      throw new RuntimeException("Unexpected HTTP method: " + _httpMethod);
    }
    if (cfg.getBoolean(KEEP_ALIVE_ENABLED_KEY)) {
      res.addHeader(HTTP.CONN_DIRECTIVE, HTTP.CONN_KEEP_ALIVE);
    }
    try {
      return (HttpEntityEnclosingRequestBase)res.clone();
    }
    catch (CloneNotSupportedException e) {
      throw new RuntimeException("Unable to create request template:" + e, e);
    }
  }

  protected String getPublishUri(int serverIdx) {
    return _httpServers.get(serverIdx) + _publishPath;
  }

  private static void setSystemPropertiesForHttpClient(Config cfg) {
    Config httpClientConf = cfg.getConfig(HTTPCLIENT_CONF_KEY);
    for (Map.Entry<String, ConfigValue> confEntry: httpClientConf.entrySet()) {
      System.setProperty(confEntry.getKey(), confEntry.getValue().unwrapped().toString());
    }
  }

  @VisibleForTesting
  static Config convertStateToConfig(State state) {
    Config config = ConfigBuilder.create().loadProps(state.getProperties(), CONF_PREFIX).build();
    return config;
  }


  @Override
  public void cleanup() throws IOException {
    _client.close();
  }

  @Override
  public long recordsWritten() {
    return _numRecordsWritten;
  }

  @Override
  public long bytesWritten() throws IOException {
    return _numBytesWritten;
  }

  @Override
  public void writeImpl(D record) throws IOException {
    if (null == record) {
      _log.debug("Empty record");
      return;
    }
    try {
      HttpEntityEnclosingRequestBase req = (HttpEntityEnclosingRequestBase)_httpRequestTemplate.clone();
      req = initializeRequest(req, record);
      Optional<HttpEntity> recordEntity = createRecordEntity(record);
      if (recordEntity.isPresent()) {
        req.setEntity(recordEntity.get());
      }

      HttpResponse resp = sendRequestWithRoundRobinRetry(req);
      if (null == resp) {
        // Given up retries
        throw new IOException("Unable to send request for " + record);
      }
      ResponseAction respAction = _responseClassifier.classify(resp);
      String failMessage = null;
      switch (respAction) {
        case ACCEPT: break;
        case LOG_WARN: _log.warn("Failed to send record: " + record); break;
        case IGNORE: break;
        case RETRY:
          failMessage = "Ran out of retries trying to send the record: " + record;
          break;
        case STASH:
          failMessage = "STASH not supported for record: " + record;
          break;
        case FAIL:
          failMessage = "Unrecoverable failure trying to send record: " + record;
          break;
        default:
          failMessage = "Unknown response action " + respAction + " for record: " + record ;
          break;
      }
      if (null != failMessage) {
        _log.error(failMessage);
        throw new RuntimeException(failMessage);
      }
    }
    catch (CloneNotSupportedException e) {
      // This should not happen because createHttpRequestTemplate() uses it.
      throw new Error("Cloning of requests not supported!");
    }
  }

  private HttpResponse sendRequestWithRoundRobinRetry(HttpEntityEnclosingRequestBase req) {
    HttpResponse resp = null;
    final int saveHttpServerIdx = _curHttpServerIdx;
    boolean retry = false;
    do {
      try {
        resp = _client.execute(_curHttpHost, req);
        if (ResponseAction.RETRY == _responseClassifier.classify(resp)) {
          switchServer();
          retry = _curHttpServerIdx != saveHttpServerIdx;
        }
        else {
          retry = false;
        }
      }
      catch (IOException ioe) {
        _log.error("HTTP request error: " + ioe, ioe);
        switchServer();
      }
    } while (retry);
    return resp;
  }

  private void switchServer() {
    setCurServerIdx((_curHttpServerIdx + 1) % _httpServers.size());
  }

  /**
   * Generate an HttpEntity representing the record to be sent as part of the PUT/POST request. The
   * default implementation sends record.toString() but this can be over-ridden.
   * @param record      the record to be sent
   * @return the resulting HttpEntity or {@link Optional#absent()} if no entity is to be sent.
   */
  protected Optional<HttpEntity> createRecordEntity(D record) {
    try {
      return Optional.<HttpEntity>of(new StringEntity(record.toString()));
    }
    catch (UnsupportedEncodingException e) {
      throw new RuntimeException("Unable to create record HTTP entity " + e, e);
    }
  }

  /**
   * Override this method to customize the request sent to the HTTP server
   * @param req     the current request which has been initialized with the URL to the current
   *                server and any HTTP connection parameters (e.g. keep-alive)
   * @param record  the record to be sent over the request
   * @return        the modified req object or a new request object
   */
  protected HttpEntityEnclosingRequestBase initializeRequest(HttpEntityEnclosingRequestBase req,
                                                             D record) {
    return req;
  }

  public Logger getLog() {
    return _log;
  }

  @VisibleForTesting
  HttpEntityEnclosingRequestBase getHttpRequestTemplate() {
    return _httpRequestTemplate;
  }
}
