package gobblin.config;

import java.util.Arrays;
import java.util.Properties;

import org.testng.Assert;
import org.testng.annotations.Test;

import com.typesafe.config.Config;

/** Unit tests for {@link ConfigBuilder} */
public class TestConfigBuilder {

  @Test
  public void testLoadProps() {
    Properties props = new Properties();
    props.put("prefix1.prop1", "value11");
    props.put("prefix1.prop2", "12");
    props.put("prefix2.prop1", "value21");
    props.put("prefix3", "value31");

    Config conf1 = ConfigBuilder.create().loadProps(props, "prefix1.").build();
    Assert.assertEquals(conf1.getString("prop1"), "value11");
    Assert.assertEquals(conf1.getInt("prop2"), 12);
    Assert.assertFalse(conf1.hasPath("prefix1.prop1"));
    Assert.assertFalse(conf1.hasPath("prefix2.prop1"));
    Assert.assertFalse(conf1.hasPath("prefix3"));

    Config conf2 = ConfigBuilder.create().loadProps(props, "").build();
    Assert.assertEquals(conf2.getString("prefix1.prop1"), "value11");
    Assert.assertEquals(conf2.getInt("prefix1.prop2"), 12);
    Assert.assertEquals(conf2.getString("prefix2.prop1"), "value21");
    Assert.assertEquals(conf2.getString("prefix3"), "value31");

    try {
      ConfigBuilder.create().loadProps(props, "prefix").build();
      Assert.fail("RuntimeException expected");
    }
    catch (RuntimeException e) {
      Assert.assertTrue(e.toString().contains("should start with a character"));
    }

    try {
      ConfigBuilder.create().loadProps(props, "prefix3").build();
      Assert.fail("RuntimeException expected");
    }
    catch (RuntimeException e) {
      Assert.assertTrue(e.toString().contains("Illegal scoped property"));
    }
  }

  @Test
  public void testAdds() {
    Config cfg = ConfigBuilder.create("defaults")
        .addPrimitive("intValue", 1)
        .addPrimitive("stringValue", "blah")
        .addPrimitive("doubleValue", "0.5")
        .addPrimitive("booleanValue", Boolean.TRUE)
        .addList("listValue", Arrays.asList("item1", "item2", "item3"))
        .build();
    Assert.assertTrue(cfg.origin().description().contains("defaults"));
    Assert.assertEquals(cfg.getInt("intValue"), 1);
    Assert.assertEquals(cfg.getString("stringValue"), "blah");
    Assert.assertEquals(cfg.getDouble("doubleValue"), 0.5);
    Assert.assertEquals(cfg.getBoolean("booleanValue"), true);

    Assert.assertEquals(cfg.getStringList("listValue"), Arrays.asList("item1", "item2", "item3"));
  }
}
