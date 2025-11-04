package hu.uni_obuda.thesis.railways.data.raildatacollector.util.resource;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.config.YamlMapFactoryBean;
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
            return new DefaultGraphQlVariables(loadYamlFromClasspath(classPathDirectory + "/" + documentName + ".yml"));
        } catch (IOException e) {
            log.error("An IO Exception occurred: {}", e.getMessage());
            throw new RuntimeException(e);
        }
    }

    public Map<String, Object> loadYamlFromClasspath(String path) throws IOException {

        Resource resource = new ClassPathResource(path);
        if (!resource.exists()) {
            throw new FileNotFoundException("YAML not found: " + path);
        }

        YamlMapFactoryBean yaml = new YamlMapFactoryBean();
        yaml.setResources(resource);
        Map<String, Object> propertyMap = yaml.getObject();

        return propertyMap != null ? propertyMap : Map.of();
    }
}