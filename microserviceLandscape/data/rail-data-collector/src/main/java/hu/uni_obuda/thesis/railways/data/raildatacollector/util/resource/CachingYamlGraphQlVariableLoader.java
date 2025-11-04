package hu.uni_obuda.thesis.railways.data.raildatacollector.util.resource;

import lombok.RequiredArgsConstructor;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@RequiredArgsConstructor
public class CachingYamlGraphQlVariableLoader {

    private final YamlGraphQlVariableLoader delegate;
    private final ConcurrentMap<String, DefaultGraphQlVariables> cache = new ConcurrentHashMap<>();

    public DefaultGraphQlVariables loadForDocument(String documentName) {
        return cache.computeIfAbsent(documentName, key -> {
            DefaultGraphQlVariables value = delegate.loadForDocument(key);
            if (value == null) {
                throw new IllegalStateException("Non-caching " + YamlGraphQlVariableLoader.class.getSimpleName() + "returned null for " + documentName);
            }
            return value;
        });
    }

}
