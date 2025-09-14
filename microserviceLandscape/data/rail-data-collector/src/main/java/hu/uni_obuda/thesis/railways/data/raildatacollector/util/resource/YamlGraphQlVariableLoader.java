package hu.uni_obuda.thesis.railways.data.raildatacollector.util.resource;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import org.springframework.core.env.*;
import org.springframework.boot.env.YamlPropertySourceLoader;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RequiredArgsConstructor
public class YamlGraphQlVariableLoader {
    
    private final String classPathDirectory;

    public DefaultGraphQlVariables loadForDocument(String documentName) {
        try {
            return new DefaultGraphQlVariables(loadYamlFromClasspath(classPathDirectory + "/" + documentName));
        } catch (IOException e) {
            log.error("An IO Exception occurred: {}", e.getMessage());
            throw new RuntimeException(e);
        }
    }

    public Map<String, Object> loadYamlFromClasspath(String path) throws IOException {

        Resource resource = new ClassPathResource(path);
        if (!resource.exists()) throw new FileNotFoundException("YAML not found: " + path);

        YamlPropertySourceLoader loader = new YamlPropertySourceLoader();
        List<PropertySource<?>> propertySourceList = loader.load("yaml:" + path, resource);

        Map<String, Object> propertyMap = new LinkedHashMap<>();
        for (PropertySource<?> ppropertySource : propertySourceList) {
            MapPropertySource mapPropertySource = (MapPropertySource) ppropertySource;
            for (String name : mapPropertySource.getPropertyNames()) {
                propertyMap.put(name, mapPropertySource.getProperty(name));
            }
        }

        return propertyMap;
    }
}