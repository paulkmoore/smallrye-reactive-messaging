package io.smallrye.reactive.messaging.camel;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.camel.ExchangePattern;

public class OutgoingExchangeMetadata {

    private Map<String, Object> properties = new ConcurrentHashMap<>();
    private ExchangePattern pattern;

    public OutgoingExchangeMetadata putProperty(String name, Object value) {
        properties.put(name, value);
        return this;
    }

    public OutgoingExchangeMetadata removeProperty(String name) {
        properties.remove(name);
        return this;
    }

    public Map<String, Object> getProperties() {
        return properties;
    }

    public OutgoingExchangeMetadata setExchangePattern(ExchangePattern pattern) {
        this.pattern = pattern;
        return this;
    }

    public ExchangePattern getExchangePattern() {
        return pattern;
    }

}
