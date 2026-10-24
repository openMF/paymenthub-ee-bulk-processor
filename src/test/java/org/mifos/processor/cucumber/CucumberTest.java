package org.mifos.processor.cucumber;

import static io.cucumber.junit.platform.engine.Constants.GLUE_PROPERTY_NAME;
import static io.cucumber.junit.platform.engine.Constants.PLUGIN_PROPERTY_NAME;

import org.junit.platform.suite.api.ConfigurationParameter;
import org.junit.platform.suite.api.IncludeEngines;
import org.junit.platform.suite.api.SelectClasspathResource;
import org.junit.platform.suite.api.Suite;

/**
 * Runs the Cucumber scenarios in src/test/resources/features as part of {@code ./gradlew test}. The Spring context
 * comes from {@link CucumberContext}.
 */
@Suite
@IncludeEngines("cucumber")
@SelectClasspathResource("features")
@ConfigurationParameter(key = GLUE_PROPERTY_NAME, value = "org.mifos.processor.cucumber")
@ConfigurationParameter(key = PLUGIN_PROPERTY_NAME, value = "pretty")
public class CucumberTest {}
