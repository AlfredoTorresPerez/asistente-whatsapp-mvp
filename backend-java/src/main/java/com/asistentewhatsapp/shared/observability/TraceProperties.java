package com.asistentewhatsapp.shared.observability;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.observability.method-tracing")
public class TraceProperties {

    private boolean enabled = true;
    private boolean logArguments = true;
    private boolean logResult = false;
    private int maxPayloadLength = 600;
    private long slowExecutionThresholdMs = 1500;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isLogArguments() {
        return logArguments;
    }

    public void setLogArguments(boolean logArguments) {
        this.logArguments = logArguments;
    }

    public boolean isLogResult() {
        return logResult;
    }

    public void setLogResult(boolean logResult) {
        this.logResult = logResult;
    }

    public int getMaxPayloadLength() {
        return maxPayloadLength;
    }

    public void setMaxPayloadLength(int maxPayloadLength) {
        this.maxPayloadLength = maxPayloadLength;
    }

    public long getSlowExecutionThresholdMs() {
        return slowExecutionThresholdMs;
    }

    public void setSlowExecutionThresholdMs(long slowExecutionThresholdMs) {
        this.slowExecutionThresholdMs = slowExecutionThresholdMs;
    }
}
