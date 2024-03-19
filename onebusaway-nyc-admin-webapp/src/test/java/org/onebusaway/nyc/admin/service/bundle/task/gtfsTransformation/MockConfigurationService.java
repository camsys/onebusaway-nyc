package org.onebusaway.nyc.admin.service.bundle.task.gtfsTransformation;

import org.onebusaway.nyc.util.configuration.ConfigurationService;

import java.util.Map;

/**
 * Mock out configuration service: simply return the defaults.
 */
public class MockConfigurationService implements ConfigurationService {
    @Override
    public String getConfigurationValueAsString(String configurationItemKey, String defaultValue) {
        return defaultValue;
    }

    @Override
    public Float getConfigurationValueAsFloat(String configurationItemKey, Float defaultValue) {
        return defaultValue;
    }

    @Override
    public Integer getConfigurationValueAsInteger(String configurationItemKey, Integer defaultValue) {
        return defaultValue;
    }

    @Override
    public Boolean getConfigurationValueAsBoolean(String configurationItemKey, Boolean defaultValue) {
        return defaultValue;
    }

    @Override
    public void setConfigurationValue(String component, String configurationItemKey, String value) throws Exception {
        throw new IllegalArgumentException("not implemented");
    }

    @Override
    public Map<String, String> getConfiguration() {
        throw new IllegalArgumentException("not implemented");
    }
}
