package hu.uni_obuda.thesis.railways.data.raildatacollector.util.hash;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class UUIDHashFunctionTest {

    @Test
    void apply_nullUuid_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> UUIDHashFunction.apply(null));
    }

    @Test
     void apply_sameUuidTwice_sameResult() {
        UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");

        Integer first = UUIDHashFunction.apply(uuid);
        Integer second = UUIDHashFunction.apply(uuid);

        assertNotNull(first);
        assertNotNull(second);
        assertEquals(first, second);
    }

    @Test
     void apply_differentUuids_differentResults() {
        UUID uuid1 = new UUID(0L, 0L);
        UUID uuid2 = new UUID(1L, 0L);

        Integer hash1 = UUIDHashFunction.apply(uuid1);
        Integer hash2 = UUIDHashFunction.apply(uuid2);

        assertNotNull(hash1);
        assertNotNull(hash2);
        assertNotEquals(hash1, hash2);
    }

    @Test
     void apply_multipleUuids_resultsArePositiveAndNonZero() {
        Set<Integer> results = new HashSet<>();

        for (int i = 0; i < 100; i++) {
            UUID uuid = new UUID(i, i * 17L);
            Integer hash = UUIDHashFunction.apply(uuid);
            assertNotNull(hash);
            assertTrue(hash > 0);
            results.add(hash);
        }

        assertTrue(results.size() > 1);
    }

    @Test
     void constructor_privateConstructorAccessibleViaReflection_instanceCreated() throws Exception {
        Constructor<UUIDHashFunction> constructor = UUIDHashFunction.class.getDeclaredConstructor();
        assertTrue(Modifier.isPrivate(constructor.getModifiers()));

        constructor.setAccessible(true);
        UUIDHashFunction instance = constructor.newInstance();
        assertNotNull(instance);
    }
}
