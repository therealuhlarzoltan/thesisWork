package hu.uni_obuda.thesis.railways.route.routeplannerservice.health;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.actuate.endpoint.ApiVersion;
import org.springframework.boot.actuate.health.*;
import org.springframework.lang.NonNull;
import reactor.core.publisher.Mono;

import java.lang.reflect.Constructor;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

@Slf4j
public sealed abstract class DegradedHealthStatusProcessor<E> implements BeanPostProcessor
        permits DegradedHealthEndpointProcessor, DegradedHealthEndpointExtensionProcessor, DegradedReactiveHealthEndpointExtensionProcessor {

    protected static final Status DEGRADED = new Status("DEGRADED");

    protected final Class<E> healthEndpointClass;

    public DegradedHealthStatusProcessor(Class<E> healthEndpointClass) {
        this.healthEndpointClass = healthEndpointClass;
    }

    @Override
    public Object postProcessAfterInitialization(@NonNull Object bean, @NonNull String beanName) throws BeansException {
        if (healthEndpointClass.equals(bean.getClass())) {
            log.info("Bean with {} and name {} is a candidate to post processing, processing it...", bean.getClass(), beanName);
            return performPostProcessing(bean, beanName);
        }
        return bean;
    }

    protected abstract Object performPostProcessing(@NonNull Object bean, @NonNull String beanName);

    protected static Object originalHealthDescriptorOrNewSystemHealth(Object originalResult) {
        if (originalResult instanceof SystemHealth systemHealth) {
            if (!systemHealth.getStatus().equals(Status.UP)) {
                return systemHealth;
            }
            boolean isDown = systemHealth.getStatus().equals(Status.DOWN);
            boolean isDegraded = !isDown && isSystemDegraded(systemHealth.getComponents().values());
            return createSystemHealth(ApiVersion.LATEST, isDegraded ? DEGRADED : systemHealth.getStatus(), systemHealth.getComponents(), systemHealth.getGroups());
        }
        return originalResult;
    }

    protected static Mono<?> originalHealthDescriptorOrNewSystemHealth(Mono<?> originalResult) {
        return originalResult.map(result -> {
            if (result instanceof SystemHealth systemHealth) {
                if (!systemHealth.getStatus().equals(Status.UP)) {
                    return systemHealth;
                }
                boolean isDown = systemHealth.getStatus().equals(Status.DOWN);
                boolean isDegraded = !isDown && isSystemDegraded(systemHealth.getComponents().values());
                return createSystemHealth(ApiVersion.LATEST, isDegraded ? DEGRADED : systemHealth.getStatus(), systemHealth.getComponents(), systemHealth.getGroups());
            }
            return result;
        });
    }

    protected static boolean isSystemDegraded(Collection<HealthComponent> components) {
        for (var component : components) {
            if (component.getStatus().equals(DEGRADED) || isSubSystemOutOfService(component)) {
                return true;
            }
        }
        return false;
    }

    protected static boolean isSubSystemOutOfService(HealthComponent component) {
        if (component instanceof Health health) {
            return health.getStatus().equals(Status.OUT_OF_SERVICE);
        }
        if (component instanceof SystemHealth systemHealth) {
            return systemHealth.getStatus().equals(Status.OUT_OF_SERVICE);
        }
        if (component instanceof CompositeHealth compositeHealth) {
            for (var c : compositeHealth.getComponents().values()) {
                if (c.getStatus().equals(Status.OUT_OF_SERVICE)) {
                    return true;
                }
            }
            return false;
        }
        return false;
    }

    protected static SystemHealth createSystemHealth(ApiVersion apiVersion, Status status, Map<String, HealthComponent> components, Set<String> group) {
        try {
            Constructor<SystemHealth> constructor = SystemHealth.class.getDeclaredConstructor(ApiVersion.class, Status.class, Map.class, Set.class);
            constructor.setAccessible(true);
            return constructor.newInstance(apiVersion, status, components, group);
        } catch (ReflectiveOperationException e) {
            log.error("A ReflectiveOperationException occurred while creating a new SystemHealth instance", e);
            throw new RuntimeException(e);
        } catch (RuntimeException e) {
            log.error("A RuntimeException occurred while creating a new SystemHealth instance", e);
            throw e;
        }
    }
}