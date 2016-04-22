package gobblin.writer.http;

import java.io.IOException;

import org.apache.http.protocol.HTTP;
import org.testng.Assert;
import org.testng.annotations.Test;

import gobblin.configuration.State;

/** Unit tests for {@link HttpWriter} */
public class TestHttpWriter {

  @Test
  /** Tests the config generation*/
  public void testConfig() throws IOException {
    State testState = new State();
    testState.setProp(HttpWriter.CONF_PREFIX + HttpWriter.KEEP_ALIVE_ENABLED_KEY, false);
    testState.setProp(HttpWriter.CONF_PREFIX + HttpWriter.HTTPCLIENT_CONF_KEY + ".http.maxConnections", 10);

    try(HttpWriter<String> stringWriter = new HttpWriter<>(testState)) {
      Assert.assertEquals(stringWriter.getHttpRequestTemplate().getHeaders(HTTP.CONN_DIRECTIVE)[0],
                          HTTP.CONN_KEEP_ALIVE);
      Assert.assertEquals(System.getProperty("http.maxConnections"), "10");
    }
  }

  @Test
  public void testWriteImpl() throws Exception {

  }
}
