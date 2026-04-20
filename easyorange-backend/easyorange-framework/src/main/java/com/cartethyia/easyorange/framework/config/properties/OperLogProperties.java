package com.cartethyia.easyorange.framework.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "operlog")
public class OperLogProperties {

    private boolean enabled = true;

    private LogLevel logLevel = LogLevel.FULL;

    private boolean saveRequestData = true;

    private boolean saveResponseData = false;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public LogLevel getLogLevel() {
        return logLevel;
    }

    public void setLogLevel(LogLevel logLevel) {
        this.logLevel = logLevel;
    }

    public boolean isSaveRequestData() {
        return saveRequestData;
    }

    public void setSaveRequestData(boolean saveRequestData) {
        this.saveRequestData = saveRequestData;
    }

    public boolean isSaveResponseData() {
        return saveResponseData;
    }

    public void setSaveResponseData(boolean saveResponseData) {
        this.saveResponseData = saveResponseData;
    }

    public enum LogLevel {
        NONE,
        MINIMAL,
        FULL
    }
}
