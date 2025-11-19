package hu.uni_obuda.thesis.railways.data.raildatacollector.util.resource;

import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class DefaultGraphQlVariablesTest {

    @Test
    void constructor_nullSource_returnsEmptyUnmodifiableMap() {
        DefaultGraphQlVariables vars = new DefaultGraphQlVariables(null);

        Map<String, Object> map = vars.asMap();
        assertNotNull(map);
        assertTrue(map.isEmpty());

        assertThrows(UnsupportedOperationException.class, () -> map.put("a", "b"));
    }

    @Test
    void constructor_emptySource_returnsEmptyUnmodifiableMap() {
        DefaultGraphQlVariables vars = new DefaultGraphQlVariables(Map.of());

        Map<String, Object> map = vars.asMap();
        assertNotNull(map);
        assertTrue(map.isEmpty());

        assertThrows(UnsupportedOperationException.class, () -> map.put("a", "b"));
    }

    @Test
    void constructor_simpleMap_keysConvertedToStringsAndValuesPreserved() {
        Map<String, Object> source = new LinkedHashMap<>();
        source.put("one", 1);
        source.put("two", 2);
        Object custom = new Object();
        source.put("x", custom);
        source.put("nullValue", null);

        DefaultGraphQlVariables vars = new DefaultGraphQlVariables(source);
        Map<String, Object> map = vars.asMap();

        assertEquals(4, map.size());
        assertTrue(map.containsKey("one"));
        assertTrue(map.containsKey("two"));
        assertTrue(map.containsKey("x"));
        assertTrue(map.containsKey("nullValue"));

        assertEquals(1, map.get("one"));
        assertEquals(2, map.get("two"));
        assertSame(custom, map.get("x"));
        assertNull(map.get("nullValue"));

        assertThrows(UnsupportedOperationException.class, () -> map.put("new", "value"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void constructor_nestedMap_list_set_andArrays_deeplyUnmodifiable() {
        Map<String, Object> innerMap = new LinkedHashMap<>();
        innerMap.put("innerKey", "innerValue");

        List<Object> innerList = new ArrayList<>();
        innerList.add("listValue");
        innerList.add(42);

        Set<Object> innerSet = new LinkedHashSet<>();
        innerSet.add("setValue1");
        innerSet.add("setValue2");

        String[] stringArray = new String[]{"a", "b"};
        int[] intArray = new int[]{1, 2, 3};

        Map<String, Object> source = new LinkedHashMap<>();
        source.put("map", innerMap);
        source.put("list", innerList);
        source.put("set", innerSet);
        source.put("stringArray", stringArray);
        source.put("intArray", intArray);

        DefaultGraphQlVariables vars = new DefaultGraphQlVariables(source);
        Map<String, Object> frozen = vars.asMap();

        assertEquals(5, frozen.size());

        Object frozenMapObj = frozen.get("map");
        assertInstanceOf(Map.class, frozenMapObj);
        Map<String, Object> frozenMap = (Map<String, Object>) frozenMapObj;
        assertEquals("innerValue", frozenMap.get("innerKey"));
        assertThrows(UnsupportedOperationException.class, () -> frozenMap.put("newKey", "newValue"));

        Object frozenListObj = frozen.get("list");
        assertInstanceOf(List.class, frozenListObj);
        List<Object> frozenList = (List<Object>) frozenListObj;
        assertEquals(2, frozenList.size());
        assertEquals("listValue", frozenList.get(0));
        assertEquals(42, frozenList.get(1));
        assertThrows(UnsupportedOperationException.class, () -> frozenList.add("x"));

        Object frozenSetObj = frozen.get("set");
        assertInstanceOf(Set.class, frozenSetObj);
        Set<Object> frozenSet = (Set<Object>) frozenSetObj;
        assertEquals(2, frozenSet.size());
        assertTrue(frozenSet.contains("setValue1"));
        assertTrue(frozenSet.contains("setValue2"));
        assertThrows(UnsupportedOperationException.class, () -> frozenSet.add("x"));

        Object frozenStringArrayObj = frozen.get("stringArray");
        assertInstanceOf(List.class, frozenStringArrayObj);
        List<String> frozenStringList = (List<String>) frozenStringArrayObj;
        assertEquals(2, frozenStringList.size());
        assertEquals("a", frozenStringList.get(0));
        assertEquals("b", frozenStringList.get(1));
        assertThrows(UnsupportedOperationException.class, () -> frozenStringList.add("c"));

        Object frozenIntArrayObj = frozen.get("intArray");
        assertInstanceOf(List.class, frozenIntArrayObj);
        List<Integer> frozenIntList = (List<Integer>) frozenIntArrayObj;
        assertEquals(3, frozenIntList.size());
        assertEquals(1, frozenIntList.get(0));
        assertEquals(2, frozenIntList.get(1));
        assertEquals(3, frozenIntList.get(2));
        assertThrows(UnsupportedOperationException.class, () -> frozenIntList.add(4));
    }

    @Test
    void asMap_returnsSameInstanceOnSubsequentCalls() {
        Map<String, Object> source = Map.of("k", "v");
        DefaultGraphQlVariables vars = new DefaultGraphQlVariables(source);

        Map<String, Object> first = vars.asMap();
        Map<String, Object> second = vars.asMap();

        assertSame(first, second);
    }
}
