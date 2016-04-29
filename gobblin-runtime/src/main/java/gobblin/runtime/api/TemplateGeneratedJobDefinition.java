package gobblin.runtime.api;

import com.typesafe.config.Config;

public interface TemplateGeneratedJobDefinition extends JobDefinition {

  TemplateJobDefintion getTemplate();

  Config getTemplateValues();

}
