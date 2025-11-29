package hu.uni_obuda.thesis.railways.data.raildatacollector.util.resource;

import org.junit.jupiter.api.Test;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

 class YamlGraphQlVariableLoaderTest {

    private static final String DIRECTORY = "graphql/emma/default-variables";
    private static final String EXISTING_DOCUMENT = "defaultVariables";
    private static final String EXISTING_PATH = DIRECTORY + "/" + EXISTING_DOCUMENT + ".yml";
    private static final String EMPTY_PATH = DIRECTORY + "/empty.yml";

    @Test
    void loadYamlFromClasspath_existingYaml_returnsPopulatedMap() throws IOException {
        YamlGraphQlVariableLoader loader = new YamlGraphQlVariableLoader(DIRECTORY);

        Map<String, Object> yaml = loader.loadYamlFromClasspath(EXISTING_PATH);

        assertNotNull(yaml);
        assertFalse(yaml.isEmpty());

        assertTrue(yaml.containsKey("arriveBy"));
        assertEquals(Boolean.FALSE, yaml.get("arriveBy"));

        assertTrue(yaml.containsKey("banned"));
        Object banned = yaml.get("banned");
        assertInstanceOf(Map.class, banned);
        assertTrue(((Map<?, ?>) banned).isEmpty());

        assertEquals(500, ((Number) yaml.get("numItineraries")).intValue());
        assertEquals("03:00", yaml.get("time"));

        Object walkSpeedObj = yaml.get("walkSpeed");
        assertInstanceOf(Number.class, walkSpeedObj);
        assertEquals(1.3888888888888888d, ((Number) walkSpeedObj).doubleValue(), 1e-12);

        assertEquals(0, ((Number) yaml.get("minTransferTime")).intValue());
        assertEquals("ERTEKESITESI_CSATORNA#INTERNET", yaml.get("distributionChannel"));
        assertEquals("ERTEKESITESI_ALCSATORNA#EMMA", yaml.get("distributionSubChannel"));
        assertEquals(86400, ((Number) yaml.get("searchWindow")).intValue());

        Object modesObj = yaml.get("modes");
        assertInstanceOf(List.class, modesObj);
        List<?> modes = (List<?>) modesObj;
        assertFalse(modes.isEmpty());
        Object firstMode = modes.getFirst();
        assertInstanceOf(Map.class, firstMode);
        Map<?, ?> firstModeMap = (Map<?, ?>) firstMode;
        assertEquals("RAIL", firstModeMap.get("mode"));

        Object searchParamsObj = yaml.get("searchParameters");
        assertInstanceOf(List.class, searchParamsObj);
        List<?> searchParams = (List<?>) searchParamsObj;
        assertEquals(2, searchParams.size());
        assertEquals("searchWindow=1440", searchParams.get(0));
        assertEquals("maxItineraries=200", searchParams.get(1));
    }

    @Test
    void loadYamlFromClasspath_emptyYaml_returnsEmptyMap() throws IOException {
        YamlGraphQlVariableLoader loader = new YamlGraphQlVariableLoader(DIRECTORY);

        Map<String, Object> yaml = loader.loadYamlFromClasspath(EMPTY_PATH);

        assertNotNull(yaml);
        assertTrue(yaml.isEmpty());
    }

    @Test
    void loadYamlFromClasspath_missingYaml_throwsFileNotFoundException() {
        YamlGraphQlVariableLoader loader = new YamlGraphQlVariableLoader(DIRECTORY);

        assertThrows(FileNotFoundException.class,
                () -> loader.loadYamlFromClasspath(DIRECTORY + "/doesNotExist.yml"));
    }

    @Test
    void loadForDocument_existingDocument_returnsDefaultGraphQlVariablesWithFrozenMap() {
        YamlGraphQlVariableLoader loader = new YamlGraphQlVariableLoader(DIRECTORY);

        DefaultGraphQlVariables vars = loader.loadForDocument(EXISTING_DOCUMENT);

        assertNotNull(vars);

        Map<String, Object> map = vars.asMap();
        assertNotNull(map);
        assertFalse(map.isEmpty());
        assertTrue(map.containsKey("arriveBy"));
        assertEquals(Boolean.FALSE, map.get("arriveBy"));

        assertThrows(UnsupportedOperationException.class, () -> map.put("newKey", "value"));

        Object modesObj = map.get("modes");
        assertInstanceOf(List.class, modesObj);
        List<String> modes = (List<String>) modesObj;
        assertFalse(modes.isEmpty());
        assertThrows(UnsupportedOperationException.class, () -> modes.add("X"));
    }

    @Test
    void loadForDocument_missingDocument_throwsRuntimeExceptionWrappingIOException() {
        YamlGraphQlVariableLoader loader = new YamlGraphQlVariableLoader(DIRECTORY);

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> loader.loadForDocument("nonExistingDocument"));

        assertNotNull(ex.getCause());
        assertInstanceOf(IOException.class, ex.getCause());
    }
}