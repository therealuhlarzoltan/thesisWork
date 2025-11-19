package hu.uni_obuda.thesis.railways.data.raildatacollector.util.resource;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CachingYamlGraphQlVariableLoaderTest {

    @Mock
    private YamlGraphQlVariableLoader delegate;

    private CachingYamlGraphQlVariableLoader cachingLoader;

    @BeforeEach
    void setUp() {
        cachingLoader = new CachingYamlGraphQlVariableLoader(delegate);
    }

    @Test
    void loadForDocument_singleCall_delegatesAndCaches() {
        DefaultGraphQlVariables vars = new DefaultGraphQlVariables(Map.of("k", "v"));
        when(delegate.loadForDocument("doc1")).thenReturn(vars);

        DefaultGraphQlVariables first = cachingLoader.loadForDocument("doc1");
        DefaultGraphQlVariables second = cachingLoader.loadForDocument("doc1");

        assertNotNull(first);
        assertSame(first, second);
        verify(delegate, times(1)).loadForDocument("doc1");
    }

    @Test
    void loadForDocument_concurrentCallsSameKey_singleDelegateInvocationAndSameInstance() throws InterruptedException {
        DefaultGraphQlVariables vars = new DefaultGraphQlVariables(Map.of("k", "v"));
        when(delegate.loadForDocument("concurrentDoc")).thenAnswer(invocation -> {
            Thread.sleep(50);
            return vars;
        });

        int threadCount = 20;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);
        List<DefaultGraphQlVariables> results = Collections.synchronizedList(new ArrayList<>());

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await();
                    DefaultGraphQlVariables v = cachingLoader.loadForDocument("concurrentDoc");
                    results.add(v);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        doneLatch.await(5, TimeUnit.SECONDS);
        executor.shutdownNow();

        assertEquals(threadCount, results.size());
        DefaultGraphQlVariables first = results.get(0);
        for (DefaultGraphQlVariables v : results) {
            assertSame(first, v);
        }
        verify(delegate, times(1)).loadForDocument("concurrentDoc");
    }

    @Test
    void loadForDocument_concurrentDifferentKeys_eachKeyCachedIndependently() throws InterruptedException {
        when(delegate.loadForDocument(anyString())).thenAnswer(invocation -> {
            String key = invocation.getArgument(0);
            return new DefaultGraphQlVariables(Map.of("key", key));
        });

        List<String> keys = List.of("a", "b", "c");
        int repetitionsPerKey = 10;
        ExecutorService executor = Executors.newFixedThreadPool(9);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(keys.size() * repetitionsPerKey);
        List<DefaultGraphQlVariables> resultsA = Collections.synchronizedList(new ArrayList<>());
        List<DefaultGraphQlVariables> resultsB = Collections.synchronizedList(new ArrayList<>());
        List<DefaultGraphQlVariables> resultsC = Collections.synchronizedList(new ArrayList<>());

        for (String key : keys) {
            for (int i = 0; i < repetitionsPerKey; i++) {
                executor.submit(() -> {
                    try {
                        startLatch.await();
                        DefaultGraphQlVariables v = cachingLoader.loadForDocument(key);
                        switch (key) {
                            case "a" -> resultsA.add(v);
                            case "b" -> resultsB.add(v);
                            case "c" -> resultsC.add(v);
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } finally {
                        doneLatch.countDown();
                    }
                });
            }
        }

        startLatch.countDown();
        doneLatch.await(5, TimeUnit.SECONDS);
        executor.shutdownNow();

        assertEquals(repetitionsPerKey, resultsA.size());
        assertEquals(repetitionsPerKey, resultsB.size());
        assertEquals(repetitionsPerKey, resultsC.size());

        DefaultGraphQlVariables aFirst = resultsA.get(0);
        for (DefaultGraphQlVariables v : resultsA) assertSame(aFirst, v);

        DefaultGraphQlVariables bFirst = resultsB.get(0);
        for (DefaultGraphQlVariables v : resultsB) assertSame(bFirst, v);

        DefaultGraphQlVariables cFirst = resultsC.get(0);
        for (DefaultGraphQlVariables v : resultsC) assertSame(cFirst, v);

        verify(delegate, times(1)).loadForDocument("a");
        verify(delegate, times(1)).loadForDocument("b");
        verify(delegate, times(1)).loadForDocument("c");
    }

    @Test
    void loadForDocument_delegateReturnsNull_throwsIllegalStateException() {
        when(delegate.loadForDocument("nullDoc")).thenReturn(null);

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> cachingLoader.loadForDocument("nullDoc"));

        assertTrue(ex.getMessage().contains("returned null"));
        verify(delegate, times(1)).loadForDocument("nullDoc");
    }
}
